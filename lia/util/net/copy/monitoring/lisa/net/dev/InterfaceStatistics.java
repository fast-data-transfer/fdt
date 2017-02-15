
package lia.util.net.copy.monitoring.lisa.net.dev;

import lia.util.net.copy.monitoring.lisa.net.Statistics;


public class InterfaceStatistics extends Statistics {

	
	private static final long serialVersionUID = 1988671591829311032L;
	
	
	protected final String name;

	
	
	
	protected String ipv4;
	
	
	protected String bcastipv4;
	
	
	protected String maskipv4;
	
	
	protected int mtu = 1500; 
	
	

	protected double rxPackets = 0D;
	protected double rxErrors = 0D;
	protected double rxDropped = 0D;
	protected double rxOverruns = 0D;
	protected double rxFrame = 0D;
	protected double rx = 0D;
	
	
	
	protected double txPackets = 0D;
	protected double txErrors = 0D;
	protected double txDropped = 0D;
	protected double txOverruns = 0D;
	protected double txCarrier = 0D;
	protected double tx = 0D;
	
	
	protected double collisions = 0D;
	protected boolean canCompress = false;
	protected double compressed = 0D;
	protected int txqueuelen = 0;
	
	
	public InterfaceStatistics(final String name) {
		super();
		this.name = name;
	}
	
	
	public final String getName() {
		return name;
	}
	
	
	public final void setIPv4(final String address) {
		this.ipv4 = address;
	}
	
	
	public final String getIPv4() {
		return ipv4;
	}
	
	
	public final void setBcastv4(final String bcast) {
		this.bcastipv4 = bcast;
	}
	
	
	public final String getBcastv4() {
		return bcastipv4;
	}
	
	
	public final void setMaskv4(final String mask) {
		this.maskipv4 = mask;
	}
	
	
	public final String getMaskv4() {
		return maskipv4;
	}
	
	
	public final void setMTU(final int mtu) {
		this.mtu = mtu;
	}
	
	
	public final int getMTU() {
		return mtu;
	}

	public final void setRX(final double rx) {
		this.rx = rx;
	}
	
	public final double getRX() {
		return rx;
	}
	
	public final void setRXPackets(final double packets) {
		this.rxPackets = packets;
	}
	
	public final double getRXPackets() {
		return rxPackets;
	}
	
	public final void setRXErrors(final double errors) {
		this.rxErrors = errors;
	}
	
	public final double getRXErrors() {
		return rxErrors;
	}
	
	public final void setRXDropped(final double dropped) {
		this.rxDropped = dropped;
	}
	
	public final double getRXDropped() {
		return rxDropped;
	}
	
	public final void setRXOverruns(final double overruns) {
		this.rxOverruns = overruns;
	}
	
	public final double getRXOverruns() {
		return rxOverruns;
	}
	
	public final void setRXFrame(final double frame) {
		this.rxFrame = frame;
	}
	
	public final double getRXFrame() {
		return rxFrame;
	}

	public final void setTX(final double tx) {
		this.tx = tx;
	}
	
	public final double getTX() {
		return tx;
	}

	public final void setTXPackets(final double packets) {
		this.txPackets = packets;
	}
	
	public final double getTXPackets() {
		return txPackets;
	}
	
	public final void setTXErrors(final double errors) {
		this.txErrors = errors;
	}
	
	public final double getTXErrors() {
		return txErrors;
	}
	
	public final void setTXDropped(final double dropped) {
		this.txDropped = dropped;
	}
	
	public final double getTXDropped() {
		return txDropped;
	}
	
	public final void setTXOverruns(final double overruns) {
		this.txOverruns = overruns;
	}
	
	public final double getTXOverruns() {
		return txOverruns;
	}
	
	public final void setTXCarrier(final double carrier) {
		this.txCarrier = carrier;
	}
	
	public final double getTXCarrier() {
		return txCarrier;
	}
	
	public final void setCompressed(final double compressed) {
		this.compressed = compressed;
	}
	
	public final double getCompressed() {
		return compressed;
	}
	
	public final void setCanCompress(boolean canCompress) {
		this.canCompress = canCompress;
	}
	
	public final boolean getCanCompress() {
		return canCompress;
	}
	
	public final void setCollisions(final double collisions) {
		this.collisions = collisions;
	}
	
	public final double getCollisions() {
		return collisions;
	}
	
	public final void setTXQueueLen(final int txqueuelen) {
		this.txqueuelen = txqueuelen;
	}
	
	public final int getTXQueueLen() {
		return txqueuelen;
	}
	
	
	public String toString() {
		
		StringBuffer buf = new StringBuffer();
		buf.append("IFNAME [name=").append(name);
		buf.append(",ipv4=").append(ipv4);
		buf.append(",bcast4=").append(bcastipv4);
		buf.append(",mask4=").append(maskipv4);
		buf.append(",mtu=").append(mtu);
		buf.append(",rx_packets=").append(rxPackets);
		buf.append(",tx_packets=").append(txPackets);
		buf.append("]");
		return buf.toString();
	}

	
} 


