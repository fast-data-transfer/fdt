/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.netstat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lia.util.net.copy.monitoring.lisa.cmdExec;
import lia.util.net.copy.monitoring.lisa.cmdExec.CommandResult;
import lia.util.net.copy.monitoring.lisa.net.PatternUtil;

/**
 * 
 * Handler task for monitoring the current connections
 *  
 * @author Ciprian Dobre 
 */
public class NetstatHandler {

	/** The stream to write output to */
	protected final PrintStream out;

	/** Utility object used for running different commands */
	protected final cmdExec exec;

	/** Pattern used to parse the output of netstat utility */
	protected final String netstatPattern = "\\S+\\s+\\d+\\s+\\d+\\s*(\\d*[\\.:]\\d*[\\.:]\\d*\\.*\\d*):(\\d+)\\s*" +
			"(\\d*[\\.:]\\d*[\\.:]\\d*\\.*\\d*):([\\d\\*]+)\\s*([a-zA-Z]*)\\s*(\\d+)\\s*\\d+\\s*([\\d-]+)/*(\\S*)";
	
	protected final Logger logger;

	protected boolean netstatPathSetup = false;
	protected String netstatPath = null;
	
	/**
	 * The constructor
	 * @param properties We need the properties of the module in order to interogate for paths
	 * @param out The stream to write output to
	 */
	public NetstatHandler(final PrintStream out, final Logger logger) {
		this.logger = logger;
		this.out = out;
		exec = cmdExec.getInstance();
	}
	
	/** Returns the path to netstat utility as declared by the user */
	private synchronized final String getNetstatPath() {
		String path = System.getProperty("net.netstat.path", "/bin,/sbin,/usr/bin,/usr/sbin");
		if (path != null && path.length() != 0)
			path = path.replace(',', ':').trim();
		return path;
	}
	
	/** Utility method used to obtain the name of an user based on an uid */
	private final String getPUID(final String uid) {
		final String pat = "([\\S&&[^:]]+):[\\S&&[^:]]+:"+uid+":";
		final Pattern pattern = PatternUtil.getPattern("uid_"+uid, pat);
		try {
			BufferedReader reader = new BufferedReader(new FileReader("/etc/passwd"));
			String line;
			while ((line = reader.readLine()) != null) {
				final Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		} catch (Exception e) { 
		}
		return "UNKNOWN";
	}

	/**
	 * Called in order to check if we should use netstat utility
	 * @return False if net.netstat.use is set to false
	 */
	private final boolean shouldRunNetstatTool() {
		String prop = System.getProperty("net.netstat.use", null);
		if (prop == null) return true;
		try {
			return Boolean.getBoolean(prop);
		} catch (Throwable t) { }
		return true;
	}

	/**
	 * Called in order to check if we should try and parse from proc the current connections
	 * @return False if net.netstat_dev.use is set to false
	 */
	private final boolean shouldRunNetstatDevTool() {
		String prop = System.getProperty("net.netstat_dev.use", null);
		if (prop == null) return true;
		try {
			return Boolean.getBoolean(prop);
		} catch (Throwable t) { }
		return true;
	}

	
	/**
	 * Method used to question for the connections currently openned
	 * @return The list of connections (as Connection objects)
	 */
	public final List<Connection> getConnections() {
		
		if (!shouldRunNetstatTool()) {
			return null;
		}
		final ArrayList<Connection> net = new ArrayList<Connection>();
		
		String netp = getNetstatPath();
		CommandResult cmdRes = exec.executeCommandReality("netstat -antep", null, 5 * 60 * 1000L, netp);
		String ret = cmdRes.getOutput();
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) {
				out.println(ret);
				out.println("Error running netstat");
			} else {
				logger.info(ret);
				logger.info("Error running netstat");
			}
			return null;
		}
		final Pattern pattern = PatternUtil.getPattern("netstat", netstatPattern);
		String lines[] = ret.split("\n"); 
		for (int i=0; i<lines.length; i++) {
			final Matcher matcher = pattern.matcher(lines[i]);
			if (matcher.find()) {
				final Connection c = new Connection();
				net.add(c);
				c.setProtocol(Connection.TCP_CONNECTION);
				final String localPort = matcher.group(2);
				final String remoteAddress = matcher.group(3);
				final String remotePort = matcher.group(4);
				final String state = matcher.group(5);
				final String uid = matcher.group(6);
				final String pid = matcher.group(7);
				final String pname = matcher.group(8);
				c.setPOwner(getPUID(uid));
				try { 
					c.setPID(Integer.parseInt(pid));
				} catch (Exception ex) {
					c.setPID(-1);
				}
				c.setPName(pname);
				try {
					c.setLocalPort(Integer.parseInt(localPort));
				} catch (Exception ex) {
					c.setLocalPort(0);
				}
				c.setRemoteAddress(remoteAddress);
				try {
					c.setRemotePort(Integer.parseInt(remotePort));
				} catch (Exception ex) {
					c.setRemotePort(0);
				}
				c.setStatus(state);
			} 
		}
		 cmdRes = exec.executeCommandReality("netstat -anuep", null, 5 * 60 * 1000L, netp);
		ret = cmdRes.getOutput(); 
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) {
				out.println(ret);
				out.println("Error running netstat");
			} else {
				logger.info(ret);
				logger.info("Error running netstat");
			}
			return net;
		}
		lines = ret.split("\n"); 
		for (int i=0; i<lines.length; i++) {
			final Matcher matcher = pattern.matcher(lines[i]);
			if (matcher.find()) {
				final Connection c = new Connection();
				net.add(c);
				c.setProtocol(Connection.UDP_CONNECTION);
				final String localPort = matcher.group(2);
				final String remoteAddress = matcher.group(3);
				final String remotePort = matcher.group(4);
				final String state = matcher.group(5);
				final String uid = matcher.group(6);
				final String pid = matcher.group(7);
				final String pname = matcher.group(8);
				c.setPOwner(getPUID(uid));
				try { 
					c.setPID(Integer.parseInt(pid));
				} catch (Exception ex) {
					c.setPID(-1);
				}
				c.setPName(pname);
				try {
					c.setLocalPort(Integer.parseInt(localPort));
				} catch (Exception ex) {
					c.setLocalPort(0);
				}
				c.setRemoteAddress(remoteAddress);
				try {
					c.setRemotePort(Integer.parseInt(remotePort));
				} catch (Exception ex) {
					c.setRemotePort(0);
				}
				c.setStatus(state);
			} 
		}
		cmdRes =exec.executeCommandReality("netstat -anwep", null, 5 * 60 * 1000L, netp); 
		ret = cmdRes.getOutput();
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) {
				out.println(ret);
				out.println("Error running netstat");
			} else {
				logger.info(ret);
				logger.info("Error running netstat");
			}
			return net;
		}
		lines = ret.split("\n"); 
		for (int i=0; i<lines.length; i++) {
			final Matcher matcher = pattern.matcher(lines[i]);
			if (matcher.find()) {
				
				final Connection c = new Connection();
				net.add(c);
				c.setProtocol(Connection.RAW_CONNECTION);
				final String localPort = matcher.group(2);
				final String remoteAddress = matcher.group(3);
				final String remotePort = matcher.group(4);
				final String state = matcher.group(5);
				final String uid = matcher.group(6);
				final String pid = matcher.group(7);
				final String pname = matcher.group(8);
				c.setPOwner(getPUID(uid));
				try { 
					c.setPID(Integer.parseInt(pid));
				} catch (Exception ex) {
					c.setPID(-1);
				}
				c.setPName(pname);
				try {
					c.setLocalPort(Integer.parseInt(localPort));
				} catch (Exception ex) {
					c.setLocalPort(0);
				}
				c.setRemoteAddress(remoteAddress);
				try {
					c.setRemotePort(Integer.parseInt(remotePort));
				} catch (Exception ex) {
					c.setRemotePort(0);
				}
				c.setStatus(state);
			} 
		}

		return net;
	}

	public static void main(String args[]) {
		
//		Pattern p = Pattern.compile("(\\d*[\\.:]\\d*[\\.:]\\d*[\\.:]*\\d*):([\\d\\*]+)");
//		Matcher m = p.matcher(":::*");
//		System.out.println(m.find());
//		if (true) return;
		
		for (int i=0; i<10; i++) {
		System.out.println("Start");
		NetstatHandler n = new NetstatHandler(System.out, null);
		List<Connection> c = n.getConnections();
		for (Connection conn : c) {
			System.out.println(conn);
		}
		System.out.println("end");
		}
	}

} // end of class NetstatHandler

 