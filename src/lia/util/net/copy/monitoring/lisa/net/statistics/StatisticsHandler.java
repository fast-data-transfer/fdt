/*
 * $Id: StatisticsHandler.java 354 2007-08-16 14:23:41Z ramiro $
 */
package lia.util.net.copy.monitoring.lisa.net.statistics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Statistics monitoring task (based on statistics of Andi Kleen)
 * Handler for statistics based on /proc/net/snmp and /proc/net/netstat
 * 
 * @author Ciprian Dobre
 */
public class StatisticsHandler {

	/** The stream to write output to */
	protected final PrintStream out;

	protected final Logger logger;

	/** The current obtained results... */

	private IPStatistics ipStatistics = null;
	private TCPStatistics tcpStatistics = null;
	private UDPStatistics udpStatistics = null;
	private TCPExtStatistics tcpExtStatistics = null;

	/** is this a 64 bits arch ? */
	private boolean is64BitArch = false;

	/** Auxiliary method that returns the value of a string */
	private final int getValueAsInt(final String val) {
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception ex) {
			return 0;
		}
	}

	/** Auxiliary method that returns the value of a string */
	private final long getValueAsLong(final String val) {
		try {
			return Long.parseLong(val.trim());
		} catch (Exception ex) {
			return 0L;
		}
	}
	
	private final HashMap<String, TreeMap<Long, String>> lastValues = new HashMap<String, TreeMap<Long,String>>();

	public final double getInstantValue(String key, long time, String newVal) {

		TreeMap<Long, String> h = null;
		if (!lastValues.containsKey(key)) {
			h = new TreeMap<Long, String>();
			lastValues.put(key, h);
		} else
			h = lastValues.get(key);
		if (h.size() == 0) {
			h.put(time, newVal);
			return 0D;
		}
		if (h.size() == 2)
			h.remove(h.firstKey());
		h.put(time, newVal);
		long first = h.firstKey();
		long last = h.lastKey();
		String s1 = h.get(first);
		String s2 = h.get(last);
		double d = 0D;
		try {
			d = Double.parseDouble(diffWithOverflowCheck(s2, s1));
		} catch (Throwable t) {
			d = 0D; 
		}
		d = d / ((last - first) / 1000D);
		return d;


//		TreeMap<Long, String> h = null;
//		if (!lastValues.containsKey(key)) {
//			h = new TreeMap<Long, String>();
//			lastValues.put(key, h);
//		} else
//			h = lastValues.get(key);
//		h.put(time, newVal);
//		if (h.size() == 1) return 0D;
//		long first = h.firstKey();
//		long last = h.lastKey();
//		String s1 = h.get(first);
//		String s2 = h.get(last);
//		double d = 0D;
//		try {
//			d = Double.parseDouble(diffWithOverflowCheck(s2, s1));
//		} catch (Throwable t) {
//			d = 0D; 
//		}
//		d = d / ((last - first) / 1000D);
//		long diffTime = 60 * 1000;
//		if (properties != null)
//			diffTime = properties.geti("stat.interval", 60, "The interval to consider when computing the instant values") * 1000;
//		if (diffTime <= 0) diffTime = 60 * 1000;
//		if ((last - first) < diffTime) {
//			h.clear();
//			h.put(first, s1);
//			h.put(last, s2);
//		} else {
//			long inner = last - diffTime;
//			String ss = diffWithOverflowCheck(s2, s1);
//			double dd = 0D;
//			try { dd = Double.parseDouble(ss); } catch (Throwable t) { dd = 0D; }
//			dd = dd * (inner - first) / (last - first);
//			h.clear();
//			h.put(inner, addWithOverflowCheck(s1, ""+dd));
//			h.put(last, s2);
//		}
//		return d;
	}
	
	/** Auxiliary method */
	private final boolean getBooleanOf(final String val) {
		return getValueAsInt(val) == 2 ? true : false;
	}

	/** Method used for parsing the values for IP */
	private final IPStatistics parseIPTable(final String headerLine, final String valuesLine, final IPStatistics ipstat) {

		final IPStatistics stat = (ipstat != null) ? ipstat : new IPStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("Forwarding")) { // ip forwarding
					stat.setForwarding(getBooleanOf(valEl));
					continue; // advance to the next token
				}
				if (el.equals("DefaultTTL")) { // default ttl value
					stat.setDefaultTTL(getValueAsLong(valEl));  
					continue;
				}
				if (el.equals("InReceives")) { // in received packets
					stat.setInReceived(valEl, getInstantValue("InReceives", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InHdrErrors")) { // in packets with header errors
					stat.setInHdrErrors(valEl, getInstantValue("InHdrErrors", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InAddrErrors")) { // in packets with incorrect address field
					stat.setInAddrErrors(valEl, getInstantValue("InAddrErrors", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ForwDatagrams")) { // number of forwarded datagrams
					stat.setForwDatagrams(valEl, getInstantValue("ForwDatagrams", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InUnknownProtos")) { // number of packets with uknown protocol
					stat.setInUnknownProtos(valEl, getInstantValue("InUnknownProtos", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InDiscards")) { // number of discarded packets
					stat.setInDiscards(valEl, getInstantValue("InDiscards", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InDelivers")) { // number of delivered packets
					stat.setInDelivers(valEl, getInstantValue("InDelivers", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutRequests")) { // number of requests sent out
					stat.setOutRequests(valEl, getInstantValue("OutRequest", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutDiscards")) { // number of outgoing dropped packets
					stat.setOutDiscards(valEl, getInstantValue("OutDiscards", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutNoRoutes")) { // number of dropped because of no route  found
					stat.setOutNoRoutes(valEl, getInstantValue("OutNoRoutes", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmTimeout")) { // number of fragments dropped after timeout
					stat.setReasmTimeout(valEl, getInstantValue("ReasmTimeout", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmReqds")) { // reassemblies required
					stat.setReasmReqds(valEl, getInstantValue("ReasmReqds", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmOKs")) { // reassembled ok
					stat.setReasmOKs(valEl, getInstantValue("ReasmsOKs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmFails")) { // failed reassembled
					stat.setReasmFails(valEl, getInstantValue("ReasmFails", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("FragOKs")) { // fragments received ok
					stat.setFragOKs(valEl, getInstantValue("FragOKs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("FragFails")) { // fragments failed
					stat.setFragFails(valEl, getInstantValue("FragFails", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("FragCreates")) { // created fragments
					stat.setFragCreates(valEl, getInstantValue("FragCreates", stat.getTime(), valEl));
				}
			}
		} catch (NoSuchElementException nse) { // done parsing
		}
		return stat;
	}

	/** Method used for parsing the statictical values for TCP stack */
	private final TCPStatistics parseTCPTable(final String headerLine, final String valuesLine, final TCPStatistics tcpstat) {
		final TCPStatistics stat = (tcpstat != null) ? tcpstat : new TCPStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("RtoMin")) { // minimum retransmission time
					stat.setRToMin(getValueAsLong(valEl));
					continue; // advance to the next token
				}
				if (el.equals("RtoMax")) { // maximum retransmission time
					stat.setRToMax(getValueAsLong(valEl));
					continue;
				}
				if (el.equals("ActiveOpens")) { // active connections openned
					stat.setActiveOpens(valEl, getInstantValue("ActiveOpens", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PassiveOpens")) { // passive connections openned
					stat.setPassiveOpens(valEl, getInstantValue("PassiveOpens", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("AttemptFails")) { // failed connection attempts
					stat.setAttemptFails(valEl, getInstantValue("AttemptFails", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("EstabResets")) { // connection resets received
					stat.setEstabResets(valEl, getInstantValue("EstabResets", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("CurrEstab")) { // connections currently established
					stat.setCurrEstab(getValueAsLong(valEl));
					continue;
				}
				if (el.equals("InSegs")) { // received segments
					stat.setInSegs(valEl, getInstantValue("InSegs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutSegs")) { // segments sent out
					stat.setOutSegs(valEl, getInstantValue("OutSegs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("RetransSegs")) { // retransmitted segments
					stat.setRetransSegs(valEl, getInstantValue("RetransSegs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InErrs")) { // bad segments receied
					stat.setInErrs(valEl, getInstantValue("InErrs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutRsts")) { // resets sent
					stat.setOutRsts(valEl, getInstantValue("OutRsts", stat.getTime(), valEl));
				}
			}
		} catch (NoSuchElementException nse) { // done parsing
		}
		return stat;
	}

	/** Method used for parsing the statictical values for UDP stack */
	private final UDPStatistics parseUDPTable(final String headerLine, final String valuesLine, final UDPStatistics udpstat) {
		final UDPStatistics stat = (udpstat != null) ? udpstat : new UDPStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("InDatagrams")) { // datagrams received
					stat.setInDatagrams(valEl, getInstantValue("InDatagrams", stat.getTime(), valEl));
					continue; // advance to the next token
				}
				if (el.equals("NoPorts")) { // datagrams with no correct ports
					stat.setNoPorts(valEl, getInstantValue("NoPorts", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InErrors")) { // error datagrams
					stat.setInErrors(valEl, getInstantValue("InErrors", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutDatagrams")) { // sent datagrams
					stat.setOutDatagrams(valEl, getInstantValue("OutDatagrams", stat.getTime(), valEl));
				}
			}
		} catch (NoSuchElementException nse) { // done parsing
		}
		return stat;
	}

	/** Method used for parsing the extended statistical values for TCP stack */
	private final TCPExtStatistics parseExtendedTCPTable(final String headerLine, final String valuesLine, final TCPExtStatistics tcpstat) {
		final TCPExtStatistics stat = (tcpstat != null) ? tcpstat : new TCPExtStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("SyncookiesSent")) { // syncookies sent
					stat.setSyncookiesSent(valEl, getInstantValue("SyncookiesSent", stat.getTime(), valEl));
					continue; // advance to the next token
				}
				if (el.equals("SyncookiesRecv")) { // syncookies received
					stat.setSyncookiesRecv(valEl, getInstantValue("SyncookiesRecv", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("SyncookiesFailed")) { // syncookies failed
					stat.setSyncookiesFailed(valEl, getInstantValue("SyncookiesFailed", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("EmbryonicRsts")) { // embryonic resets
					stat.setEmbryonicRsts(valEl, getInstantValue("EmbryonicRsts", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PruneCalled")) { // packets prunned because of buffer overflow
					stat.setPruneCalled(valEl, getInstantValue("PruneCalled", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("RcvPruned")) { // packets prunned from received queue
					stat.setRcvPruned(valEl, getInstantValue("RcvPruned", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OfoPruned")) { // packets prunned from out-of-order queue - overfow
					stat.setOfoPruned(valEl, getInstantValue("OfoPruned", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TW")) {
					stat.setTW(valEl, getInstantValue("TW", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TWRecycled")) {
					stat.setTWRecycled(valEl, getInstantValue("TWRecycled", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TWKilled")) {
					stat.setTWKilled(valEl, getInstantValue("TWKilled", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PAWSPassive")) {
					stat.setPAWSPassive(valEl, getInstantValue("PAWSPassive", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PAWSActive")) {
					stat.setPAWSActive(valEl, getInstantValue("PAWSActive", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PAWSEstab")) {
					stat.setPAWSEstab(valEl, getInstantValue("PAWEstab", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("DelayedACKs")) {
					stat.setDelayedACKs(valEl, getInstantValue("DelayedACKs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("DelayedACKLocked")) {
					stat.setDelayedACKLocked(valEl, getInstantValue("DelayedACKLocked", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("DelayedACKLost")) {
					stat.setDelayedACKLost(valEl, getInstantValue("DelayedACKLost", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ListenOverflows")) {
					stat.setListenOverflows(valEl, getInstantValue("ListenOverflows", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ListenDrops")) {
					stat.setListenDrops(valEl, getInstantValue("ListenDrops", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPPrequeued")) {
					stat.setTCPPrequeued(valEl, getInstantValue("TCPPrequeued", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDirectCopyFromBacklog")) {
					stat.setTCPDirectCopyFromBacklog(valEl, getInstantValue("TCPDirectCopyFromBacklog", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDirectCopyFromPrequeue")) {
					stat.setTCPDirectCopyFromPrequeue(valEl, getInstantValue("TCPDirectCopyFromPrequeue", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPPrequeueDropped")) {
					stat.setTCPPrequeueDropped(valEl, getInstantValue("TCPPrequeueDropped", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPHPHits")) {
					stat.setTCPHPHits(valEl, getInstantValue("TCPHPHits", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPHPHitsToUser")) {
					stat.setTCPHPHitsToUser(valEl, getInstantValue("TCPHPHitsToUser", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("SockMallocOOM")) {
					stat.setSockMallocOOM(valEl, getInstantValue("SockMallocOOM", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPPureAcks")) {
					stat.setTCPPureAcks(valEl, getInstantValue("TCPPureAcks", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPHPAcks")) {
					stat.setTCPHPAcks(valEl, getInstantValue("TCPHPAcks", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPRenoRecovery")) {
					stat.setTCPRenoRecovery(valEl, getInstantValue("TCPRenoRecovery", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSackRecovery")) {
					stat.setTCPSackRecovery(valEl, getInstantValue("TCPSackRecovery", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSACKReneging")) {
					stat.setTCPSACKReneging(valEl, getInstantValue("TCPSACKReneging", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPFACKReorder")) {
					stat.setTCPFACKReorder(valEl, getInstantValue("TCPFACKReorder", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSACKReorder")) {
					stat.setTCPSACKReorder(valEl, getInstantValue("TCPSACKReorder", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPRenoReorder")) {
					stat.setTCPRenoReorder(valEl, getInstantValue("TCPRenoReorder", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPTSReorder")) {
					stat.setTCPTSReorder(valEl, getInstantValue("TCPTSReorder", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPFullUndo")) {
					stat.setTCPFullUndo(valEl, getInstantValue("TCPFullUndo", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPPartialUndo")) {
					stat.setTCPPartialUndo(valEl, getInstantValue("TCPPartialUndo", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDSACKUndo")) {
					stat.setTCPDSACKUndo(valEl, getInstantValue("TCPDSACKUndo", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPLossUndo")) {
					stat.setTCPLossUndo(valEl, getInstantValue("TCPLossUndo", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPLoss")) {
					stat.setTCPLoss(valEl, getInstantValue("TCPLoss", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPLostRetransmit")) {
					stat.setTCPLostRetransmit(valEl, getInstantValue("TCPLostRetransmit", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPRenoFailures")) {
					stat.setTCPRenoFailures(valEl, getInstantValue("TCPRenoFailures", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSackFailures")) {
					stat.setTCPSackFailures(valEl, getInstantValue("TCPSackFailures", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPLossFailures")) {
					stat.setTCPLossFailures(valEl, getInstantValue("TCPLossFailures", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPFastRetrans")) {
					stat.setTCPFastRetrans(valEl, getInstantValue("TCPFastRetrans", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPForwardRetrans")) {
					stat.setTCPForwardRetrans(valEl, getInstantValue("TCPForwardRetrans", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSlowStartRetrans")) {
					stat.setTCPSlowStartRetrans(valEl, getInstantValue("TCPSlowStartRetrans", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPTimeouts")) {
					stat.setTCPTimeouts(valEl, getInstantValue("TCPTimeouts", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPRenoRecoveryFail")) {
					stat.setTCPRenoRecoveryFail(valEl, getInstantValue("TCPRenoRecoveryFail", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSackRecoveryFail")) {
					stat.setTCPSackRecoveryFail(valEl, getInstantValue("TCPSackRecoveryFail", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPSchedulerFailed")) {
					stat.setTCPSchedulerFailed(valEl, getInstantValue("TCPSchedulerFailed", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPRcvCollapsed")) {
					stat.setTCPRcvCollapsed(valEl, getInstantValue("TCPRcvCollapsed", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDSACKOldSent")) {
					stat.setTCPDSACKOldSent(valEl, getInstantValue("TCPDSACKOldSent", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDSACKOfoSent")) {
					stat.setTCPDSACKOfoSent(valEl, getInstantValue("TCPDSACKOfoSent", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDSACKRecv")) {
					stat.setTCPDSACKRecv(valEl, getInstantValue("TCPDSACKRecv", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPDSACKOfoRecv")) {
					stat.setTCPDSACKOfoRecv(valEl, getInstantValue("TCPDSACKOfoRecv", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortOnSyn")) {
					stat.setTCPAbortOnSyn(valEl, getInstantValue("TCPAbortOnSyn", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortOnData")) {
					stat.setTCPAbortOnData(valEl, getInstantValue("TCPAbortOnData", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortOnClose")) {
					stat.setTCPAbortOnClose(valEl, getInstantValue("TCPAbortOnClose", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortOnMemory")) {
					stat.setTCPAbortOnMemory(valEl, getInstantValue("TCPAbortOnMemory", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortOnTimeout")) {
					stat.setTCPAbortOnTimeout(valEl, getInstantValue("TCPAbortOnTimeout", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortOnLinger")) {
					stat.setTCPAbortOnLinger(valEl, getInstantValue("TCPAbortOnLinger", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPAbortFailed")) {
					stat.setTCPAbortFailed(valEl, getInstantValue("TCPAbortFailed", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("TCPMemoryPressures")) {
					stat.setTCPMemoryPressures(valEl, getInstantValue("TCPMemoryPressures", stat.getTime(), valEl));
					continue;
				}
			}
		} catch (NoSuchElementException nse) { // done parsing
		}
		return stat;
	}

	/**
	 * The constructor
	 * @param properties We need the properties of the module in order to interogate for paths
	 * @param out The stream to write output to
	 */
	public StatisticsHandler(final PrintStream out, final Logger logger) {
		this.out = out;
		this.logger = logger;
	}

	public void getStatistics() {

		ipStatistics = null;
		tcpStatistics = null;
		udpStatistics = null;
		tcpExtStatistics = null;

		// first parse /proc/net/snmp
		try {
			BufferedReader br = new BufferedReader(new FileReader("/proc/net/snmp"));
			while (true) {
				// we need to read two lines at a time, the header and the actual values
				final String header = br.readLine();
				if (header == null) // end of stream
					break;
				final String values = br.readLine();
				if (values == null) // end of stream
					break;
				if (header.startsWith("Ip: ") && values.startsWith("Ip: ")) {
					ipStatistics = parseIPTable(header.substring(4), values.substring(4), null);
					if (logger.isLoggable(Level.FINEST))
						logger.log(Level.FINE, ipStatistics.toString());
					continue;
				}
				if (header.startsWith("Tcp: ") && values.startsWith("Tcp: ")) {
					tcpStatistics = parseTCPTable(header.substring(5), values.substring(5), null);
					if (logger.isLoggable(Level.FINEST))
						logger.log(Level.FINE, tcpStatistics.toString());
					continue;
				}
				if (header.startsWith("Udp: ") && values.startsWith("Udp: ")) {
					udpStatistics = parseUDPTable(header.substring(5), values.substring(5), null);
					if (logger.isLoggable(Level.FINEST))
						logger.log(Level.FINE, udpStatistics.toString());
					continue;
				}
			}
			br.close();
		} catch (FileNotFoundException fne) {
			if (out != null)
				out.println("File /proc/net/snmp not found.");
			else
				logger.info("File /proc/net/snmp not found.");
		} catch (IOException ioe) {
			if (out != null) out.println(ioe.getLocalizedMessage());
			else logger.warning(ioe.getLocalizedMessage());
		}

		// second parse /proc/net/netstat
		try {
			BufferedReader br = new BufferedReader(new FileReader("/proc/net/netstat"));
			while (true) {
				// we need to read two lines at a time, the header and the actual values
				final String header = br.readLine();
				if (header == null) // end of stream
					break;
				final String values = br.readLine();
				if (values == null) // end of stream
					break;
				if (header.startsWith("TcpExt: ") && values.startsWith("TcpExt: ")) {
					tcpExtStatistics = parseExtendedTCPTable(header.substring(8), values.substring(8), null);
					if (logger.isLoggable(Level.FINEST))
						logger.log(Level.FINE, tcpExtStatistics.toString());
					continue;
				}
			}
			br.close();
		} catch (FileNotFoundException fne) {
			if (out != null)
				out.println("File /proc/net/netstat not found.");
			else
				logger.info("File /proc/net/netstat not found.");
		} catch (IOException ioe) {
			if (out != null) out.println(ioe.getLocalizedMessage());
			else logger.warning(ioe.getLocalizedMessage());
		}
	}

	public final IPStatistics getIPStatistics() {
		return ipStatistics;
	}

	public final TCPStatistics getTCPStatistics() {
		return tcpStatistics;
	}

	public final UDPStatistics getUDPStatistics() {
		return udpStatistics;
	}

	public final TCPExtStatistics getTCPExtStatistics() {
		return tcpExtStatistics;
	}

	final static NumberFormat nf = NumberFormat.getInstance();
	
	static {
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(4);
	}
	
	private final String prepareString(String str) {
		
		// first try to make it double
		try {
			double d = Double.parseDouble(str);
			if (!Double.isInfinite(d) && !Double.isNaN(d)) {
				String n = nf.format(d);
				n = n.replaceAll(",", "");
				return n;
			}
		} catch (Throwable t) { }
		
		if (!str.contains(".")) {
			return str+".0000";
		}
		int nr = str.lastIndexOf('.')+1;
		nr = str.length() - nr;
		for (int i=nr; i<4; i++)
			str += "0";
		return str;
	}

	
	public String addWithOverflowCheck(String newVal, String oldVal)
	throws NumberFormatException {

		if (newVal == null)
			return oldVal;
		if (oldVal == null)
			return newVal;

		if (is64BitArch) {
			String str = prepareString(newVal);
			BigDecimal newv = null;
			try {
				newv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			str = prepareString(oldVal);
			BigDecimal oldv = null;
			try {
				oldv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			return newv.add(oldv).toString();
		}
//		otherwise we still assume 32 bits arch
		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return addWithOverflowCheck(newVal, oldVal);
		}
//		so it's still 32 bits arch
		return "" + (newv + oldv);
	}

	public String divideWithOverflowCheck(String newVal, String oldVal)
	throws NumberFormatException {

		if (is64BitArch) {
			String str = prepareString(newVal);
			BigDecimal newv = null;
			try {
				newv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			str = prepareString(oldVal);
			BigDecimal oldv = null;
			try {
				oldv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			return newv.divide(oldv, BigDecimal.ROUND_FLOOR).toString();
		}
//		otherwise we still assume 32 bits arch
		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return divideWithOverflowCheck(newVal, oldVal);
		}
//		so it's still 32 bits arch
		return "" + (newv / oldv);
	}

	public String mulWithOverflowCheck(String newVal, String oldVal)
	throws NumberFormatException {
		if (is64BitArch) {
			String str = prepareString(newVal);
			BigDecimal newv = null;
			try {
				newv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			str = prepareString(oldVal);
			BigDecimal oldv = null;
			try {
				oldv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			return newv.multiply(oldv).toString();
		}
//		otherwise we still assume 32 bits arch
		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return mulWithOverflowCheck(newVal, oldVal);
		}
//		so it's still 32 bits arch
		return "" + (newv * oldv);
	}

	public String diffWithOverflowCheck(String newVal, String oldVal)
	throws NumberFormatException {
		if (is64BitArch) {
			String str = prepareString(newVal);
			BigDecimal newv = null;
			try {
				newv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			str = prepareString(oldVal);
			BigDecimal oldv = null;
			try {
				oldv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception "+t+" for "+str);
			}
			if (newv.compareTo(oldv) >= 0)
				return newv.subtract(oldv).toString();
			BigInteger overflow = new BigInteger("1").shiftLeft(64);
			BigDecimal d = new BigDecimal(overflow.toString());
			return newv.add(d).subtract(oldv).toString();
		}
//		otherwise we still assume 32 bits arch
		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return diffWithOverflowCheck(newVal, oldVal);
		}
//		so it's still 32 bits arch
		if (newv >= oldv) {
			return "" + (newv - oldv);
		}
		long vmax = 1L << 32; // 32 bits
		return "" + (newv - oldv + vmax);
	}
	
	public static void main(String args[]) {
		
		StatisticsHandler h = new StatisticsHandler(System.out, null);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);

		double d = h.getInstantValue("test", 1000, "105395052");
		d = h.getInstantValue("test", 6000, "105395986");
		System.out.println(nf.format(d));
	}
	
	public void clear() {
		ipStatistics = null;
		tcpStatistics = null;
		udpStatistics = null;
		tcpExtStatistics = null;
	}

} // end of class StatisticsHandler


