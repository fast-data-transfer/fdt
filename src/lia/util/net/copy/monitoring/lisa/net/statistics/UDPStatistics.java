/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.statistics;

import lia.util.net.copy.monitoring.lisa.net.Statistics;

/**
 * Statistics regarding the udp protocol suite
 * @author Ciprian Dobre
 */
public class UDPStatistics extends Statistics {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1988671591829311032L;

	/** Datagrams received */
	protected String inDatagrams = "0";
	protected double inDatagramsI = 0D;
	
	/** Datagrams to unknown ports received */
	protected String noPorts = "0";
	protected double noPortsI = 0D;
	
	/** Datagrams with errors received */
	protected String inErrors = "0";
	protected double inErrorsI = 0D;
	
	/** Sent datagrams */
	protected String outDatagrams = "0";
	protected double outDatagramsI = 0D;
	
	public UDPStatistics() {
		super();
	}
	
	public final void setInDatagrams(final String inDatagrams, final double inDatagramsI) {
		this.inDatagrams = inDatagrams;
		this.inDatagramsI = inDatagramsI;
	}
	
	public final String getInDatagrams() {
		return inDatagrams;
	}

	public final double getInDatagramsI() {
		return inDatagramsI;
	}

	public final String getInDatagramsAsString() {
		return inDatagrams+" datagrams received";
	}
	
	public final void setNoPorts(final String noPorts, final double noPortsI) {
		this.noPorts = noPorts;
		this.noPortsI = noPortsI;
	}
	
	public final String getNoPorts() {
		return noPorts;
	}

	public final double getNoPortsI() {
		return noPortsI;
	}

	public final String getNoPortsAsString() {
		return noPorts+" datagrams to unknown port received";
	}
	
	public final void setInErrors(final String inErrors, final double inErrorsI) {
		this.inErrors = inErrors;
		this.inErrorsI = inErrorsI;
	}
	
	public final String getInErrors() {
		return inErrors;
	}

	public final double getInErrorsI() {
		return inErrorsI;
	}

	public final String getInErrorsAsString() {
		return inErrors+" datagrams received with errors";
	}

	public final void setOutDatagrams(final String outDatagrams, final double outDatagramsI) {
		this.outDatagrams = outDatagrams;
		this.outDatagramsI = outDatagramsI;
	}
	
	public final String getOutDatagrams() {
		return outDatagrams;
	}

	public final double getOutDatagramsI() {
		return outDatagramsI;
	}

	public final String getOutDatagramsAsString() {
		return outDatagrams+" datagrams sent";
	}
	
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append("UDP Statistics:\n");
		buf.append(getInDatagramsAsString()).append("\n");
		buf.append(getNoPortsAsString()).append("\n");
		buf.append(getInErrorsAsString()).append("\n");
		buf.append(getOutDatagramsAsString()).append("\n");
		return buf.toString();
	}
	
} // end of class UDPStatistics

