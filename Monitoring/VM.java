package Monitoring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

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
	public static HostSystem hostSystem;
	
	public static void buildInventory()
	{
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
			
		    hostSystem = (HostSystem) new InventoryNavigator(
		            rootFolder).searchManagedEntity(
		                "HostSystem", "dummyHost");
			
		} catch (RemoteException e) {
			e.printStackTrace();
		}
        if(allVirtualMachines == null || allVirtualMachines.length == 0)
        {
        	System.out.println("ERROR: Couldn't Retrieve VMs");
            return;
        }

        System.out.println("DEBUG: Initilizing inventory for monitoring");
        System.out.println("DEBUG: Fetched information for " + allVirtualMachines.length + " VMs");

        for(ManagedEntity vm : allVirtualMachines)
        {
        	for(String monitoredVM : Config.getVmsForMonitoring())
        	{
        		if(vm.getName().equals(monitoredVM))
         			inventory.add(new VM(vm));
        	}
        }
                
		System.out.println("DEBUG: Inventory: " + inventory);        
        System.out.println("INFO: Built inventory for " + inventory.size() + " VMs to be monitored.");
	}
	
	public VM(ManagedEntity vm){
		this.setVm((VirtualMachine) vm);
	}
	
	public static VM[] getInventory() 
	{
		VM[] inventoryArray = new VM[inventory.size()];
		return (VM[]) inventory.toArray(inventoryArray);
	}
	
	public void printStatistics()
	{
		System.out.println("STATISTICS for " + getVm().getName());
       	VirtualMachineConfigInfo vminfo = getVm().getConfig();
    	GuestInfo guestInfo = getVm().getGuest();
    	VirtualMachineSummary summary = getVm().getSummary();
    	
    	System.out.println("GuestOS: " + vminfo.getGuestFullName());
    	System.out.println("CPU Allocation Limit: " + vminfo.getCpuAllocation().getLimit() + " MHz");
    	System.out.println("Memory Allocation Limit: " + vminfo.getMemoryAllocation().getLimit() + " MB");
    	System.out.println("IP Address: " + guestInfo.getIpAddress());
    	System.out.println("Hostname: " + guestInfo.getHostName());
    	System.out.println("Storage: " + summary.storage.committed + " Bytes");
	}
	
	public boolean isHostReachable()
	{
		GuestInfo guestInfo = getVm().getGuest();
		try {
			if(guestInfo.getIpAddress() == null){
				System.out.println("Couldn't retrieve IP address for host " + getVm().getName() + ". It could be powering up.");
				return true;
			}
			else
			{	
				InetAddress inet = InetAddress.getByName(guestInfo.getIpAddress());
				boolean reachable = (inet.isReachable(3200) || inet.isReachable(3200));
				System.out.println("DEBUG: Pinging IP: " + guestInfo.getIpAddress() + ". Reachable?: " + reachable);
				return reachable;
			}
		} catch (IOException e) {
			System.out.println("Exception while testing host for reachability");
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
			System.out.println("INFO: Making new snapshot for " + getVm().getName());
			getVm().createSnapshot_Task("current", String.valueOf(System.currentTimeMillis()) , false, true).waitForTask();
		} catch (RemoteException | InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public void removeAllSnapshots()
	{
		System.out.println("INFO: Removing all snapshot trees for" + getVm().getName());
		try {
			getVm().removeAllSnapshots_Task();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public synchronized void rescue()
	{
		System.out.println("Attempting to rescue " + getVm().getName());		
		try {
			ResourcePool targetResourcePool = selectTargetResourcePool();
			HostSystem targetHostSystem = targetResourcePool.getOwner().getHosts()[0];

			System.out.println("INFO: Attempting to gracefully shutdown " + getVm().getName());
			getVm().shutdownGuest();
			Thread.sleep(15 * 1000); // since shutDownGuest() returns immediately
			
			if(getVm().getRuntime().getPowerState() == VirtualMachinePowerState.poweredOn)
			{
				System.out.println("INFO: Forcing shutdown for " + getVm().getName());
				getVm().powerOffVM_Task().waitForTask();
			}
			
			System.out.println("INFO: Reverting " + getVm().getName() + " to most recent snapshot");
			getVm().revertToCurrentSnapshot_Task(null);
			
			System.out.println("INFO: Migrating to ResourcePool " + targetResourcePool.getName() + " located on Host " + targetHostSystem.getName());
	
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
					System.out.println("ERROR: No resource pools are available");
					break;
				}
				for(ManagedEntity rp : allResourcePools)
				{				
					if(rp.getName().equals(nextResourcePoolName))
						nextResourcePool = (ResourcePool) rp;						
				}
				if(nextResourcePool.getRuntime().getOverallStatus() == ManagedEntityStatus.red)
				{
					System.out.println("WARN: Red status encountered on " + nextResourcePool.getName());
					nextResourcePoolName = roundRobinNextResourcePool(nextResourcePoolName);
				}
				else
				{
					System.out.println("INFO: Status of ResourcePool " + nextResourcePool.getName() + " is " + nextResourcePool.getRuntime().getOverallStatus());
					break;
				}
			}
			
			System.out.println("INFO: RoundRobin Selecting ResourcePool " + nextResourcePool.getName() + " as the target.");
			return nextResourcePool;
		} catch (RemoteException e) {
			System.out.println("FATAL: Couldn't select resource pool");
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







