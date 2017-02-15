



package apmon;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import apmon.host.cmdExec;

public class MonitoredJob{
	int pid;
	
	String workDir;
	
	String clusterName; 
	
	String nodeName;
	private static Logger logger = Logger.getLogger("apmon");
    cmdExec exec = null;
	
	public MonitoredJob(int _pid, String _workDir, String _clusterName, String _nodeName){
		pid = _pid;
		workDir = _workDir;
		clusterName = _clusterName;
		nodeName = _nodeName;
		exec = new cmdExec();
	}
	
	public int getPid()
	{
		return pid;
	}
	
	public HashMap readJobDiskUsage() {
		HashMap hm = new HashMap();
		String cmd=null, aux=null, line=null, result=null;
		double workdir_size=0.0, disk_total=0.0, disk_used=0.0, disk_free=0.0, disk_usage=0.0;

		if (workDir == null)
			return null;
			
		cmd = "du -Lscm " + workDir + " | tail -1 | cut -f 1";
		result = exec.executeCommandReality(cmd,"");
		workdir_size = Double.parseDouble(result);
		hm.put(ApMonMonitoringConstants.LJOB_WORKDIR_SIZE, new Double(workdir_size));
		
		cmd = "df -m " + workDir + " | tail -1";
		result = exec.executeCommand(cmd,"");
		StringTokenizer st = new StringTokenizer(result, " %");
		st.nextToken();
		
		aux = st.nextToken();
		disk_total = Double.parseDouble(aux);
        hm.put(ApMonMonitoringConstants.LJOB_DISK_TOTAL, new Double(workdir_size));
		
		aux = st.nextToken();
		disk_used = Double.parseDouble(aux);
        hm.put(ApMonMonitoringConstants.LJOB_DISK_USED, new Double(workdir_size));
		
		aux = st.nextToken();
		disk_free = Double.parseDouble(aux);
        hm.put(ApMonMonitoringConstants.LJOB_DISK_FREE, new Double(workdir_size));
		
		aux = st.nextToken();
		disk_usage = Double.parseDouble(aux);
        hm.put(ApMonMonitoringConstants.LJOB_DISK_USAGE, new Double(workdir_size));
		
		return hm;
	}

	
	public Vector getChildren() {
		Vector pids, ppids, children;
		String cmd=null, result=null;
		int nProcesses = 0, nChildren = 1;
		int i, j;
		
		cmd = "ps --no-headers -eo ppid,pid";
		result = exec.executeCommandReality(cmd,"");
		boolean pidFound = false;
		if (result == null) {
			logger.warning("The child processes could not be determined");
			return null;
		}

        StringTokenizer st = new StringTokenizer(result, " \n");
        nProcesses = st.countTokens() / 2;
        
        pids = new Vector();
        ppids = new Vector();
        children = new Vector();
        children.add(new Integer(pid));
        while(st.hasMoreTokens()){
            i = Integer.parseInt(st.nextToken());
            j = Integer.parseInt(st.nextToken());
            if (j == pid)
                pidFound = true;
            ppids.add(new Integer(i));
            pids.add(new Integer(j));
            if(i == ((Integer)children.elementAt(0)).intValue()){
                children.add(new Integer(j));
                nChildren++;
            }
        }
		
		if (!pidFound)
		    return null;

		i = 1;
	
		while (i < nChildren) {
			 
			for (j = 0; j < nProcesses; j++) {
				if (ppids.elementAt(j).equals(children.elementAt(i))) {
					children.add(pids.elementAt(j));
					nChildren++;
				}
			}
			i++;
		}

		return children;
	}
	
	public static long parsePSTime(String s) {
		long days, hours, mins, secs;
		if (s.indexOf('-') > 0) {
			StringTokenizer st = new StringTokenizer(s,"-:");
			days = Long.parseLong(st.nextToken());
			hours = Long.parseLong(st.nextToken());
			mins = Long.parseLong(st.nextToken());
			secs = Long.parseLong(st.nextToken());
			return 24 * 3600 * days + 3600 * hours + 60 * mins + secs;
		} 
        if (s.indexOf(':') > 0 && s.indexOf(':') !=
            s.lastIndexOf(':')) {
            StringTokenizer st = new StringTokenizer(s,":");
            hours = Long.parseLong(st.nextToken());
            mins = Long.parseLong(st.nextToken());
            secs = Long.parseLong(st.nextToken());
            return 3600 * hours + 60 * mins + secs;
        } 
        
        if (s.indexOf(':') > 0) {
            StringTokenizer st = new StringTokenizer(s,":");
            mins = Long.parseLong(st.nextToken());
            secs = Long.parseLong(st.nextToken());
            return 60 * mins + secs;
        }
        
        return -1;
	}
	
	public HashMap readJobInfo() throws IOException {
		Vector children;
        HashMap ret = new HashMap();
		String cmd=null, result=null;
		BufferedReader fp;
		String line = null;
		
		int i;
		
		double rsz=0.0, vsz=0.0;
		double etime=0.0, cputime=0.0;
		double pcpu=0.0, pmem=0.0;
		
		double _rsz, _vsz;
		double _etime, _cputime;
		double _pcpu, _pmem;
		
		long apid, fd = 0;
		
		
		Vector mem_cmd_list = new Vector();
		

		
		children = getChildren();
		
		if (children == null)
			return null;
		
		logger.fine("Number of children for process " + pid  + ": " + children.size());
		
		
		cmd = "ps --no-headers --pid ";
		for (i = 0; i < children.size()-1; i++)
			cmd = cmd + children.elementAt(i) + ",";
		cmd = cmd + children.elementAt(children.size()-1);
		
		cmd = cmd + " -o pid,etime,time,%cpu,%mem,rsz,vsz,comm"; 
		result = exec.executeCommandReality(cmd,"");

		StringTokenizer rst = new StringTokenizer(result, "\n");
		while (rst.hasMoreTokens()) 
		{
		    line = rst.nextToken();
			StringTokenizer st = new StringTokenizer(line, " \t");
		
			apid = Long.parseLong(st.nextToken());
			_etime = (double)parsePSTime(st.nextToken());
			_cputime = (double)parsePSTime(st.nextToken());
			_pcpu = Double.parseDouble(st.nextToken());
			_pmem = Double.parseDouble(st.nextToken());
			_rsz = Double.parseDouble(st.nextToken());
			_vsz = Double.parseDouble(st.nextToken());
			String cmdName = st.nextToken();
			
			etime = etime > _etime ? etime : _etime;
			cputime += _cputime;
			pcpu += _pcpu;
			
			String mem_cmd_s = ""+_rsz+"_"+_vsz+"_"+cmdName;
			
			if(mem_cmd_list.indexOf(mem_cmd_s)==-1){
				pmem += _pmem;
				vsz += _vsz;
				rsz += _rsz;
				mem_cmd_list.add(mem_cmd_s);
				long _fd = countOpenFD(apid);
				if(_fd!=-1)
					fd += _fd;
			}
		}
		
		ret.put(ApMonMonitoringConstants.LJOB_RUN_TIME, new Double(etime));
		ret.put(ApMonMonitoringConstants.LJOB_CPU_TIME, new Double(cputime));
		ret.put(ApMonMonitoringConstants.LJOB_CPU_USAGE, new Double(pcpu));
		ret.put(ApMonMonitoringConstants.LJOB_MEM_USAGE, new Double(pmem));
		ret.put(ApMonMonitoringConstants.LJOB_RSS, new Double(rsz));
		ret.put(ApMonMonitoringConstants.LJOB_VIRTUALMEM, new Double(vsz));
		ret.put(ApMonMonitoringConstants.LJOB_OPEN_FILES, new Double(fd));
		
		return ret;
	}
	
	
	public long countOpenFD(long pid) {
		
		long open_files;
		
		String dir = "/proc/" + pid + "/fd";
		File f = new File(dir);
		
		if(f.exists()){
			if(f.canRead()){
				open_files = (f.list()).length - 2;


				logger.log(Level.FINE, "Counting open_files for $pid: |@list| => $open_files");
			}else{
				open_files = -1;
				logger.log(Level.SEVERE, "ProcInfo: cannot count the number of opened files for job" + pid);
			}
		}else{
			open_files = -1;
			logger.log(Level.SEVERE, "ProcInfo: job " + pid + "not exist.");
		}	
		return open_files;
	}
	
	public String toString(){
		return new String("[" + pid + "]" + " "+ workDir + " " + " " + clusterName + " " + nodeName);
	}

} 
