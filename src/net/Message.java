package net;

public class Message {

	private String messageType;
	private float version;
	private byte[] fileId;
	private String chunkNo;
	private int replicationDeg;
	private byte[] body;
	private String senderIp;
	
	public Message(byte[] input) {
		process(input);
	}

	public Message(String msg, float version, byte[] fileId, int chunkNo, String senderIp)
	{
	
	}
	
	private void process(byte[] input)
	{
		
	}
	
	public byte[] buildBuffer()
	{
		return null;
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
	
	

}
