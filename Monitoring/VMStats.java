package Monitoring;

public class VMStats {

	private String name;
	private Integer cpu;
	private Integer memory;

	
	public String toString()
	{
		return "[" + name + "] (cpu: " + cpu + " MHz) (memory: " + memory + " MB)\n";		
	}
	
	public String cpuStatsForMonitoring()
	{
		return "283.cpu." + name + " " + cpu.toString() + " " + System.currentTimeMillis() / 1000L + "\n";
	}

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getCpu() {
		return cpu;
	}

	public void setCpu(Integer cpu) {
		this.cpu = cpu;
	}

	public Integer getMemory() {
		return memory;
	}

	public void setMemory(Integer memory) {
		this.memory = memory;
	}

}
