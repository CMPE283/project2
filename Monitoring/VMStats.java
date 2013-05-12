package Monitoring;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class VMStats {

	private String name;
	private Integer cpu;
	private Integer memory;
	private long timeUpdated;

	
	public String toString()
	{
		return "[" + name + "] (cpu: " + cpu + " MHz) (memory: " + memory + " MB)\n";		
	}
	
	public String realTimeCpuStats()
	{
		return "283.realtime.cpu." + name + " " + cpu.toString() + " " + getTimeUpdated() + "\n";
	}

	public String realTimeMemoryStats() {
		return "283.realtime.memory." + name + " " + memory.toString() + " " + getTimeUpdated() + "\n";
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

	public DBObject getMongoDoc() {
		BasicDBObject document = new BasicDBObject("name", name).
				append("cpu", cpu).
				append("memory", memory).
				append("time", timeUpdated);	
		
		return document;
	}

	public long getTimeUpdated() {
		return timeUpdated;
	}

	public void setTimeUpdated(long timeUpdated) {
		this.timeUpdated = timeUpdated;
	}


}
