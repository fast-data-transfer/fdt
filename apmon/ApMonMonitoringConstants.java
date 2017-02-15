



package apmon;

import java.util.HashMap;
import java.util.Iterator;


public final class ApMonMonitoringConstants {

    
    public static final long SYS_LOAD1            =   0x1L;
    public static final Long LSYS_LOAD1           =   new Long(SYS_LOAD1);
    public static final long SYS_LOAD5            =   0x2L;
    public static final Long LSYS_LOAD5           =   new Long(SYS_LOAD5);
    public static final long SYS_LOAD15           =   0x4L;
    public static final Long LSYS_LOAD15          =   new Long(SYS_LOAD15);
    
    public static final long SYS_CPU_USR          =   0x8L;
    public static final Long LSYS_CPU_USR         =   new Long(SYS_CPU_USR);
    public static final long SYS_CPU_SYS          =   0x10L;
    public static final Long LSYS_CPU_SYS         =   new Long(SYS_CPU_SYS);
    public static final long SYS_CPU_IDLE         =   0x20L;
    public static final Long LSYS_CPU_IDLE        =   new Long(SYS_CPU_IDLE);
    public static final long SYS_CPU_NICE         =   0x40L;
    public static final Long LSYS_CPU_NICE        =   new Long(SYS_CPU_NICE);
    public static final long SYS_CPU_USAGE        =   0x80L;
    public static final Long LSYS_CPU_USAGE       =   new Long(SYS_CPU_USAGE);
    
    public static final long SYS_MEM_FREE         =   0x100L;
    public static final Long LSYS_MEM_FREE        =   new Long(SYS_MEM_FREE);
    public static final long SYS_MEM_USED         =   0x200L;
    public static final Long LSYS_MEM_USED        =   new Long(SYS_MEM_USED);
    public static final long SYS_MEM_USAGE        =   0x400L;
    public static final Long LSYS_MEM_USAGE       =   new Long(SYS_MEM_USAGE);
    
    public static final long SYS_PAGES_IN         =   0x800L;
    public static final Long LSYS_PAGES_IN        =   new Long(SYS_PAGES_IN);
    public static final long SYS_PAGES_OUT        =   0x1000L;
    public static final Long LSYS_PAGES_OUT       =   new Long(SYS_PAGES_OUT);
    
    public static final long SYS_NET_IN           =   0x2000L;
    public static final Long LSYS_NET_IN          =   new Long(SYS_NET_IN);
    public static final long SYS_NET_OUT          =   0x4000L;
    public static final Long LSYS_NET_OUT         =   new Long(SYS_NET_OUT);
    public static final long SYS_NET_ERRS         =   0x8000L;
    public static final Long LSYS_NET_ERRS        =   new Long(SYS_NET_ERRS);
    
    public static final long SYS_SWAP_FREE        =   0x10000L;
    public static final Long LSYS_SWAP_FREE       =   new Long(SYS_SWAP_FREE);
    public static final long SYS_SWAP_USED        =   0x20000L;
    public static final Long LSYS_SWAP_USED       =   new Long(SYS_SWAP_USED);
    public static final long SYS_SWAP_USAGE       =   0x40000L;
    public static final Long LSYS_SWAP_USAGE      =   new Long(SYS_SWAP_USAGE);

    public static final long SYS_PROCESSES        =   0x80000L;
    public static final Long LSYS_PROCESSES       =   new Long(SYS_PROCESSES);
    
    public static final long SYS_NET_SOCKETS      =   0x100000L;
    public static final Long LSYS_NET_SOCKETS     =   new Long(SYS_NET_SOCKETS);

    public static final long SYS_NET_TCP_DETAILS  =   0x200000L;
    public static final Long LSYS_NET_TCP_DETAILS =   new Long(SYS_NET_TCP_DETAILS);

    public static final long SYS_UPTIME           =   0x400000L;
    public static final Long LSYS_UPTIME          =   new Long(SYS_UPTIME);

    static HashMap  HT_SYS_NAMES_TO_CONSTANTS = null;
    private static HashMap  HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES = null;
    
    static {
        HT_SYS_NAMES_TO_CONSTANTS = new HashMap();
        
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_load1",  LSYS_LOAD1);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_load5",  LSYS_LOAD5);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_load15", LSYS_LOAD15);

        HT_SYS_NAMES_TO_CONSTANTS.put("sys_cpu_usr", LSYS_CPU_USR);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_cpu_sys", LSYS_CPU_SYS);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_cpu_idle", LSYS_CPU_IDLE);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_cpu_nice", LSYS_CPU_NICE);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_cpu_usage", LSYS_CPU_USAGE);
        
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_mem_free", LSYS_MEM_FREE);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_mem_used", LSYS_MEM_USED);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_mem_usage", LSYS_MEM_USAGE);
       
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_pages_in", LSYS_PAGES_IN);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_pages_out", LSYS_PAGES_OUT);

        HT_SYS_NAMES_TO_CONSTANTS.put("sys_net_in", LSYS_NET_IN);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_net_out", LSYS_NET_OUT);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_net_errs", LSYS_NET_ERRS);

        HT_SYS_NAMES_TO_CONSTANTS.put("sys_swap_free", LSYS_SWAP_FREE);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_swap_used", LSYS_SWAP_USED);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_swap_usage", LSYS_SWAP_USAGE);
        
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_processes", LSYS_PROCESSES);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_net_sockets", LSYS_NET_SOCKETS);
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_net_tcp_details", LSYS_NET_TCP_DETAILS);
        
        HT_SYS_NAMES_TO_CONSTANTS.put("sys_uptime", LSYS_UPTIME);
        

        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES = new HashMap();
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_LOAD1,  "load1");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_LOAD5,  "load5");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_LOAD15, "load15");
        
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_CPU_USR, "cpu_usr");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_CPU_SYS, "cpu_sys");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_CPU_IDLE, "cpu_idle");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_CPU_NICE, "cpu_nice");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_CPU_USAGE, "cpu_usage");
        
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_MEM_FREE, "mem_free");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_MEM_USED, "mem_used");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_MEM_USAGE, "mem_usage");

        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_PAGES_IN, "pages_in");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_PAGES_OUT, "pages_out");
        
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_NET_IN, "in");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_NET_OUT, "out");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_NET_ERRS, "errs");
        
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_SWAP_FREE, "swap_free");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_SWAP_USED, "swap_used");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_SWAP_USAGE, "swap_usage");
        
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_PROCESSES, "processes");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_NET_SOCKETS, "sockets");
        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_NET_TCP_DETAILS, "sockets_tcp");

        HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES.put(LSYS_UPTIME, "uptime");
        
    }
    
    
    
    private static String getName(Long param, HashMap hm) {
        if(param == null) return null;
        if(!hm.containsValue(param)) return null;
        
        for(Iterator it=hm.keySet().iterator(); it.hasNext();){
            String key = (String)it.next();
            if(hm.get(key).equals(param)) return key;
        }
        
        
        return null;
    }

    private static Long getIdx(String name, HashMap hm) {
        if( name == null ) return null;
        return (Long)hm.get(name);
    }
    
    private static String getMLParamName(Long idx, HashMap hm) {
        if(idx == null) return null;
        return (String)hm.get(idx);
    }

    
    
    public static String getSysName(Long param) {
        return getName(param, HT_SYS_NAMES_TO_CONSTANTS);
    }
          
    public static Long getSysIdx(String name) {
        return getIdx(name, HT_SYS_NAMES_TO_CONSTANTS);
    }
    
    public static String getSysMLParamName(Long idx) {
        return getMLParamName(idx, HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES);
    }

    public static String getSysMLParamName(long idx) {
        return getMLParamName(new Long(idx), HT_SYS_CONSTANTS_TO_ML_PARAM_NAMES);
    }
    
    
    public static final long GEN_HOSTNAME		=	0x1L;
    public static final Long LGEN_HOSTNAME					=	new Long(GEN_HOSTNAME);
    public static final long GEN_IP				=	0x2L;
    public static final Long LGEN_IP						=	new Long(GEN_IP);
    public static final long GEN_CPU_MHZ		=	0x4L;
    public static final Long LGEN_CPU_MHZ  				    =   new Long(GEN_CPU_MHZ);
    public static final long GEN_NO_CPUS		=   0x8L;
    public static final Long LGEN_NO_CPUS 				    =   new Long(GEN_NO_CPUS);
    public static final long GEN_TOTAL_MEM		=   0x10L;
    public static final Long LGEN_TOTAL_MEM     			=   new Long(GEN_TOTAL_MEM);
    public static final long GEN_TOTAL_SWAP		=   0x20L;
    public static final Long LGEN_TOTAL_SWAP    			=   new Long(GEN_TOTAL_SWAP);
	public static final long GEN_CPU_VENDOR_ID	=   0x40L;
    public static final Long LGEN_CPU_VENDOR_ID				=   new Long(GEN_CPU_VENDOR_ID);
	public static final long GEN_CPU_FAMILY		=   0x80L;
    public static final Long LGEN_CPU_FAMILY    			=   new Long(GEN_CPU_FAMILY);
	public static final long GEN_CPU_MODEL     	=   0x100L;
    public static final Long LGEN_CPU_MODEL    				=   new Long(GEN_CPU_MODEL);
	public static final long GEN_CPU_MODEL_NAME	=   0x200L;
    public static final Long LGEN_CPU_MODEL_NAME    		=   new Long(GEN_CPU_MODEL_NAME);
	public static final long GEN_BOGOMIPS		=   0x400L;
    public static final Long LGEN_BOGOMIPS    				=   new Long(GEN_BOGOMIPS);
	
    private static HashMap  HT_GEN_NAMES_TO_CONSTANTS = null;
    private static HashMap  HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES = null;

    static {
        HT_GEN_NAMES_TO_CONSTANTS = new HashMap();
        
        HT_GEN_NAMES_TO_CONSTANTS.put("hostname",  LGEN_HOSTNAME);
        HT_GEN_NAMES_TO_CONSTANTS.put("ip",  LGEN_IP);
        HT_GEN_NAMES_TO_CONSTANTS.put("cpu_MHz", LGEN_CPU_MHZ);
        HT_GEN_NAMES_TO_CONSTANTS.put("no_CPUs", LGEN_NO_CPUS);
        HT_GEN_NAMES_TO_CONSTANTS.put("total_mem", LGEN_TOTAL_MEM);
        HT_GEN_NAMES_TO_CONSTANTS.put("total_swap", LGEN_TOTAL_SWAP);
		HT_GEN_NAMES_TO_CONSTANTS.put("cpu_vendor_id", LGEN_CPU_VENDOR_ID);
		HT_GEN_NAMES_TO_CONSTANTS.put("cpu_family", LGEN_CPU_FAMILY);
		HT_GEN_NAMES_TO_CONSTANTS.put("cpu_model", LGEN_CPU_MODEL);
		HT_GEN_NAMES_TO_CONSTANTS.put("cpu_model_name", LGEN_CPU_MODEL_NAME);
		HT_GEN_NAMES_TO_CONSTANTS.put("bogomips", LGEN_BOGOMIPS);

        
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES = new HashMap();
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_HOSTNAME, "hostname");
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_IP, "ip");
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_CPU_MHZ, "cpu_MHZ");
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_NO_CPUS, "no_CPUs");
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_TOTAL_MEM, "total_mem");
        HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_TOTAL_SWAP, "total_swap");
		HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_CPU_VENDOR_ID, "cpu_vendor_id");
		HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_CPU_FAMILY, "cpu_family");
		HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_CPU_MODEL, "cpu_model");
		HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_CPU_MODEL_NAME, "cpu_model_name");
		HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES.put(LGEN_BOGOMIPS, "bogomips");
    }
    
    public static String getGenName(Long param) {
        return getName(param, HT_GEN_NAMES_TO_CONSTANTS);
    }
          
    public static Long getGenIdx(String name) {
        return getIdx(name, HT_GEN_NAMES_TO_CONSTANTS);
    }
    
    public static String getGenMLParamName(Long idx) {
        return getMLParamName(idx, HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES);
    }

    public static String getGenMLParamName(long idx) {
        return getMLParamName(new Long(idx), HT_GEN_CONSTANTS_TO_ML_PARAM_NAMES);
    }

    

    public static final long JOB_RUN_TIME       =   0x1L;
    public static final Long LJOB_RUN_TIME      =   new Long(JOB_RUN_TIME);
    public static final long JOB_CPU_TIME       =   0x2L;
    public static final Long LJOB_CPU_TIME      =   new Long(JOB_CPU_TIME);
    public static final long JOB_CPU_USAGE      =   0x4L;
    public static final Long LJOB_CPU_USAGE     =   new Long(JOB_CPU_USAGE);
    
    public static final long JOB_MEM_USAGE      =   0x8L;
    public static final Long LJOB_MEM_USAGE     =   new Long(JOB_MEM_USAGE);
    public static final long JOB_WORKDIR_SIZE   =   0x10L;
    public static final Long LJOB_WORKDIR_SIZE  =   new Long(JOB_WORKDIR_SIZE);
    public static final long JOB_DISK_TOTAL     =   0x20L;
    public static final Long LJOB_DISK_TOTAL    =   new Long(SYS_CPU_IDLE);
    public static final long JOB_DISK_USED      =   0x40L;
    public static final Long LJOB_DISK_USED     =   new Long(JOB_DISK_USED);
    public static final long JOB_DISK_FREE      =   0x80L;
    public static final Long LJOB_DISK_FREE     =   new Long(JOB_DISK_FREE);
    
    public static final long JOB_DISK_USAGE     =   0x100L;
    public static final Long LJOB_DISK_USAGE    =   new Long(JOB_DISK_USAGE);
    public static final long JOB_VIRTUALMEM     =   0x200L;
    public static final Long LJOB_VIRTUALMEM    =   new Long(JOB_VIRTUALMEM);
    public static final long JOB_RSS            =   0x400L;
    public static final Long LJOB_RSS           =   new Long(JOB_RSS);
	public static final long JOB_OPEN_FILES     =   0x800L;
    public static final Long LJOB_OPEN_FILES    =   new Long(JOB_OPEN_FILES);

    private static HashMap  HT_JOB_NAMES_TO_CONSTANTS = null;
    private static HashMap  HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES = null;
    
    static {
        HT_JOB_NAMES_TO_CONSTANTS = new HashMap();
        
        HT_JOB_NAMES_TO_CONSTANTS.put("job_run_time",  LJOB_RUN_TIME);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_cpu_time",  LJOB_CPU_TIME);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_cpu_usage", LJOB_CPU_USAGE);

        HT_JOB_NAMES_TO_CONSTANTS.put("job_mem_usage", LJOB_MEM_USAGE);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_workdir_size", LJOB_WORKDIR_SIZE);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_disk_total", LJOB_DISK_TOTAL);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_disk_used", LJOB_DISK_USED);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_disk_free", LJOB_DISK_FREE);
        
        HT_JOB_NAMES_TO_CONSTANTS.put("job_disk_usage", LJOB_DISK_USAGE);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_virtualmem", LJOB_VIRTUALMEM);
        HT_JOB_NAMES_TO_CONSTANTS.put("job_rss", LJOB_RSS);
		HT_JOB_NAMES_TO_CONSTANTS.put("job_open_files", LJOB_OPEN_FILES);
        
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES = new HashMap();
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_RUN_TIME, "run_time");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_CPU_TIME, "cpu_time");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_CPU_USAGE, "cpu_usage");
        
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_MEM_USAGE, "mem_usage");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_WORKDIR_SIZE, "workdir_size");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_DISK_TOTAL, "disk_total");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_DISK_USED, "disk_used");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_DISK_FREE, "disk_free");
        
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_DISK_USAGE, "disk_usage");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_VIRTUALMEM, "disk_virtualmem");
        HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_RSS, "disk_rss");
		HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES.put(LJOB_OPEN_FILES, "open_files");

    }    

    public static String getJobName(Long param) {
        return getName(param, HT_JOB_NAMES_TO_CONSTANTS);
    }
          
    public static Long getJobIdx(String name) {
        return getIdx(name, HT_JOB_NAMES_TO_CONSTANTS);
    }
    
    public static String getJobMLParamName(Long idx) {
        return getMLParamName(idx, HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES);
    }

    public static String getJobMLParamName(long idx) {
        return getMLParamName(new Long(idx), HT_JOB_CONSTANTS_TO_ML_PARAM_NAMES);
    }
}
