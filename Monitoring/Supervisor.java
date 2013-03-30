package Monitoring;

public class Supervisor {

	public static void main(String[] args) {
		VM.buildInventory();
		VM.getInventory()[0].printStatistics();
		System.out.println("Reachable? : " + VM.getInventory()[0].isHostUp());
	}

}
