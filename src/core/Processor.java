package core;

import gui.StartWindow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.MulticastReceiver;
import net.MulticastSender;
import utils.ChunkManager;

public class Processor {

	public static final float version = (float) 1.0;
	private StartWindow gui;
	private MulticastReceiver mcReceiver;
	private MulticastReceiver mdbReceiver;
	private MulticastReceiver mdrReceiver;
	private MulticastSender mcSender;
	private MulticastSender mdbSender;
	private MulticastSender mdrSender;
	private HashMap<String, Chunk> chunks = new HashMap<String, Chunk>(); // fileId+ChuckNo em hex
	private HashMap<String, String> myFiles = new HashMap<String, String>(); // fileId -> Talvez tenhamos que
	// verificar se a data de
	// alteracao mudou!
	private HashMap<String, Long> nrChunksByFile = new HashMap<String, Long>();
	private ConcurrentLinkedQueue<Message> messageQueue;
	private ConcurrentLinkedQueue<Chunk> waitingChunks;
	private ConcurrentLinkedQueue<Message> outgoingQueue;
	private HashMap<String, String> filesToBeRestored = new HashMap<String, String>();// -> string is new name

	private long usedSpace = 0;

	public Processor(final String[] args) {
		messageQueue = new ConcurrentLinkedQueue<>();
		waitingChunks = new ConcurrentLinkedQueue<>();
		outgoingQueue = new ConcurrentLinkedQueue<>();
		mcReceiver = new MulticastReceiver(args[0], Integer.parseInt(args[1]),
				this);
		mdbReceiver = new MulticastReceiver(args[2], Integer.parseInt(args[3]),
				this);
		mdrReceiver = new MulticastReceiver(args[4], Integer.parseInt(args[5]),
				this);
		mcSender = new MulticastSender(args[0], Integer.parseInt(args[1]));
		mdbSender = new MulticastSender(args[2], Integer.parseInt(args[3]));
		mdrSender = new MulticastSender(args[4], Integer.parseInt(args[5]));
	}

	public void addGui(StartWindow newGui) {
		gui = newGui;
	}

	public void newInputMessage(Message message) {
		messageQueue.add(message);
	}

	public void process() {
		mcReceiver.start();
		mdbReceiver.start();
		mdrReceiver.start();
		mcSender.start();
		mdbSender.start();
		mdrSender.start();
		while (true) {
			Message msg = messageQueue.poll();

			if (msg != null) { // empty queue

				switch (msg.getMessageType()) {
				case "PUTCHUNK": {
					processPutChunk(msg);
					break;
				}
				case "STORED": {
					processStored(msg);
					break;
				}
				case "GETCHUNK": {
					processGetChunk(msg);
					break;
				}
				case "CHUNK": {
					processChunk(msg);
					break;
				}
				case "DELETE": {
					processDelete(msg);
					break;
				}
				case "REMOVED": {
					processRemoved(msg);
					break;
				}
				}

			}
			
			Message out = outgoingQueue.poll();
			if (out != null) { // empty queue
				gui.log("Sent " + out.toString());

				switch (out.getMessageType()) {
				case "PUTCHUNK": {
					mdbSender.send(out);
					break;
				}
				case "GETCHUNK": {
					mcSender.send(out);
					break;
				}
				case "DELETE": {
					mcSender.send(out);
					break;
				}
				case "REMOVED": {
					mcSender.send(out);
					break;
				}
				case "STORED": {
					mcSender.send(out);
					break;
				}
				case "CHUNK": {
					mdrSender.send(out);
					break;
				}
				}

			}

			Chunk chk = waitingChunks.poll(); // user mandou guardar - esperam
			// confirmacoes
			if (chk != null) {
				if (chk.shouldResend()) {
					if(chk.timeOver())
						sendPutChunk(chk);
					waitingChunks.add(chk);
				}
			}
		}
	}

	private void processRemoved(Message msg) { // is kept in line till random
		// delay
		Chunk chk;

		synchronized (chunks) {
			chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()));
		}

		if (chk == null)
			return;

		if (msg.ready()) {
			chk.removeHostWithChunk(msg.getSenderIp());
			if(chk.getCounter()<chk.getReplicationDeg())
			{

				Message newMsg = new Message("PUTCHUNK", version, msg.getTextFileId());
				newMsg.setReplicationDeg(chk.getReplicationDeg());
				newMsg.setChunkNo(msg.getChunkNo());

				try {
					newMsg.setBody(chk.load());
				} catch (IOException e) {
					gui.log("Couldn't load chunk's " + chk.getChunkId() + " body");
					return;
				}

				outgoingQueue.add(newMsg);
			}
		} else
			messageQueue.add(msg);
	}

	private void processDelete(Message msg) {
		//TODO chunks VAZIO ??? :(
 		Iterator<String> it = chunks.keySet().iterator();
  		while (it.hasNext()) {
  			String hash = it.next();  			
  			if (hash.startsWith(msg.getTextFileId())) {
  				System.out.println(waitingChunks.remove(hash));
  				it.remove();
				usedSpace -= ChunkManager.deleteChunks("./", hash);
  			}
  		}

		gui.setUsedSpace(usedSpace);
	}

	private void processChunk(Message msg) { // if chunk is received, kills
		// waiting getchunk message
		Chunk chk;

		synchronized (chunks) {
			chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo())); //TODO
		}

		if (chk == null)
			return;

		if (chk.isMine()) {
			// write to disk -> if all chunks are present, merge file with
			// name in filesToBeRestored
			String filename = chk.getChunkId();

			if(ChunkManager.countChunks("./", filename) == nrChunksByFile.get(chk.getTextFileId())) {
				try {
					ChunkManager.mergeChunks("./", filename, filesToBeRestored.get(filename));
				} catch (IOException e) {
					gui.log("Couldn't restore " + filename);
				}
			}
		} else { // Remove get chunk msg if in queue
			for (Message m : messageQueue) {
				if (m.getMessageType().equals("CHUNK")
						&& Chunk.getChunkId(m.getTextFileId(), m.getChunkNo()).equals(chk.getChunkId())) {
					messageQueue.remove(m);
					break;
				}
			}
		}

		gui.setUsedSpace(usedSpace);
	}

	private void processGetChunk(Message msg) { // is kept in line till random
		// delay
		if (msg.ready()) {
			Chunk chk; 

			synchronized (chunks) {
				chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()));
			}

			if (chk == null)
				return;

			Message newMsg = new Message("CHUNK", version, msg.getTextFileId());
			newMsg.setChunkNo(msg.getChunkNo());

			try {
				newMsg.setBody(chk.load());
			} catch (IOException e) {
				gui.log("processGetChunk couldn't load chunk's " + chk.getChunkId() + " body");
				return;
			}

			outgoingQueue.add(newMsg);
		} else
			messageQueue.add(msg);
	}

	private void sendPutChunk(Chunk chk) {
		Message msg = new Message("PUTCHUNK", version, chk.getTextFileId());
		msg.setChunkNo(chk.getChunkNo());
		msg.setReplicationDeg(chk.getReplicationDeg());

		try {
			msg.setBody(chk.load());
		} catch (IOException e) {
			gui.log("sendPutChunk couldn't load chunk's " + chk.getChunkId() + " body, skipping this save");
			return;
		}

		outgoingQueue.add(msg);
		chk.notifySent();
	}

	private void processStored(Message msg) {

		gui.log("processStored Received " + msg.toString());
		Chunk c;
		synchronized (chunks) {
			c=chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()));
		}
		if (c != null)
			c.addHostWithChunk(msg.getSenderIp());
	}

	private void processPutChunk(Message msg) { // is kept in line till random
		// delay

		synchronized (myFiles) {
			if(myFiles.containsKey(msg.getTextFileId()))
				return;
		}
		

		if (msg.ready()) {
			Message newMsg = new Message("STORED", version, msg.getTextFileId());
			newMsg.setChunkNo(msg.getChunkNo());
			

			if (usedSpace + msg.getBody().length <= gui.getMaxUsedSpace()) {
				Chunk chunk = new Chunk(msg.getTextFileId(), msg.getChunkNo(),
						msg.getReplicationDeg(), msg.getSenderIp());

				try {
					chunk.save(msg.getBody());
				} catch (IOException e) {
					gui.log("Couldn't write " + chunk.getChunkId() + " to disk");

					return;
				}

				usedSpace += msg.getBody().length;
				synchronized (chunks) {
					chunks.put(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()),chunk);
				}
				outgoingQueue.add(newMsg);
			}

			gui.log("processPutChunk Received " + msg.toString());
		} else {
			messageQueue.add(msg);
		}
	}

	public void removeFile(String fileName) {
		String fileId = null;
		
		for (Entry<String, String> pair : myFiles.entrySet()) {
			if (pair.getValue().equals(fileName)) {
				fileId = pair.getKey();
				break;
			}
		}
		// chunks will be deleted when message is received
		if (fileId == null)
		{
			gui.log("File to be removed not found");
			return;
		}
		myFiles.remove(fileId);
		Message message = new Message("DELETE", version, fileId);
		outgoingQueue.add(message);
		notifyGuiFileChange();
	}

	public void addFile(String fileName, int repDeg) {
		//  break in chunks, add fileID to myfiles, add chunks to hash
		byte[] sha = new byte[32];
		long nrChunks = 0;

		try {
			nrChunks = ChunkManager.createChunks(fileName, "./", sha);
		} catch (IOException e) {
			gui.log("Couldn't create chunks for " + fileName);

			return;
		}
		
		String fileId = Message.bytesToHex(sha);

		gui.log("Created " + nrChunks + " chunks for " + fileName);
		nrChunksByFile.put(fileId, Long.valueOf(nrChunks));

		synchronized (myFiles) {
			myFiles.put(fileId, fileName);
		}

		for(int i = 0; i < nrChunks; i++) {
			Chunk chunk = new Chunk(fileId, i, repDeg, "", true);
			waitingChunks.add(chunk);
		}

		notifyGuiFileChange();
	}

	public void setSpaceLimit(int mbLimit) { //TODO find chunks with high rep degree
		/*// if necessary add REMOVED msgs to outgoingqueue
		if(usedSpace <= mbLimit*1000000)
			return;

		String[] results = new String[2];
		usedSpace -= ChunkManager.deleteFirstChunk("./Chunks", results);

		Message message = new Message("REMOVED", version, results[0], Integer.parseInt(results[1]));
		outgoingQueue.add(message);

		setSpaceLimit(mbLimit);*/
	}

	public void restoreFile(String fileName, String newLocation) {
		gui.log("Trying to restore " + fileName + " to " + newLocation);
		// TODO find chunks in hash, add CHUNK messages to outgoingqueue
		// TODO add new name to filestoberestored hashmap
	}

	private void notifyGuiFileChange() {
		synchronized (myFiles) {
			gui.replaceFileList(myFiles.values().toArray());
		}
	}

}
