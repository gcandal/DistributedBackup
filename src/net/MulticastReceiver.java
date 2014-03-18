package net;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import core.Message;
import core.Processor;

public class MulticastReceiver extends Thread {

	private static final int BUFSIZE=65000;
	private MulticastSocket socket;
	private String addr;
	private int port;
	private boolean end;
	private Processor processor;

	public MulticastReceiver(String addr, int port, Processor processor) {
		this.addr = addr;
		this.port = port;
		this.processor = processor;
		end = false;
	}

	@Override
	public void run() {
		try{

			socket = new MulticastSocket(port);
			InetAddress iaddr = InetAddress.getByName(addr);
			socket.joinGroup(iaddr);

			byte []buf = new byte[BUFSIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while(!end)
			{
				socket.receive(packet);
				processMessage(buf,packet.getLength(),packet.getAddress().getHostAddress());
			}

			socket.leaveGroup(iaddr);
			socket.close();
		}
		catch(Exception e)
		{
			//TODO send to gui
		}
	}
	
	private void processMessage(byte[] buf, int size, String ip) {
		Message msg;
		try {
			msg = new Message(buf,size,ip);
			processor.newInputMessage(msg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
