package net;

import core.Processor;

public class Message {

	private String messageType;
	private float version;
	private byte[] fileId;
	private String chunkNo;
	private int replicationDeg=-1;
	private byte[] body;
	private String senderIp; //???
	
	public Message(byte[] input, int size) {
		process(input,size);
	}

	public Message(String msg, float version, byte[] fileId, int chunkNo) //? chunkNo int?
	{
		messageType=msg;
		this.version=version;
		this.fileId=fileId;
		this.chunkNo=Integer.toString(chunkNo);
	}
	
	private void process(byte[] input, int size) // the size is smaller than the length of the byte array
	{
		//find crlfcrlf
		// separate data from header
		// convert to types and store
	}
	
	public byte[] buildBuffer()
	{		
		StringBuilder sb = new StringBuilder();
		
		sb.append(messageType);
		sb.append(" ");
		sb.append(Processor.version);
		sb.append(" ");
		sb.append(bytesToHex(fileId));
		sb.append(" ");
		if(replicationDeg>=0)
			sb.append(replicationDeg);
		sb.append("\r\n\r\n");
		String headerstr = sb.toString();
		//TODO log to gui
		byte[] header = headerstr.getBytes();
		if(body!=null)
		{
			int legth = header.length + body.length;
			byte[] out = new byte[legth];
			System.arraycopy(header,0,out,0,header.length);
			System.arraycopy(body,0,out,header.length,body.length);
			return out;
		}
		return header;
	}
	
	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}

	public byte[] getFileId() {
		return fileId;
	}

	public void setFileId(byte[] fileId) {
		this.fileId = fileId;
	}

	public String getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(String chunkNo) {
		this.chunkNo = chunkNo;
	}

	public int getReplicationDeg() {
		return replicationDeg;
	}

	public void setReplicationDeg(int replicationDeg) {
		this.replicationDeg = replicationDeg;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public String getSenderIp() {
		return senderIp;
	}

	public void setSenderIp(String senderIp) {
		this.senderIp = senderIp;
	}
	
	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	

}
