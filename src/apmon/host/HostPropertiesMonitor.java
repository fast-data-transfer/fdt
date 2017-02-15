package apmon.host;

import java.util.HashMap;
import java.util.Hashtable;

public class HostPropertiesMonitor {

	final static String osName = System.getProperty("os.name");
	static ProcReader reader = null;
	static MacHostPropertiesMonitor macHostMonitor = null;
	
	static {
//		if (System.getProperty("os.name").indexOf("Linux") == -1 && System.getProperty("os.name").indexOf("Mac") == -1) {
//			System.loadLibrary("system");
//		} else  
		if (System.getProperty("os.name").indexOf("Linux") != -1){
			reader = new ProcReader();
		} else {
			macHostMonitor = new MacHostPropertiesMonitor();
		}
	}
	
    public HashMap getHashParams() {
        if (osName.indexOf("Linux") != -1) {
            return reader.getHashedValues();
        }
        
        return null;
    }

	public String getMacAddressesCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getMacAddress();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getMacAddresses();
		return "";
	}
	
	public void updateCall() {

		if (osName.indexOf("Linux") != -1) {
			reader.update();
			return;
		} 
		if (osName.indexOf("Mac") != -1) {
			macHostMonitor.update();
			return;
		}
	}
	
	public String getCpuUsageCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getCPUUsage();
		}
		if (osName.indexOf("Mac") != -1) {
			return macHostMonitor.getCpuUsage();
		}
		return "";
	}

	public String getCpuUSRCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getCPUUsr();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getCpuUSR();
		return "";
	}
	
	
	public String getCpuSYSCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getCPUSys();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getCpuSYS();
		return null;
	}
	
	public String getCpuNICECall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getCPUNice();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getCpuNICE();
		return null;
	}
	
	
	public String getCpuIDLECall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getCPUIdle();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getCpuIDLE();
		return null;
	}
	
	
	public String getPagesInCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getPagesIn();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getPagesIn();
		return null;
	}
	
	
	public String getPagesOutCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getPagesOut();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getPagesOut();
		return null;
	}
	
	
	public String getMemUsageCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getMemUsage();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getMemUsage();
		return null;
	}
	
	
	public String getMemUsedCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getMemUsed();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getMemUsed();
		return null;
	}
	
	
    public String getMemTotalCall() {
        
        if (osName.indexOf("Linux") != -1) {
            return reader.getMemTotal();
        }
        return null;
    }


    public String getSwapFreeCall() {
        if (osName.indexOf("Linux") != -1) {
            return reader.getSwapFree();
        }
        
        return null;
    }
    
    public String getSwapTotalCall() {
        if (osName.indexOf("Linux") != -1) {
            return reader.getSwapTotal();
        }
        
        return null;
    }
    

    public String getMemFreeCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getMemFree();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getMemFree();
		return null;
	}
	
	
	public String getDiskIOCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getDiskIO();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getDiskIO();
		return null;
	}
	
	
	public String getDiskTotalCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getDiskTotal();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getDiskTotal();
		return null;
	}
	
	public String getDiskUsedCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getDiskUsed();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getDiskUsed();
		return null;
	}
	
	
	public String getDiskFreeCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getDiskFree();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getDiskFree();
		return null;
	}
	
	
	public String getNoProcessesCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getNoProcesses();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getNoProcesses();
		return null;
	}
	
	public String getLoad1Call() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getLoad1();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getLoad1();
		return null;
	}
	
	public String getLoad5Call() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getLoad5();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getLoad5();
		return null;
	}
	
	public String getLoad15Call() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getLoad15();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getLoad15();
		return null;
	}
	
	public String[] getNetInterfacesCall() {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getNetInterfaces();
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getNetInterfaces();
		return null;
	}

	public String getNetInCall(String ifName) {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getNetIn(ifName);
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getNetIn(ifName);
		return null;
	}
	
	public String getNetOutCall(String ifName) {
		
		if (osName.indexOf("Linux") != -1) {
			return reader.getNetOut(ifName);
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getNetOut(ifName);
		return null;
	}
    
    public Hashtable getPState() {
        
        if (osName.indexOf("Linux") != -1) {
            return reader.getProcessesState();
        }
        if (osName.indexOf("Mac") != -1)
            return macHostMonitor.getProcessesState();
        return null;        
    }
    
    public Hashtable getSockets() {
        
        if (osName.indexOf("Linux") != -1) {
            return reader.getNetSockets();
        }
        if (osName.indexOf("Mac") != -1)
            return macHostMonitor.getNetSockets();
        return null;        
    }
    
    public Hashtable getTCPDetails() {
        
        if (osName.indexOf("Linux") != -1) {
            return reader.getTcpDetails();
        }
        if (osName.indexOf("Mac") != -1)
            return macHostMonitor.getTcpDetails();
        return null;        
    }
	
	public void stopIt() {
		reader.stopIt();
	}
	
} // end of class HostPropertiesMonitor

