package net;

import gui.StartWindow;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import core.Message;
import core.Processor;

public class UnicastReceiver extends Thread {

	private static final int BUFSIZE=65000;
	private DatagramSocket socket;
	private int port;
	private boolean end;
	private Processor processor;
	private StartWindow gui;

	public UnicastReceiver(int port, Processor processor, StartWindow gui) {
		this.port = port;
		this.processor = processor;
		end = false;
		this.gui = gui;
	}

	@Override
	public void run() {
		try{

			socket = new DatagramSocket(port);
			byte []buf = new byte[BUFSIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			while(!end)
			{
				socket.receive(packet);
				processMessage(buf,packet.getLength(),packet.getAddress().getHostAddress());
			}

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