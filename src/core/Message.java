package core;

import java.util.Random;


public class Message {

	private String messageType;
	private float version;
	private byte[] fileId;
	private String fileIdString = "";
	private int chunkNo;
	private int replicationDeg=-1;
	private byte[] body;
	private String senderIp;
	private long timestamp;
	private long randomDelay;

	public Message(byte[] input, int size, String ip) throws Exception {
		timestamp = System.currentTimeMillis();
		Random r = new Random();
		randomDelay = r.nextLong()%400;
		senderIp = ip;
		process(input,size);
	}

	public Message(String msg, float version, byte[] fileId)
	{
		messageType=msg;
		this.version=version;
		this.fileId=fileId;
	}

	public Message(String msg, float version, String fileId) {
		messageType=msg;
		this.version=version;
		fileIdString=fileId;
	}

	private void process(byte[] input, int size) throws Exception // the size is smaller than the length of the byte array
	{

		if(input.length < size)
			throw new Exception();

		char[] header = new char[100];
		int i;
		for(i = 0; i < input.length; i++)
		{
			if(input[i]=='\r' && input[i+1]=='\n' && input[i+2]=='\r' && input[i+3]=='\n')
				break;

			header[i] = (char) input[i];
		}

		String headerstr = String.valueOf(header, 0, i);

		String[] inputs = headerstr.split(" ");

		//TODO check if valid
		messageType = inputs[0];
		version = Float.parseFloat(inputs[1]);
		fileId = hexStringToByteArray(inputs[2]);
		if(!messageType.equals("DELETE"))
			chunkNo = Integer.parseInt(inputs[3]);

		if(messageType.equals("PUTCHUNK"))
		{
			replicationDeg=Integer.parseInt(inputs[4]);
		}

		if(messageType.equals("PUTCHUNK") || messageType.equals("CHUNK"))
		{
			int bodysize = size - i - 4;
			body = new byte[bodysize];
			for(int j = 0; j < bodysize; j++)
			{
				body[j] = input[i+4+j];
			}
		}
	}

	public byte[] buildBuffer()
	{		
		StringBuilder sb = new StringBuilder();

		sb.append(messageType);
		sb.append(" ");

		if(messageType.equals("DELETE")) {
			sb.append(bytesToHex(fileId));
			sb.append("\r\n\r\n");

			return sb.toString().getBytes();
		}

		sb.append(Processor.version);
		sb.append(" ");
		if(fileIdString.equals(""))
			sb.append(bytesToHex(fileId));
		else
			sb.append(fileIdString);
		sb.append(" ");
		if(!messageType.equals("DELETE")) {
			sb.append(chunkNo);
			sb.append(" ");
		}
		if(replicationDeg>=0)
			sb.append(replicationDeg);
		sb.append("\r\n\r\n");
		String headerstr = sb.toString();
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

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(messageType);
		sb.append(" ");

		if(messageType.equals("DELETE")) {
			sb.append(bytesToHex(fileId));
			sb.append("\r\n\r\n");

			return sb.toString();
		}

		sb.append(Processor.version);
		sb.append(" ");
		if(fileIdString.equals(""))
			sb.append(bytesToHex(fileId));
		else
			sb.append(fileIdString);
		sb.append(" ");
		if(!messageType.equals("DELETE")) {
			sb.append(chunkNo);
			sb.append(" ");
		}
		if(replicationDeg>=0)
			sb.append(replicationDeg);
		if(body!=null)
			sb.append(" Body length: " + body.length);
		
		return sb.toString();
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
	
	public String getTextFileId() {
		return bytesToHex(fileId);
	}

	public void setFileId(byte[] fileId) {
		this.fileId = fileId;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(int chunkNo) {
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

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	public boolean ready() {
		long dif = System.currentTimeMillis() - (timestamp + randomDelay);
		return (dif>0);
	}

}
