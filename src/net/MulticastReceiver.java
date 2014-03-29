package net;

import gui.StartWindow;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;

import core.Message;
import core.Processor;

public class MulticastReceiver extends Thread {

	private static final int BUFSIZE=65000;
	private MulticastSocket socket;
	private String addr;
	private int port;
	private boolean end;
	private Processor processor;
	private String iface;
	private StartWindow gui;

	public MulticastReceiver(String addr, int port, String iface, Processor processor, StartWindow gui) {
		this.addr = addr;
		this.port = port;
		this.processor = processor;
		this.iface = iface;
		end = false;
		this.gui = gui;
	}

	@Override
	public void run() {
		try{

			socket = new MulticastSocket(port); 
			SocketAddress socketAddress = new InetSocketAddress(addr, port);
			NetworkInterface networkInterface = NetworkInterface.getByName(iface);
			socket.joinGroup(socketAddress, networkInterface);

			byte []buf = new byte[BUFSIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while(!end)
			{
				socket.receive(packet);
				processMessage(buf,packet.getLength(),packet.getAddress().getHostAddress());
			}

			socket.leaveGroup(socketAddress, networkInterface);
			socket.close();
		}
		catch(Exception e)
		{
			gui.log("error while receiving package");
		}
	}
	
	private void processMessage(byte[] buf, int size, String ip) {
		Message msg;
		try {
			msg = new Message(buf,size,ip);
			processor.newInputMessage(msg);
		} catch (Exception e) {
			gui.log("error while processing new message");
		}
	}
}
