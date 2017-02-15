/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.netstat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lia.util.net.copy.monitoring.lisa.net.PatternUtil;

/**
 * Replacer for the netstat utility, by reading the /proc filesystem it can find out the
 * open connections of the system
 * From http://www.ussg.iu.edu/hypermail/linux/kernel/0409.1/2166.html :
 * It will first list all listening TCP sockets, and next list all established
 TCP connections. A typical entry of /proc/net/tcp would look like this (split 
 up into 3 parts because of the length of the line):
 
 46: 010310AC:9C4C 030310AC:1770 01 
 | | | | | |--> connection state
 | | | | |------> remote TCP port number
 | | | |-------------> remote IPv4 address
 | | |--------------------> local TCP port number
 | |---------------------------> local IPv4 address
 |----------------------------------> number of entry
 
 00000150:00000000 01:00000019 00000000 
 | | | | |--> number of unrecovered RTO timeouts
 | | | |----------> number of jiffies until timer expires
 | | |----------------> timer_active (see below)
 | |----------------------> receive-queue
 |-------------------------------> transmit-queue
 
 1000 0 54165785 4 cd1e6040 25 4 27 3 -1
 | | | | | | | | | |--> slow start size threshold, 
 | | | | | | | | | or -1 if the treshold
 | | | | | | | | | is >= 0xFFFF
 | | | | | | | | |----> sending congestion window
 | | | | | | | |-------> (ack.quick<<1)|ack.pingpong
 | | | | | | |---------> Predicted tick of soft clock
 | | | | | | (delayed ACK control data)
 | | | | | |------------> retransmit timeout
 | | | | |------------------> location of socket in memory
 | | | |-----------------------> socket reference count
 | | |-----------------------------> inode
 | |----------------------------------> unanswered 0-window probes
 |---------------------------------------------> uid
 * @deprecated
 * 
 * @author Ciprian Dobre
 */
public class Netstat {

	/** Possible values for states in /proc/net/tcp */
	final String states[] = { "ESTBLSH",   "SYNSENT",   "SYNRECV",   "FWAIT1",   "FWAIT2",   "TMEWAIT",
			   "CLOSED",    "CLSWAIT",   "LASTACK",   "LISTEN",   "CLOSING",  "UNKNOWN"
			};
	
	/** Pattern used when parsing through /proc/net/tcp */
	private final String netPattern = "\\d+:\\s+([\\dA-F]+):([\\dA-F]+)\\s+([\\dA-F]+):([\\dA-F]+)\\s+([\\dA-F]+)\\s+"+
		"[\\dA-F]+:[\\dA-F]+\\s+[\\dA-F]+:[\\dA-F]+\\s+[\\dA-F]+\\s+([\\d]+)\\s+[\\d]+\\s+([\\d]+)";
	
	/** The stream to write output to */
	protected final PrintStream out;
	
	protected final HashMap<String, String> pids = new HashMap<String, String>();

//	protected final HashMap<String, Integer> inodes; 
	
	/** The constructor 
	 * @param properties We need the properties of the module in order to interogate for paths
	 * @param out The stream to write output to
	 */
	public Netstat(final PrintStream out) {
		this.out = out;
//		inodes = new HashMap<String, Integer>();
	}

	/** Utility method that converts an address from a hexa representation as founded in /proc to String representation */
	private final String getAddress(final String hexa) {
		try {
			// first let's convert the address to Integer
			final long v = Long.parseLong(hexa, 16);
			// because in /proc the order is little endian and java uses big endian order we also need to invert the order
			final long adr = (v >>> 24) | (v << 24) | 
			((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
			// and now it's time to output the result
			return ((adr >> 24) & 0xff) + "." + ((adr >> 16) & 0xff) + "." + ((adr >> 8) & 0xff) + "." + (adr & 0xff);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "0.0.0.0";
		}
	}

	private final int getInt16(final String hexa) {
		try {
			return Integer.parseInt(hexa, 16);
		} catch (Exception ex) {
			ex.printStackTrace();
			return -1;
		}
	}
	
	/** Returns the path to stat utility as declared by the user */
	private final String getStatPath() {
		String path = System.getProperty("net.stat.path", "/usr/bin/"); // if no properties than just use sbin
		path = path.trim();
		if (!path.endsWith("/")) path = path + "/"; // make sure that path always ends in "/";
		return path;
	}

//	/** Method used to question the open processes on the system */ 
//	private void init() {
//		
//		inodes.clear();
//		final cmdExec exec = cmdExec.getInstance();
//		final File dir[] = new File("/proc").listFiles();
//		if (dir == null || dir.length == 0) return;
//		for (int i=0; i<dir.length; i++) {
//			if (!dir[i].canRead() || !dir[i].isDirectory()) continue; // not interested in regular files
//			final String pid = dir[i].getName();
//			if (pid == null || pid.length() == 0) continue;
//			try {
//				Long.parseLong(pid);
//			} catch (Exception ex) {
//				continue; // also only interested in pid processes
//			}
//			final File fd = new File("/proc/"+pid+"/fd");
//			if (!fd.exists() || !fd.canRead()) continue;
//			final File dir1[] = fd.listFiles();
//			if (dir1 == null || dir1.length == 0) continue;
//			for (int j=0; j<dir1.length; j++) {
//				if (!dir1[j].canRead()) continue;
//				final String id = dir1[j].getName();
//				if (id == null || id.length() == 0) continue;
//				try {
//					Long.parseLong(id);
//				} catch (Exception ex) {
//					continue; // also only interested in pid processes
//				}
//				// let's check the inode and the type of file 
//				final String ret = exec.executeCommandReality(getStatPath()+"stat -L -c=%i-%F "+dir1[j].getAbsolutePath(), null);
//				
//				System.out.println(ret);
//				
//				if (exec.isError() || ret == null || ret.length() == 0) {
//					continue;
//				}
//				if (!ret.startsWith("=")) continue;
//				final String type = ret.substring(ret.indexOf("-")+1, ret.length()-1);
//				if (!type.equals("socket")) continue;
//				// check the uid
//				inodes.put(ret.substring(1, ret.indexOf("-")).trim(), Integer.parseInt(pid));
//			}
//		}
//		
//	}

	/** Utility method used to obtain the name of an user based on an uid */
	private final String getPUID(final String uid) {
		if (pids.containsKey(uid))
			return pids.get(uid);
		final String pat = "([\\S&&[^:]]+):[\\S&&[^:]]+:"+uid+":";
		final Pattern pattern = PatternUtil.getPattern("uid_"+uid, pat);
		try {
		BufferedReader in = new BufferedReader(new FileReader("/etc/passwd"));
		String line;
		while ((line = in.readLine()) != null) {
			final Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				final String puid = matcher.group(1);
				pids.put(uid, puid);
				return puid;
			}
		}
		in.close();
		} catch (Throwable t) { }
		pids.put(uid, "UNKNOWN");
		return "UNKNOWN";
	}
	
	private final String getPName(final int pid) {
		final String pat = "Name:\\s*(\\S+)";
		final Pattern pattern = PatternUtil.getPattern("pname", pat);
		try {
		BufferedReader in = new BufferedReader(new FileReader("/proc/"+pid+"/status"));
		String line;
		while ((line = in.readLine()) != null) {
			final Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		in.close();
		} catch (Throwable t) { }
		return "UNKNOWN";
	}
	
	/**
	 * Method used to question for the connections currently openned
	 * @return The list of connections (as Connection objects)
	 */
	public List<Connection> getConnections() {
		
		final ArrayList<Connection> net = new ArrayList<Connection>();

//		init();
		
		// read from /proc/net/tcp the list of currently openned socket connections
		try {
		BufferedReader in = new BufferedReader(new FileReader("/proc/net/tcp"));
		String line;
		while ((line = in.readLine()) != null) {
			Pattern pattern = PatternUtil.getPattern("/proc/net/tcp", netPattern);
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				final Connection c = new Connection();
				c.setProtocol(Connection.TCP_CONNECTION);
				net.add(c);
				final String localPortHexa = matcher.group(2);
				final String remoteAddressHexa = matcher.group(3);
				final String remotePortHexa = matcher.group(4);
				final String statusHexa = matcher.group(5);
				final String uid = matcher.group(6);
				final String inode = matcher.group(7);
				c.setLocalPort(getInt16(localPortHexa));
				c.setRemoteAddress(getAddress(remoteAddressHexa));
				c.setRemotePort(getInt16(remotePortHexa));
				try {
					c.setStatus(states[Integer.parseInt(statusHexa, 16) - 1]);
				} catch (Exception ex) {
					c.setStatus(states[11]); // unknwon
				}
//				if (inodes.containsKey(inode)) { 
//					c.setPID(inodes.get(inode));
//					c.setPName(getPName(c.getPID()));
//				} else {
					c.setPID(-1); // unknown
					c.setPName("UNKNOWN");
//				}
				c.setPOwner(getPUID(uid));
			}
		}
		in.close();
		} catch (Throwable t) { }
		
		// read from /proc/net/udp the list of currently openned socket connections
		try {
		BufferedReader in = new BufferedReader(new FileReader("/proc/net/udp"));
		String line;
		while ((line = in.readLine()) != null) {
			Pattern pattern = PatternUtil.getPattern("/proc/net/tcp", netPattern);
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				final Connection c = new Connection();
				c.setProtocol(Connection.UDP_CONNECTION);
				net.add(c);
				final String localPortHexa = matcher.group(2);
				final String remoteAddressHexa = matcher.group(3);
				final String remotePortHexa = matcher.group(4);
				final String statusHexa = matcher.group(5);
				final String uid = matcher.group(6);
				final String inode = matcher.group(7);
				c.setLocalPort(getInt16(localPortHexa));
				c.setRemoteAddress(getAddress(remoteAddressHexa));
				c.setRemotePort(getInt16(remotePortHexa));
				try {
					c.setStatus(states[Integer.parseInt(statusHexa, 16) - 1]);
				} catch (Exception ex) {
					c.setStatus(states[11]); // unknwon
				}
//				if (inodes.containsKey(inode)) { 
//					c.setPID(inodes.get(inode));
//					c.setPName(getPName(c.getPID()));
//				} else {
					c.setPID(-1); // unknown
					c.setPName("UNKNOWN");
//				}
				c.setPOwner(getPUID(uid));
			}
		}
		in.close();
		} catch (Throwable t) { }
		
		// read from /proc/net/raw the list of currently openned socket connections
		try {
		BufferedReader in = new BufferedReader(new FileReader("/proc/net/raw"));
		String line;
		while ((line = in.readLine()) != null) {
			Pattern pattern = PatternUtil.getPattern("/proc/net/tcp", netPattern);
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				final Connection c = new Connection();
				c.setProtocol(Connection.RAW_CONNECTION);
				net.add(c);
//				final String localAddressHexa = matcher.group(1);
				final String localPortHexa = matcher.group(2);
				final String remoteAddressHexa = matcher.group(3);
				final String remotePortHexa = matcher.group(4);
				final String statusHexa = matcher.group(5);
				final String uid = matcher.group(6);
				final String inode = matcher.group(7);
				c.setLocalPort(getInt16(localPortHexa));
				c.setRemoteAddress(getAddress(remoteAddressHexa));
				c.setRemotePort(getInt16(remotePortHexa));
				try {
					c.setStatus(states[Integer.parseInt(statusHexa, 16) - 1]);
				} catch (Exception ex) {
					c.setStatus(states[11]); // unknwon
				}
//				if (inodes.containsKey(inode)) { 
//					c.setPID(inodes.get(inode));
//					c.setPName(getPName(c.getPID()));
//				} else {
					c.setPID(-1); // unknown
					c.setPName("UNKNOWN");
//				}
				c.setPOwner(getPUID(uid));
			}
		}
		in.close();
		} catch (Throwable t) { }
		
		return net;
	}
	
	public static void main(String args[]) {
		System.out.println("Start");
		Netstat n = new Netstat(System.out);
		List<Connection> c = n.getConnections();
		for (Connection conn : c) {
			System.out.println(conn);
		}
		System.out.println("end");
	}
	
	
} // end of class Netstat


