package Monitoring;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class VMStats {

	private String name;
	private Integer cpu;
	private Integer memory;
	private long timeUpdated;
	private Integer diskUsageAverage;  // kb/s 125
	private Integer diskReadAverage;   // kb/s 130
	private Integer diskWriteAverage;  // kb/s 131
	private Integer diskTotalLantency; // milliseconds 132
	private Integer netUsageAverage;   // kb/s 143
	private Integer datastoreReadAverage; // kb/s 180
	private Integer datastoreWriteAverage; // kb/s 181
	private Integer netBytesRxAverage; // kb/s 394
	private Integer netBytesTxAverage; // kb/s 395

	
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

	public String realTimeDiskUsageAverageStats() {
		return "283.realtime.diskUsageAverage." + name + " " + diskUsageAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeDiskReadAverageStats() {
		return "283.realtime.diskReadAverage." + name + " " + diskReadAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeDiskWriteAverageStats() {
		return "283.realtime.diskWriteAverage." + name + " " + diskWriteAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeDiskTotalLantencyStats() {
		return "283.realtime.diskTotalLantency." + name + " " + diskTotalLantency.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeNetUsageAverageStats() {
		return "283.realtime.netUsageAverage." + name + " " + netUsageAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeDatastoreReadAverageStats() {
		return "283.realtime.datastoreReadAverage." + name + " " + datastoreReadAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeDatastoreWriteAverageStats() {
		return "283.realtime.datastoreWriteAverage." + name + " " + datastoreWriteAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeNetBytesRxAverageStats() {
		return "283.realtime.netBytesRxAverage." + name + " " + netBytesRxAverage.toString() + " " + getTimeUpdated() + "\n";
	}
	
	public String realTimeNetBytesTxAverageStats() {
		return "283.realtime.netBytesTxAverage." + name + " " + netBytesTxAverage.toString() + " " + getTimeUpdated() + "\n";
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
	
	public Integer getDiskUsageAverage()
	{
		return diskUsageAverage;
	}
	public void setDiskUsageAverage(Integer diskUsgAvg)
	{
		this.diskUsageAverage = diskUsgAvg;
	}
	
	public Integer getDiskReadAverage()
	{
		return diskReadAverage;
	}
	public void setDiskReadAverage(Integer diskRdAvg)
	{
		this.diskReadAverage = diskRdAvg;
	}
	
	public Integer getDiskWriteAverage()
	{
		return diskWriteAverage;
	}
	public void setDiskWriteAverage(Integer diskWrtAvg)
	{
		this.diskWriteAverage = diskWrtAvg;
	}
	
	public Integer getDiskTotalLantency()
	{
		return diskTotalLantency;
	}
	public void setDiskTotalLantency(Integer totalLag)
	{
		this.diskTotalLantency = totalLag;
	}
	
	public Integer getNetUsageAverage()
	{
		return netUsageAverage;
	}
	public void setNetUsageAverage(Integer netUsage)
	{
		this.netUsageAverage = netUsage;
	}
	
	public Integer getDatastoreReadAverage()
	{
		return datastoreReadAverage;
	}
	public void setDatastoreReadAverage(Integer readAvg)
	{
		this.datastoreReadAverage = readAvg;
	}
	
	public Integer getDatastareWriteAverage()
	{
		return datastoreWriteAverage;
	}
	public void setDatastoreWriteAverage(Integer writeAvg)
	{
		this.datastoreWriteAverage = writeAvg;
	}
	
	public Integer getNetBytesRxAverage()
	{
		return netBytesRxAverage;
	}
	public void setNetBytesRxAverage(Integer RxAvg)
	{
		this.netBytesRxAverage = RxAvg;
	}
	
	public Integer getNetBytesTxAverage()
	{
		return netBytesTxAverage;
	}
	public void setNetBytesTxAverage(Integer TxAvg)
	{
		this.netBytesTxAverage = TxAvg;
	}
	
	public DBObject getMongoDoc() {
		BasicDBObject document = new BasicDBObject("name", name).
				append("cpu", cpu).
				append("memory", memory).
				append("diskUsageAverage", diskReadAverage).
				append("diskReadAverage", diskReadAverage).
				append("diskWriteAverage", diskWriteAverage).
				append("diskTotalLantency", diskTotalLantency).
				append("netUsageAverage", netUsageAverage).
				append("datastoreReadAverage", datastoreReadAverage).
				append("datastoreWriteAverage", datastoreWriteAverage).
				append("netBytesRxAverage", netBytesRxAverage).
				append("netBytesTxAverage", netBytesTxAverage).	
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
