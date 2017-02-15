
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


public class Netstat {

	
	final String states[] = { "ESTBLSH",   "SYNSENT",   "SYNRECV",   "FWAIT1",   "FWAIT2",   "TMEWAIT",
			   "CLOSED",    "CLSWAIT",   "LASTACK",   "LISTEN",   "CLOSING",  "UNKNOWN"
			};
	
	
	private final String netPattern = "\\d+:\\s+([\\dA-F]+):([\\dA-F]+)\\s+([\\dA-F]+):([\\dA-F]+)\\s+([\\dA-F]+)\\s+"+
		"[\\dA-F]+:[\\dA-F]+\\s+[\\dA-F]+:[\\dA-F]+\\s+[\\dA-F]+\\s+([\\d]+)\\s+[\\d]+\\s+([\\d]+)";
	
	
	protected final PrintStream out;
	
	protected final HashMap<String, String> pids = new HashMap<String, String>();


	
	
	public Netstat(final PrintStream out) {
		this.out = out;

	}

	
	private final String getAddress(final String hexa) {
		try {
			
			final long v = Long.parseLong(hexa, 16);
			
			final long adr = (v >>> 24) | (v << 24) | 
			((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
			
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
	
	
	private final String getStatPath() {
		String path = System.getProperty("net.stat.path", "/usr/bin/"); 
		path = path.trim();
		if (!path.endsWith("/")) path = path + "/"; 
		return path;
	}
















































	
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
	
	
	public List<Connection> getConnections() {
		
		final ArrayList<Connection> net = new ArrayList<Connection>();


		
		
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
					c.setStatus(states[11]); 
				}




					c.setPID(-1); 
					c.setPName("UNKNOWN");

				c.setPOwner(getPUID(uid));
			}
		}
		in.close();
		} catch (Throwable t) { }
		
		
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
					c.setStatus(states[11]); 
				}




					c.setPID(-1); 
					c.setPName("UNKNOWN");

				c.setPOwner(getPUID(uid));
			}
		}
		in.close();
		} catch (Throwable t) { }
		
		
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
					c.setStatus(states[11]); 
				}




					c.setPID(-1); 
					c.setPName("UNKNOWN");

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
	
	
} 


