package Monitoring;

public class Config {
    public static String getVmwareHostURL() { return "https://130.65.157.14/sdk" ; }
    public static String getVmwareLogin() { return "administrator" ; }
    public static String getVmwarePassword() { return "12!@qwQW" ; }
    public static String getDashboardLocation() { return "dashboard.log" ; }
    
    public static String getCarbonHost()
    {
    	return "130.65.157.246";
    }
    
    public static Integer getCarbonPort()
    {
    	return new Integer(2003);
    }
    
    public static String getMongoHost()
    {
    	return "130.65.157.246";
    }
    
    public static Integer getMongoPort()
    {
    	return new Integer(27017);
    }
    
    
    public static String[] getVmsForMonitoring() 
    {
    	return new String[] {
    			"VM_1",
    			"VM_4",
    			"VM_2"
    	}; 
    }
    public static String[] getAvailableResourcePools()
    {
    	return new String[] {
    			"rp1", "rp2"
    	};
    }

}
