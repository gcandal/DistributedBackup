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
import utils.StateKeeper;

public class Processor extends Thread{

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
	private StateKeeper state;

	private int[] sizes;

	public Processor(final String[] args,StartWindow newGui) {
		messageQueue = new ConcurrentLinkedQueue<>();
		waitingChunks = new ConcurrentLinkedQueue<>();
		outgoingQueue = new ConcurrentLinkedQueue<>();
		mcReceiver = new MulticastReceiver(args[0], Integer.parseInt(args[1]),args[6],
				this,gui);
		mdbReceiver = new MulticastReceiver(args[2], Integer.parseInt(args[3]),args[6],
				this,gui);
		mdrReceiver = new MulticastReceiver(args[4], Integer.parseInt(args[5]),args[6],
				this,gui);
		mcSender = new MulticastSender(args[0], Integer.parseInt(args[1]),gui);
		mdbSender = new MulticastSender(args[2], Integer.parseInt(args[3]),gui);
		mdrSender = new MulticastSender(args[4], Integer.parseInt(args[5]),gui);
		chunks = new ConcurrentHashMap<String, Chunk>();
		myFiles = new ConcurrentHashMap<String, String>();
		nrChunksByFile = new ConcurrentHashMap<String, Long>();
		filesToBeRestored = new ConcurrentHashMap<String, String>();
		state = new StateKeeper("state.ser",30000);
		gui=newGui;
		sizes = new int[2];
		sizes[0] = 0;
		sizes[1] = 1;
	}

	public void newInputMessage(Message message) {
		messageQueue.add(message);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			state.load();
			chunks = (ConcurrentHashMap<String, Chunk>) state.getObject("chunks");
			myFiles = (ConcurrentHashMap<String, String>) state.getObject("myFiles");
			nrChunksByFile = (ConcurrentHashMap<String, Long>) state.getObject("nrChunksByFile");
			filesToBeRestored = (ConcurrentHashMap<String, String>) state.getObject("filesToBeRestored");
			sizes[0] = ((int[]) state.getObject("sizes"))[0];
			sizes[1] = ((int[]) state.getObject("sizes"))[1];
			gui.setUsedSpace(sizes[0]);
			gui.setMaxUsedSpace(sizes[1]);
			notifyGuiFileChange();
			gui.log("Loaded from last session.");
		} catch (Exception e) {
			gui.log("Starting as new system");
			state.addObject("chunks",chunks);
			state.addObject("myFiles", myFiles);
			state.addObject("nrChunksByFile", nrChunksByFile);
			state.addObject("filesToBeRestored", filesToBeRestored);
			state.addObject("sizes",sizes);
			sizes[1] = gui.getMaxUsedSpace();
		}
		process();
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
					processRemoved(msg,true);
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
				} else if(chk.getCounter() < chk.getReplicationDeg())
				{
					gui.log("GIVING UP ON CHUNK " + chk.getChunkId());
					gui.log(" -- file " + myFiles.get(chk.getTextFileId()) + " will not get the desired repdeg");
				}
			}
			try {
				if(state.saveIfTime())
					gui.log("Saved state");
			} catch (Exception e) {
				gui.log("Couldn't save state");
			}
		}
	}

	private void processRemoved(Message msg, boolean send) { 
		Chunk chk;

		synchronized (chunks) {
			chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(),
					msg.getChunkNo()));
		}

		if (chk == null)
			return;

		chk.removeHostWithChunk(msg.getSenderIp());

		if(!send)
			return;

		if (msg.ready()) {
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
			gui.log("processRemoved");
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

		sizes[0] -= ChunkManager.deleteChunks("./", msg.getTextFileId());
		gui.setUsedSpace(sizes[0]);
		gui.log("processDelete for " + msg.getTextFileId());
	}

	private void processChunk(Message msg) { 
		// if chunk is received, kills
		// waiting getchunk message
		Chunk chk = chunks.get(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()));

		if (chk == null)
			return;
		
		gui.log("processChunk " + msg.getTextFileId() + " " + msg.getChunkNo());

		if (chk.isMine()) {
			// write to disk -> if all chunks are present, merge file with			
			// name in filesToBeRestored
			
			try {
				chk.save(msg.getBody());
			} catch (IOException e) {
				gui.log("Couldn't write " + chk.getChunkId() + " to disk");
				e.printStackTrace();
			}
			
			String filename = chk.getTextFileId();
			long nrChunks = nrChunksByFile.get(filename);
			String newName = filesToBeRestored.get(filename);
			if (ChunkManager.countChunks("./", filename) == nrChunks
					&& newName != null) {
				
				String fileRealName = myFiles.get(filename);
				
				if(fileRealName.contains("/"))
					fileRealName = fileRealName.substring(fileRealName
						.lastIndexOf('/') + 1);
				else
					fileRealName = fileRealName.substring(fileRealName
						.lastIndexOf('\\') + 1);
				
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
			gui.log("processChunk " + msg.getTextFileId() + " " + msg.getChunkNo());

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

		if (c != null) {
			c.addHostWithChunk(msg.getSenderIp());
		}
	}

	private void processPutChunk(Message msg) {

		if (myFiles.containsKey(msg.getTextFileId()))
			return;	

		if (msg.ready()) {
			Message newMsg = new Message("STORED", version, msg.getTextFileId());
			newMsg.setChunkNo(msg.getChunkNo());

			for (Message m : messageQueue) {
				if (m.getMessageType().equals("REMOVED")
						&& Chunk.getChunkId(m.getTextFileId(), m.getChunkNo())
						.equals(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()))) {
					processRemoved(m, false);
					messageQueue.remove(m);
					break;
				}
			}

			if(chunks.containsKey(Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo())))
			{
				outgoingQueue.add(newMsg);
			} else
			{
				if (sizes[0] + msg.getBody().length <= sizes[1]*1000000) {
					Chunk chunk = new Chunk(msg.getTextFileId(), msg.getChunkNo(),
							msg.getReplicationDeg(), msg.getSenderIp());

					try {
						chunk.save(msg.getBody());
					} catch (IOException e) {
						gui.log("Couldn't write " + chunk.getChunkId() + " to disk");

						return;
					}

					sizes[0] += msg.getBody().length;
					gui.setUsedSpace(sizes[0]);
					chunks.put(
							Chunk.getChunkId(msg.getTextFileId(), msg.getChunkNo()),
							chunk);

					outgoingQueue.add(newMsg);
				}
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
		// no need to do it here


		if (fileId == null) {
			gui.log("File to be removed not found");
			return;
		}
		myFiles.remove(fileId);

		Message message = new Message("DELETE", version, fileId);
		outgoingQueue.add(message);
		notifyGuiFileChange();
	}

	public void addFile(String fileName, int repDeg) {
		// break in chunks, add fileID to myfiles, add chunks to hash
		gui.log("Please wait the file is being broken into chunks");
		byte[] sha = new byte[32];
		long nrChunks = 0;

		try {
			Long[] chksize = new Long[1];
			nrChunks = ChunkManager.createChunks(fileName, "./", sha,chksize);
			sizes[0] += chksize[0];
			gui.setUsedSpace(sizes[0]);
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
		
		sizes[1] = mbLimit;

		if(sizes[0] <= mbLimit*1000000)
			return;

		PriorityQueue<Chunk> orderedChunks = new PriorityQueue<Chunk>(chunks.size()+1, new Chunk.ChunkCompare());

		Iterator<Chunk> it = chunks.values().iterator();
		while (it.hasNext()) {
			Chunk chunk = it.next();
			orderedChunks.add(chunk);
		}

		while(sizes[0] > mbLimit*1000000){
			Chunk chk = orderedChunks.poll();
			if(chk == null)
			{
				gui.log("Error in selecting best chunk to be erased");
				return;
			}
			gui.log("Removed chunk " + chk.getChunkId() + " repdeg " + chk.getReplicationDeg() + " currentrepdeg " + chk.getCounter());
			sizes[0] = (int) (sizes[0] - ChunkManager.deleteChunk("./",chk.getChunkId()));
			chunks.remove(chk.getChunkId());
			Message message = new Message("REMOVED", version,chk.getTextFileId());
			message.setChunkNo(chk.getChunkNo());
			outgoingQueue.add(message);
			gui.setUsedSpace(sizes[0]);
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
