package Monitoring;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

public class Dashboard {

	private File file;
	static Logger logger = Logger.getLogger("Monitoring283");

	public Dashboard(String filename)
	{
		file = new File(filename);		
	}
	
	public void update(String content)
	{
		try {
			if (!file.exists()) {
				file.createNewFile();
			}			
			logger.info("Updating Dashboard");
			FileWriter fw;
			fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

		} catch (IOException e) {
			logger.error("Couldn't upodate dashboard");
			e.printStackTrace();
		}

	}
}
