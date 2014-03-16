package core;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.Message;
import net.MulticastReceiver;
import net.MulticastSender;

public class Processor {

	public static final String version = "1.0";
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

				// exemplo acesso a hashmap
				synchronized (chunks) {
					// ...
				}

			}
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
