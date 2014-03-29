package net;

import gui.StartWindow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import core.Message;

public class MulticastSender {

	private static final int TTL = 1;
	private MulticastSocket socket;
	private InetAddress addr;
	private String addrStr;
	private int port;
	private StartWindow gui;

	public MulticastSender(String addr, int port, StartWindow gui) {
		addrStr=addr;
		this.port=port;
		this.gui = gui;
	}

	public void start() {
		try {
			this.addr = InetAddress.getByName(addrStr);
			socket = new MulticastSocket(port);
			socket.setTimeToLive(TTL);
		} catch (Exception e) {
			gui.log("Not able to start sender socket.");
		}
	}

	public void send(Message msg) {
		byte[] buf = msg.buildBuffer();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			gui.log("Not able to send message.");
		}
	}

}
