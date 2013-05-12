package Monitoring;

import java.io.DataOutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

public class Carbon {
	public Carbon(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}

	static Logger logger = Logger.getLogger("Monitoring283");
	private String host;
	private int port;

	
	
	public void dispatchStatistics(String [] allTheStatistics)
	{
		for(String s : allTheStatistics)
			dispatchStatistics(s);
	}
	
	public void dispatchStatistics(String statistics)
	{
		logger.info("Attempting to send string: " + statistics);
		try {
			Socket connection = new Socket(host, port);
			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
			dos.writeBytes(statistics);
			connection.close();
		}
		catch(Exception e)
		{
			logger.error("Error while submitting statistics to Carbon");
			e.printStackTrace();
		}
	}
}
