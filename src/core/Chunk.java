package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

public class Chunk {

	private byte[] fileId;
	private int chunkNo;
	private int replicationDeg;
	private String chunkIp;
	private HashSet<String> hostsWithChunk;
	private boolean mine;
	
	// STORED
	private long timeInterval = 500;
	private long lastSend;
	private int sendTimes = 0;
	
	public Chunk(byte[] fileId, int i, int replicationDeg, String ip) {
		initialize(fileId,i,replicationDeg,ip,false);
	}
	
	public Chunk(byte[] fileId, int i, int replicationDeg, String ip, boolean mine)
	{
		initialize(fileId,i,replicationDeg,ip,mine);
	}
	
	private void initialize(byte[] fileId, int i, int replicationDeg, String ip, boolean mine)
	{
		hostsWithChunk = new HashSet<String>();
		this.fileId = fileId;
		this.chunkNo = i;
		this.replicationDeg = replicationDeg;
		this.chunkIp = ip;
		this.mine = mine;
	}
	
	public void notifySent()
	{
		lastSend = System.currentTimeMillis();
		sendTimes++;
		timeInterval *= 2;
	}
	
	public boolean shouldResend()
	{
		long dif = System.currentTimeMillis() - lastSend;
		if(timeInterval < dif)
			return false;
		
		return (mine && hostsWithChunk.size()<replicationDeg && sendTimes < 5);
		
	}
	
	public void addHostWithChunk(String ip)
	{
		hostsWithChunk.add(ip);
	}

	public byte[] getFileId() {
		return fileId;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public int getReplicationDeg() {
		return replicationDeg;
	}
	
	public int getCounter() {
		return hostsWithChunk.size();
	}
	
	
	
	public byte[] getHash()
	{
		return getHash(fileId, chunkNo);
	}
	
	public static byte[] getHash(byte[] fileId, int chunkNo) //TODO missing leading 0
	{
		String num = ""; // Sugestão: String num = ChunkManager.numToAscii(chunkNo);
		num += chunkNo;
		byte[] cnum = num.getBytes();
		byte[] hash = new byte[cnum.length + fileId.length];
		System.arraycopy(fileId, 0, hash, 0, fileId.length);
		System.arraycopy(cnum, 0, hash, fileId.length, cnum.length);
		return hash;
	}

	public String getChunkIp() {
		return chunkIp;
	}

	public void setChunkIp(String chunkIp) {
		this.chunkIp = chunkIp;
	}

	public boolean isMine() {
		return mine;
	}

	public void setMine(boolean mine) {
		this.mine = mine;
	}
	
	public void save(byte[] data) throws IOException {
		File to = new File(Message.bytesToHex(fileId) + chunkNo);
		to.createNewFile();
		FileOutputStream toStream = new FileOutputStream(to);
		toStream.write(data);
		toStream.close();
	}

	public byte[] load() throws IOException {
		File from = new File(Message.bytesToHex(fileId) + chunkNo);
		FileInputStream fromStream = new FileInputStream(from);
		
		byte data[] = new byte[(int) from.length()];
		fromStream.read(data);
		fromStream.close();
		
		return data;
	}

	public void removeHostWithChunk(String senderIp) {
		hostsWithChunk.remove(senderIp);		
	}
}
