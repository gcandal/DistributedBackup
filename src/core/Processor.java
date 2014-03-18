package core;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.MulticastReceiver;
import net.MulticastSender;

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

	public Processor(final String[] args) {
		messageQueue = new ConcurrentLinkedQueue<>();
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
		//messageQueue.notify();
	}

	public void process() {
		mcReceiver.run();
		mdbReceiver.run();
		mdrReceiver.run();
		mcSender.start();
		mdbSender.start();
		mdrSender.start();
		while (true) {
			/*try {
				synchronized (messageQueue) {
					messageQueue.wait();
				}
			} catch (InterruptedException e) {
			}*/
			Message msg = messageQueue.poll();
			if (msg != null) { // empty queue

				switch(msg.getMessageType())
				{
				case "PUTCHUNK":
				{
					processPutChunk(msg);
				}
				case "STORED":
				{
					processStored(msg);
				}
				case "CHUNK":
				{
					
				}
				case "GETCHUNK":
				{
					
				}
				}

			}
			
			Chunk chk = waitingChunks.poll();
			if(chk!=null)
			{
				if(chk.shouldResend())
				{
					sendStored(chk);
					chk.notifySent();
				} else
					waitingChunks.add(chk);
			}
		}
	}

	private void sendStored(Chunk chk) {
		Message msg = new Message("STORED", version, chk.getFileId(), chk.getChunkNo());
		mcSender.send(msg);
	}

	private void processStored(Message msg) {

		byte[] key = Chunk.getHash(msg.getFileId(), msg.getChunkNo());
		
		Chunk c = chunks.get(key);
		if(c!=null)
			c.addHostWithChunk(msg.getSenderIp());
		
	}

	private void processPutChunk(Message msg) {
		
		//TODO check if I already have file
		
		if(msg.ready())
		{
			Message newMsg = new Message("STORED", version, msg.getFileId(), msg.getChunkNo());
			mcSender.send(newMsg);
			Chunk chunk = new Chunk(msg.getFileId(), msg.getChunkNo(), msg.getReplicationDeg(),msg.getSenderIp());
			//TODO Store chunk in disk
			msg.getBody();
			synchronized (chunks) {
				chunks.put(chunk.getHash(), chunk);
			}
		}else
		{
			messageQueue.add(msg);
		}
	}

	public void removeFile(String fileName) {

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

	}

	public void setSpaceLimit(int mbLimit) {

	}
	
	public void restoreFile(String fileName, String newLocation)
	{
		
	}

}
