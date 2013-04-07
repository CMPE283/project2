package Monitoring;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Supervisor {
	static Logger logger = Logger.getLogger("Monitoring283");
	static Dashboard dashboard = new Dashboard(Config.getDashboardLocation());
	static Timer timer = new Timer();

	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		logger.info("Initializing...");

		Supervisor supervisor = new Supervisor();
		supervisor.setupGracefulExit();
				
		VM.buildInventory();

		logger.info("Scheduling ALL tasks");
		supervisor.scheduleAllTasks();
	}
	
	public void scheduleAllTasks()
	{
		timer.schedule(new StatisticsTask() , 0,  5 * 1000);
//		timer.schedule(new SnapshotsTask()  , 0, 30 * 60 * 1000);
//		timer.schedule(new RescueTask()  , 0, 2 * 1000);
	}
	
	class StatisticsTask extends TimerTask
	{
		public void run()
		{
			logger.debug("StatisticsTask woke up");
			for(VM vm : VM.getInventory())
				dashboard.update(vm.getStatistics());			
		}
	}
	
	class SnapshotsTask extends TimerTask
	{
		public void run()
		{
			logger.debug("SnapshotsTask woke up");
			for(VM vm : VM.getInventory())
			{
				vm.removeAllSnapshots();
				vm.snapshot();
			}
		}
	}
	
	class RescueTask extends TimerTask
	{
		public void run()
		{
			logger.debug("RescueTask woke up");
			for(VM vm : VM.getInventory())
			{
				if(vm.isHostNotReachable())
					vm.rescue();
			}
		}
		
	}
	
	public void setupGracefulExit()
	{
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	logger.info("Purging all scheduled tasks");
		    	timer.purge();
		    	logger.info("Cancelling all scheduled tasks");
		    	timer.cancel();
		    	logger.info("Exiting the monitoring application");		    	
		    }
		 });
	}
}
