package sliner.com;

public class Main
{
	public static void main(String []args)
	{
		CmdLineParse parser = new CmdLineParse(args);
		
		SyncManager mgr = new SyncManager();
		
		mgr.start(parser);
	}
}
