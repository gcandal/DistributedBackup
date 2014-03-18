package core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Chunk {

	private byte[] fileId;
	private int chunkNo;
	private int replicationDeg;
	private int counter;
	
	public Chunk(byte[] fileId, int i, int replicationDeg) {
		this.fileId = fileId;
		this.chunkNo = i;
		this.replicationDeg = replicationDeg;
		this.counter = 0;
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
	
	public void decrementCount() {
		this.counter--;
	}
	
	public void incrementCount() {
		this.counter++;
	}
	
	public int getCounter() {
		return counter;
	}
	
	public byte[] getHash()
	{
		String num = "";
		num += chunkNo;
		byte[] cnum = num.getBytes();
		byte[] hash = new byte[cnum.length + fileId.length];
		System.arraycopy(fileId, 0, hash, 0, fileId.length);
		System.arraycopy(cnum, 0, hash, fileId.length, cnum.length);
		return hash;
	}

	public void save(byte[] data) throws IOException {
		File to = new File(Message.bytesToHex(fileId) + chunkNo);
		to.createNewFile();
		FileOutputStream toStream = new FileOutputStream(to);
		toStream.write(data);
		toStream.close();
	}
}
