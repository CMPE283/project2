package Monitoring;

import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

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
	
	public VMStats refreshStatistics()
	{
    	VirtualMachineQuickStats quickStats = getVm().getSummary().getQuickStats();

    	statistics = new VMStats();
    	statistics.setName(getVm().getName());
    	statistics.setCpu(quickStats.getOverallCpuUsage());
    	statistics.setMemory(quickStats.getHostMemoryUsage());
    	    	
    	return statistics;
	}
	
	
	public boolean postStatisticsToCarbon(String hostname, Integer port)
	{
		logger.info("Attempting to send string: " + statistics.cpuStatsForMonitoring());
		
		try {
		Socket conn = new Socket(hostname, port.intValue());
		DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
		dos.writeBytes(statistics.cpuStatsForMonitoring());
		conn.close();
		}
		catch(Exception e)
		{
			logger.error("Error while submitting log entry");
			e.printStackTrace();
		}
		
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

}







