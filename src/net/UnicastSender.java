package net;



import gui.StartWindow;



import java.io.IOException;

import java.net.DatagramPacket;

import java.net.DatagramSocket;

import java.net.InetAddress;



import core.Message;



public class UnicastSender {



	private DatagramSocket socket;

	private int port;

	private StartWindow gui;



	public UnicastSender(int port, StartWindow gui) {

		this.port=port;

		this.gui = gui;

	}



	public void start() {

		try {

			socket = new DatagramSocket();

		} catch (Exception e) {

			e.printStackTrace();

			gui.log("Not able to start sender socket.");

		}

	}



	public void send(Message msg) {

		byte[] buf = msg.buildBuffer();

		try {

			DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(msg.getSenderIp()), port);

			socket.send(packet);

		} catch (IOException e) {

			gui.log("Not able to send message to " + msg.getSenderIp());

		}

	}



}

