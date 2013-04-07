package Monitoring;

import java.util.Timer;
import java.util.TimerTask;

public class Supervisor {

	public static void main(String[] args) {
		VM.buildInventory();

		System.out.println("Scheduling ALL tasks");
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
			for(VM vm : VM.getInventory())
				vm.printStatistics();			
		}
	}
	
	class SnapshotsTask extends TimerTask
	{
		public void run()
		{
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
			for(VM vm : VM.getInventory())
			{
				if(vm.isHostNotReachable())
					vm.rescue();
			}
		}
		
	}
}
