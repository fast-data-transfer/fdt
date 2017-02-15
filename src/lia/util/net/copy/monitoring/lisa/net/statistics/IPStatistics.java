/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.statistics;

import java.text.NumberFormat;

import lia.util.net.copy.monitoring.lisa.net.Statistics;

/**
 * Statistics regarding the ip protocol suite
 * 
 * @author Ciprian Dobre
 */
public class IPStatistics extends Statistics {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1988671591829311032L;

	/** Is ip forwarding enabled or disabled */
	protected boolean forwarding = false;
	
	/** Default TTL value */
	protected long defaultTTL = 0L;
	
	/** Total number of received packets */
	protected String inReceives = "0";
	protected double inReceivesI = 0D;
	
	/** Packets received with incorrect headers */
	protected String inHdrErrors = "0";
	protected double inHdrErrorsI = 0D;
	
	/** Packets received with invalid address field */
	protected String inAddrErrors = "0";
	protected double inAddrErrorsI = 0D;
	
	/** Number of forwarded datagrams */
	protected String forwDatagrams = "0";
	protected double forwDatagramsI = 0D;
	
	/** Number of packets with unknown protocol number */
	protected String inUnknownProtos = "0";
	protected double inUnknownProtosI = 0D;
	
	/** The number of discarded packets */
	protected String inDiscards = "0";
	protected double inDiscardsI = 0D;
	
	/** The number of packets delivered */
	protected String inDelivers = "0";
	protected double inDeliversI = 0D;
	
	/** Requests sent out */
	protected String outRequests = "0";
	protected double outRequestsI = 0D;
	
	/** Outgoing packets dropped */
	protected String outDiscards = "0";
	protected double outDiscardsI = 0D;
	
	/** Outgoing dropped packets because of missing route */
	protected String outNoRoutes = "0";
	protected double outNoRoutesI = 0D;
	
	/** Fragments dropped after time out */
	protected String reasmTimeout = "0";
	protected double reasmTimeoutI = 0D;
	
	/** Reassemblies requires */
	protected String reasmReqds = "0";
	protected double reasmReqdsI = 0D;
	
	/** Packets reassembled ok */
	protected String reasmOKs = "0";
	protected double reasmOKsI = 0D;
	
	/** Packets reassembled with failure */
	protected String reasmFails = "0";
	protected double reasmFailsI = 0D;
	
	/** Fragments received ok */
	protected String fragOKs = "0";
	protected double fragOKsI = 0D;
	
	/** Fragments failed */
	protected String fragFails = "0";
	protected double fragFailsI = 0D;
	
	/** Created fragments */
	protected String fragCreates = "0";
	protected double fragCreatesI = 0D;
	
	public IPStatistics() {
		super();
	}
	
	public final void setForwarding(final boolean forwarding) {
		this.forwarding = forwarding;
	}
	
	public final boolean getForwarding() {
		return forwarding;
	}
	
	public final String getForwardingAsString() {
		return "Forwarding is "+(forwarding ? "enabled" : "disabled");
	}
	
	public final void setDefaultTTL(final long defaultTTL) {
		this.defaultTTL = defaultTTL;
	}
	
	public final long getDefaultTTL() {
		return defaultTTL;
	}

	public final String getDefaultTTLAsString() {
		return "Default TTL is "+defaultTTL;
	}
	
	public final void setInReceived(final String packetsReceived, final double packetsReceivedI) {
		this.inReceives = packetsReceived;
		this.inReceivesI = packetsReceivedI;
	}
	
	public final String getInReceived() {
		return inReceives;
	}

	public final double getInReceivedI() {
		return inReceivesI;
	}
	
	static final NumberFormat nf = NumberFormat.getInstance();
	static {
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
	}

	public final String getInReceivedAsString() {
		return inReceives+" total packets received ["+nf.format(inReceivesI)+"]";
	}
	
	public final void setInHdrErrors(final String hdrErrors, final double hdrErrorsI) {
		this.inHdrErrors = hdrErrors;
		this.inHdrErrorsI = hdrErrorsI;
	}
	
	public final String getInHdrErrors() {
		return inHdrErrors;
	}

	public final double getInHdrErrorsI() {
		return inHdrErrorsI;
	}

	public final String getInHdrErrorsAsString() {
		return inHdrErrors+" with invalid headers";
	}
	
	public final void setInAddrErrors(final String inAddrErrors, final double inAddrErrorsI) {
		this.inAddrErrors = inAddrErrors;
		this.inAddrErrorsI = inAddrErrorsI;
	}
	
	public final String getInAddrErrors() {
		return inAddrErrors;
	}

	public final double getInAddrErrorsI() {
		return inAddrErrorsI;
	}

	public final String getInAddrErrorsAsString() {
		return inAddrErrors+" with invalid addresses";
	}
	
	public final void setForwDatagrams(final String forwDatagrams, final double forwDatagramsI) {
		this.forwDatagrams = forwDatagrams;
		this.forwDatagramsI = forwDatagramsI;
	}
	
	public final String getForwDatagrams() {
		return forwDatagrams;
	}

	public final double getForwDatagramsI() {
		return forwDatagramsI;
	}

	public final String getForwDatagramsAsString() {
		return forwarding+" datagrams forwarded";
	}
	
	public final void setInUnknownProtos(final String inUnknownProtos, final double inUnknownProtosI) {
		this.inUnknownProtos = inUnknownProtos;
		this.inUnknownProtosI = inUnknownProtosI;
	}
	
	public final String getInUnknownProtos() {
		return inUnknownProtos;
	}

	public final double getInUnknownProtosI() {
		return inUnknownProtosI;
	}

	public final String getInUnknownProtosAsString() {
		return inUnknownProtos+" with unknown protocol";
	}
	
	public final void setInDiscards(final String inDiscards, final double inDiscardsI) {
		this.inDiscards = inDiscards;
		this.inDiscardsI = inDiscardsI;
	}
	
	public final String getInDiscards() {
		return inDiscards;
	}

	public final double getInDiscardsI() {
		return inDiscardsI;
	}

	public final String getInDiscardsAsString() {
		return inDiscards+" incoming packets discarded";
	}
	
	public final void setInDelivers(final String inDelivers, final double inDeliversI) {
		this.inDelivers = inDelivers;
		this.inDeliversI = inDeliversI;
	}
	
	public final String getInDelivers() {
		return inDelivers;
	}

	public final double getInDeliversI() {
		return inDeliversI;
	}

	public final String getInDeliversAsString() {
		return inDelivers+" incoming packets delivered";
	}
	
	public final void setOutRequests(final String outRequests, final double outRequestsI) {
		this.outRequests = outRequests;
		this.outRequestsI = outRequestsI;
	}

	public final String getOutRequests() {
		return outRequests;
	}

	public final double getOutRequestsI() {
		return outRequestsI;
	}

	public final String getOutRequestsAsString() {
		return outRequests+" requests sent out";
	}
	
	public final void setOutDiscards(final String outDiscards, final double outDiscardsI) {
		this.outDiscards = outDiscards;
		this.outDiscardsI = outDiscardsI;
	}
	
	public final String getOutDiscards() {
		return outDiscards;
	}

	public final double getOutDiscardsI() {
		return outDiscardsI;
	}
	
	public final String getOutDiscardsAsString() {
		return outDiscards+" outgoing packets dropped";
	}
	
	public final void setOutNoRoutes(final String outNoRoutes, final double outNoRoutesI) {
		this.outNoRoutes = outNoRoutes;
		this.outNoRoutesI = outNoRoutesI;
	}
	
	public final String getOutNoRoutes() {
		return outNoRoutes;
	}

	public final double getOutNoRoutesI() {
		return outNoRoutesI;
	}

	public final String getOutNoRoutesAsString() {
		return outNoRoutes+" dropped because of missing route";
	}
	
	public final void setReasmTimeout(final String reasmTimeout, final double reasmTimeoutI) {
		this.reasmTimeout = reasmTimeout;
		this.reasmTimeoutI = reasmTimeoutI;
	}
	
	public final String getReasmTimeout() {
		return reasmTimeout;
	}

	public final double getReasmTimeoutI() {
		return reasmTimeoutI;
	}

	public final String getReasmTimeoutAsString() {
		return reasmTimeout+" fragments dropped after timeout";
	}
	
	public final void setReasmReqds(final String reasmReqds, final double reasmReqdsI) {
		this.reasmReqds = reasmReqds;
		this.reasmReqdsI = reasmReqdsI;
	}
	
	public final String getReasmReqds() {
		return reasmReqds;
	}

	public final double getReasmReqdsI() {
		return reasmReqdsI;
	}

	public final String getReasmReqdsAsString() {
		return reasmReqds+" reassemblies required";
	}
	
	public final void setReasmOKs(final String reasmOKs, final double reasmOKsI) {
		this.reasmOKs = reasmOKs;
		this.reasmOKsI = reasmOKsI;
	}
	
	public final String getReasmOKs() {
		return reasmOKs;
	}

	public final double getReasmOKsI() {
		return reasmOKsI;
	}

	public final String getReasmOKsAsString() {
		return reasmOKs+" packets reassembled ok";
	}

	public final void setReasmFails(final String reasmFails, final double reasmFailsI) {
		this.reasmFails = reasmFails;
		this.reasmFailsI = reasmFailsI;
	}
	
	public final String getReasmFails() {
		return reasmFails;
	}

	public final double getReasmFailsI() {
		return reasmFailsI;
	}

	public final String getReasmFailsAsString() {
		return reasmFails+" packet reassembles failed";
	}
	
	public final void setFragOKs(final String fragOKs, final double fragOKsI) {
		this.fragOKs = fragOKs;
		this.fragOKsI = fragOKsI;
	}
	
	public final String getFragOKs() {
		return fragOKs;
	}

	public final double getFragOKsI() {
		return fragOKsI;
	}

	public final String getFragOKsAsString() {
		return fragOKs+" fragments received ok";
	}
	
	public final void setFragFails(final String fragFails, final double fragFailsI) {
		this.fragFails = fragFails;
		this.fragFailsI = fragFailsI;
	}
	
	public final String getFragFails() {
		return fragFails;
	}

	public final double getFragFailsI() {
		return fragFailsI;
	}

	public final String getFragFailsAsString() {
		return fragFails+" fragments failed";
	}
	
	public final void setFragCreates(final String fragCreates, final double fragCreatesI) {
		this.fragCreates = fragCreates;
		this.fragCreatesI = fragCreatesI;
	}

	public final String getFragCreates() {
		return fragCreates;
	}

	public final double getFragCreatesI() {
		return fragCreatesI;
	}

	public final String getFragCreatesAsString() {
		return fragCreates+" fragments created";
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("IP Statistics:\n");
		buf.append(getForwardingAsString()).append("\n");
		buf.append(getDefaultTTLAsString()).append("\n");
		buf.append(getInReceivedAsString()).append("\n");
		buf.append(getInHdrErrorsAsString()).append("\n");
		buf.append(getInAddrErrorsAsString()).append("\n");
		buf.append(getForwDatagramsAsString()).append("\n");
		buf.append(getInUnknownProtosAsString()).append("\n");
		buf.append(getInDiscardsAsString()).append("\n");
		buf.append(getInDeliversAsString()).append("\n");
		buf.append(getOutRequestsAsString()).append("\n");
		buf.append(getOutDiscardsAsString()).append("\n");
		buf.append(getOutNoRoutesAsString()).append("\n");
		buf.append(getReasmTimeoutAsString()).append("\n");
		buf.append(getReasmReqdsAsString()).append("\n");
		buf.append(getReasmOKsAsString()).append("\n");
		buf.append(getReasmFailsAsString()).append("\n");
		buf.append(getFragOKsAsString()).append("\n");
		buf.append(getFragFailsAsString()).append("\n");
		buf.append(getFragCreatesAsString()).append("\n");
		return buf.toString();
	}
	
} // end of class IPStatistics


