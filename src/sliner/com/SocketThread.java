package sliner.com;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.google.protobuf.CodedInputStream;

import sliner.com.FileSyncMsg.FileSyncHeader;
import sliner.com.FileSyncMsg.FileSyncHeader.FileSyncMessageType;
import sliner.com.FileSyncMsg.FileSyncHeader.FileSyncVersion;
import sliner.com.FileSyncMsg.FileSyncMessage;

public class SocketThread extends Thread
{
	Socket open_socket = null;
	DataInputStream sock_in = null;
	DataOutputStream sock_out = null;
	int buf_len = 1024;
	ByteBuffer msg_len = null;
	byte[] recv_buffer = null;
	int input_head;
	int input_bytes;
	SyncManager syncMgr = null;


	public SocketThread(SyncManager mgr, socketType type, String address, int port)
	{
		syncMgr = mgr;
		
		socketType = type;
		
		switch(socketType)
		{
		case SOCKET_SERVER:
			localAddress = address;
			localPort = port;
			break;
		case SOCKET_CLIENT:
			remoteAddress = address;
			remotePort = port;		
		}
		
		recv_buffer = new byte[buf_len];
		msg_len = ByteBuffer.allocate(4);
		input_head = 0;
		input_bytes = 0;
	}

	public void run()
	{
		
		while(true)
		{
			try 
			{
				switch(socketType)
				{
				case SOCKET_SERVER:
					open_socket = openServerSocket();
					break;
				case SOCKET_CLIENT:
					open_socket = openClientSocket();
					break;
				}			
			}
			catch (IllegalArgumentException e)
			{
				//port out of range
				System.out.println(e.getMessage());
			}
			catch (IOException e)
			{
				if( e instanceof UnknownHostException)
				{
					System.out.println("Failed to resolve given address, " + e.getMessage());
				}
				else
				{
					switch(socketType)
					{
					case SOCKET_SERVER:
						System.out.println("Listen/Accept failed, " + e.getMessage());
						break;
					case SOCKET_CLIENT:
						System.out.println("Connect failed, " + e.getMessage());
						break;
					}
				}
			}
			
			if(open_socket == null)
			{
				try
				{
					//sleep at least 5 seconds between connect attempts
					Thread.sleep(5000);
				}
				catch (InterruptedException e)
				{
					//don't care if it's interrupted
				}
				continue;
			}

			try
			{
				FileSyncMessage.Builder b_msg = FileSyncMessage.newBuilder();
				FileSyncHeader.Builder b_head = FileSyncHeader.newBuilder();
				b_head.setVersion(FileSyncVersion.VERSION_ONE);
				b_head.setMsgType(FileSyncMessageType.HELLO_MSG);

				b_msg.setHeader(b_head.build());
				FileSyncMessage msg = b_msg.build();
				ByteBuffer msg_size = 
					ByteBuffer.allocate(4).putInt(msg.getSerializedSize());

				sock_in = new DataInputStream(open_socket.getInputStream());
				sock_out = new DataOutputStream(open_socket.getOutputStream());
				
				msg_size.rewind();
				System.out.println("sending msg_size " + msg_size.getInt());
				
				for(int i = 0; i < 4; i++)
				{
					System.out.println("Sending bytes " + i + " is " + msg.toByteArray()[i]);
				}
				sock_out.write(msg_size.array());
				msg.writeTo(sock_out);
				
				System.out.println("Waiting for data");
				
				CodedInputStream new_msg = recvMessage();
				
				FileSyncMessage parsed_msg = FileSyncMessage.parseFrom(new_msg);
				
				System.out.println("Got msg " + 
						parsed_msg.getHeader().getMsgType() + " with version " +
						parsed_msg.getHeader().getVersion());
				try
				{
					Thread.sleep(3000);
				}
				catch (InterruptedException e)
				{
					//don't care if it's interrupted
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if(sock_in != null)
				{
					try
					{
						sock_in.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (open_socket != null)
				{
					try
					{
						open_socket.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public enum socketType
	{
		SOCKET_CLIENT,
		SOCKET_SERVER
	}
	
	private Socket openClientSocket() throws IOException
	{
		Socket client_sock = null;

		InetAddress remote_address = InetAddress.getByName(remoteAddress);

		System.out.print("Trying to connect to ");
		System.out.print(remote_address.getHostAddress());
		System.out.println(", " + remotePort);
		client_sock = new Socket(remote_address, remotePort);
		
		if( client_sock.isConnected() )
		{
			System.out.println("Client connected to " + client_sock.getRemoteSocketAddress());
		}
		
		return client_sock;
	}

	private Socket openServerSocket() throws IOException
	{
		ServerSocket server_sock = null;
		Socket accept_sock = null;
		
		InetAddress local_address = InetAddress.getByName(localAddress);
		
		server_sock = new ServerSocket(localPort, 1, local_address);
		
		System.out.print("Listening for connection on ");
		System.out.print(local_address.getHostAddress());
		System.out.println(", " + localPort);
		
		accept_sock = server_sock.accept();
		
		System.out.println("Server connected to " + accept_sock.getRemoteSocketAddress());

		server_sock.close();
		return accept_sock;
	}

	private CodedInputStream recvMessage() throws IOException
	{
		int length = 0;
		int msg_len_bytes = msg_len.capacity();

		//First get the 4 bytes of the message length
		recvBytes(msg_len_bytes);
		
		for(int i =0; i < input_bytes + 1; i++)
		{
			System.out.println("recv_buffer " + i + " is " + recv_buffer[i]);
		}

		//convert the message length bytes to an integer
		msg_len.rewind();
		msg_len.put(recv_buffer, input_head, msg_len_bytes);
		msg_len.rewind();
		System.out.println("recv_index at " + input_head + " recv_bytes at " + input_bytes);
		length = msg_len.getInt();
		
		//Adjust input head past length bytes to beginning of message
		input_bytes -= msg_len_bytes;
		input_head += msg_len_bytes;

		//Make sure the message can fit into our buffer
		if(length > buf_len)
		{
			System.out.println("Length to big for buffer");
			return null;
		}
		
		//Get the bytes for the message
		recvBytes(length);
		
		//Convert the bytes to a stream so we can parse just the parts of
		//receive array that we need for a single message
		CodedInputStream coded_stream = 
				CodedInputStream.newInstance(recv_buffer, input_head, length);
		
		input_bytes -= length;
		if(input_bytes == 0)
		{
			input_head = 0;
		}
		else
		{
			input_head += length;
			System.out.println("Recv index is at " + input_head);
		}
		
		return coded_stream;
	}
	
	private void recvBytes(int bytes) throws IOException
	{
		if( input_bytes >= bytes)
		{
			//do nothing because we already have the bytes needed for the request
			return;
		}
		
		int read_head = input_head + input_bytes;
		int space_left = recv_buffer.length - read_head;
		
		if(space_left < bytes)
		{
			System.out.println("Have to shuffle recv bytes");
			for(int i = 0; i < input_bytes; ++i)
			{
				recv_buffer[i] = recv_buffer[i + input_head];
			}
			
			input_head = 0;
			read_head = input_head + input_bytes;
			space_left = recv_buffer.length - read_head;
		}
		
		while(input_bytes < bytes)
		{			
			read_head = input_head + input_bytes;
			space_left = recv_buffer.length - read_head;
			input_bytes += sock_in.read(recv_buffer, read_head, space_left);
		}
	}

	private socketType socketType;
	private String remoteAddress;
	private int remotePort;
	private String localAddress;
	private int localPort;
}
