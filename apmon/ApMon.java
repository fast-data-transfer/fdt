



package apmon;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import apmon.host.cmdExec;



public class ApMon {
	
	static final String APMON_VERSION = "2.2.2";
	
	public static final int MAX_DGRAM_SIZE = 8192;
	public static final int XDR_STRING = 0;
	public static final int XDR_INT32 = 2;
	public static final int XDR_REAL32 = 4;
	public static final int XDR_REAL64 = 5;
	public static final int DEFAULT_PORT = 8884; 
	
	
	public static final int JOB_MONITOR_INTERVAL = 20;
	
	public static final int SYS_MONITOR_INTERVAL = 20;
	
	public static final int RECHECK_INTERVAL = 600;
	
	public static final int MAX_MSG_RATE = 20;
	
	public static final int GEN_MONITOR_INTERVALS = 100;
	
	
	static final int FILE_INIT = 1;
	
	static final int LIST_INIT = 2;
	
	static final int DIRECT_INIT = 3;
	
	
	
	Object initSource = null;
	
	int initType;
	
	
	long recheckInterval = RECHECK_INTERVAL;
	
	
	long crtRecheckInterval = RECHECK_INTERVAL;
	
	String clusterName;	
	String nodeName;	 
	
	Vector destAddresses;	
	Vector destPorts; 	
	Vector destPasswds;	
	
	byte []buf;	
	int dgramSize;	
	
	
	Hashtable confResources; 
	ByteArrayOutputStream baos;
	DatagramSocket dgramSocket;
	
	
	BkThread bkThread = null;
	
	
	boolean bkThreadStarted = false;
	
	
	Object mutexBack = new Object();
	
	
	Object mutexCond = new Object();
	
	
	boolean condChanged = false;
	
	
	boolean recheckChanged, jobMonChanged,sysMonChanged;
	
	
	boolean autoDisableMonitoring = true;
	
	
	boolean confCheck = false;
	
	
	boolean sysMonitoring = false;
	
	
	boolean jobMonitoring = false;
	
	
	boolean genMonitoring = false;
	
	
	long jobMonitorInterval = JOB_MONITOR_INTERVAL;
	long sysMonitorInterval = SYS_MONITOR_INTERVAL; 
	
	int maxMsgRate = MAX_MSG_RATE;
	
	
	int genMonitorIntervals = GEN_MONITOR_INTERVALS;
	
	 
	long sysMonitorParams, jobMonitorParams, genMonitorParams;
	
	
	long lastJobInfoSend;
	
	long lastSysInfoSend;
	
	double lastUtime;
	
	double lastStime;
	
	
	
	
    String myHostname = null; 
	
	String myIP = null;
	
	int numCPUs;
	
	Vector netInterfaces = new Vector();
	
	Vector allMyIPs = new Vector();
	
	
	String sysClusterName = "ApMon_userSend"; 
	
	String sysNodeName = null; 
	
	Vector monJobs = new Vector();
    
    Hashtable sender = new Hashtable();

    private static Logger logger = Logger.getLogger("apmon");
			
	static String osName = System.getProperty("os.name");
	
	static {
	    
		
	    FileHandler fh = null;
	    try {
		fh = new FileHandler("apmon.log");
		fh.setFormatter(new SimpleFormatter());
	    } catch(Exception e) {
		e.printStackTrace();
	    }
		
	    logger.setUseParentHandlers(false);
	    logger.addHandler(fh);
	    logger.setLevel(Level.INFO);
	    
	}
	
	
	public ApMon(String filename) throws ApMonException, SocketException, IOException {
		
		initType = FILE_INIT;
		initMonitoring();
		initSource = filename;
		initialize(filename, true);
        initSenderRef();
	}
	
	
	public void addJobToMonitor(int pid, String workDir, String clusterName, String nodeName)
	{
		MonitoredJob job = new MonitoredJob(pid,workDir,clusterName,nodeName);
		if(!monJobs.contains(job))
			monJobs.add(job);
		else
			logger.warning("Job <" + job + "> already exsist.");
	}
	
	
	public void removeJobToMonitor(int pid)
	{
		int i;
		for(i = 0; i < monJobs.size(); i++){
			MonitoredJob job = (MonitoredJob)monJobs.elementAt(i);
			if(job.getPid() == pid){
				monJobs.remove(job);
				break;
			}
		}
	}
	
	
	public void setMonitorClusterNode(String cName, String nName) {
	    if (cName != null)
		sysClusterName = cName;
	    if (nName != null)
		sysNodeName = nName;
	}
	
	
	void initialize(String filename, boolean firstTime) 
	throws ApMonException, SocketException, IOException {
		Vector destAddresses = new Vector();
		Vector destPorts = new Vector();
		Vector destPasswds = new Vector();
		
		Hashtable confRes = new Hashtable();
		try {
			loadFile(filename, destAddresses, destPorts, destPasswds, confRes);
		} catch (Exception e) {
			if (firstTime) {
				if (e instanceof IOException)
					throw (IOException)e;
				if (e instanceof ApMonException)
					throw (ApMonException)e;
			}
			else {
				logger.warning("Configuration not reloaded successfully, keeping the previous one");
				return;
			}
		}
		
		synchronized(this) {
			arrayInit(destAddresses, destPorts, destPasswds, firstTime);
			this.confResources = confRes;
		}
	}
	
	
	public ApMon(Vector destList) throws ApMonException, SocketException, IOException {
		initType = LIST_INIT;
		initMonitoring();
		initSource = destList;
		initialize(destList, true);
        initSenderRef();
	}
	
	
	void initialize(Vector destList, boolean firstTime)
	throws ApMonException, SocketException, IOException {	
		int i;
		Vector destAddresses = new Vector();
		Vector destPorts = new Vector();
		Vector destPasswds = new Vector();
		String dest;
		Hashtable confRes = new Hashtable();
		

		logger.info("Initializing destination addresses & ports:");
		try {
			for (i = 0; i < destList.size(); i++) {
				dest = (String)destList.get(i);
				if (dest.startsWith("http")) { 
					loadURL(dest, destAddresses, destPorts, destPasswds, confRes);
				} else { 
					addToDestinations(dest, destAddresses, destPorts, destPasswds);
				}
			}
		} catch (Exception e) {
			if (firstTime) {
				if (e instanceof IOException) throw (IOException)e;
				if (e instanceof ApMonException) throw (ApMonException)e;
				if (e instanceof SocketException) throw (SocketException)e;
			} else {
				logger.warning("Configuration not reloaded successfully, keeping the previous one");
				return;
			}
		}
		
		synchronized(this) {
			arrayInit(destAddresses, destPorts, destPasswds, firstTime);
			this.confResources = confRes;
		}
	}
	
	
	public ApMon(Vector destAddresses, Vector destPorts) 
	throws ApMonException, SocketException, IOException {
		this.initType = DIRECT_INIT;
		arrayInit(destAddresses, destPorts, null);
        initSenderRef();
	}
	
	
	public ApMon(Vector destAddresses, Vector destPorts, Vector destPasswds) 
	throws ApMonException, SocketException, IOException {
		this.initType = DIRECT_INIT;
		initMonitoring();
		arrayInit(destAddresses, destPorts, destPasswds);
        initSenderRef();
	}
	
	
	void loadFile(String filename, Vector destAddresses, Vector destPorts,
			Vector destPasswds, Hashtable confRes) 
	throws IOException, ApMonException {
		String line, tmp;
		BufferedReader in = new BufferedReader(new FileReader(filename));
		confRes.put(new File(filename), new Long(System.currentTimeMillis()));
		
		
		logger.info("Loading file " + filename + "...");
		
		
		while((line = in.readLine()) != null) {
			tmp = line.trim();
			
			if (tmp.length() == 0 || tmp.startsWith("#"))
				continue;
			if (tmp.startsWith("xApMon_loglevel")) {
			    StringTokenizer lst = new StringTokenizer(tmp, " =");
			    lst.nextToken();
			    setLogLevel(lst.nextToken());
			    continue;
			}
			if (tmp.startsWith("xApMon_")) {
				parseXApMonLine(tmp);
				continue;
			}
		
			addToDestinations(tmp, destAddresses, destPorts, destPasswds);
		}
	}
	
	
	void loadURL(String url, Vector destAddresses, Vector destPorts, 
				 Vector destPasswds, Hashtable confRes) 
	throws IOException, ApMonException {
		
		System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
		System.setProperty("sun.net.client.defaultReadTimeout", "5000");
		URL destURL = null;
		try {
			destURL = new URL(url);
		} catch (MalformedURLException e) {
			throw new ApMonException(e.getMessage());
		}
		
		URLConnection urlConn = destURL.openConnection();
		long lmt = urlConn.getLastModified();
		confRes.put(new URL(url), new Long(lmt));

		logger.info("Loading from URL " + url + "...");

		BufferedReader br = new BufferedReader(new InputStreamReader(destURL.openStream()));
		String destLine;
		while ((destLine = br.readLine()) != null) {
			String tmp2 = destLine.trim();
			
			if (tmp2.length() == 0 || tmp2.startsWith("#")) 
				continue;                                                                                                
			if (tmp2.startsWith("xApMon_loglevel")) {
			    StringTokenizer lst = new StringTokenizer(tmp2, " =");
			    lst.nextToken();
			    setLogLevel(lst.nextToken());
			    continue;
			}
			if (tmp2.startsWith("xApMon_")) {
				parseXApMonLine(tmp2);
				continue;
				
			}
			addToDestinations(tmp2, destAddresses, destPorts, destPasswds);
		}
		br.close();
	}
	
	
	void addToDestinations(String line, Vector destAddresses, 
				Vector destPorts, Vector destPasswds) {
		String addr;
		int port = DEFAULT_PORT;	
		String tokens[] = line.split("(\\s)+");
		String passwd = "";
		
		if (tokens == null)
			return; 
		
		line = tokens[0].trim();
		if (tokens.length > 1)
			passwd = tokens[1].trim();
		
		
		StringTokenizer st = new StringTokenizer(line, ":");
		addr = st.nextToken();
		try {
			if (st.hasMoreTokens())
				port = Integer.parseInt(st.nextToken());
			else 
				port = DEFAULT_PORT;
		} catch (NumberFormatException e) {
			logger.warning("Wrong address: " + line);
		}
		
		destAddresses.add(addr);
		destPorts.add(new Integer(port));
		if (passwd != null)
			destPasswds.add(passwd);
	}
	
	
	
	void arrayInit(Vector destAddresses, Vector destPorts, Vector destPasswds) 
	throws ApMonException, SocketException, IOException {
		arrayInit(destAddresses, destPorts, destPasswds, true);
	}
	
	
	
	void arrayInit(Vector destAddresses, Vector destPorts, Vector destPasswds, boolean firstTime) 
	throws ApMonException, SocketException, IOException {
        
		Vector tmpAddresses, tmpPorts, tmpPasswds;
		
		if (destAddresses.size() == 0 || destPorts.size() == 0)
			throw new ApMonException("No destination hosts specified");
		
		tmpAddresses = new Vector();
		tmpPorts = new Vector();
		tmpPasswds = new Vector();
		
		

		for (int i = 0; i < destAddresses.size();i++) {
			InetAddress inetAddr = InetAddress.getByName((String)destAddresses.get(i));
			String ipAddr = inetAddr.getHostAddress();
			
			
			if (!tmpAddresses.contains(ipAddr)) {
				tmpAddresses.add(ipAddr);
				tmpPorts.add(destPorts.get(i));
				if (destPasswds != null) {
					tmpPasswds.add(destPasswds.get(i));
				}
				logger.info("adding destination: " + ipAddr + ":" + destPorts.get(i));
			}
		}
		
		synchronized(this) {
			this.destPorts = new Vector(tmpPorts);
			this.destAddresses = new Vector(tmpAddresses);
			this.destPasswds = new Vector(tmpPasswds);
		}
		
		
		if (firstTime) {
			cmdExec exec = new cmdExec();
			String hostnameCmd = "hostname";
			if ( System.getProperty("os.name").indexOf("Linux") != -1 )
				hostnameCmd = "hostname -f";
			myHostname = exec.executeCommand( hostnameCmd,"");
			exec.stopIt();
			sysNodeName = new String(myHostname);
			
			dgramSocket = new DatagramSocket();
			lastJobInfoSend = System.currentTimeMillis();
			
			try {
				lastSysInfoSend = BkThread.getBootTime();
			} catch (Exception e) {
				logger.warning("Error reading boot time from /proc/stat/.");
				lastSysInfoSend = 0;
			}
			
			lastUtime = lastStime = 0;
			
			BkThread.getNetConfig(netInterfaces, allMyIPs);
			if (allMyIPs.size() > 0)
				this.myIP = (String) allMyIPs.get(0);
			else
				this.myIP = "unknown";
			
			try {
				baos = new ByteArrayOutputStream();
			} catch(Throwable t){
				logger.log(Level.WARNING, "", t);
				throw new ApMonException("Got General Exception while encoding:" + t);
			}
		}
		
		setJobMonitoring(jobMonitoring, jobMonitorInterval);
		setSysMonitoring(sysMonitoring, sysMonitorInterval);
		setGenMonitoring(genMonitoring, genMonitorIntervals);
		setConfRecheck(confCheck, recheckInterval);
	}
	
	
	public void sendTimedParameters(String clusterName, String nodeName,
				 int nParams, Vector paramNames, Vector valueTypes, 
				 Vector paramValues, int timestamp)
	throws ApMonException, UnknownHostException, SocketException, IOException {
		
		int i;
		
		if(!shouldSend())
			return;
		
		if (clusterName != null) { 
			
			this.clusterName = new String(clusterName);
			
			if (nodeName != null)
				this.nodeName = new String(nodeName);
			else { 
				this.nodeName = this.myHostname;
			} 
		} 
		
        updateSEQ_NR();
		
		encodeParams(nParams, paramNames, valueTypes, paramValues, timestamp);
		
		synchronized (this) {
			
			for (i = 0; i < destAddresses.size(); i++) {
				InetAddress destAddr = InetAddress.getByName((String)destAddresses.get(i));
				int port = ((Integer)destPorts.get(i)).intValue();
				
				String header = "v:"+APMON_VERSION+"_jp:";
				String passwd = "";
				if (destPasswds != null && destPasswds.size() == destAddresses.size()) {
					passwd = (String)destPasswds.get(i);
				}
				header += passwd;
				
				byte[] newBuff = null;
				try {
					XDROutputStream xdrOS = new XDROutputStream(baos);
					
					xdrOS.writeString(header);
					xdrOS.pad();
                    xdrOS.writeInt(((Integer)sender.get("INSTANCE_ID")).intValue());
                    xdrOS.pad();
                    xdrOS.writeInt(((Integer)sender.get("SEQ_NR")).intValue());
                    xdrOS.pad();
					
					xdrOS.flush();
					byte[] tmpbuf = baos.toByteArray();
					baos.reset();
					
					newBuff = new byte[tmpbuf.length + buf.length]; 
					System.arraycopy(tmpbuf, 0, newBuff, 0, tmpbuf.length);
					System.arraycopy(buf, 0, newBuff, tmpbuf.length, buf.length);
					
				}catch(Throwable t) {
					logger.warning("Cannot add ApMon header...."+t);
					newBuff = buf;
				}
				
				if (newBuff == null || newBuff.length == 0) {
					logger.warning("Cannot send null or 0 length buffer!!");
					continue;
				}
				
				dgramSize = newBuff.length;
				DatagramPacket dp = new DatagramPacket(newBuff, dgramSize, destAddr, port);
				try {
					dgramSocket.send(dp);
				} catch (IOException e) {
					logger.warning("Error sending parameters to " + 
							destAddresses.get(i));
					dgramSocket.close();
					dgramSocket = new DatagramSocket();
				}
				
				if(logger.isLoggable(Level.FINE)) {
				    logger.fine(" Datagram with size " + dgramSize +
				            " sent to " + destAddresses.get(i) +
				            ", containing parameters:\n" + 
				            printParameters(paramNames, valueTypes, paramValues));
				}
			}
		} 
	}
	
	
	public void sendParameters(String clusterName, String nodeName, int nParams, Vector paramNames,
					Vector valueTypes, Vector paramValues)
	throws ApMonException, UnknownHostException, SocketException, IOException {
		sendTimedParameters(clusterName, nodeName, nParams, paramNames, valueTypes, paramValues,-1);
	}
	
	
	public void sendParameter(String clusterName, String nodeName, String paramName,
				int valueType, Object paramValue) 
	throws ApMonException, UnknownHostException, SocketException, IOException {
		Vector paramNames = new Vector();
		paramNames.add(paramName);
		Vector valueTypes = new Vector();
		valueTypes.add(new Integer(valueType));
		Vector paramValues = new Vector();
		paramValues.add(paramValue);
		
	sendParameters(clusterName, nodeName, 1, paramNames, valueTypes, paramValues);
	}

	
	public void sendTimedParameter(String clusterName, String nodeName, String paramName,
			int valueType, Object paramValue, int timestamp) 
	throws ApMonException, UnknownHostException, SocketException, IOException {
		Vector paramNames = new Vector();
		paramNames.add(paramName);
		Vector valueTypes = new Vector();
		valueTypes.add(new Integer(valueType));
		Vector paramValues = new Vector();
		paramValues.add(paramValue);
		
	sendTimedParameters(clusterName, nodeName, 1, paramNames, valueTypes, paramValues, timestamp);
	}

	
	public void sendParameter(String clusterName, String nodeName, String paramName, int paramValue) 
	throws ApMonException, UnknownHostException, SocketException, IOException {
		sendParameter(clusterName, nodeName, paramName, XDR_INT32, new Integer(paramValue));
	}

	
	public void sendTimedParameter(String clusterName, String nodeName, String paramName,
					int paramValue, int timestamp) 
	throws ApMonException, UnknownHostException, SocketException, IOException {
		sendTimedParameter(clusterName, nodeName, paramName, XDR_INT32, new Integer(paramValue), timestamp);
	}
	
	
	public void sendParameter(String clusterName, String nodeName,
				String paramName, double paramValue)
		throws ApMonException, UnknownHostException, SocketException, IOException {
		
		sendParameter(clusterName, nodeName, paramName, XDR_REAL64, new Double(paramValue));
	}

	
	public void sendTimedParameter(String clusterName, String nodeName,
				String paramName, double paramValue, int timestamp) 
	throws ApMonException, UnknownHostException, SocketException, IOException {
		
		sendTimedParameter(clusterName, nodeName, paramName, XDR_REAL64, new Double(paramValue), timestamp);
	}
		
	
	public void sendParameter(String clusterName, String nodeName,
				String paramName, String paramValue) 
		throws ApMonException, UnknownHostException, SocketException, IOException {
		
		sendParameter(clusterName, nodeName, paramName, XDR_STRING, paramValue);
	}
	
	
	public void sendTimedParameter(String clusterName, String nodeName,
				String paramName, String paramValue, int timestamp) 
	throws ApMonException, UnknownHostException, SocketException, IOException {
		
		sendTimedParameter(clusterName, nodeName, paramName, XDR_STRING, paramValue, timestamp);
	}	
	
	
	void ensureSize(ByteArrayOutputStream baos) throws ApMonException {
		if (baos == null) throw new ApMonException("Null ByteArrayOutputStream");
		if (baos.size() > MAX_DGRAM_SIZE)
			throw new ApMonException("Maximum datagram size exceeded");
	}
    
	
	
	void encodeParams(int nParams, Vector paramNames, Vector valueTypes,
					Vector paramValues, int timestamp)
	throws ApMonException {
		int i, valType;
		try {
			XDROutputStream xdrOS = new XDROutputStream(baos);
			
			ensureSize(baos);
            xdrOS.writeString(clusterName);
			xdrOS.pad();
			xdrOS.writeString(nodeName);
			xdrOS.pad();
			xdrOS.writeInt(nParams);
			xdrOS.pad();
			
			for (i = 0; i < nParams; i++) {
				
				
				xdrOS.writeString((String)paramNames.get(i));
				xdrOS.pad();
				
				valType = ((Integer)valueTypes.get(i)).intValue();
				xdrOS.writeInt(valType);
				xdrOS.pad();
				
				switch (valType) {
					case XDR_STRING:
						xdrOS.writeString((String)paramValues.get(i));
						break;
					case XDR_INT32:
						int ival = ((Integer)paramValues.get(i)).intValue();
						xdrOS.writeInt(ival);
						break;
					case XDR_REAL64: 
						double dval = ((Double)paramValues.get(i)).doubleValue();
						xdrOS.writeDouble(dval);
						break;
					default:
						throw new ApMonException("Unknown type for XDR encoding");
				}
				xdrOS.pad();

			}
			
			
			if(timestamp > 0) {
				xdrOS.writeInt(timestamp);
				xdrOS.pad();
			}
			
			ensureSize(baos);
			xdrOS.flush();
			buf = baos.toByteArray();
			baos.reset();
			if(logger.isLoggable(Level.FINE)) {
	            logger.fine("Send buffer length: " + buf.length + "B");
			}
				
		} catch(Throwable t){
			logger.log(Level.WARNING, "", t);
			throw new ApMonException("Got General Exception while encoding:" + t);
		}
	}
	
	
	public boolean getConfCheck() {
		boolean val;
		synchronized(mutexBack) {
			val = this.confCheck;
		}
		return val;
	}
	
	
	public void setConfRecheck(boolean confCheck, long interval) {
		int val = -1;
		if (confCheck)
			logger.info("Enabling configuration reloading (interval "
						+ interval + " s)");
			
		synchronized(mutexBack) {
			if (initType == DIRECT_INIT) { 
				logger.warning("setConfRecheck(): no configuration file/URL to reload\n");				
			} else {			
				this.confCheck = confCheck;
				if (confCheck) {
					if (interval > 0) {
						this.recheckInterval = interval;
						this.crtRecheckInterval = interval;
					} else {
						this.recheckInterval = RECHECK_INTERVAL;
						this.crtRecheckInterval = RECHECK_INTERVAL;
					}
					val = 1;
				} else {
					if (jobMonitoring == false && sysMonitoring == false)
						val = 0;
				}
			}
		} 
		
		if (val == 1) {
			setBackgroundThread(true);
			return;
		}
		if (val == 0) {
			setBackgroundThread(false);
			return;
		}
	}
	
	
	public long getRecheckInterval() {
		long val;
		synchronized(mutexBack) {
			val = this.recheckInterval;
		}
		return val;
	}
	
	
	long getCrtRecheckInterval() {
		long val;
		synchronized(mutexBack) {
			val = this.crtRecheckInterval;
		}
		return val;
	}
	
	
	public void setRecheckInterval(long val) {
		if (val > 0)
			setConfRecheck(true, val);
		else
			setConfRecheck(false, val);
	}
	
	void setCrtRecheckInterval(long val) {
		synchronized(mutexBack) {
			crtRecheckInterval = val;
		}
	}

	
	public void setJobMonitoring(boolean jobMonitoring, long interval) {
		int val = -1;
		if (jobMonitoring)
			logger.info("Enabling job monitoring, time interval " + interval + " s");
		else
			logger.info("Disabling job monitoring...");
				
		synchronized(mutexBack) {
			this.jobMonitoring = jobMonitoring;
			this.jobMonChanged = true;
			if (jobMonitoring == true) {
				if (interval > 0)
					this.jobMonitorInterval = interval;
				else
					this.jobMonitorInterval = JOB_MONITOR_INTERVAL;
				val = 1;
			} else {
				
				if (this.sysMonitoring == false && this.confCheck == false)
					val = 0;
			}
		}
		
		if (val == 1) {
			setBackgroundThread(true);
			return;
		}
		if (val == 0) {
			setBackgroundThread(false);
			return;
		}
	}
	
	
	public long getJobMonitorInterval() {
		long val;
		synchronized(mutexBack) {
			val = this.jobMonitorInterval;
		}
		return val;
	}
	
	
	public boolean getJobMonitoring() {
		boolean val;
		synchronized(mutexBack) {
			val = this.jobMonitoring;
		}
		return val;
	}
	
	
	public void setSysMonitoring(boolean sysMonitoring, long interval) {
		int val = -1;		
		if (sysMonitoring)
			logger.info("Enabling system monitoring, time interval "
					+ interval + " s");
		else
			logger.info("Disabling system monitoring...");
		
		
		synchronized(mutexBack) {
			this.sysMonitoring = sysMonitoring;
			this.sysMonChanged = true;
			if (sysMonitoring == true) {
				if (interval > 0)
					this.sysMonitorInterval = interval;
				else 
					this.sysMonitorInterval = SYS_MONITOR_INTERVAL;
				val = 1;
			}else {
				
				if (this.jobMonitoring == false && this.confCheck == false)
					val = 0;
			}
		}
		
		if (val == 1) {
			setBackgroundThread(true);
			return;
		}
		
		if (val == 0) {
			setBackgroundThread(false);
			return;
		}
	}
	
	
	public long getSysMonitorInterval() {
		long val;
		synchronized(mutexBack) {
			val = this.sysMonitorInterval;
		}
		
		return val;
	}
	
	
	public boolean getSysMonitoring() {
		boolean val;
		synchronized(mutexBack) {
			val = this.sysMonitoring;
		}
		return val;
	}
	
	
	public void setGenMonitoring(boolean genMonitoring, int nIntervals) {
		
		logger.info("Setting general information monitoring to " + genMonitoring);
		
		synchronized(mutexBack) {
			this.genMonitoring = genMonitoring;
			this.recheckChanged = true;
			if (genMonitoring == true) {
				if (nIntervals > 0)
					this.genMonitorIntervals = nIntervals;
				else 
					this.genMonitorIntervals = GEN_MONITOR_INTERVALS; 
			}
		}
		
		
		if (genMonitoring && this.sysMonitoring == false) {
			setSysMonitoring(true, SYS_MONITOR_INTERVAL);
		}
	}
	
	
	public boolean getGenMonitoring() {
		boolean val;
		synchronized(mutexBack) {
			val = this.genMonitoring;
		}
		return val;
	}

    public Double getSystemParameter(String paramName) {
	if (bkThread == null) {
	    logger.info("The background thread is not started - returning null");
	    return null;
	}

	if (bkThread.monitor == null) {
	    logger.info("No HostPropertiesMonitor defined - returning null");
	    return null;
	}
	 
	HashMap hms = bkThread.monitor.getHashParams();
	if (hms == null) {
	    logger.info("No parameters map defined - returning null");
	    return null;
	}

	Long paramId = (Long)ApMonMonitoringConstants.HT_SYS_NAMES_TO_CONSTANTS.get("sys_" + paramName);

	if (paramId == null) {
	    logger.info("The parameter " + paramName + " does not exist.");
	    return null;
	}

	String paramValue = (String)hms.get(paramId);
	double dVal = -1;
	try {
	    dVal = Double.parseDouble(paramValue);
	} catch (Exception e) {
	    logger.info("Could not obtain parameter value from the map: " + 
			paramName);
	    return null;
	}
	return new Double(dVal);
    }

	
	void setBackgroundThread(boolean val) {
		boolean stoppedThread = false;
		
		synchronized(mutexCond) {
			condChanged = false;
			if (val == true)
				if (!bkThreadStarted) {
					bkThreadStarted = true;
					bkThread = new BkThread(this);
					
					bkThread.start();
				} else {
					condChanged = true;
					mutexCond.notify();
				}
			
			if (val == false && bkThreadStarted) {
				bkThread.stopIt();
				condChanged = true;
				mutexCond.notify();
				stoppedThread = true;
				logger.info("[Stopping the thread for configuration reloading...]\n");
			}
		}
		if (stoppedThread) {
			try {
				
				
				bkThread.join();
			} catch (Exception e) {}
			bkThreadStarted = false;
		}
	}
	
     
    public static void setLogLevel(String newLevel_s) {
		int i;
		String levels_s[] = {"FATAL", "WARNING", "INFO", "FINE", "DEBUG"};
		Level levels[] = {Level.SEVERE, Level.WARNING, Level.INFO, 
				   Level.FINE, Level.FINEST};
	
		for (i = 0; i < 5; i++)
		    if (newLevel_s.equals(levels_s[i])) 
			break;
	
		if (i >= 5) {
		    logger.warning("[ setLogLevel() ] Invalid level value: " +
				   newLevel_s);
		    return;
		}
	
		logger.info("Setting logging level to " + newLevel_s);
		logger.setLevel(levels[i]);
    }
	
	 
    public void setMaxMsgRate(int maxRate) {
		this.maxMsgRate  = maxRate;
	}
	
	
	public void stopIt() {
		
		if (bkThreadStarted) {
			if (jobMonitoring) {
			    logger.info("Sending last job monitoring packet...");
				
				bkThread.sendJobInfo();
			}		
		}
		dgramSocket.close();
		if (bkThread != null)
			bkThread.monitor.stopIt();
		setBackgroundThread(false);
	}
	
	
	void initMonitoring() {
		autoDisableMonitoring = true;
		sysMonitoring = false;
		jobMonitoring = false;
		genMonitoring = false;
		confCheck = false;
		
		recheckInterval = RECHECK_INTERVAL;
		crtRecheckInterval = RECHECK_INTERVAL;
		jobMonitorInterval = JOB_MONITOR_INTERVAL;
		sysMonitorInterval = SYS_MONITOR_INTERVAL;
	
        sysMonitorParams = 0L;
        
		
		sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_USAGE;
		
		sysMonitorParams |= ApMonMonitoringConstants.SYS_LOAD1;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_LOAD5;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_LOAD15;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_USR;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_SYS;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_IDLE;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_CPU_NICE;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_MEM_FREE;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_MEM_USAGE;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_MEM_USED;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_PAGES_IN;
        sysMonitorParams |= ApMonMonitoringConstants.SYS_PAGES_OUT;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_IN;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_OUT;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_PROCESSES;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_SOCKETS;
        
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_TCP_DETAILS;
        
        sysMonitorParams |= ApMonMonitoringConstants.SYS_SWAP_USED;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_SWAP_FREE;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_SWAP_USAGE;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_NET_ERRS;
		
        sysMonitorParams |= ApMonMonitoringConstants.SYS_UPTIME;
				
		genMonitorParams = 0L;
		
        genMonitorParams |= ApMonMonitoringConstants.GEN_HOSTNAME;
        genMonitorParams |= ApMonMonitoringConstants.GEN_IP;

		if (osName.indexOf("Linux") >= 0) {
			genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_MHZ;
			
            genMonitorParams |= ApMonMonitoringConstants.GEN_NO_CPUS;
			
            genMonitorParams |= ApMonMonitoringConstants.GEN_TOTAL_MEM;
			
            genMonitorParams |= ApMonMonitoringConstants.GEN_TOTAL_SWAP;
			
			genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_VENDOR_ID;
			
			genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_FAMILY;
			
			genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_MODEL;
			
			genMonitorParams |= ApMonMonitoringConstants.GEN_CPU_MODEL_NAME;
			
			genMonitorParams |= ApMonMonitoringConstants.GEN_BOGOMIPS;
		}
		
		jobMonitorParams = 0L;
		
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_RUN_TIME;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_CPU_TIME;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_CPU_USAGE;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_MEM_USAGE;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_WORKDIR_SIZE;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_TOTAL;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_USED;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_FREE;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_DISK_USAGE;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_VIRTUALMEM;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_RSS;
		
        jobMonitorParams |= ApMonMonitoringConstants.JOB_OPEN_FILES;
	}
	
	
	protected void parseXApMonLine(String line) {
		boolean flag = false, found;
		
		
		String tmp = line.replaceFirst("xApMon_", "");
		StringTokenizer st = new StringTokenizer(tmp, " \t=");
		String param = st.nextToken();
		String value = st.nextToken();
		
		
		if (value.equals("on")) flag = true;
		if (value.equals("off")) flag = false;
		
		synchronized(mutexBack) {
			found = false;
			if (param.equals("job_monitoring")) {
				this.jobMonitoring = flag;
				found = true;
			}
			if (param.equals("sys_monitoring")) {
				this.sysMonitoring = flag;
				found = true;
			}
			if (param.equals("job_interval")) {
				this.jobMonitorInterval = Long.parseLong(value); 
				found = true;
			}
			if (param.equals("sys_interval")) {
				this.sysMonitorInterval = Long.parseLong(value); 
				found = true;
			}
			if (param.equals("general_info")) {
				this.genMonitoring = flag; 
				found = true;
			}
			if (param.equals("conf_recheck")) {
				this.confCheck = flag;
				found = true;
			}
			if (param.equals("recheck_interval")) {
				this.recheckInterval = this.crtRecheckInterval =
				Long.parseLong(value); 
				found = true;
			}
			if (param.equals("maxMsgRate")) {
				this.maxMsgRate = Integer.parseInt(value);
				found = true;
			}
			if (param.equals("auto_disable")) {
				this.autoDisableMonitoring = flag;
				found = true;
			}
		}
		
		if (found)
			return;
		
		
		synchronized(mutexBack) {
            found = false;
            Long val = null;
            
			if (param.startsWith("sys")) {
				val = ApMonMonitoringConstants.getSysIdx(param);
                long lval = val.longValue();
                if(flag) {
                    sysMonitorParams |= lval;
                } else {
                    sysMonitorParams &= ~lval;
                }
			} else if (param.startsWith("job")) {
                val = ApMonMonitoringConstants.getJobIdx(param);
                long lval = val.longValue();
                if(flag) {
                    jobMonitorParams |= lval;
                } else {
                    jobMonitorParams &= ~lval;
                }
            }

            if (val == null) {
                logger.warning("Invalid parameter name in the configuration file: " + param);
            } else {
                found = true;
            }

		}
		
		if(!found)
			logger.warning("Invalid parameter name in the configuration file: " + param);
	}
	
    void setSenderRef(Hashtable s){
        sender = s;
    }
    
    void initSenderRef(){
        sender.put("SEQ_NR", new Integer(0));
        sender.put("INSTANCE_ID", new Integer((new Random()).nextInt(0x7FFFFFFF)));
    }
    
    void updateSEQ_NR(){
        Integer seq_nr = (Integer) sender.get("SEQ_NR");
        sender.put("SEQ_NR", new Integer((seq_nr.intValue()+1)%2000000000));
    }
    
	
	String printParameters(Vector paramNames, Vector valueTypes, Vector paramValues) {
		int i;
		String []typeNames = {"XDR_STRING", "", "XDR_INT32", "", "XDR_REAL32", "XDR_REAL64"};
		
		String res = "";
		for (i = 0; i < paramNames.size(); i++) {
			String name = (String)paramNames.get(i);
			int valType = ((Integer)valueTypes.get(i)).intValue();
			res += (name + " (" + typeNames[valType] + "): ");
			switch(valType) {
				case XDR_STRING:
					res += (String)paramValues.get(i);
					break;
				case XDR_INT32:
					res += (Integer)paramValues.get(i);
					break;
				case XDR_REAL32:
					res += (Float)paramValues.get(i);					
					break;
				case XDR_REAL64:
					res += (Double)paramValues.get(i);					
					break;
			}
			res += "\n";

		}
		return res;
	}
	
	
	protected long prvTime = 0;
	protected double prvSent = 0;
	protected double prvDrop = 0;
	protected long crtTime = 0;
	protected long crtSent = 0;
	protected long crtDrop = 0;
	protected double hWeight = Math.exp(-5.0/60.0);

	 
	public boolean shouldSend() {

        long now = (new Date()).getTime()/1000;
		boolean doSend;

        if(now != crtTime){
			
			prvSent = hWeight * prvSent + (1.0 - hWeight) * crtSent / (now - crtTime);
			prvTime = crtTime;
			logger.log(Level.FINE, "previously sent: " + crtSent + " dropped: " + crtDrop);
			
			crtTime = now;
			crtSent = 0;
			crtDrop = 0;
        }
		
		
        int valSent = (int)(prvSent * hWeight + crtSent * (1.0 - hWeight));

        doSend = true;
		
        int level = this.maxMsgRate - this.maxMsgRate / 10;

        if(valSent > (this.maxMsgRate - level))
                doSend = (new Random()).nextInt(this.maxMsgRate / 10) < (this.maxMsgRate - valSent);

        
        if(doSend){
			crtSent++;
        }else{
			crtDrop++;
        }

        return doSend;
	}

	public String getMyHostname() {
		return myHostname;
	}

	public String getMyIP() {
		return myIP;
	}
	
}
 
