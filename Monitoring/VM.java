package Monitoring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.*;

public class VM {

	private VirtualMachine vm;
	public static ArrayList<VM> inventory = new ArrayList<VM>();
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
    	System.out.println("Storage: " + summary.storage.committed + "Bytes");
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
}







