package core;

import gui.StartWindow;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.MulticastReceiver;
import net.MulticastSender;
import utils.ChunkManager;

//TODO save app state each minute(?)

public class Processor {

	public static final float version = (float) 1.0;
	private StartWindow gui;
	private MulticastReceiver mcReceiver;
	private MulticastReceiver mdbReceiver;
	private MulticastReceiver mdrReceiver;
	private MulticastSender mcSender;
	private MulticastSender mdbSender;
	private MulticastSender mdrSender;
	private ConcurrentHashMap<String, Chunk> chunks; // fileId+ChuckNo
	private ConcurrentHashMap<String, String> myFiles; // fileId, filename
	private ConcurrentHashMap<String, Long> nrChunksByFile; // fileId, nr
	private ConcurrentHashMap<String, String> filesToBeRestored;// fileid hash,newpath
	private ConcurrentLinkedQueue<Message> messageQueue;
	private ConcurrentLinkedQueue<Chunk> waitingChunks;
	private ConcurrentLinkedQueue<Message> outgoingQueue;

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

		chunks = new ConcurrentHashMap<String, Chunk>();
		myFiles = new ConcurrentHashMap<String, String>();
		nrChunksByFile = new ConcurrentHashMap<String, Long>();
		filesToBeRestored = new ConcurrentHashMap<String, String>();
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

			Chunk chk = waitingChunks.poll(); // waiting for store confirmation
			if (chk != null) {
				if (chk.shouldResend()) {
					if (chk.timeOver())
						sendPutChunk(chk);
					waitingChunks.add(chk);
				}
			}
		}
	}

	private void processRemoved(Message msg) { 
		Chunk chk;

		synchronized (chunks) {
			chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(),
					msg.getChunkNo()));
		}

		if (chk == null)
			return;

		if (msg.ready()) {
			chk.removeHostWithChunk(msg.getSenderIp());
			if (chk.getCounter() < chk.getReplicationDeg()) {

				Message newMsg = new Message("PUTCHUNK", version,
						msg.getTextFileId());
				newMsg.setReplicationDeg(chk.getReplicationDeg());
				newMsg.setChunkNo(msg.getChunkNo());

				try {
					newMsg.setBody(chk.load());
				} catch (IOException e) {
					gui.log("Couldn't load chunk's " + chk.getChunkId()
							+ " body");
					return;
				}

				outgoingQueue.add(newMsg);
			}
		} else
			messageQueue.add(msg);
	}

	private void processDelete(Message msg) {
		Iterator<Chunk> it = chunks.values().iterator();
		while (it.hasNext()) {
			Chunk chunk = it.next();
			if (chunk.getTextFileId().equals(msg.getTextFileId())) {
				waitingChunks.remove(chunk);
				it.remove();
			}
		}

		usedSpace -= ChunkManager.deleteChunks("./", msg.getTextFileId());
		gui.setUsedSpace(usedSpace);
	}

	private void processChunk(Message msg) { 
		// if chunk is received, kills
		// waiting getchunk message
		Chunk chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()));

		if (chk == null)
			return;

		if (chk.isMine()) {
			// write to disk -> if all chunks are present, merge file with
			// name in filesToBeRestored
			String filename = chk.getTextFileId();
			long nrChunks = nrChunksByFile.get(filename);
			String newName = filesToBeRestored.get(filename);
			if (ChunkManager.countChunks("./", filename) == nrChunks
					&& newName != null) {
				String fileRealName = myFiles.get(filename);
				fileRealName = fileRealName.substring(fileRealName
						.lastIndexOf('/') + 1);
				try {
					ChunkManager.mergeChunks("./", filename, newName,
							fileRealName);
					gui.log("File " + fileRealName + " restored.");
				} catch (IOException e) {
					gui.log("Couldn't restore " + fileRealName);
				}
				filesToBeRestored.remove(filename);
			}

		} else { // Remove get chunk msg if in queue
			for (Message m : messageQueue) {
				if (m.getMessageType().equals("CHUNK")
						&& Chunk.getChunkId(m.getTextFileId(), m.getChunkNo())
								.equals(chk.getChunkId())) {
					messageQueue.remove(m);
					break;
				}
			}
		}
	}

	private void processGetChunk(Message msg) { 
		if (msg.ready()) {
			Chunk chk;

			chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(),
					msg.getChunkNo()));

			if (chk == null)
				return;
			if (chk.isMine())
				return;

			Message newMsg = new Message("CHUNK", version, msg.getTextFileId());
			newMsg.setChunkNo(msg.getChunkNo());

			try {
				newMsg.setBody(chk.load());
			} catch (IOException e) {
				gui.log("processGetChunk couldn't load chunk's "
						+ chk.getChunkId() + " body");
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
			gui.log("sendPutChunk couldn't load chunk's " + chk.getChunkId()
					+ " body, skipping this save");
			return;
		}

		outgoingQueue.add(msg);
		chk.notifySent();
	}

	private void processStored(Message msg) {

		gui.log("processStored Received " + msg.toString());
		Chunk c;
		c = chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()));

		if (c != null)
			if (!c.isMine())
				c.addHostWithChunk(msg.getSenderIp());
	}

	private void processPutChunk(Message msg) {

		if (myFiles.containsKey(msg.getTextFileId()))
			return;

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
				chunks.put(
						Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()),
						chunk);

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

			// chunks will be deleted when message is received
			// no need to do it here
			
			
			if (fileId == null) {
				gui.log("File to be removed not found");
				return;
			}
			myFiles.remove(fileId);
		}
		Message message = new Message("DELETE", version, fileId);
		outgoingQueue.add(message);
		notifyGuiFileChange();
	}

	public void addFile(String fileName, int repDeg) {
		// break in chunks, add fileID to myfiles, add chunks to hash
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

		myFiles.put(fileId, fileName);

		for (int i = 0; i < nrChunks; i++) {
			Chunk chunk = new Chunk(fileId, i, repDeg, "", true);
			chunks.put(chunk.getChunkId(), chunk);
			waitingChunks.add(chunk);
		}

		notifyGuiFileChange();
	}

	public void setSpaceLimit(int mbLimit) {
		
		if(usedSpace <= mbLimit*1000000)
			return;
		
		PriorityQueue<Chunk> orderedChunks = new PriorityQueue<Chunk>(chunks.size(), new Chunk.ChunkCompare());
		
		Iterator<Chunk> it = chunks.values().iterator();
		while (it.hasNext()) {
			Chunk chunk = it.next();
			orderedChunks.add(chunk);
		}
		
		while(usedSpace <= mbLimit*1000000){
			Chunk chk = orderedChunks.poll();
			gui.log("Removed chunk " + chk.getChunkId() + " repdeg " + chk.getReplicationDeg() + " currentrepdeg " + chk.getCounter());
			usedSpace -= ChunkManager.deleteChunk("./",chk.getTextFileId());
			chunks.remove(chk.getChunkId());
			Message message = new Message("REMOVED", version,chk.getTextFileId());
			message.setChunkNo(chk.getChunkNo());
			outgoingQueue.add(message);
			gui.setUsedSpace(usedSpace);
		}		
	}

	public void restoreFile(String fileName, String newLocation) {
		gui.log("Trying to restore " + fileName + " to " + newLocation);

		String fileId = null;
		for (Entry<String, String> pair : myFiles.entrySet()) {
			if (pair.getValue().equals(fileName)) {
				fileId = pair.getKey();
				break;
			}

		}

		if (fileId == null) {
			gui.log("The file " + fileName + " was not found in hash");
			return;
		}

		filesToBeRestored.put(fileId, newLocation);

		Iterator<Chunk> it = chunks.values().iterator();
		while (it.hasNext()) {
			Chunk chunk = it.next();
			if (chunk.getTextFileId().equals(fileId)) {
				Message msg = new Message("GETCHUNK", version,
						chunk.getTextFileId());
				msg.setChunkNo(chunk.getChunkNo());
				outgoingQueue.add(msg);
			}

		}

	}

	private void notifyGuiFileChange() {
		gui.replaceFileList(myFiles.values().toArray());

	}

}
