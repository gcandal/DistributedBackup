package net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastSender {

	private static final int TTL = 1;
	private MulticastSocket socket;
	private InetAddress addr;
	private String addrStr;
	private int port;

	public MulticastSender(String addr, int port) {
		addrStr=addr;
		this.port=port;
	}

	public void start() {
		try {
			this.addr = InetAddress.getByName(addrStr);
			socket = new MulticastSocket(port);
			socket.setTimeToLive(TTL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
	}

	public void send(Message msg) {
		byte[] buf = msg.buildBuffer();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}

}
