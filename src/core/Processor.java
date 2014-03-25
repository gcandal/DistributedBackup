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
				}
				case "STORED": {
					processStored(msg);
				}
				case "GETCHUNK": {
					processGetChunk(msg);
				}
				case "CHUNK": {
					processChunk(msg);
				}
				case "DELETE": {
					processDelete(msg);
				}
				case "REMOVED": {
					processRemoved(msg);
				}
				}

			}
			
			Message out = outgoingQueue.poll();
			if (out != null) { // empty queue
				gui.log("Sent " + out.toString());

				switch (out.getMessageType()) {
				case "PUTCHUNK": {
					mdbSender.send(out);
				}
				case "GETCHUNK": {
					mcSender.send(out);
				}
				case "DELETE": {
					mcSender.send(out);
				}
				case "REMOVED": {
					mcSender.send(out);
				}
				case "STORED": {
					mcSender.send(out);
				}
				case "CHUNK": {
					mdrSender.send(out);
				}
				}

			}

			Chunk chk = waitingChunks.poll(); // user mandou guardar - esperam
			// confirmacoes
			if (chk != null) {
				if (chk.shouldResend()) {
					sendPutChunk(chk);
				} else
					waitingChunks.add(chk);
			}
		}
	}

	private void processRemoved(Message msg) { // is kept in line till random
		// delay
		Chunk chk;

		synchronized (chunks) {
			chk = chunks
					.get(Chunk.getHash(msg.getFileId(), msg.getChunkNo()));
		}

		if (chk == null)
			return;

		if (msg.ready()) {
			chk.removeHostWithChunk(msg.getSenderIp());
			if(chk.getCounter()<chk.getReplicationDeg())
			{

				Message newMsg = new Message("PUTCHUNK", version, msg.getFileId());
				newMsg.setReplicationDeg(chk.getReplicationDeg());
				newMsg.setChunkNo(msg.getChunkNo());

				try {
					newMsg.setBody(chk.load());
				} catch (IOException e) {
					gui.log("Couldn't load chunk's " + Message.bytesToHex(chk.getFileId()) + chk.getChunkNo() + " body");
					//messageQueue.add(msg);

					return;
				}

				outgoingQueue.add(newMsg);
			}
		} else
			messageQueue.add(msg);
	}

	private void processDelete(Message msg) {


 		Iterator<String> it = chunks.keySet().iterator();
  		while (it.hasNext()) {
  			String hash = it.next();
 			boolean belongs = true;
  			for (int i = 0; i < 256; i++) {
  				if (hash.charAt(i) != msg.getTextFileId().charAt(i)) {
  					belongs = false;
  					break;
  				}
  			}
  
  			if (belongs) {
  				it.remove();
				usedSpace -= ChunkManager.deleteChunks("./", hash);
  			}
  		}

		gui.setUsedSpace(usedSpace);
	}

	/*
	private boolean equalByteArrays(byte[] first, byte[] second) {
		if(first.length != second.length)
			return false;

		for(int i = 0; i < first.length; i++)
			if(first[i] != second[i])
				return false;

		return true;
	}*/

	private void processChunk(Message msg) { // if chunk is received, kills
		// waiting getchunk message
		Chunk chk;

		synchronized (chunks) {
			chk = chunks
					.get(Chunk.getHash(msg.getFileId(), msg.getChunkNo())); //TODO
		}

		if (chk == null)
			return;

		if (chk.isMine()) {
			// write to disk -> if all chunks are present, merge file with
			// name in filesToBeRestored
			byte[] sha = chk.getHash();
			String filename = Message.bytesToHex(sha);

			if(ChunkManager.countChunks("./", filename) == nrChunksByFile.get(Message.bytesToHex(sha))) {
				try {
					ChunkManager.mergeChunks("./", filename, filesToBeRestored.get(filename));
				} catch (IOException e) {
					gui.log("Couldn't restore " + filename);
				}
			}
		} else { // Remove get chunk msg if in queue
			for (Message m : messageQueue) {
				if (m.getMessageType().equals("CHUNK")
						&& Chunk.getHash(m.getFileId(), m.getChunkNo()).equals(
								chk.getHash())) {
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
				chk = chunks.get(Message.bytesToHex(Chunk.getHash(msg.getFileId(),
						msg.getChunkNo())));
			}

			if (chk == null)
				return;

			Message newMsg = new Message("CHUNK", version, msg.getFileId());
			newMsg.setChunkNo(msg.getChunkNo());

			try {
				newMsg.setBody(chk.load());
			} catch (IOException e) {
				gui.log("processGetChunk couldn't load chunk's " + chk.getFileId() + chk.getChunkNo() + " body");
				return;
			}

			outgoingQueue.add(newMsg);
		} else
			messageQueue.add(msg);
	}

	private void sendPutChunk(Chunk chk) {
		Message msg = new Message("PUTCHUNK", version, chk.getFileId());
		msg.setChunkNo(chk.getChunkNo());
		msg.setReplicationDeg(chk.getReplicationDeg());

		try {
			msg.setBody(chk.load());
		} catch (IOException e) {
			gui.log("sendPutChunk couldn't load chunk's " + chk.getFileId() + chk.getChunkNo() + " body, skipping this save");

			return;
		}

		outgoingQueue.add(msg);
		chk.notifySent();
	}

	private void processStored(Message msg) {

		gui.log("processStored Received " + msg.toString());

		byte[] key = Chunk.getHash(msg.getFileId(), msg.getChunkNo());

		Chunk c;

		synchronized (chunks) {
			c =chunks.get(Message.bytesToHex(key));
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
			Message newMsg = new Message("STORED", version, msg.getFileId());
			newMsg.setChunkNo(msg.getChunkNo());
			

			if (usedSpace + msg.getBody().length <= gui.getMaxUsedSpace()) {
				Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(),
						msg.getReplicationDeg(), msg.getSenderIp());

				try {
					chunk.save(msg.getBody());
				} catch (IOException e) {
					gui.log("Couldn't write " + chunk.getFileId() + chunk.getChunkNo() + " to disk");

					return;
				}

				usedSpace += msg.getBody().length;
				synchronized (chunks) {
					chunks.put(Message.bytesToHex(chunk.getHash()), chunk);
				}
				outgoingQueue.add(newMsg);
			}

			gui.log("processPutChunk Received " + msg.toString());
		} else {
			messageQueue.add(msg);
		}
	}

	public void removeFile(String fileName) {

		//TODO
		// remove chunks, 
		
		
		// build message to be sent and put in outgoingQueue
		// delete from disk
		String fileId = null;


		for (Entry<String, String> pair : myFiles.entrySet()) {
			if (pair.getValue().equals(fileName)) {
				fileId = pair.getKey();
				break;
			}
		}
	
		if (fileId == null)
		{
			gui.log("File to be removed not found");
			myFiles.remove(fileId);
		}
		
		usedSpace -= ChunkManager.deleteChunks("./",fileId);

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

		gui.log("Created " + nrChunks + " chunks for " + fileName);
		nrChunksByFile.put(Message.bytesToHex(sha), Long.valueOf(nrChunks));

		synchronized (myFiles) {
			myFiles.put(Message.bytesToHex(sha), fileName);
		}

		for(int i = 0; i < nrChunks; i++) {
			Chunk chunk = new Chunk(sha, i, repDeg, "", true);
			sendPutChunk(chunk);
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
