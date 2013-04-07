package Monitoring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.*;

public class VM {

	private VirtualMachine vm;
	public static ArrayList<VM> inventory = new ArrayList<VM>();
	public static ManagedEntity[] allResourcePools; 
	
	static Logger logger = Logger.getLogger("Monitoring283");
	
	public static void buildInventory()
	{
		logger.debug("Building Inventory");
		
		ServiceInstance si = null;
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
	
	public String getStatistics()
	{
		StringBuffer content = new StringBuffer();
		
		content.append("STATISTICS for " + getVm().getName());
       	VirtualMachineConfigInfo vminfo = getVm().getConfig();
    	GuestInfo guestInfo = getVm().getGuest();
    	VirtualMachineSummary summary = getVm().getSummary();
    	
    	content.append("GuestOS: " + vminfo.getGuestFullName());
    	content.append("CPU Allocation Limit: " + vminfo.getCpuAllocation().getLimit() + " MHz");
    	content.append("Memory Allocation Limit: " + vminfo.getMemoryAllocation().getLimit() + " MB");
    	content.append("IP Address: " + guestInfo.getIpAddress());
    	content.append("Hostname: " + guestInfo.getHostName());
    	content.append("Storage: " + summary.storage.committed + " Bytes");
    	
    	return content.toString();
	}
	
	public boolean isHostReachable()
	{
		GuestInfo guestInfo = getVm().getGuest();
		try {
			if(guestInfo.getIpAddress() == null){
				logger.warn("Couldn't retrieve IP address for host " + getVm().getName() + ". It could be powering up.");
				return true;
			}
			else
			{	
				InetAddress inet = InetAddress.getByName(guestInfo.getIpAddress());
				boolean reachable = (inet.isReachable(3200) || inet.isReachable(3200));
				logger.debug("Pinging IP: " + guestInfo.getIpAddress() + ". Reachable?: " + reachable);
				return reachable;
			}
		} catch (IOException e) {
			logger.error("Exception while testing host for reachability");
			return false;
		}
	}
		
	public boolean isHostNotReachable()
	{
		return !isHostReachable();
	}
	
	public void snapshot()
	{
		try {
			logger.info("Making new snapshot for " + getVm().getName());
			getVm().createSnapshot_Task("current", String.valueOf(System.currentTimeMillis()) , false, true).waitForTask();
		} catch (RemoteException | InterruptedException e) {
			logger.error("Couldn't create a snapshot");
			e.printStackTrace();
		}		
	}
	
	public void removeAllSnapshots()
	{
		logger.info("Removing all snapshot trees for" + getVm().getName());
		try {
			getVm().removeAllSnapshots_Task();
		} catch (RemoteException e) {
			logger.error("Couldn't remove previous snapshots");
			e.printStackTrace();
		}
	}

	public synchronized void rescue()
	{
		logger.info("Attempting to rescue " + getVm().getName());		
		try {
			ResourcePool targetResourcePool = selectTargetResourcePool();
			HostSystem targetHostSystem = targetResourcePool.getOwner().getHosts()[0];

			logger.info("Attempting to gracefully shutdown " + getVm().getName());
			getVm().shutdownGuest();
			Thread.sleep(15 * 1000); // since shutDownGuest() returns immediately
			
			if(getVm().getRuntime().getPowerState() == VirtualMachinePowerState.poweredOn)
			{
				logger.warn("Forcing shutdown for " + getVm().getName());
				getVm().powerOffVM_Task().waitForTask();
			}
			
			logger.info("Reverting " + getVm().getName() + " to most recent snapshot");
			getVm().revertToCurrentSnapshot_Task(null);
			
			logger.info("Migrating to ResourcePool " + targetResourcePool.getName() + " located on Host " + targetHostSystem.getName());
	
			Task migration = getVm().migrateVM_Task(targetResourcePool, 
					targetHostSystem, 
					VirtualMachineMovePriority.highPriority, 
					VirtualMachinePowerState.poweredOff);
			
			if(migration.waitForTask() == Task.SUCCESS)
				getVm().powerOnVM_Task(null).waitForTask();
		} catch (RemoteException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ResourcePool selectTargetResourcePool()
	{
		try {
			String currentPoolName = vm.getResourcePool().getName();
			String nextResourcePoolName = roundRobinNextResourcePool(currentPoolName);
			ResourcePool nextResourcePool = null;
		
			while(true)
			{
				if(nextResourcePoolName.equals(currentPoolName)){
					logger.error("No resource pools are available");
					break;
				}
				for(ManagedEntity rp : allResourcePools)
				{				
					if(rp.getName().equals(nextResourcePoolName))
						nextResourcePool = (ResourcePool) rp;						
				}
				if(nextResourcePool.getRuntime().getOverallStatus() == ManagedEntityStatus.red)
				{
					logger.warn("Red status encountered on " + nextResourcePool.getName() + ". Moving on the next ResourcePool");
					nextResourcePoolName = roundRobinNextResourcePool(nextResourcePoolName);
				}
				else
				{
					logger.debug("Status of ResourcePool " + nextResourcePool.getName() + " is " + nextResourcePool.getRuntime().getOverallStatus());
					break;
				}
			}
			
			logger.info("INFO: RoundRobin Selecting ResourcePool " + nextResourcePool.getName() + " as the target.");
			return nextResourcePool;
		} catch (RemoteException e) {
			logger.error("Couldn't select target resource pool");
			e.printStackTrace();
			return null;
		}
	}
	
	private String roundRobinNextResourcePool(String currentPool)
	{
		String pool = null;
		for(int i = 0; i < Config.getAvailableResourcePools().length ; i++)
		{
			if(Config.getAvailableResourcePools()[i].equals(currentPool))
			{
				if(i == Config.getAvailableResourcePools().length - 1)
					pool = Config.getAvailableResourcePools()[0];
				else
					pool = Config.getAvailableResourcePools()[i+1];					
			}
			
		}	
		return pool;
	}

	public VirtualMachine getVm() {
		return vm;
	}

	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}
}







