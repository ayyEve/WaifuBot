package waifu;

import javax.swing.Timer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ServerData 
{
	ArrayList<String> usedCharacters;
	Map<String, WaifuList> list;
	
	int timeout = 60_000; // time before you can add another waifu
	String nameText = "waifu"; // what to call people in the list
	String groupText = "harem"; // what to call the group of people
	String activator = "!waifu"; // what activates the bot
	String role = "tablekat"; // role needed to change settings
	
	boolean allowPolygamy = false;
	
	public ServerData()
	{
		usedCharacters = new ArrayList<String>();
		list = new HashMap<String, WaifuList>();
	}
	
	public void save(BufferedWriter bw) throws IOException
	{
		// save things
		bw.write(nameText);  bw.newLine();
		bw.write(groupText); bw.newLine();
		bw.write(activator); bw.newLine();
		bw.write(role); bw.newLine();
		bw.write(timeout+"");   bw.newLine();
		bw.write(allowPolygamy + ""); bw.newLine();
		
		// save the used characters
		for (int i = 0; i < usedCharacters.size(); i++)
		{
			bw.write(usedCharacters.get(i));
			bw.newLine();
		}
		bw.write("end");
		bw.newLine();
		
		for (Entry<String, WaifuList> entry : list.entrySet())
		{
			bw.write(entry.getKey() + "," + entry.getValue());
			bw.newLine();
		}
		bw.write("end");
		bw.newLine();
	}
	
	public static ServerData load(BufferedReader br) throws IOException
	{
		ServerData sd = new ServerData();
		
		// load things
		sd.nameText  = br.readLine();
		sd.groupText = br.readLine();
		sd.activator = br.readLine();
		sd.role = br.readLine();
		sd.timeout   = Integer.parseInt(br.readLine());
		sd.allowPolygamy = Boolean.parseBoolean(br.readLine());
		
		// load used characters
		for (String name : loadToEnd(br)) 
		{
			sd.usedCharacters.add(name);
		}
		
		// load all the saved lists
		for (String in : loadToEnd(br))
		{
			String[] args = in.split(",");
			try 
			{
				for (int i = 1; i < args.length; i++)
					sd.add2(args[0], args[i]);
			} 
			catch(Exception e)
			{
				Main.printError(e);
			}
		}
		return sd;
	}
	
	private static String[] loadToEnd(BufferedReader br)
	{
		ArrayList<String> out = new ArrayList<String>();
		
		while (true)
		{
			try
			{
				String s = br.readLine();
				if (s.equals("end")) return out.toArray(new String[0]);
				out.add(s);
			} 
			catch (IOException e) 
			{
				Main.printError(e);
			}
		}
	}

	public int add(String id, String name)
	{
		return check(id).add(name);
	}
	
	public void add2(String id, String name)
	{
		check(id).add2(name);
	}
	
	public boolean remove(String id, String name)
	{
		return check(id).remove(name);
	}
	
	private WaifuList check(String id)
	{
		if (!list.containsKey(id)) list.put(id, new WaifuList());
		WaifuList l = list.get(id);
		if (l == null) l = new WaifuList(); //TODO is this necessary?
		return l;
	}
	
	public WaifuData list(String id)
	{
		try
		{
			if (list.get(id) == null) return new WaifuData();
			return list.get(id).list();
		}
		catch (Exception e)
		{
			Main.printError(e);
			return new WaifuData();
		}
	}
	
	public void clear(String id)
	{
		if (list.get(id) != null) 
		{
			try
			{
				for (int i = 0; i < list.get(id).list.size(); i++)
					usedCharacters.remove(list.get(id).list.get(i));
			}
			catch (Exception e)
			{
				Main.printError(e);
			}
			
			list.get(id).list.clear();
		}
	}
	
	public class WaifuList
	{
		 private Timer t;
		 ArrayList<String> list;
		 boolean canadd = true;
		 
		 WaifuList()
		 {
			 t = new Timer(0, new ActionListener()
			 {
				 public void actionPerformed(ActionEvent arg0) 
				 {
					 t.stop();
					 canadd = true;
				 }
			 });
			 
			 list = new ArrayList<String>();
			 canadd = true;
		 }

		 public void add2(String name)
		 {
			 list.add(name);
		 }
		 
		 public int add(String name)
		 {
			 if (usedCharacters.contains(name) && !allowPolygamy) return 0;
			 if (canadd)
			 {
				 list.add(name);
				 usedCharacters.add(name);
				 if (timeout == 0) return 1;
				 t.setInitialDelay(timeout);
				 t.start();
				 canadd = false;
				 return 1;
			 }
			 return 2;
		 }
		 
		 public boolean remove(String name)
		 {
			usedCharacters.remove(name);
			return list.remove(name);
		 }
		 
		 public void clear()
		 {
			if (!list.isEmpty()); list.clear();
		 }
		 
		 public WaifuData list()
		 {
			String out = "";
			
			for (String s : list) 
			{
				out += "- " + s + "\n";
			}
			
			return new WaifuData(out, list.size());
		 }
		 
		 public String toString()
		 {
			String out = "";
			for (String s : list) out += s + ",";
			return out;
		 }
	}
}
