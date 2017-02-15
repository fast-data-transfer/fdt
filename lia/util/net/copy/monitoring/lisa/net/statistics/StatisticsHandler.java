
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


public class StatisticsHandler {

	
	protected final PrintStream out;

	protected final Logger logger;

	

	private IPStatistics ipStatistics = null;
	private TCPStatistics tcpStatistics = null;
	private UDPStatistics udpStatistics = null;
	private TCPExtStatistics tcpExtStatistics = null;

	
	private boolean is64BitArch = false;

	
	private final int getValueAsInt(final String val) {
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception ex) {
			return 0;
		}
	}

	
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








































	}
	
	
	private final boolean getBooleanOf(final String val) {
		return getValueAsInt(val) == 2 ? true : false;
	}

	
	private final IPStatistics parseIPTable(final String headerLine, final String valuesLine, final IPStatistics ipstat) {

		final IPStatistics stat = (ipstat != null) ? ipstat : new IPStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("Forwarding")) { 
					stat.setForwarding(getBooleanOf(valEl));
					continue; 
				}
				if (el.equals("DefaultTTL")) { 
					stat.setDefaultTTL(getValueAsLong(valEl));  
					continue;
				}
				if (el.equals("InReceives")) { 
					stat.setInReceived(valEl, getInstantValue("InReceives", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InHdrErrors")) { 
					stat.setInHdrErrors(valEl, getInstantValue("InHdrErrors", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InAddrErrors")) { 
					stat.setInAddrErrors(valEl, getInstantValue("InAddrErrors", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ForwDatagrams")) { 
					stat.setForwDatagrams(valEl, getInstantValue("ForwDatagrams", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InUnknownProtos")) { 
					stat.setInUnknownProtos(valEl, getInstantValue("InUnknownProtos", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InDiscards")) { 
					stat.setInDiscards(valEl, getInstantValue("InDiscards", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InDelivers")) { 
					stat.setInDelivers(valEl, getInstantValue("InDelivers", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutRequests")) { 
					stat.setOutRequests(valEl, getInstantValue("OutRequest", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutDiscards")) { 
					stat.setOutDiscards(valEl, getInstantValue("OutDiscards", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutNoRoutes")) { 
					stat.setOutNoRoutes(valEl, getInstantValue("OutNoRoutes", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmTimeout")) { 
					stat.setReasmTimeout(valEl, getInstantValue("ReasmTimeout", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmReqds")) { 
					stat.setReasmReqds(valEl, getInstantValue("ReasmReqds", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmOKs")) { 
					stat.setReasmOKs(valEl, getInstantValue("ReasmsOKs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("ReasmFails")) { 
					stat.setReasmFails(valEl, getInstantValue("ReasmFails", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("FragOKs")) { 
					stat.setFragOKs(valEl, getInstantValue("FragOKs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("FragFails")) { 
					stat.setFragFails(valEl, getInstantValue("FragFails", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("FragCreates")) { 
					stat.setFragCreates(valEl, getInstantValue("FragCreates", stat.getTime(), valEl));
				}
			}
		} catch (NoSuchElementException nse) { 
		}
		return stat;
	}

	
	private final TCPStatistics parseTCPTable(final String headerLine, final String valuesLine, final TCPStatistics tcpstat) {
		final TCPStatistics stat = (tcpstat != null) ? tcpstat : new TCPStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("RtoMin")) { 
					stat.setRToMin(getValueAsLong(valEl));
					continue; 
				}
				if (el.equals("RtoMax")) { 
					stat.setRToMax(getValueAsLong(valEl));
					continue;
				}
				if (el.equals("ActiveOpens")) { 
					stat.setActiveOpens(valEl, getInstantValue("ActiveOpens", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PassiveOpens")) { 
					stat.setPassiveOpens(valEl, getInstantValue("PassiveOpens", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("AttemptFails")) { 
					stat.setAttemptFails(valEl, getInstantValue("AttemptFails", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("EstabResets")) { 
					stat.setEstabResets(valEl, getInstantValue("EstabResets", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("CurrEstab")) { 
					stat.setCurrEstab(getValueAsLong(valEl));
					continue;
				}
				if (el.equals("InSegs")) { 
					stat.setInSegs(valEl, getInstantValue("InSegs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutSegs")) { 
					stat.setOutSegs(valEl, getInstantValue("OutSegs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("RetransSegs")) { 
					stat.setRetransSegs(valEl, getInstantValue("RetransSegs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InErrs")) { 
					stat.setInErrs(valEl, getInstantValue("InErrs", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutRsts")) { 
					stat.setOutRsts(valEl, getInstantValue("OutRsts", stat.getTime(), valEl));
				}
			}
		} catch (NoSuchElementException nse) { 
		}
		return stat;
	}

	
	private final UDPStatistics parseUDPTable(final String headerLine, final String valuesLine, final UDPStatistics udpstat) {
		final UDPStatistics stat = (udpstat != null) ? udpstat : new UDPStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("InDatagrams")) { 
					stat.setInDatagrams(valEl, getInstantValue("InDatagrams", stat.getTime(), valEl));
					continue; 
				}
				if (el.equals("NoPorts")) { 
					stat.setNoPorts(valEl, getInstantValue("NoPorts", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("InErrors")) { 
					stat.setInErrors(valEl, getInstantValue("InErrors", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OutDatagrams")) { 
					stat.setOutDatagrams(valEl, getInstantValue("OutDatagrams", stat.getTime(), valEl));
				}
			}
		} catch (NoSuchElementException nse) { 
		}
		return stat;
	}

	
	private final TCPExtStatistics parseExtendedTCPTable(final String headerLine, final String valuesLine, final TCPExtStatistics tcpstat) {
		final TCPExtStatistics stat = (tcpstat != null) ? tcpstat : new TCPExtStatistics();
		final StringTokenizer tok = new StringTokenizer(headerLine);
		final StringTokenizer valTok = new StringTokenizer(valuesLine);
		String el;
		try {
			while ((el = tok.nextToken(" \t\n")) != null) {
				String valEl = valTok.nextToken(" \t\n");
				if (valEl == null) break;
				if (el.equals("SyncookiesSent")) { 
					stat.setSyncookiesSent(valEl, getInstantValue("SyncookiesSent", stat.getTime(), valEl));
					continue; 
				}
				if (el.equals("SyncookiesRecv")) { 
					stat.setSyncookiesRecv(valEl, getInstantValue("SyncookiesRecv", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("SyncookiesFailed")) { 
					stat.setSyncookiesFailed(valEl, getInstantValue("SyncookiesFailed", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("EmbryonicRsts")) { 
					stat.setEmbryonicRsts(valEl, getInstantValue("EmbryonicRsts", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("PruneCalled")) { 
					stat.setPruneCalled(valEl, getInstantValue("PruneCalled", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("RcvPruned")) { 
					stat.setRcvPruned(valEl, getInstantValue("RcvPruned", stat.getTime(), valEl));
					continue;
				}
				if (el.equals("OfoPruned")) { 
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
		} catch (NoSuchElementException nse) { 
		}
		return stat;
	}

	
	public StatisticsHandler(final PrintStream out, final Logger logger) {
		this.out = out;
		this.logger = logger;
	}

	public void getStatistics() {

		ipStatistics = null;
		tcpStatistics = null;
		udpStatistics = null;
		tcpExtStatistics = null;

		
		try {
			BufferedReader br = new BufferedReader(new FileReader("/proc/net/snmp"));
			while (true) {
				
				final String header = br.readLine();
				if (header == null) 
					break;
				final String values = br.readLine();
				if (values == null) 
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

		
		try {
			BufferedReader br = new BufferedReader(new FileReader("/proc/net/netstat"));
			while (true) {
				
				final String header = br.readLine();
				if (header == null) 
					break;
				final String values = br.readLine();
				if (values == null) 
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

		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return addWithOverflowCheck(newVal, oldVal);
		}

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

		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return divideWithOverflowCheck(newVal, oldVal);
		}

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

		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return mulWithOverflowCheck(newVal, oldVal);
		}

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

		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return diffWithOverflowCheck(newVal, oldVal);
		}

		if (newv >= oldv) {
			return "" + (newv - oldv);
		}
		long vmax = 1L << 32; 
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

} 


