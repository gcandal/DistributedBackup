package utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class StateKeeper implements Serializable {

	private static final long serialVersionUID = -5955928138625994161L;
	private HashMap<String,Object> toBeStored;
	private String filename;
	private long lastsave = 0;
	private long timedif;
	
	public StateKeeper(String fn, long time) {
		toBeStored = new HashMap<String,Object>();
		filename = fn;
		timedif = time;
	}
	
	public boolean addObject(String name, Object o)
	{
		if(toBeStored.containsKey(name))
			return false;
		toBeStored.put(name,o);
		return true;
	}
	
	public boolean saveIfTime() throws Exception
	{
		long current = System.currentTimeMillis();
		if(timedif < (current - lastsave))
		{
			 lastsave=current;
	         FileOutputStream fileOut =
	         new FileOutputStream(filename);
             ObjectOutputStream out = new ObjectOutputStream(fileOut);
             out.writeObject(toBeStored);
             out.close();
             fileOut.close();
             return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public void load() throws Exception
	{
		 FileInputStream fileIn = new FileInputStream(filename);
         ObjectInputStream in = new ObjectInputStream(fileIn);
         toBeStored = (HashMap<String,Object>) in.readObject();
         in.close();
         fileIn.close();
	}
	
	public Object getObject(String name)
	{
		return toBeStored.get(name);
	}

}
