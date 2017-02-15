
package apmon.host;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;




public class MacHostPropertiesMonitor {

	protected String[] networkInterfaces;
	protected String activeInterface;
	protected String cpuUsage = "0";
	protected String cpuUSR = "0";
	protected String cpuSYS = "0";
	protected String cpuIDLE = "0";
	protected String nbProcesses = "0";
	protected String load1 = "0";
	protected String load5 = "0";
	protected String load15 = "0";
	protected String memUsed = "0";
	protected String memFree = "0";
	protected String memUsage = "0";
	protected String netIn = "0";
	protected String netOut = "0";
	protected String pagesIn = "0";
	protected String pagesOut = "0";
	protected String macAddress = "unknown";
	protected String diskIO = "0";
	protected String diskIn = "0";
	protected String diskOut = "0";
	protected String diskFree = "0";
	protected String diskUsed = "0";
	protected String diskTotal = "0";
	protected String command = "";
	protected static Object lock = new Object();
	protected static int ptnr = 0;
	protected cmdExec execute = null;
	protected String sep = null;
    private Parser parser = null;
    protected Hashtable netSockets = null;
    protected Hashtable netTcpDetails = null;
	
	public MacHostPropertiesMonitor() {

		parser = new Parser();
        execute = new cmdExec();
		execute.setTimeout(3 * 1000);
		sep = System.getProperty("file.separator");
		
		command = sep + "sbin" + sep	+ "ifconfig -l -u";
		String result = execute.executeCommand(command, "lo0");
		
		if (result == null || result.equals("")) {
			System.out.println(command + ": No result???");
		} else {
			int where = result.indexOf("lo0");
			networkInterfaces =
				result
					.substring(where + 3, result.length())
					.replaceAll("  ", " ")
					.trim()
					.split(
					" ");

			
			for (int i = 0; i < networkInterfaces.length; i++) {
				String current = networkInterfaces[i];
				command = sep + "sbin" + sep + "ifconfig " + current;
				result = execute.executeCommand(command, current);
				
				if (result == null || result.equals("")) {
					System.out.println(command + ": No result???");
				} else {
					if (result.indexOf("inet ") != -1) {
						int pointI = result.indexOf("ether");
						int pointJ = result.indexOf("media", pointI);
						macAddress =
							result.substring(pointI + 5, pointJ).trim();
						
						activeInterface = current;
					}
				}
			}
		}

		
		command = sep + "bin" + sep + "df -k -h "	+ sep;
		result = execute.executeCommand(command, "/dev");
		
		if (result == null || result.equals("")) {
			System.out.println(command + ": No result???");
		} else {
			parseDf(result);
		}

		update();
	}

	public String getMacAddresses() {
		return macAddress;
	}

	public void update() {

		if (execute == null) {
			execute = new cmdExec();
			execute.setTimeout(100 * 1000);
		}

		
		command = sep + "usr" + sep + "bin" + sep
				+ "top -d -l2 -n1 -F -R -X";
		String result = execute.executeCommand(command, "PID", 2);
		
		if (result == null || result.equals("")) {
			System.out.println("No result???");
		} else {
			parseTop(result);
		}
	}

	private void parseIfConfig(String toParse) {

		
		if (toParse.indexOf("inet ") != -1) {
			int pointI = toParse.indexOf("ether");
			int pointJ = toParse.indexOf("media", pointI);
			macAddress = toParse.substring(pointJ + 5, pointI).trim();
			System.out.println("Mac Address:" + macAddress);
		}
	}

	private void parseDf(String toParse) {

		

		int pointI = toParse.indexOf("/dev/");
		int pointJ = 0;
		int pointK = 0;

		
		try {
			pointJ = toParse.indexOf(" ", pointI);
			pointK = indexOfUnitLetter(toParse, pointJ);
			diskTotal = toParse.substring(pointJ, pointK).trim();
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
		try {
			pointI = toParse.indexOf(" ", pointK);
			pointJ = indexOfUnitLetter(toParse, pointI);
			diskUsed = toParse.substring(pointI, pointJ).trim();
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
		try {
			pointK = toParse.indexOf(" ", pointJ);
			pointI = indexOfUnitLetter(toParse, pointK);
			diskFree = toParse.substring(pointK, pointI).trim();
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
	}

	private int indexOfUnitLetter(String inside, int from) {

		int temp = inside.indexOf('K', from);
		if (temp == -1 || (temp - from > 10)) {
			temp = inside.indexOf('M', from);
			if (temp == -1 || (temp - from > 10)) {
				temp = inside.indexOf('G', from);
				if (temp == -1 || (temp - from > 10)) {
					temp = inside.indexOf('B', from);
					if (temp == -1 || (temp - from > 10)) {
						temp = inside.indexOf('T', from);
						if (temp == -1 || (temp - from > 10)) {
							temp = inside.indexOf('b', from);
							if (temp - from > 10)
								temp = -1;
						}
					}
				}
			}
		}
		return temp;
	}

	private int lastIndexOfUnitLetter(String inside, int from) {

		int temp = inside.lastIndexOf('K', from);
		if (temp == -1 || (from - temp > 10)) {
			temp = inside.lastIndexOf('M', from);
			if (temp == -1 || (from - temp > 10)) {
				temp = inside.lastIndexOf('G', from);
				if (temp == -1 || (from - temp > 10)) {
					temp = inside.lastIndexOf('B', from);
					if (temp == -1 || (from - temp > 10)) {
						temp = inside.lastIndexOf('T', from);
						if (temp == -1 || (from - temp > 10)) {
							temp = inside.lastIndexOf('b', from);
							if (from - temp > 10)
								temp = -1;
						}
					}
				}
			}
		}
		return temp;
	}
	private double howMuchKiloBytes(char a) {

		switch (a) {
			case 'T' :
				return 1073741824.0;
			case 'G' :
				return 1048576.0;
			case 'M' :
				return 1024.0;
			case 'K' :
				return 1.0;
			case 'B' :
				return 0.0009765625;
			default :
				return 1.0;
		}
	}

	private double howMuchMegaBytes(char a) {

		switch (a) {
			case 'T' :
				return 1048576.0;
			case 'G' :
				return 1024.0;
			case 'M' :
				return 1.0;
			case 'K' :
				return 0.0009765625;
			case 'B' :
				return 0.0000009537;
			default :
				return 1.0;
		}
	}
    
    private void parseNetstat(){
        String output = execute.executeCommandReality("netstat -an", "");
        if (output != null && !output.equals("")) {
            parser.parse(output);
            String line = parser.nextLine();
            
            netSockets.put("tcp", new Integer(0));
            netSockets.put("udp", new Integer(0));
            netSockets.put("unix", new Integer(0));
            netSockets.put("icm", new Integer(0));

            netTcpDetails.put("ESTABLISHED", new Integer(0));
            netTcpDetails.put("SYN_SENT", new Integer(0));
            netTcpDetails.put("SYN_RECV", new Integer(0));
            netTcpDetails.put("FIN_WAIT1", new Integer(0));
            netTcpDetails.put("FIN_WAIT2", new Integer(0));
            netTcpDetails.put("TIME_WAIT", new Integer(0));
            netTcpDetails.put("CLOSED", new Integer(0));
            netTcpDetails.put("CLOSE_WAIT", new Integer(0));
            netTcpDetails.put("LAST_ACK", new Integer(0));
            netTcpDetails.put("LISTEN", new Integer(0));
            netTcpDetails.put("CLOSING", new Integer(0));
            netTcpDetails.put("UNKNOWN", new Integer(0));
            
            try{
                String key = null;
                int value = 0;
                while (line != null) {
                    if (line != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                        line = parser.nextLine();
                        continue;
                    }
                    if(line.startsWith("tcp")){            
                        key = "tcp";
                        value = ((Integer)netSockets.get(key)).intValue();
                        value++;
                        netSockets.put(key, new Integer(value));
                        StringTokenizer st = new StringTokenizer(line, " []"); 
                        while(st.hasMoreTokens()){
                            String element = st.nextToken();
                            key = element;
                            if(netTcpDetails.containsKey(element)){
                                value = ((Integer)netTcpDetails.get(key)).intValue();
                                value++;
                                netTcpDetails.put(key, new Integer(value));
                            }
                        }
                    }
                    if(line.startsWith("udp")){
                        key = "udp";
                        value = ((Integer)netSockets.get(key)).intValue();
                        value++;
                        netSockets.put(key, new Integer(value));
                    }
                    if(line.startsWith("unix")){
                        key = "unix";
                        value = ((Integer)netSockets.get(key)).intValue();
                        value++;
                        netSockets.put(key, new Integer(value));
                    }
                    if(line.startsWith("icm")){
                        key = "icm";
                        value = ((Integer)netSockets.get(key)).intValue();
                        value++;
                        netSockets.put(key, new Integer(value));
                    }
                    line = parser.nextLine();
                }
            }catch (Exception e) {
            }
        }
    }

	private void parseTop(String toParse) {

		

		int pointA = 0;
		int pointB = 0;
		int unitPos = 0;
		double sum = 0.0;

		
		try {
			pointA = toParse.indexOf("Procs:");
			
			pointA = toParse.indexOf("Procs:", pointA + 6) + 6;
			
			pointB = toParse.indexOf(",", pointA + 1);
			nbProcesses = toParse.substring(pointA, pointB).trim();
			
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
		try {
			pointA = toParse.indexOf("LoadAvg:", pointA);
			pointA += 9;
			pointB = toParse.indexOf(",", pointA);
			load1 = toParse.substring(pointA, pointB).trim();
			pointA = toParse.indexOf(",", pointB + 1);
			load5 = toParse.substring(pointB + 1, pointA).trim();
			pointB = toParse.indexOf("CPU:", pointA + 1);
			pointB = toParse.lastIndexOf(".", pointB);
			load15 = toParse.substring(pointA + 1, pointB).trim();
			
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
		try {
			pointB = toParse.indexOf("CPU:", pointB + 1) + 4;
			pointA = toParse.indexOf("% user", pointB);
			cpuUSR = toParse.substring(pointB, pointA).trim();
			pointA = toParse.indexOf(",", pointA);
			pointB = toParse.indexOf("% sys", pointA + 1);
			cpuSYS = toParse.substring(pointA + 1, pointB).trim();
			pointA = toParse.indexOf(",", pointB);
			pointB = toParse.indexOf("% idle", pointA + 1);
			cpuIDLE = toParse.substring(pointA + 1, pointB).trim();
			sum = 100.0 - Double.parseDouble(cpuIDLE);
			cpuUsage = String.valueOf(sum);
			
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
		try {
			pointA = toParse.indexOf("PhysMem", pointB);
			pointA += 8;
			pointB = toParse.indexOf("M used", pointA);
			pointA = toParse.lastIndexOf(",", pointB);
			memUsed = toParse.substring(pointA + 1, pointB).trim();
			pointB = toParse.indexOf("M free", pointB);
			pointA = toParse.lastIndexOf(",", pointB);
			memFree = toParse.substring(pointA + 1, pointB).trim();
			
			sum = Double.parseDouble(memUsed) + Double.parseDouble(memFree);
			double percentage = Integer.parseInt(memUsed) / sum * 100;
			memUsage = String.valueOf(percentage);
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};

		
		try {
			pointA = toParse.indexOf("VirtMem:", pointB + 6);
			pointB = toParse.indexOf("pagein", pointA);
			pointA = toParse.lastIndexOf(",", pointB);
			pagesIn = toParse.substring(pointA + 1, pointB).trim();
			pointA = toParse.indexOf("pageout", pointB);
			pointB = toParse.lastIndexOf(",", pointA);
			pagesOut = toParse.substring(pointB + 1, pointA).trim();
			
		} catch (java.lang.StringIndexOutOfBoundsException e) {
			System.out.println("Can't find pages in :" + toParse);
		};

		
		try {
			pointA = toParse.indexOf("Networks:", pointB) + 9;
			pointB = toParse.indexOf("data =", pointA) + 6;
			pointA = toParse.indexOf("in", pointB);
			unitPos = lastIndexOfUnitLetter(toParse, pointA);
			netIn = toParse.substring(pointB, unitPos).trim();
			
			double factor =
				howMuchMegaBytes(
					(toParse.substring(unitPos, unitPos + 1).toCharArray())[0]);
			netIn = String.valueOf(Double.parseDouble(netIn) * factor * 4);
			pointB = toParse.indexOf("out", pointA);
			unitPos = lastIndexOfUnitLetter(toParse, pointB);
			factor =
				howMuchMegaBytes(
					(toParse.substring(unitPos, unitPos + 1).toCharArray())[0]);
			netOut = toParse.substring(pointA + 3, unitPos).trim();
			
			netOut = String.valueOf(Double.parseDouble(netOut) * factor);
			
		} catch (java.lang.StringIndexOutOfBoundsException e) {
			System.out.println(e);
		};

		
		try {
			pointB = toParse.indexOf("Disks:", pointA) + 6;
			pointA = toParse.indexOf("data =", pointB) + 6;
			pointB = toParse.indexOf("in,", pointA);
			unitPos = lastIndexOfUnitLetter(toParse, pointB);
			diskIn = toParse.substring(pointA, unitPos).trim();
			pointA = toParse.indexOf("out", pointB);
			unitPos = lastIndexOfUnitLetter(toParse, pointA);
			diskOut = toParse.substring(pointB + 3, unitPos).trim();

			
			diskIO = diskOut;
		} catch (java.lang.StringIndexOutOfBoundsException e) {
		};
	}
    
    public Hashtable getProcessesState() {
        Hashtable states = null;
        String output = execute.executeCommandReality("ps -e -A -o state", "");
        if (output != null && !output.equals("")) {
            
            parser.parse(output);
            String line = parser.nextLine();
            int nr = 0;
            
            states = new Hashtable();
            states.put("D", new Integer(0));
            states.put("R", new Integer(0));
            states.put("S", new Integer(0));
            states.put("T", new Integer(0));
            states.put("Z", new Integer(0));
    
            while (line != null) {
                if (line != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                    line = parser.nextLine();
                    continue;
                }
                Enumeration e = states.keys();
                while(e.hasMoreElements()){
                    String key = (String)e.nextElement();
                    if(line.startsWith(key)){
                        int x = ((Integer)states.get(key)).intValue();
                        x++;
                        states.put(key, new Integer(x));
                    }
                }
                line = parser.nextLine();
            }
        }
        return states;
    }

	public String getCpuUsage() {
		return cpuUsage;
	}

	public String getCpuUSR() {
		return cpuUSR;
	}

	public String getCpuSYS() {
		return cpuSYS;
	}

	public String getCpuNICE() {
		return "0";
	}

	public String getCpuIDLE() {
		return cpuIDLE;
	}

	public String getPagesIn() {
		return pagesIn;
	}

	public String getPagesOut() {
		return pagesOut;
	}

	public String getMemUsage() {
		return memUsage;
	}

	public String getMemUsed() {
		return memUsed;
	}

	public String getMemFree() {
		return memFree;
	}

	public String getDiskIO() {
		return diskIO;
	}

	public String getDiskTotal() {
		return diskTotal;
	}

	public String getDiskUsed() {
		return diskUsed;
	}

	public String getDiskFree() {
		return diskFree;
	}

	public String getNoProcesses() {
		return nbProcesses;
	}
    
    public Hashtable getNetSockets() {
        return netSockets;
    }
    
    public Hashtable getTcpDetails() {
        return netTcpDetails;
    }

	public String getLoad1() {
		return load1;
	}

	public String getLoad5() {
		return load5;
	}

	public String getLoad15() {
		return load15;
	}

	public String[] getNetInterfaces() {
		return networkInterfaces;
	}

	public String getNetIn(String ifName) {
		if (ifName.equalsIgnoreCase(activeInterface))
			return netIn;
		else
			return "0";
	}

	public String getNetOut(String ifName) {
		if (ifName.equalsIgnoreCase(activeInterface))
			return netOut;
		return "0";
	}
}
