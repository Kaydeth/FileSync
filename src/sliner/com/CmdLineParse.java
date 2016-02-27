package sliner.com;

public class CmdLineParse
{
	public SocketThread.socketType socketType;
	public String cmdAddress;
	public int cmdPort;
	public String sharedDir;
	public String getLocalDir;
	public String getRemoteDir;
	
	public CmdLineParse(String [] args)
	{
		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			
			if(arg.equalsIgnoreCase("-client"))
			{
				socketType = SocketThread.socketType.SOCKET_CLIENT;
			}
			else if(arg.equalsIgnoreCase("-server"))
			{
				socketType = SocketThread.socketType.SOCKET_SERVER;
			}
			else if(arg.equalsIgnoreCase("-share"))
			{
				i++;
				sharedDir = args[i];
			}
			else if(arg.equalsIgnoreCase("-getlocal"))
			{
				i++;
				getLocalDir = args[i];
			}
			else if(arg.equalsIgnoreCase("-getremote"))
			{
				i++;
				getRemoteDir = args[i];				
			}
			else if(arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("-port"))
			{
				i++;
				cmdPort = Integer.parseInt(args[i]);
			}
			else
			{
				//The last argument should the address
				cmdAddress = arg;
			}
		}
	}
}