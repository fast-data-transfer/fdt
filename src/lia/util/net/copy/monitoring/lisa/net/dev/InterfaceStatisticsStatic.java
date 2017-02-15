/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.dev;

import java.util.LinkedList;

import lia.util.net.copy.monitoring.lisa.net.Statistics;

/**
 * 
 * @author Ciprian Dobre
 */
public class InterfaceStatisticsStatic extends Statistics {

	/********* Types of interface encapsulations *****************/
	
	public static final byte TYPE_ETHERNET = 0;
	public static final byte TYPE_FIBER = 1;
	
	public static final byte PORT_TP = 1;
	public static final byte PORT_AUI = 2;
	public static final byte PORT_BNC = 4;
	public static final byte PORT_MII = 8;
	public static final byte PORT_FIBRE = 16;
	
	public static final byte SUPPORTED_10BaseT_Half = 1;
	public static final byte SUPPORTED_10BaseT_Full = 2;
	public static final byte SUPPORTED_100BaseT_Half = 4;
	public static final byte SUPPORTED_100BaseT_Full = 8;
	public static final byte SUPPORTED_1000BaseT_Half = 16;
	public static final byte SUPPORTED_1000BaseT_Full = 32;
	
	public static final byte LINK_HALF = 0;
	public static final byte LINK_DUPLEX = 1;
	

	/** The name of the interface */
	protected final String name;
	
	/** The encapsulation type */
	protected byte encap = 0;
	
	/** The hardware address of the interface */
	protected String hwAddr;
	
	protected byte supportedPorts = -1; // -1 means error
	protected byte supportedLinkModes = -1; // -1 means error
	protected boolean supportsAutoNegotiation = false;
	
	protected int maxSpeed = -1; // supported speed in Mb/s
	protected byte duplex = -1; // -1 means unknown
	protected byte port = -1; // -1 means unknown
	
	/** The constructor - only the name is final */
	public InterfaceStatisticsStatic(final String name) {
		super();
		this.name = name;
	}

	/** Method used to retrieve the name of the interface */
	public final String getName() {
		return name;
	}
	
	/** Setter for the type of encapsulation for the network interface */
	public final void setEncap(final byte type) {
		this.encap = type;
	}
	
	/** Getter for the type of encapsulation */
	public final byte getEncap() {
		return encap;
	}
	
	/** Getter for the type of encapsulation */
	public final String getEncapAsString() {
		switch (encap) {
		case TYPE_ETHERNET : return "Ethernet";
		case TYPE_FIBER : return "Fiber";
		}
		return "Unknown";
	}
	
	/** Setter for the hardware address */
	public final void setHwAddr(final String hwAddr) {
		this.hwAddr = hwAddr;
	}
	
	/** Getter for the hardware address */
	public final String getHwAddr() {
		return hwAddr;
	}
	
	/** Utility method for printing out the properties */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("IFNAME [name=").append(name);
		buf.append(",encap=").append(getEncapAsString());
		buf.append(",hwaddr=").append(hwAddr);
		buf.append(",supportedPort=").append(supportedPorts);
		buf.append(",supportedLinkModes=").append(supportedLinkModes);
		buf.append(",supportsAutoNeg=").append(supportsAutoNegotiation);
		buf.append(",maxSpeed=").append(maxSpeed);
		buf.append(",duplex=").append(duplex);
		buf.append(",port=").append(port);
		buf.append("]");
		return buf.toString();
	}
	public byte getSupportedPorts() {
		return supportedPorts;
	}
	
	private boolean flagSet(byte b, byte flag) {
		return ((b & flag) == flag);
	}
	
	public String[] getSupportedPortsAsString() {
		
		if (supportedPorts < 0) return new String[] { "UNKNOWN" };
		
		final LinkedList<String> list = new LinkedList<String>();
		if (flagSet(supportedPorts, PORT_TP)) list.addLast("TP");
		if (flagSet(supportedPorts, PORT_AUI)) list.addLast("AUI");
		if (flagSet(supportedPorts, PORT_BNC)) list.addLast("BNC");
		if (flagSet(supportedPorts, PORT_MII)) list.addLast("MII");
		if (flagSet(supportedPorts, PORT_FIBRE)) list.addLast("FIBRE");
		
		if (list.size() == 0) return new String[] { "NONE" };
		return list.toArray(new String[0]); 
	}

	public void setSupportedPorts(byte supportedPorts) {
		if (this.supportedPorts < 0) this.supportedPorts = 0;
		this.supportedPorts = (byte)(this.supportedPorts | supportedPorts);
	}

	public byte getSupportedLinkModes() {
		return supportedLinkModes;
	}
	
	public String[] getSupportedLinkModesAsString() {
		if (supportedLinkModes < 0) return new String[] { "UNKNOWN" };
		final LinkedList<String> list = new LinkedList<String>();
		if (flagSet(supportedLinkModes, SUPPORTED_10BaseT_Half)) list.add("10BaseT_Half");
		if (flagSet(supportedLinkModes, SUPPORTED_10BaseT_Full)) list.add("10BaseT_Full");
		if (flagSet(supportedLinkModes, SUPPORTED_100BaseT_Half)) list.add("100BaseT_Half");
		if (flagSet(supportedLinkModes, SUPPORTED_100BaseT_Full)) list.add("100BaseT_Full");
		if (flagSet(supportedLinkModes, SUPPORTED_1000BaseT_Half)) list.add("1000BaseT_Half");
		if (flagSet(supportedLinkModes, SUPPORTED_1000BaseT_Full)) list.add("1000BaseT_Full");
		if (list.size() == 0) return new String[] { "NONE" };
		return list.toArray(new String[0]);
	}

	public void setSupportedLinkModes(byte supportedLinkModes) {
		if (this.supportedLinkModes < 0) this.supportedLinkModes = 0;
		this.supportedLinkModes = (byte)(this.supportedLinkModes | supportedLinkModes);
	}

	public boolean supportsAutoNegotiation() {
		return supportsAutoNegotiation;
	}

	public void setSupportsAutoNegotiation(boolean supportsAutoNegotiation) {
		this.supportsAutoNegotiation = supportsAutoNegotiation;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}
	
	public String getMaxSpeedAsString() {
		if (maxSpeed < 0)
			return "UNKNOWN";
		return maxSpeed+" Mb/s";
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public byte getDuplex() {
		return duplex;
	}

	public void setDuplex(byte duplex) {
		this.duplex = duplex;
	}

	public byte getPort() {
		return port;
	}

	public void setPort(byte port) {
		this.port = port;
	}

} // end of class InterfaceStatisticsStatic

