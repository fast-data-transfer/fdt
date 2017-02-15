/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * 
 * @author Ciprian Dobre
 */
public class HostPropertiesMonitor {
	
	final static String osName = System.getProperty("os.name");
	ProcReader reader = null;
	MacHostPropertiesMonitor macHostMonitor = null;

	static {
		if (osName.indexOf("Win")>=0) {
			saveSystemLibrary(HostPropertiesMonitor.class);
		} 
	}
	
	String macAddress;
	HashMap<String, Double> cpuvm;
	HashMap<String, Double> mem;
	HashMap<String, Double> disk;
	int processes;
	HashMap<String, Double> load;
	HashMap<String, Double> net;
	
	public static void saveSystemLibrary(Class baseClass) {
		try {
			URL url = baseClass.getResource("system.dll"); // here should be the system library package (for jni)
			File file = new File("system.dll");
			if (!file.exists()) {
				byte[] buffer = new byte[1024];
				URLConnection con = url.openConnection();
				InputStream in = con.getInputStream();
				FileOutputStream out = new FileOutputStream(file);
				int n;
				while ((n = in.read(buffer, 0, buffer.length)) != -1)
					out.write(buffer, 0, n);
				in.close();
				out.close();
			}
			System.load(file.getAbsolutePath());
		} catch (Exception ex) { 
			ex.printStackTrace();
		}
	}
	
	public HostPropertiesMonitor(final Logger logger) {
		if (osName.indexOf("Linux") != -1){
			reader = new ProcReader(logger);
		} else if (osName.indexOf("Mac") != -1){
			macHostMonitor = new MacHostPropertiesMonitor(logger);
		}
	}
	
	public native String getMacAddresses();
	
	public String getMacAddressesCall() {
		if (osName.indexOf("Linux") != -1) {
			return macAddress;
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getMacAddresses();
		return getMacAddresses();
	}
	
	public native void update();
	
	public void updateCall() {
		if (osName.indexOf("Linux") != -1) {
			try {
				macAddress = reader.getMACAddress();
				cpuvm = reader.getCPUVM();
				mem = reader.getMEM();
				disk = reader.getDISK();
				processes = reader.getProcesses();
				load = reader.getLOAD();
				net = reader.getNet();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return;
		} 
		if (osName.indexOf("Mac") != -1) {
			macHostMonitor.update();
			return;
		}
		update();
	}
	
	private final double get(String val) {
		try {
			return Double.parseDouble(val);
		} catch (Throwable t) { }
		return -1.0;
	}
	
	public native String getCpuUsage();
	
	public double getCpuUsageCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_Usage");
		}
		if (osName.indexOf("Mac") != -1) {
			return get(macHostMonitor.getCpuUsage());
		}
		return get(getCpuUsage());
	}

	public native String getCpuUSR();
	
	public double getCpuUSRCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_usr");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getCpuUSR());
		return get(getCpuUSR());
	}
	
	public native String getCpuSYS();
	
	public double getCpuSYSCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_sys");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getCpuSYS());
		return get(getCpuSYS());
	}
	
	public native String getCpuNICE();
	
	public double getCpuNICECall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_nice");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getCpuNICE());
		return get(getCpuNICE());
	}
	
	public native String getCpuIDLE();
	
	public double getCpuIDLECall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_idle");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getCpuIDLE());
		return get(getCpuIDLE());
	}
	
	public double getCPUIoWaitCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_iowait");
		} 
		return -1.0;
	}
	
	public double getCPUIntCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_int");
		}
		return -1.0;
	}
	
	public double getCPUSoftIntCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_softint");
		}
		return -1.0;
	}
	
	public double getCPUStealCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("CPU_steal");
		}
		return -1.0;
	}
	
	public native String getPagesIn();
	
	public double getPagesInCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("Page_in");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getPagesIn());
		return get(getPagesIn());
	}
	
	public native String getPagesOut();
	
	public double getPagesOutCall() {
		
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("Page_out");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getPagesOut());
		return get(getPagesOut());
	}
	
	/** Get swap in - works in linux flavors only */
	public double getSwapInCall() {
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("Swap_in");
		}
		return -1.0;
	}
	
	public double getSwapOutCall() {
		if (osName.indexOf("Linux") != -1) {
			if (cpuvm == null) return -1.0;
			return cpuvm.get("Swap_out");
		}
		return -1.0;
	}
	
	public native String getMemUsage();
	
	public double getMemUsageCall() {
		if (osName.indexOf("Linux") != -1) {
			if (mem == null) return -1.0;
			return mem.get("MemUsage");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getMemUsage());
		return get(getMemUsage());
	}
	
	public native String getMemUsed();
	
	public double getMemUsedCall() {
		if (osName.indexOf("Linux") != -1) {
			if (mem == null) return -1.0;
			return mem.get("MemUsed");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getMemUsed());
		return get(getMemUsed());
	}
	
	public native String getMemFree();
	
	public double getMemFreeCall() {
		if (osName.indexOf("Linux") != -1) {
			if (mem == null) return -1.0;
			return mem.get("MemFree");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getMemFree());
		return get(getMemFree());
	}
	
	public native String getDiskIO();
	
	public double getDiskIOCall() {
		if (osName.indexOf("Linux") != -1) {
			if (disk == null) return -1.0;
			return disk.get("DiskIO");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getDiskIO());
		return get(getDiskIO());
	}
	
	public native String getDiskTotal();
	
	public double getDiskTotalCall() {
		if (osName.indexOf("Linux") != -1) {
			if (disk == null) return -1.0;
			return disk.get("DiskTotal");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getDiskTotal());
		return get(getDiskTotal());
	}
	
	public native String getDiskUsed();
	
	public double getDiskUsedCall() {
		if (osName.indexOf("Linux") != -1) {
			if (disk == null) return -1.0;
			return disk.get("DiskUsed");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getDiskUsed());
		return get(getDiskUsed());
	}
	
	public native String getDiskFree();
	
	public double getDiskFreeCall() {
		if (osName.indexOf("Linux") != -1) {
			if (disk == null) return -1.0;
			return disk.get("DiskFree");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getDiskFree());
		return get(getDiskFree());
	}
	
	public native String getNoProcesses();
	
	public int getNoProcessesCall() {
		if (osName.indexOf("Linux") != -1) {
			return processes;
		}
		if (osName.indexOf("Mac") != -1)
			return (int)get(macHostMonitor.getNoProcesses());
		return (int)get(getNoProcesses());
	}
	
	public native String getLoad1();
	
	public double getLoad1Call() {
		if (osName.indexOf("Linux") != -1) {
			if (load == null) return -1.0;
			return load.get("Load1");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getLoad1());
		return get(getLoad1());
	}
	
	public native String getLoad5();
	
	public double getLoad5Call() {
		if (osName.indexOf("Linux") != -1) {
			if (load == null) return -1.0;
			return load.get("Load5");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getLoad5());
		return get(getLoad5());
	}
	
	public native String getLoad15();
	
	public double getLoad15Call() {
		if (osName.indexOf("Linux") != -1) {
			if (load == null) return -1.0;
			return load.get("Load15");
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getLoad15());
		return get(getLoad15());
	}
	
	public native String[] getNetInterfaces();
	
	public String[] getNetInterfacesCall() {
		if (osName.indexOf("Linux") != -1) {
			if (net == null) return null;
			final String[] ret = new String[net.size() / 2];
			int i=0;
			for (Iterator<String> it = net.keySet().iterator();it.hasNext() && i<ret.length;) {
				final String key = it.next();
				if (key.startsWith("In_")) {
					ret[i] = key.substring(3);
					i++;
				}
			}
			while (i < ret.length) ret[i++] = null;
			return ret;
		}
		if (osName.indexOf("Mac") != -1)
			return macHostMonitor.getNetInterfaces();
		return getNetInterfaces();
	}

	public native String getNetIn(String ifName);
	
	public double getNetInCall(String ifName) {
		
		if (osName.indexOf("Linux") != -1) {
			if (net == null || !net.containsKey("In_"+ifName)) return -1.0;
			return net.get("In_"+ifName);
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getNetIn(ifName));
		return get(getNetIn(ifName));
	}
	
	public native String getNetOut(String ifName);
	
	public double getNetOutCall(String ifName) {
		
		if (osName.indexOf("Linux") != -1) {
			if (net == null || !net.containsKey("Out_"+ifName)) return -1.0;
			return net.get("Out_"+ifName);
		}
		if (osName.indexOf("Mac") != -1)
			return get(macHostMonitor.getNetOut(ifName));
		return get(getNetOut(ifName));
	}

} // end of class HostPropertiesMonitor

