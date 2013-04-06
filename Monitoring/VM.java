package Monitoring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
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
		this.vm = (VirtualMachine) vm;
	}
	
	public static VM[] getInventory() 
	{
		VM[] inventoryArray = new VM[inventory.size()];
		return (VM[]) inventory.toArray(inventoryArray);
	}
	
	public void printStatistics()
	{
		System.out.println("STATISTICS for " + vm.getName());
       	VirtualMachineConfigInfo vminfo = vm.getConfig();
    	GuestInfo guestInfo = vm.getGuest();
    	VirtualMachineSummary summary = vm.getSummary();
    	
    	System.out.println("GuestOS: " + vminfo.getGuestFullName());
    	System.out.println("CPU Allocation Limit: " + vminfo.getCpuAllocation().getLimit() + " MHz");
    	System.out.println("Memory Allocation Limit: " + vminfo.getMemoryAllocation().getLimit() + " MB");
    	System.out.println("IP Address: " + guestInfo.getIpAddress());
    	System.out.println("Hostname: " + guestInfo.getHostName());
    	System.out.println("Storage: " + summary.storage.committed + " Bytes");
	}
	
	public boolean isHostUp()
	{
		GuestInfo guestInfo = vm.getGuest();
		try {
			InetAddress inet = InetAddress.getByName(guestInfo.getIpAddress());
			return inet.isReachable(3200);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isHostDown()
	{
		return !isHostUp();
	}
	
	public void snapshot()
	{
		try {
			vm.createSnapshot_Task("current", String.valueOf(System.currentTimeMillis()) , false, true);
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
	}
	
	public void removeAllSnapshots()
	{
	//	System.out.println("INFO: Removing " + vm.getSnapshot().rootSnapshotList.length + " snapshot trees for " + vm.getName());
		try {
			vm.removeAllSnapshots_Task();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void rescue()
	{
		try {
			vm.revertToCurrentSnapshot_Task(null);
			Task migration = vm.migrateVM_Task((ResourcePool) getOtherResourcePool(), hostSystem, VirtualMachineMovePriority.highPriority, 
					VirtualMachinePowerState.poweredOff);
			
			if(migration.waitForTask() == Task.SUCCESS)
			{
				vm.powerOnVM_Task(null);
			}
									
		} catch (RemoteException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ManagedEntity getOtherResourcePool()
	{
		try {
			for(ManagedEntity resourcePool : VM.allResourcePools)
			{
				if(!resourcePool.getName().equals("Resources") && !resourcePool.getName().equals(vm.getResourcePool().getName()))
				{
					System.out.println("Migrating to " + resourcePool.getName());
					return resourcePool;					
				}
			}
			return vm.getResourcePool();		
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}
}







