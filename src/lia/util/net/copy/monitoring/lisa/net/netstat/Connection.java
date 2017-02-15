/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.netstat;

import lia.util.net.copy.monitoring.lisa.net.Statistics;

/**
 * Informations about a given connection
 * 
 * @author Ciprian Dobre
 */
public class Connection extends Statistics {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1988671591829311032L;

	/** Types of connection protocol ***/
	public static final byte TCP_CONNECTION = 0;
	public static final byte UDP_CONNECTION = 1;
	public static final byte RAW_CONNECTION = 2;
	
	/** The protocol of the connection (can be tcp, udp or raw) */
	protected byte protocol;
	
	/** The owner of the connection (username) */
	protected String powner;
	
	/** The pid of the owner process */ 
	protected int pid;
	
	/** The name of the program owning the connection */
	protected String pname;
	
	/** Local port */
	protected int localPort;
	
	/** Remote address of the connection */
	protected String remoteAddress;
	
	/** Remote port */
	protected int remotePort;
	
	/** Status of the connection */
	protected String status;
	
	public Connection() {
		super();
	}
	
	public final void setProtocol(final byte protocol) {
		this.protocol = protocol;
	}
	
	public final byte getProtocol() {
		return protocol;
	}
	
	public final String getProtocolAsString() {
		switch (protocol) {
		case TCP_CONNECTION: return "TCP";
		case UDP_CONNECTION: return "UDP";
		case RAW_CONNECTION: return "RAW";
		}
		return "UNKNOWN";
	}
	
	public final void setPOwner(final String owner) {
		this.powner = owner;
	}
	
	public final String getPOwner() {
		return powner;
	}
	
	public final void setPID(final int pid) {
		this.pid = pid;
	}
	
	public final int getPID() {
		return pid;
	}
	
	public final void setPName(final String pname) {
		this.pname = pname;
	}
	
	public final String getPName() {
		return pname;
	}
	
	public final void setLocalPort(final int localPort) {
		this.localPort = localPort;
	}
	
	public final int getLocalPort() {
		return localPort;
	}
	
	public final void setRemoteAddress(final String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
	
	public final String getRemoteAddress() {
		return remoteAddress;
	}
	
	public final void setRemotePort(final int remotePort) {
		this.remotePort = remotePort;
	}
	
	public final int getRemotePort() {
		return remotePort;
	}
	
	public final void setStatus(final String status) {
		this.status = status;
	}
	
	public final String getStatus() {
		return status;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[Prot=").append(getProtocolAsString());
		buf.append(",POwner=").append(powner);
		buf.append(",PID=").append(pid);
		buf.append(",PName=").append(pname);
		buf.append(",LPort=").append(localPort);
		buf.append(",RAddress=").append(remoteAddress);
		buf.append(",RPort=").append(remotePort);
		buf.append(",Status=").append(status);
		buf.append("]");
		return buf.toString();
	}
	
} // end of class Connection


