package waifu;

public class WaifuData 
{
	public String names;
	int count;
	
	public WaifuData()
	{
		names = "";
		count = 0;
	}
	
	public WaifuData(String names, int count)
	{
		this.names = names;
		this.count = count;
	}
	
	public String get()
	{
		if (count == 0) return "no " + Main.getNameText() + "s";
		return count + " " + Main.getNameText() + (count > 1 ? "s" : "") + ":\n" + names;
	}
}
