package core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.MulticastReceiver;
import net.MulticastSender;

//TODO geral -> adicionar syncronized a todos os acessos a chunks e myfiles

public class Processor {

	public static final float version = (float) 1.0;
	private MulticastReceiver mcReceiver;
	private MulticastReceiver mdbReceiver;
	private MulticastReceiver mdrReceiver;
	private MulticastSender mcSender;
	private MulticastSender mdbSender;
	private MulticastSender mdrSender;
	private HashMap<byte[], Chunk> chunks; // fileId+ChuckNo
	private HashMap<byte[], String> myFiles; // fileId -> Talvez tenhamos que
												// verificar se a data de
												// alteracao mudou!
	private ConcurrentLinkedQueue<Message> messageQueue;
	private ConcurrentLinkedQueue<Chunk> waitingChunks;
	private ConcurrentLinkedQueue<Message> outgoingQueue;
	private HashMap<byte[], String> filesToBeRestored;// -> string is new name

	public Processor(final String[] args) {
		messageQueue = new ConcurrentLinkedQueue<>();
		waitingChunks = new ConcurrentLinkedQueue<>();
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

				switch (out.getMessageType()) {
				case "PUTCHUNK": {
					mdbSender.send(out);
				}
				case "GETCHUNK": {
					mcSender.send(out);
				}
				case "DELETE": {
					mcSender.send(msg);
				}
				case "REMOVED": {
					mcSender.send(msg);
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

		Chunk chk = chunks
				.get(Chunk.getHash(msg.getFileId(), msg.getChunkNo()));
		if (chk == null)
			return;

		if (msg.ready()) {
			chk.removeHostWithChunk(msg.getSenderIp());
			Message newMsg = new Message("PUTCHUNK", version, msg.getFileId(),
					msg.getChunkNo());
			newMsg.setReplicationDeg(chk.getReplicationDeg());
			newMsg.setBody(chk.load());
			mdbSender.send(newMsg);
		} else
			messageQueue.add(msg);

	}

	private void processDelete(Message msg) {
		Iterator<byte[]> it = chunks.keySet().iterator();
		while (it.hasNext()) {
			byte[] hash = it.next();
			boolean belongs = true;
			for (int i = 0; i < 256; i++) {
				if (hash[i] != msg.getFileId()[i]) {
					belongs = false;
					break;
				}
			}

			if (belongs) {
				it.remove();
				// TODO Delete files ---- to get byte[] for acess hashmap use
				// Chunk.getHash(msg.getFileId(),
				// msg.getChunkNo())
			}
		}
	}

	private void processChunk(Message msg) { // if chunk is received, kills
												// waiting getchunk message
		Chunk chk = chunks
				.get(Chunk.getHash(msg.getFileId(), msg.getChunkNo()));
		if (chk == null)
			return;

		if (chk.isMine()) {
			// TODO write to disk -> if all chunks are present, merge file with
			// name in filesToBeRestored
		} else { // Remove get chunk msg if in queue
			for (Message m : messageQueue) {
				if (m.getMessageType() == "CHUNK"
						&& Chunk.getHash(m.getFileId(), m.getChunkNo()).equals(
								chk.getHash())) {
					messageQueue.remove(m);
					break;
				}
			}
		}

	}

	private void processGetChunk(Message msg) { // is kept in line till random
												// delay
		if (msg.ready()) {
			Chunk chk = chunks.get(Chunk.getHash(msg.getFileId(),
					msg.getChunkNo()));
			if (chk == null)
				return;

			Message newMsg = new Message("CHUNK", version, msg.getFileId(),
					msg.getChunkNo());
			newMsg.setBody(chk.load());
			mdrSender.send(newMsg);
		} else
			messageQueue.add(msg);
	}

	private void sendPutChunk(Chunk chk) {
		Message msg = new Message("PUTCHUNK", version, chk.getFileId(),
				chk.getChunkNo());
		msg.setReplicationDeg(chk.getReplicationDeg());
		msg.setBody(chk.load());
		outgoingQueue.add(msg);
		chk.notifySent();
	}

	private void processStored(Message msg) {

		byte[] key = Chunk.getHash(msg.getFileId(), msg.getChunkNo());

		Chunk c = chunks.get(key);
		if (c != null)
			c.addHostWithChunk(msg.getSenderIp());

	}

	private void processPutChunk(Message msg) { // is kept in line till random
												// delay

		if (msg.ready()) {
			Message newMsg = new Message("STORED", version, msg.getFileId(),
					msg.getChunkNo());
			mcSender.send(newMsg);

			if (!myFiles.containsKey(msg.getFileId())) {
				Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(),
						msg.getReplicationDeg(), msg.getSenderIp());
				// TODO Store chunk in disk
				msg.getBody();
				synchronized (chunks) {
					chunks.put(chunk.getHash(), chunk);
				}
			}

		} else {
			messageQueue.add(msg);
		}
	}

	public void removeFile(String fileName) {

		// TODO build message to be sent and put in outgoingQueue
		// delete from disk

		byte[] sha = null;

		for (Entry<byte[], String> pair : myFiles.entrySet()) {
			if (pair.getValue().equals(fileName)) {
				sha = pair.getKey();
				myFiles.remove(sha);
				break;
			}
		}

		if (sha == null)
			throw new NullPointerException("file not found");

	}

	public void addFile(String fileName, int repDeg) {
		// TODO break in chunks, add fileID to myfiles, add chunks to hash
		// sendPutChunk(chk);
	}

	public void setSpaceLimit(int mbLimit) {
		// TODO if necessary add REMOVED msgs to outgoingqueue
	}

	public void restoreFile(String fileName, String newLocation) {
		// TODO find chunks in hash, add CHUNK messages to outgoingqueue
		// TODO add new name to filestoberestored hashmap
	}

	public String[] getMyFiles() {
		String[] list;
		synchronized (myFiles) {
			list = (String[]) myFiles.values().toArray();
		}
		return list;
	}

}
