/**
 * \file MonitoredJob.java
 */

/*
 * ApMon - Application Monitoring Tool
 * Version: 2.2.2
 *
 * Copyright (C) 2006 California Institute of Technology
 *
 * Permission is hereby granted, free of charge, to use, copy and modify 
 * this software and its documentation (the "Software") for any
 * purpose, provided that existing copyright notices are retained in 
 * all copies and that this notice is included verbatim in any distributions
 * or substantial portions of the Software. 
 * This software is a part of the MonALISA framework (http://monalisa.cacr.caltech.edu).
 * Users of the Software are asked to feed back problems, benefits,
 * and/or suggestions about the software to the MonALISA Development Team
 * (developers@monalisa.cern.ch). Support for this software - fixing of bugs,
 * incorporation of new features - is done on a best effort basis. All bug
 * fixes and enhancements will be made available under the same terms and
 * conditions as the original software,
 
 * IN NO EVENT SHALL THE AUTHORS OR DISTRIBUTORS BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF,
 * EVEN IF THE AUTHORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 * THE AUTHORS AND DISTRIBUTORS SPECIFICALLY DISCLAIM ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. THIS SOFTWARE IS
 * PROVIDED ON AN "AS IS" BASIS, AND THE AUTHORS AND DISTRIBUTORS HAVE NO
 * OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS.
 */

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
	/* the job's working dierctory */
	String workDir;
	/* the cluster name that will be included in the monitoring datagrams */
	String clusterName; 
	/* the node name that will be included in the monitoring datagrams */
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
			/* find the children of the i-th child */ 
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
		
		/*
		this list contains strings of the form "rsz_vsz_command" for every pid;
		it is used to avoid adding several times processes that have multiple 
		threads and appear in ps as sepparate processes, occupying exactly the 
		same amount of memory and having the same command name. For every line 
		from the output of the ps command we verify if the rsz_vsz_command 
		combination is already in the list.
		*/
		Vector mem_cmd_list = new Vector();
		

		/* get the list of the process' descendants */
		children = getChildren();
		
		if (children == null)
			return null;
		
		logger.fine("Number of children for process " + pid  + ": " + children.size());
		
		/* issue the "ps" command to obtain information on all the descendants */
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
			//mem_cmd_list.add(mem_cmd_s);
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
	
	/** count the number of open files for the given pid */
	public long countOpenFD(long pid) {
		
		long open_files;
		
		String dir = "/proc/" + pid + "/fd";
		File f = new File(dir);
		
		if(f.exists()){
			if(f.canRead()){
				open_files = (f.list()).length - 2;
//				if(pid == mypid)
//					open_files -= 2;
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
