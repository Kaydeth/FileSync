package sliner.com;

import java.io.File;

import sliner.com.FileSyncMsg.FileSyncMessage;

public class SyncManager
{
	File[] sharedFiles;
	SocketThread mgrThread;
	
	public SyncManager()
	{
	}
	
	public void start(CmdLineParse cmd_line)
	{
		sharedDir = cmd_line.sharedDir;
		getLocalDir = cmd_line.getLocalDir;
		getRemoteDir = cmd_line.getRemoteDir;
		
		mgrThread = new SocketThread(this, cmd_line.socketType, 
				cmd_line.cmdAddress, cmd_line.cmdPort);
		mgrThread.start();
	}
	
	public void getSharedFiles()
	{
		sharedFiles = new File(sharedDir).listFiles();
	}
	
	public void processProtoMessage( FileSyncMessage msg )
	{
		switch(msg.getHeader().getMsgType())
		{
		case DIR_EXCHANGE:
			break;
		case HELLO_MSG:
			break;
		default:
			break;
		}
	}
	
	private String sharedDir;
	private String getLocalDir;
	private String getRemoteDir;
}
