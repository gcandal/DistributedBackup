package net;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastReceiver extends Thread {

	private static final int BUFSIZE=65000;
	private MulticastSocket socket;
	private String addr;
	private int port;
	private boolean end;

	public MulticastReceiver(String addr, int port) {
		this.addr = addr;
		this.port = port;
		end = false;
	}

	@Override
	public void run() {
		try{

			MulticastSocket socket = new MulticastSocket(port);
			InetAddress iaddr = InetAddress.getByName(addr);
			socket.joinGroup(iaddr);

			byte []buf = new byte[BUFSIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while(!end)
			{
				socket.receive(packet);
				processHeader(buf);
			}

			socket.leaveGroup(iaddr);
			socket.close();
		}
		catch(Exception e)
		{
			//TODO send to gui
		}
	}
	
	public String processHeader(byte[] buf) {
		// 
		return "";
	}
}
