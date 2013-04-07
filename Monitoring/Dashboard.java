package Monitoring;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

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
		 Calendar cal = Calendar.getInstance();
		 String preamble = String.format("MONITORING DASHBOARD\nUpdated on: %tc\n", cal);
		
		try {
			if (!file.exists()) {
				file.createNewFile();
			}			
			logger.info("Updating Dashboard");
			logger.trace(preamble + content);
			FileWriter fw;
			fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(preamble + content);
			bw.close();

		} catch (IOException e) {
			logger.error("Couldn't update dashboard");
			e.printStackTrace();
		}

	}
}
