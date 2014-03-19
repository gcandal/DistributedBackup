package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChunkManager {
	private static int CHUNK_SIZE = 64000;
	private static MessageDigest md;

	public static long createChunks(String originalFilePath,
			String destinyChunkPath, byte[] sha) throws IOException {

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
		
		sha = fileToSHA256(from.getName());
		
		return lastChunkNo + 1;
	}

	public static void mergeChunks(String filepath, final String filename, String newFilePath) throws IOException {
		File[] chunks = new File(filepath).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.matches(filename+"[0-9]{6}$");
			}
		});

		File to = new File(newFilePath + filename);
		to.delete();
		to.createNewFile();
		
		FileOutputStream toStream = new FileOutputStream(to, true);
		FileChannel out = toStream.getChannel();

		for(File chunk: chunks) {
			FileInputStream fromStream = new FileInputStream(chunk);
			FileChannel in = fromStream.getChannel(); 
			out.transferFrom(in, out.position(), chunk.length());

			fromStream.close();
		}

		toStream.close();
	}
	
	public static void deleteChunks(String filepath, final String filename) {
		File[] chunks = new File(filepath).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.matches(filename+"[0-9]{6}$");
			}
		});
		
		for(File chunk: chunks)
			chunk.delete();
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

	public static byte[] fileToSHA256(String filePath) {

		if(md == null) {
			try {
				md = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();

				return new byte[0];
			}
		}

		File file = new File(filePath);
		String text = String.valueOf(file.lastModified()) + file.getName();
		
		md.update(text.getBytes());

		return md.digest();
	}
}
