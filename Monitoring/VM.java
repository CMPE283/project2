package Monitoring;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.mongodb.AggregationOutput;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VM {

	private VirtualMachine vm;
	public static ArrayList<VM> inventory = new ArrayList<VM>();
	public static ManagedEntity[] allResourcePools; 
	static ServiceInstance si = null;

	
	static Logger logger = Logger.getLogger("Monitoring283");
	private VMStats statistics;
	
	public static void buildInventory()
	{
		logger.debug("Building Inventory");
		
		try {
			si = new ServiceInstance(new URL(Config.getVmwareHostURL()),
					Config.getVmwareLogin(), 
					Config.getVmwarePassword(), 
					true);
		} catch (RemoteException | MalformedURLException e) {
			e.printStackTrace();
		}
        
        Folder rootFolder = si.getRootFolder();
        ManagedEntity[] allVirtualMachines = null;
		try {
			allVirtualMachines = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
			allResourcePools = new InventoryNavigator(rootFolder).searchManagedEntities("ResourcePool");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
        if(allVirtualMachines == null || allVirtualMachines.length == 0)
        {
        	logger.error("Couldn't Retrieve VMs");
            return;
        }

        logger.debug("Initilizing inventory for monitoring");
        logger.debug("Fetched information for " + allVirtualMachines.length + " VMs");

        for(ManagedEntity vm : allVirtualMachines)
        {
        	for(String monitoredVM : Config.getVmsForMonitoring())
        	{
        		if(vm.getName().equals(monitoredVM))
         			inventory.add(new VM(vm));
        	}
        }
                
		logger.debug("Inventory: " + inventory);        
        logger.info("Built inventory for " + inventory.size() + " VMs to be monitored.");
	}
	
		
	public VM(ManagedEntity vm){
		this.setVm((VirtualMachine) vm);
	}
	
	public static VM[] getInventory() 
	{
		VM[] inventoryArray = new VM[inventory.size()];
		return (VM[]) inventory.toArray(inventoryArray);
	}
	
	public VMStats refreshRealTimeStatistics()
	{
    	VirtualMachineQuickStats quickStats = getVm().getSummary().getQuickStats();

    	statistics = new VMStats();
    	statistics.setName(getVm().getName());
    	statistics.setCpu(quickStats.getOverallCpuUsage());
    	statistics.setMemory(quickStats.getHostMemoryUsage());
    	statistics.setTimeUpdated(System.currentTimeMillis() / 1000L);
    	    	
    	return statistics;
	}
	
		
	public boolean saveStatistics(DB db)
	{
		DBCollection collection = db.getCollection(getVm().getName());
		collection.insert(statistics.getMongoDoc());
		return true;
	}
	
	
				
	public VirtualMachine getVm() {
		return vm;
	}
	
	public static ServiceInstance getSi(){
		return si;
	}

	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}


	public VMStats getStatistics() {
		return statistics;
	}


	public void setStatistics(VMStats statistics) {
		this.statistics = statistics;
	}


	public Iterable<DBObject> aggregateStatistics(DB db, String field, Long delta) {
		DBCollection collection = db.getCollection(getVm().getName());

		DBObject match = new BasicDBObject("$match", new BasicDBObject("time", new BasicDBObject("$gt", delta)));
		
		DBObject fields = new BasicDBObject(field, 1);
		fields.put("_id", 0);
		DBObject project = new BasicDBObject("$project", fields);
		
		DBObject groupFields = new BasicDBObject( "_id", "$vm");
		groupFields.put("average", new BasicDBObject( "$avg", "$" + field));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		AggregationOutput aggregationOutput = collection.aggregate(match, project, group);
		logger.info(aggregationOutput);
		
		return aggregationOutput.results();
	}
	
	public String[] getAllAggregatedStatistics(DB db, Long delta, String label) {
		String [] allFields = {"cpu", "memory"};
		List<String> allMessages = new ArrayList<String>();
		
		for(String field : allFields){
			Iterable<DBObject> results = aggregateStatistics(db, field, delta);
			for(DBObject result : results){
				logger.info(field + ": (average: " + result.get("average") + ")");
				allMessages.add("283.aggregated." + getVm().getName() + "." + label + "." + field + " " + result.get("average") + " " + System.currentTimeMillis() / 1000L + "\n");
			}
		}
		
		String [] allStatistics = new String[allMessages.size()];
		return allMessages.toArray(allStatistics);
	}


	public String[] getAllRealTimeStatistics() {
		String [] allStats = {statistics.realTimeCpuStats(), statistics.realTimeMemoryStats()};
		return allStats;
	}

}







