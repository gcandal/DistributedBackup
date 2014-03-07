package core;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.Message;
import net.MulticastReceiver;
import net.MulticastSender;

public class Processor {

	private MulticastReceiver mcReceiver;
	private MulticastReceiver mdbReceiver;
	private MulticastReceiver mdrReceiver;
	private MulticastSender mcSender;
	private MulticastSender mdbSender;
	private MulticastSender mdrSender;
	private HashMap<byte[],Chunk> chunks; //fileId+ChuckNo
	private HashMap<byte[], String> myFiles; //fileId
	private ConcurrentLinkedQueue<Message> messageQueue;
	
	public Processor(final String[] args) {
		messageQueue = new ConcurrentLinkedQueue<>();
		mcReceiver = new MulticastReceiver(args[0], Integer.parseInt(args[1]));
		mdbReceiver = new MulticastReceiver(args[2], Integer.parseInt(args[3]));
		mdrReceiver = new MulticastReceiver(args[4], Integer.parseInt(args[5]));
		
		// senders...
	}
	
	
	public void newInputMessage(Message message){
		messageQueue.add(message);
	}
	
	public void process()
	{
		while(true)
		{
			Message msg = messageQueue.poll();
			if(msg!=null) // empty queue
			{
				// process msg
				
				//exemplo acesso a hashmap
				synchronized (chunks) {
					//...
				}
				
			}
		}
	}
	
	public void removeFileFromHash(String fileName)
	{
	}
	
}
