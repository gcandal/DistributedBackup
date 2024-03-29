package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import core.Message;

public class ChunkManager {
	private static int CHUNK_SIZE = 64000;
	private static MessageDigest md;

	public static long createChunks(String originalFilePath,
			String destinyChunkPath, byte[] sha, Long[] totalSize) throws IOException {

		File from = new File(originalFilePath);
		FileInputStream fromStream = new FileInputStream(from);
		FileChannel in = fromStream.getChannel(); 
		
		totalSize[0] = (long) 0;
		int chunkNo = 0 ;
		long lastChunkNo = from.length() / CHUNK_SIZE;
		byte[] newSha = fileToSHA256(from.getName());
		
		while(chunkNo < lastChunkNo) {
			totalSize[0] +=	writeChunk(chunkNo, in, destinyChunkPath + Message.bytesToHex(newSha), CHUNK_SIZE);
			chunkNo++;
		}

		totalSize[0] += writeChunk(chunkNo, in, destinyChunkPath + Message.bytesToHex(newSha), from.length() % CHUNK_SIZE);

		fromStream.close();
		in.close();
		
		
		for(int i = 0; i < newSha.length; i++)
			sha[i] = newSha[i];
		
		return lastChunkNo + 1;
	}

	public static void mergeChunks(String filepath, final String filename, String newFilePath, String newname) throws IOException {
		File[] chunks = new File(filepath).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.matches(filename+"[0-9]{6}$");
			}
		});

		String newfile = newFilePath;
		if(!newfile.endsWith("/"))
			newfile += "/";
		newfile += newname;
		
		File to = new File(newfile);
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
	
	public static long deleteChunks(String filepath, final String filename) {
		long freedSpace = 0;
		
		File[] chunks = new File(filepath).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.matches("^" + filename + "[0-9]{6}$");
			}
		});
		
		for(File chunk: chunks) {
			freedSpace += chunk.length();
			chunk.delete();
		}
		
		return freedSpace;
	}

	private static long writeChunk(int chunkNo, FileChannel in, String filename, long targetSize) throws IOException {
		int position = 0;

		File to = new File(filename + numToAscii(chunkNo));
		//System.out.println(to.getAbsolutePath());
		to.createNewFile();
		
		FileOutputStream toStream = new FileOutputStream(to);
		FileChannel out = toStream.getChannel();

		while(position < targetSize)
			position += out.transferFrom(in, position, CHUNK_SIZE);

		toStream.close();
		out.close();
		
		return to.length();
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
				return null;
			}
		}

		File file = new File(filePath);
		String text = String.valueOf(file.lastModified()) + file.getName();
		
		md.update(text.getBytes());

		return md.digest();
	}
	
	public static long getFolderSize(String path) {
        long folderSize = 0;
        File[] fileList = new File(path).listFiles();
        
        for(File file: fileList)
        	folderSize += file.length();
        
        return folderSize;
    }
	
	public static long deleteChunk(String path, String filename) {
		File chunkToDelete = new File(path + filename);
		long size = chunkToDelete.length();
		
		chunkToDelete.delete();
		
		return size;
	}
	
	public static long countChunks(String path, final String fileName) {
		File[] chunks = new File(path).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String name) {
				return name.matches("^" + fileName + "[0-9]{6}$");
			}
		});
		
		return (long) chunks.length;
	}
}