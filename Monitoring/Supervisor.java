package Monitoring;

import java.util.Timer;
import java.util.TimerTask;

public class Supervisor {

	public static void main(String[] args) {
		VM.buildInventory();

		System.out.println("Scheduling all tasks");
		Supervisor supervisor = new Supervisor();
		supervisor.scheduleAllTasks();
	}

	
	public void scheduleAllTasks()
	{
		Timer timer = new Timer();
		timer.schedule(new StatisticsTask() , 0, 5 * 60 * 1000);
		timer.schedule(new SnapshotsTask()  , 0, 30 * 60 * 1000);
		timer.schedule(new RescueTask()  , 0, 2 * 1000);
	}
	
	class StatisticsTask extends TimerTask
	{
		public void run()
		{
			VM.getInventory()[0].printStatistics();			
		}
	}
	
	class SnapshotsTask extends TimerTask
	{
		public void run()
		{
			VM.getInventory()[0].removeAllSnapshots();
			VM.getInventory()[0].snapshot();
		}
	}
	
	class RescueTask extends TimerTask
	{
		public void run()
		{
			System.out.println("UP? : " + VM.getInventory()[0].isHostUp());
			if(VM.getInventory()[0].isHostDown())
			{
				VM.getInventory()[0].rescue();
			}
		}
		
	}
}
