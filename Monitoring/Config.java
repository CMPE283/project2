package Monitoring;

public class Config {
    public static String getVmwareHostURL() { return "https://130.65.157.177/sdk" ; }
    public static String getVmwareLogin() { return "administrator" ; }
    public static String getVmwarePassword() { return "12!@qwQW" ; }
    public static String[] getVmsForMonitoring() 
    {
    	return new String[] {
    			"SampleVM1"
    	}; 
    }
    public static String[] getAvailableResourcePools()
    {
    	return new String[] {
    			"rp1", "rp2"
    	};
    }

}
