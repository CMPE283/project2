package Monitoring;


import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.vmware.vim25.mo.*;

public class VM {

	public static ArrayList<ManagedEntity> inventory = new ArrayList<ManagedEntity>();
	public static void build_inventory()
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
         			inventory.add(vm);
        	}
        }
		System.out.println("DEBUG: Inventory: " + inventory);        
        System.out.println("INFO: Built inventory for " + inventory.size() + " VMs to be monitored.");
	}
}
