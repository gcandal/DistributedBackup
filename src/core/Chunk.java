package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;

import utils.ChunkManager;

public class Chunk implements Serializable {

	private static final long serialVersionUID = -5008936668929321920L;
	private String fileIdString;
	private int chunkNo;
	private String chunkId;
	private int replicationDeg;
	private String chunkIp;
	private HashSet<String> hostsWithChunk;
	private boolean mine;
	
	// STORED
	private long timeInterval = 500;
	private long lastSend;
	private int sendTimes = 0;
	
	public static class ChunkCompare implements Comparator<Chunk>
	{

		@Override
		public int compare(Chunk o1, Chunk o2) {
			int o1dif = o1.getCounter() - o1.getReplicationDeg();
			int o2dif = o2.getCounter() - o2.getReplicationDeg();
			
			return -(o1dif-o2dif); // to get the highest in top of heap
		}
		
	}
	
	public Chunk(String fileId, int i, int replicationDeg, String ip) {
		initialize(fileId,i,replicationDeg,ip,false);
	}
	
	public Chunk(String fileId, int i, int replicationDeg, String ip, boolean mine)
	{
		initialize(fileId,i,replicationDeg,ip,mine);
	}
	
	private void initialize(String fileId, int i, int replicationDeg, String ip, boolean mine)
	{
		hostsWithChunk = new HashSet<String>();
		this.fileIdString = fileId;
		this.chunkNo = i;
		String num = ChunkManager.numToAscii(chunkNo);
		chunkId = fileIdString + num;
		this.replicationDeg = replicationDeg;
		this.chunkIp = ip;
		this.mine = mine;
		this.lastSend = 0;
	}
	
	public void restart()
	{
		lastSend = 0;
		timeInterval = 500;
		sendTimes = 0;
	}
	
	public void notifySent()
	{
		lastSend = System.currentTimeMillis();
		sendTimes++;
		timeInterval *= 2;
	}
	
	public boolean timeOver()
	{
		long dif = System.currentTimeMillis() - lastSend;
		return dif >= timeInterval;
	}
	
	public boolean shouldResend()
	{
		return (hostsWithChunk.size()<replicationDeg && sendTimes < 5);	
	}
	
	public void addHostWithChunk(String ip)
	{
		hostsWithChunk.add(ip);
	}

	public String getTextFileId() {
		return fileIdString;
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
		File to = new File(chunkId);
		to.createNewFile();
		FileOutputStream toStream = new FileOutputStream(to);
		toStream.write(data);
		toStream.close();
	}

	public byte[] load() throws IOException {
		File from = new File(chunkId);
		FileInputStream fromStream = new FileInputStream(from);
		
		byte data[] = new byte[(int) from.length()];
		fromStream.read(data);
		fromStream.close();
		
		return data;
	}

	public void removeHostWithChunk(String senderIp) {
		hostsWithChunk.remove(senderIp);		
	}
	
	public String getChunkId()
	{
		return chunkId;
	}
	
	public static String getChunkId(String fileId, int chunkNo)
	{
		return fileId + ChunkManager.numToAscii(chunkNo);
	}
	/*
	@Override
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		
		if((o instanceof String))
			if(((String)o).equals(chunkId))
				return true;
		
		if(this == o)
			return true;
		
		return false;
	}*/
}
