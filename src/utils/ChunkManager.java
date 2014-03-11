package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class ChunkManager {
	private static int CHUNK_SIZE = 64000;

	public static void createChunks(String originalFilePath,
			String destinyChunkPath) throws IOException {
		
		File from = new File(originalFilePath);
		FileInputStream fromStream = new FileInputStream(from);
		FileChannel in = fromStream.getChannel(); 

		int chunkNo = 0 ;
		long lastChunkNo = from.length() / CHUNK_SIZE;
		
		
		while(chunkNo < lastChunkNo) {
			writeChunk(chunkNo, in, destinyChunkPath + from.getName(), CHUNK_SIZE);
			chunkNo++;
		}
		
		writeChunk(chunkNo, in, destinyChunkPath + from.getName(), from.length() % CHUNK_SIZE);
		
		fromStream.close();
		in.close();
	}
	
	public static void mergeChunks(String filepath, final String filename, String newFilePath) throws IOException {
		File[] chunks = new File(filepath).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.matches(filename+"[0-9]{6}$");
			}
		});
		
		File to = new File("coisas");
		FileOutputStream toStream = new FileOutputStream(to);
		FileChannel out = toStream.getChannel();
		
		for(File chunk: chunks) {
			FileInputStream fromStream = new FileInputStream(chunk);
			FileChannel in = fromStream.getChannel(); 
			
			out.transferFrom(in, 0, chunk.length());
			
			fromStream.close();
		}
		
		toStream.close();
	}
	
	private static void writeChunk(int chunkNo, FileChannel in, String filename, long targetSize) throws IOException {
		int position = 0;
		
		File to = new File(filename + numToAscii(chunkNo));
		to.createNewFile();
		FileOutputStream toStream = new FileOutputStream(to);
		FileChannel out = toStream.getChannel();
		
		while(position < targetSize)
			position += out.transferFrom(in, position, CHUNK_SIZE);
		
		toStream.close();
		out.close();
	}
	
	public static void createMockFile(String filepath, int size) throws IOException {
		File mock = new File(filepath);
		mock.createNewFile();
		FileOutputStream buf = new FileOutputStream(mock);
		
		int i = 0;
		while(i++ < size)
			buf.write(1);
		
		buf.close();
	}
	
	public static int getChunkSize() {
		return CHUNK_SIZE;
	}
	
	public static String numToAscii(int num) {
		StringBuilder ascii = new StringBuilder("000000");
		int position = 5;
		
		while(num > 0) {
			ascii.setCharAt(position, Character.forDigit(num % 10, 10));
			num /= 10;
			position--;
		}
		
		return ascii.toString();
	}
}
