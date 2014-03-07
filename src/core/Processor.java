package core;

import java.util.HashMap;
import net.MulticastReceiver;
import net.MulticastSender;

public class Processor {

	MulticastReceiver mcReceiver;
	MulticastReceiver mdbReceiver;
	MulticastReceiver mdrReceiver;
	MulticastSender mcSender;
	MulticastSender mdbSender;
	MulticastSender mdrSender;
	HashMap<byte[],Chunk> chunks; //fileId+ChuckNo
	HashMap<byte[], String> myFiles; //fileId
		
	public Processor() {
		// TODO Auto-generated constructor stub
	}

}
