package core;

public class Chunk {

	private byte[] fileId;
	private String chunkNo;
	private int replicationDeg;
	private int counter;
	
	public Chunk(byte[] fileId, String chunkNo, int replicationDeg) {
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.replicationDeg = replicationDeg;
		this.counter = 0;
	}

	public byte[] getFileId() {
		return fileId;
	}

	public String getChunkNo() {
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


}
