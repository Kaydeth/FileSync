package sliner.com;

public class FileData
{
	FileData(String file_name, String absolute_path, String file_size,
		String time_stamp)
	{
		fileName = file_name;
		absolutePath = absolute_path;
		fileSize = file_size;
		timeStamp = time_stamp;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public String getAbsolutePath()
	{
		return absolutePath;
	}
	
	public String getFileSize()
	{
		return fileSize;
	}
	
	public String getTimeStamp()
	{
		return timeStamp;
	}
	
	private String fileName;
	private String absolutePath;
	private String fileSize;
	private String timeStamp;
}
