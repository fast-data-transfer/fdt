/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.statistics;

import lia.util.net.copy.monitoring.lisa.net.Statistics;

/**
 * Statistics regarding the tcp protocol suite
 * @author Ciprian Dobre
 */
public class TCPStatistics extends Statistics {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1988671591829311032L;

	/** The minimum restransmission timeout value */
	protected long rToMin = 0;
	
	/** The maximum retransmission timeout value */
	protected long rToMax = 0;
	
	/** Active connection openned */
	protected String activeOpens = "0";
	protected double activeOpensI = 0D;
	
	/** Passive connection openned */
	protected String passiveOpens = "0";
	protected double passiveOpensI = 0D;
	
	/** Failed connection attempts */
	protected String attemptFails = "0";
	protected double attemptFailsI = 0D;
	
	/** Connection resets received */
	protected String estabResets = "0";
	protected double estabResetsI = 0D;
	
	/** Connections established */
	protected long currEstab = 0;
	
	/** Segments received */
	protected String inSegs = "0";
	protected double inSegsI = 0;
	
	/** Segments sent */
	protected String outSegs = "0";
	protected double outSegsI = 0D;
	
	/** Segments retransmitted */
	protected String retransSegs = "0";
	protected double retransSegsI = 0D;
	
	/** Bad segments received */
	protected String inErrs = "0";	
	protected double inErrsI = 0D;	
	
	/** Resets sent */
	protected String outRsts = "0";
	protected double outRstsI = 0D;
	
	public TCPStatistics() {
		super();
	}
	
	public final void setRToMin(final long rToMin) {
		this.rToMin = rToMin;
	}
	
	public final long getRToMin() {
		return rToMin;
	}
	
	public final String getRToMinAsString() {
		return "The minimum retransmission timeout value is "+rToMin;
	}
	
	public final void setRToMax(final long rToMax) {
		this.rToMax = rToMax;
	}
	
	public final long getRToMax() {
		return rToMax;
	}
	
	public final String getRToMaxAsString() {
		return "The maximum retransmission timeout value is "+rToMax;
	}
	
	public final void setActiveOpens(final String activeOpens, final double activeOpensI) {
		this.activeOpens = activeOpens;
		this.activeOpensI = activeOpensI;
	}
	
	public final String getActiveOpens() {
		return activeOpens;
	}

	public final double getActiveOpensI() {
		return activeOpensI;
	}

	public final String getActiveOpensAsString() {
		return activeOpens+" active connections openings";
	}
	
	public final void setPassiveOpens(final String passiveOpens, final double passiveOpensI) {
		this.passiveOpens = passiveOpens;
		this.passiveOpensI = passiveOpensI;
	}
	
	public final String getPassiveOpens() {
		return passiveOpens;
	}

	public final double getPassiveOpensI() {
		return passiveOpensI;
	}

	public final String getPassiveOpensAsString() {
		return passiveOpens+" passive connection openings";
	}
	
	public final void setAttemptFails(final String attemptFails, final double attemptFailsI) {
		this.attemptFails = attemptFails;
		this.attemptFailsI = attemptFailsI;
	}
	
	public final String getAttemptFails() {
		return attemptFails;
	}

	public final double getAttemptFailsI() {
		return attemptFailsI;
	}

	public final String getAttemptFailsAsString() {
		return attemptFails+" failed connection attempts";
	}
	
	public final void setEstabResets(final String estabResets, final double estabResetsI) {
		this.estabResets = estabResets;
		this.estabResetsI = estabResetsI;
	}
	
	public final String getEstabResets() {
		return estabResets;
	}

	public final double getEstabResetsI() {
		return estabResetsI;
	}

	public final String getEstabResetsAsString() {
		return estabResets+" connection resets received";
	}
	
	public final void setCurrEstab(final long currEstab) {
		this.currEstab = currEstab;
	}
	
	public final long getCurrEstab() {
		return currEstab;
	}
	
	public final String getCurrEstabAsString() {
		return currEstab+" connections established";
	}
	
	public final void setInSegs(final String inSegs, final double inSegsI) {
		this.inSegs = inSegs;
		this.inSegsI = inSegsI;
	}
	
	public final String getInSegs() {
		return inSegs;
	}

	public final double getInSegsI() {
		return inSegsI;
	}

	public final String getInSegsAsString() {
		return inSegs+" segments received";
	}

	public final void setOutSegs(final String outSegs, final double outSegsI) {
		this.outSegs = outSegs;
		this.outSegsI = outSegsI;
	}
	
	public final String getOutSegs() {
		return outSegs;
	}

	public final double getOutSegsI() {
		return outSegsI;
	}

	public final String getOutSegsAsString() {
		return outSegs+" segments send out";
	}
	
	public final void setRetransSegs(final String retransSegs, final double retransSegsI) {
		this.retransSegs = retransSegs;
		this.retransSegsI = retransSegsI;
	}
	
	public final String getRetransSegs() {
		return retransSegs;
	}

	public final double getRetransSegsI() {
		return retransSegsI;
	}

	public final String getRetransSegsAsString() {
		return retransSegs+" segments retransmited";
	}
	
	public final void setInErrs(final String inErrs, final double inErrsI) {
		this.inErrs = inErrs;
		this.inErrsI = inErrsI;
	}
	
	public final String getInErrs() {
		return inErrs;
	}

	public final double getInErrsI() {
		return inErrsI;
	}

	public final String getInErrsAsString() {
		return inErrs+" bad segments received";
	}
	
	public final void setOutRsts(final String outRsts, final double outRstsI) {
		this.outRsts = outRsts;
		this.outRstsI = outRstsI;
	}
	
	public final String getOutRsts() {
		return outRsts;
	}

	public final double getOutRstsI() {
		return outRstsI;
	}

	public final String getOutRstsAsString() {
		return outRsts+" resets sent";
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("TCP Statistics:\n");
		buf.append(getRToMinAsString()).append("\n");
		buf.append(getRToMaxAsString()).append("\n");
		buf.append(getActiveOpensAsString()).append("\n");
		buf.append(getPassiveOpensAsString()).append("\n");
		buf.append(getAttemptFailsAsString()).append("\n");
		buf.append(getEstabResetsAsString()).append("\n");
		buf.append(getCurrEstabAsString()).append("\n");
		buf.append(getInSegsAsString()).append("\n");
		buf.append(getOutSegsAsString()).append("\n");
		buf.append(getRetransSegsAsString()).append("\n");
		buf.append(getInErrsAsString()).append("\n");
		buf.append(getOutRstsAsString()).append("\n");
		return buf.toString();
	}
	
} // end of class TCPStatistics


