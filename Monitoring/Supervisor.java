package Monitoring;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class Supervisor {
	static Logger logger = Logger.getLogger("Monitoring283");
	static Dashboard dashboard = new Dashboard(Config.getDashboardLocation());
	static Carbon carbon = new Carbon(Config.getCarbonHost(), Config.getCarbonPort().intValue());
	static Timer timer = new Timer();
	private DB db;

	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		logger.info("Initializing...");

		Supervisor supervisor = new Supervisor();
		supervisor.setupGracefulExit();
		
		supervisor.connectToMongoDB();
		
				
		VM.buildInventory();
		
		logger.info("Scheduling ALL tasks");
		supervisor.scheduleAllTasks();
	}
	
	private void connectToMongoDB() {
		try {
			MongoClient mongoClient = new MongoClient(Config.getMongoHost(), Config.getMongoPort().intValue());
			db = mongoClient.getDB("283");			
		} catch (Exception e) {
			logger.error("Error connecting to MongoDB");
			e.printStackTrace();
		}		
	}

	public void scheduleAllTasks()
	{
		timer.schedule(new RealTimeStatisticsTask() , 0, 10 * 1000);
		timer.schedule(new FifteenMinuteStatisticsAggregationTask() , 0, 15 * 60 * 1000);
		timer.schedule(new HourlyStatisticsAggregationTask(), 0, 60 * 60 * 1000);
		timer.schedule(new DailyStatisticsAggregationTask(), 0, 24 * 60 * 60 * 1000);
	}
	
	class RealTimeStatisticsTask extends TimerTask
	{
		public void run()
		{
			logger.trace("StatisticsTask woke up");
			
			for(VM vm : VM.getInventory())
			{
				vm.refreshRealTimeStatistics();
				carbon.dispatchStatistics(vm.getAllRealTimeStatistics());
				vm.saveStatistics(db);
			}
		}
	}
	
	class FifteenMinuteStatisticsAggregationTask extends TimerTask
	{
		public void run()
		{
			Long fifteenMinutesAgo = (System.currentTimeMillis() / 1000L) - (15 * 60);
			
			for(VM vm : VM.getInventory())
			{
				carbon.dispatchStatistics(vm.getAllAggregatedStatistics(db, fifteenMinutesAgo, "fifteen_minute"));
			}
		}
	}
		
	class HourlyStatisticsAggregationTask extends TimerTask
	{
		public void run()
		{
			Long oneHourAgo = (System.currentTimeMillis() / 1000L) - (60 * 60);
			
			for(VM vm : VM.getInventory())
			{
				carbon.dispatchStatistics(vm.getAllAggregatedStatistics(db, oneHourAgo, "hourly"));
			}
		}
	}
		
	class DailyStatisticsAggregationTask extends TimerTask
	{
		public void run()
		{
			Long oneDayAgo = (System.currentTimeMillis() / 1000L) - (24 * 60 * 60);
			
			for(VM vm : VM.getInventory())
			{
				carbon.dispatchStatistics(vm.getAllAggregatedStatistics(db, oneDayAgo, "daily"));
			}
		}
	}
		

	
	
	public void setupGracefulExit()
	{
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	logger.info("Closing the connection with the ESXi Server");
		    	VM.getSi().getServerConnection().logout();
		    	logger.info("Purging all scheduled tasks");
		    	timer.purge();
		    	logger.info("Cancelling all scheduled tasks");
		    	timer.cancel();
		    	logger.info("Exiting the monitoring application");		    	
		    }
		 });
	}
}
