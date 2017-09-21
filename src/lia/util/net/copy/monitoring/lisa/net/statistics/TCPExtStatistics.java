/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.statistics;

import lia.util.net.copy.monitoring.lisa.net.Statistics;

/**
 * Extended statistics regarding the tcp protocol suite
 *
 * @author Ciprian Dobre
 */
public class TCPExtStatistics extends Statistics {

    /**
     * <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1988671591829311032L;

    /**
     * SYN cookies sent
     */
    protected String syncookiesSent;
    protected double syncookiesSentI;

    /**
     * SYN cookies received
     */
    protected String syncookiesRecv;
    protected double syncookiesRecvI;

    /**
     * SYN cookies failed
     */
    protected String syncookiesFailed;
    protected double syncookiesFailedI;

    /**
     * Packets pruned from receive queue because of buffer overrun
     */
    protected String pruneCalled;
    protected double pruneCalledI;

    /**
     * resets received because of embryonic sockets
     */
    protected String embryonicRsts;
    protected double embryonicRstsI;

    /**
     * Packets prunned from receive queue
     */
    protected String rcvPruned;
    protected double rcvPrunedI;

    /**
     * Packets prunned from out-of-order queue because of overflow
     */
    protected String ofoPruned;
    protected double ofoPrunedI;

    /**
     * tcp sockets finished time wait in fast timer
     */
    protected String tW;
    protected double tWI;

    /**
     * sockets recicled by time stamp
     */
    protected String tWRecycled;
    protected double tWRecycledI;

    /**
     * TCP sockets finished time wait in slow timer
     */
    protected String tWKilled;
    protected double tWKilledI;

    /**
     * passive connections rejected because of timestamp
     */
    protected String pAWSPassive;
    protected double pAWSPassiveI;

    /**
     * active connection rejected because of timestamp
     */
    protected String pAWSActive;
    protected double pAWSActiveI;

    /**
     * packets rejected in established connection because of timestamp
     */
    protected String pAWSEstab;
    protected double pAWSEstabI;

    /**
     * delayed acks sent
     */
    protected String delayedACKs;
    protected double delayedACKsI;

    /**
     * delayed acks further delayed because of socket lock
     */
    protected String delayedACKLocked;
    protected double delayedACKLockedI;

    /**
     * Quick ack mode activated times
     */
    protected String delayedACKLost;
    protected double delayedACKLostI;

    /**
     * how many times the listening queue of a socket overflowed
     */
    protected String listenOverflows;
    protected double listenOverflowsI;

    /**
     * SYNs to LISTEN sockets ignored
     */
    protected String listenDrops;
    protected double listenDropsI;

    /**
     * packets directly queued to recvmsg - urgwent packets
     */
    protected String tCPPrequeued;
    protected double tCPPrequeuedI;

    /**
     * packets directly received from backlog
     */
    protected String tCPDirectCopyFromBacklog;
    protected double tCPDirectCopyFromBacklogI;

    /**
     * packets directly received from prequeue
     */
    protected String tCPDirectCopyFromPrequeue;
    protected double tCPDirectCopyFromPrequeueI;

    /**
     * packets dropped from prequeue
     */
    protected String tCPPrequeueDropped;
    protected double tCPPrequeueDroppedI;

    /**
     * packet headers predicted
     */
    protected String tCPHPHits;
    protected double tCPHPHitsI;

    /**
     * packet headers predicted and directly queued to user
     */
    protected String tCPHPHitsToUser;
    protected double tCPHPHitsToUserI;

    /**
     * how many times oom was encoutered when sending
     */
    protected String sockMallocOOM;
    protected double sockMallocOOMI;

    /**
     * Pure ack recvd
     */
    protected String tCPPureAcks;
    protected double tCPPureAcksI;

    protected String tCPHPAcks;
    protected double tCPHPAcksI;

    protected String tCPRenoRecovery;
    protected double tCPRenoRecoveryI;

    protected String tCPSackRecovery;
    protected double tCPSackRecoveryI;

    protected String tCPSACKReneging;
    protected double tCPSACKRenegingI;

    protected String tCPFACKReorder;
    protected double tCPFACKReorderI;

    protected String tCPSACKReorder;
    protected double tCPSACKReorderI;

    protected String tCPRenoReorder;
    protected double tCPRenoReorderI;

    protected String tCPtSReorder;
    protected double tCPtSReorderI;

    protected String tCPFullUndo;
    protected double tCPFullUndoI;

    protected String tCPPartialUndo;
    protected double tCPPartialUndoI;

    protected String tCPDSACKUndo;
    protected double tCPDSACKUndoI;

    protected String tCPLossUndo;
    protected double tCPLossUndoI;

    protected String tCPLoss;
    protected double tCPLossI;

    protected String tCPLostRetransmit;
    protected double tCPLostRetransmitI;

    protected String tCPRenoFailures;
    protected double tCPRenoFailuresI;

    protected String tCPSackFailures;
    protected double tCPSackFailuresI;

    protected String tCPLossFailures;
    protected double tCPLossFailuresI;

    protected String tCPFastRetrans;
    protected double tCPFastRetransI;

    protected String tCPForwardRetrans;
    protected double tCPForwardRetransI;

    protected String tCPSlowStartRetrans;
    protected double tCPSlowStartRetransI;

    protected String tCPtimeouts;
    protected double tCPtimeoutsI;

    protected String tCPRenoRecoveryFail;
    protected double tCPRenoRecoveryFailI;

    protected String tCPSackRecoveryFail;
    protected double tCPSackRecoveryFailI;

    protected String tCPSchedulerFailed;
    protected double tCPSchedulerFailedI;

    protected String tCPRcvCollapsed;
    protected double tCPRcvCollapsedI;

    protected String tCPDSACKOldSent;
    protected double tCPDSACKOldSentI;

    protected String tCPDSACKOfoSent;
    protected double tCPDSACKOfoSentI;

    protected String tCPDSACKRecv;
    protected double tCPDSACKRecvI;

    protected String tCPDSACKOfoRecv;
    protected double tCPDSACKOfoRecvI;

    protected String tCPAbortOnSyn;
    protected double tCPAbortOnSynI;

    protected String tCPAbortOnData;
    protected double tCPAbortOnDataI;

    protected String tCPAbortOnClose;
    protected double tCPAbortOnCloseI;

    protected String tCPAbortOnMemory;
    protected double tCPAbortOnMemoryI;

    protected String tCPAbortOntimeout;
    protected double tCPAbortOntimeoutI;

    protected String tCPAbortOnLinger;
    protected double tCPAbortOnLingerI;

    protected String tCPAbortFailed;
    protected double tCPAbortFailedI;

    protected String tCPMemoryPressures;
    protected double tCPMemoryPressuresI;

    public TCPExtStatistics() {
        super();
    }

    public final void setSyncookiesSent(final String syncookiesSent, final double syncookiesSentI) {
        this.syncookiesSent = syncookiesSent;
        this.syncookiesSentI = syncookiesSentI;
    }

    public final String getSyncookiesSent() {
        return syncookiesSent;
    }

    public final double getSyncookiesSentI() {
        return syncookiesSentI;
    }

    public final String getSyncookiesSentAsString() {
        return syncookiesSent + " SYN cookies sent";
    }

    public final void setSyncookiesRecv(final String syncookiesRecv, final double syncookiesRecvI) {
        this.syncookiesRecv = syncookiesRecv;
        this.syncookiesRecvI = syncookiesRecvI;
    }

    public final String getSyncookiesRecv() {
        return syncookiesRecv;
    }

    public final double getSyncookiesRecvI() {
        return syncookiesRecvI;
    }

    public final String getSyncookiesRecvAsString() {
        return syncookiesRecv + " SYN cookies received";
    }

    public final void setSyncookiesFailed(final String syncookiesFailed, final double syncookiesFailedI) {
        this.syncookiesFailed = syncookiesFailed;
        this.syncookiesFailedI = syncookiesFailedI;
    }

    public final String getSyncookiesFailed() {
        return syncookiesFailed;
    }

    public final double getSyncookiesFailedI() {
        return syncookiesFailedI;
    }

    public final String getSyncookiesFailedAsString() {
        return syncookiesFailed + " invalid SYN cookies received";
    }

    public final void setEmbryonicRsts(final String embryonicRsts, final double embryonicRstsI) {
        this.embryonicRsts = embryonicRsts;
        this.embryonicRstsI = embryonicRstsI;
    }

    public final String getEmbryonicRsts() {
        return embryonicRsts;
    }

    public final double getEmbryonicRstsI() {
        return embryonicRstsI;
    }

    public final String getEmbryonicRstsAsString() {
        return embryonicRsts + " resets received for embryonic SYN_RECV sockets";
    }

    public final void setPruneCalled(final String pruneCalled, final double pruneCalledI) {
        this.pruneCalled = pruneCalled;
        this.pruneCalledI = pruneCalledI;
    }

    public final String getPruneCalled() {
        return pruneCalled;
    }

    public final double getPruneCalledI() {
        return pruneCalledI;
    }

    public final String getPruneCalledAsString() {
        return pruneCalled + " packets pruned from receive queue because of socket buffer overrun";
    }

    public final void setRcvPruned(final String rcvPruned, final double rcvPrunedI) {
        this.rcvPruned = rcvPruned;
        this.rcvPrunedI = rcvPrunedI;
    }

    public final String getRcvPruned() {
        return rcvPruned;
    }

    public final double getRcvPrunedI() {
        return rcvPrunedI;
    }

    public final String getRcvPrunedAsString() {
        return rcvPruned + " packets pruned from receive queue";
    }

    public final void setOfoPruned(final String ofoPruned, final double ofoPrunedI) {
        this.ofoPruned = ofoPruned;
        this.ofoPrunedI = ofoPrunedI;
    }

    public final String getOfoPruned() {
        return ofoPruned;
    }

    public final double getOfoPrunedI() {
        return ofoPrunedI;
    }

    public final String getOfoPrunedAsString() {
        return ofoPruned + " packets dropped from out-of-order queue because of socket buffer overrun";
    }

    public final void setTW(final String tW, final double tWI) {
        this.tW = tW;
        this.tWI = tWI;
    }

    public final String getTW() {
        return tW;
    }

    public final double getTWI() {
        return tWI;
    }

    public final String getTWAsString() {
        return tW + " TCP sockets finished time wait in fast timer";
    }

    public final void setTWRecycled(final String tWRecycled, final double tWRecycledI) {
        this.tWRecycled = tWRecycled;
        this.tWRecycledI = tWRecycledI;
    }

    public final String getTWRecycled() {
        return tWRecycled;
    }

    public final double getTWRecycledI() {
        return tWRecycledI;
    }

    public final String getTWRecycledAsString() {
        return tWRecycled + " time wait sockets recycled by time stamp";
    }

    public final void setTWKilled(final String tWKilled, final double tWKilledI) {
        this.tWKilled = tWKilled;
        this.tWKilledI = tWKilledI;
    }

    public final String getTWKilled() {
        return tWKilled;
    }

    public final double getTWKilledI() {
        return tWKilledI;
    }

    public final String getTWKilledAsString() {
        return tWKilled + " TCP sockets finished time wait in slow timer";
    }

    public final void setPAWSPassive(final String pAWSPassive, final double pAWSPassiveI) {
        this.pAWSPassive = pAWSPassive;
        this.pAWSPassiveI = pAWSPassiveI;
    }

    public final String getPAWSPassive() {
        return pAWSPassive;
    }

    public final double getPAWSPassiveI() {
        return pAWSPassiveI;
    }

    public final String getPAWSPassiveAsString() {
        return pAWSPassive + " passive connections rejected because of time stamp";
    }

    public final void setPAWSActive(final String pAWSActive, final double pAWSActiveI) {
        this.pAWSActive = pAWSActive;
        this.pAWSActiveI = pAWSActiveI;
    }

    public final String getPAWSActive() {
        return pAWSActive;
    }

    public final double getPAWSActiveI() {
        return pAWSActiveI;
    }

    public final String getPAWSActiveAsString() {
        return pAWSActive + " active connections rejected because of time stamp";
    }

    public final void setPAWSEstab(final String pAWSEstab, final double pAWSEstabI) {
        this.pAWSEstab = pAWSEstab;
        this.pAWSEstabI = pAWSEstabI;
    }

    public final String getPAWSEstab() {
        return pAWSEstab;
    }

    public final double getPAWSEstabI() {
        return pAWSEstabI;
    }

    public final String getPAWSEstabAsString() {
        return pAWSEstab + " packets rejects in established connections because of timestamp";
    }

    public final void setDelayedACKs(final String delayedACKs, final double delayedACKsI) {
        this.delayedACKs = delayedACKs;
        this.delayedACKsI = delayedACKsI;
    }

    public final String getDelayedACKs() {
        return delayedACKs;
    }

    public final double getDelayedACKsI() {
        return delayedACKsI;
    }

    public final String getDelayedACKsAsString() {
        return delayedACKs + " delayed acks sent";
    }

    public final void setDelayedACKLocked(final String delayedACKLocked, final double delayedACKLockedI) {
        this.delayedACKLocked = delayedACKLocked;
        this.delayedACKLockedI = delayedACKLockedI;
    }

    public final String getDelayedACKLocked() {
        return delayedACKLocked;
    }

    public final double getDelayedACKLockedI() {
        return delayedACKLockedI;
    }

    public final String getDelayedACKLockedAsString() {
        return delayedACKLocked + " delayed acks further delayed because of locked socket";
    }

    public final void setDelayedACKLost(final String delayedACKLost, final double delayedACKLostI) {
        this.delayedACKLost = delayedACKLost;
        this.delayedACKLostI = delayedACKLostI;
    }

    public final String getDelayedACKLost() {
        return delayedACKLost;
    }

    public final double getDelayedACKLostI() {
        return delayedACKLostI;
    }

    public final String getDelayedACKLostAsString() {
        return "Quick ack mode was activated " + delayedACKLost + " times";
    }

    public final void setListenOverflows(final String listenOverflows, final double listenOverflowsI) {
        this.listenOverflows = listenOverflows;
        this.listenOverflowsI = listenOverflowsI;
    }

    public final String getListenOverflows() {
        return listenOverflows;
    }

    public final double getListenOverflowsI() {
        return listenOverflowsI;
    }

    public final String getListenOverflowsAsString() {
        return listenOverflows + " times the listen queue of a socket overflowed";
    }

    public final void setListenDrops(final String listenDrops, final double listenDropsI) {
        this.listenDrops = listenDrops;
        this.listenDropsI = listenDropsI;
    }

    public final String getListenDrops() {
        return listenDrops;
    }

    public final double getListenDropsI() {
        return listenDropsI;
    }

    public final String getListenDropsAsString() {
        return listenDrops + " SYNs to LISTEN sockets ignored";
    }

    public final void setTCPPrequeued(final String tCPPrequeued, final double tCPPrequeuedI) {
        this.tCPPrequeued = tCPPrequeued;
        this.tCPPrequeuedI = tCPPrequeuedI;
    }

    public final String getTCPPrequeued() {
        return tCPPrequeued;
    }

    public final double getTCPPrequeuedI() {
        return tCPPrequeuedI;
    }

    public final String getTCPPrequeuedAsString() {
        return tCPPrequeued + " packets directly queued to recvmsg prequeue";
    }

    public final void setTCPDirectCopyFromBacklog(final String tCPDirectCopyFromBacklog, final double tCPDirectCopyFromBacklogI) {
        this.tCPDirectCopyFromBacklog = tCPDirectCopyFromBacklog;
        this.tCPDirectCopyFromBacklogI = tCPDirectCopyFromBacklogI;
    }

    public final String getTCPDirectCopyFromBacklog() {
        return tCPDirectCopyFromBacklog;
    }

    public final double getTCPDirectCopyFromBacklogI() {
        return tCPDirectCopyFromBacklogI;
    }

    public final String getTCPDirectCopyFromBacklogAsString() {
        return tCPDirectCopyFromBacklog + " packets directly received from backlog";
    }

    public final void setTCPDirectCopyFromPrequeue(final String tCPDirectCopyFromPrequeue, final double tCPDirectCopyFromPrequeueI) {
        this.tCPDirectCopyFromPrequeue = tCPDirectCopyFromPrequeue;
        this.tCPDirectCopyFromPrequeueI = tCPDirectCopyFromPrequeueI;
    }

    public final String getTCPDirectCopyFromPrequeue() {
        return tCPDirectCopyFromPrequeue;
    }

    public final double getTCPDirectCopyFromPrequeueI() {
        return tCPDirectCopyFromPrequeueI;
    }

    public final String getTCPDirectCopyFromPrequeueAsString() {
        return tCPDirectCopyFromPrequeue + " packets directly received from prequeue";
    }

    public final void setTCPPrequeueDropped(final String tCPPrequeueDropped, final double tCPPrequeueDroppedI) {
        this.tCPPrequeueDropped = tCPPrequeueDropped;
        this.tCPPrequeueDroppedI = tCPPrequeueDroppedI;
    }

    public final String getTCPPrequeueDropped() {
        return tCPPrequeueDropped;
    }

    public final double getTCPPrequeueDroppedI() {
        return tCPPrequeueDroppedI;
    }

    public final String getTCPPrequeueDroppedAsString() {
        return tCPPrequeueDropped + " packets dropped from prequeue";
    }

    public final void setTCPHPHits(final String tCPHPHits, final double tCPHPHitsI) {
        this.tCPHPHits = tCPHPHits;
        this.tCPHPHitsI = tCPHPHitsI;
    }

    public final String getTCPHPHits() {
        return tCPHPHits;
    }

    public final double getTCPHPHitsI() {
        return tCPHPHitsI;
    }

    public final String getTCPHPHitsAsString() {
        return tCPHPHits + " packets header predicted";
    }

    public final void setTCPHPHitsToUser(final String tCPHPHitsToUser, final double tCPHPHitsToUserI) {
        this.tCPHPHitsToUser = tCPHPHitsToUser;
        this.tCPHPHitsToUserI = tCPHPHitsToUserI;
    }

    public final String getTCPHPHitsToUser() {
        return tCPHPHitsToUser;
    }

    public final double getTCPHPHitsToUserI() {
        return tCPHPHitsToUserI;
    }

    public final String getTCPHPHitsToUserAsString() {
        return tCPHPHitsToUser + " packets header predicted and directly queued to user";
    }

    public final void setSockMallocOOM(final String sockMallocOOM, final double sockMallocOOMI) {
        this.sockMallocOOM = sockMallocOOM;
        this.sockMallocOOMI = sockMallocOOMI;
    }

    public final String getSockMallocOOM() {
        return sockMallocOOM;
    }

    public final double getSockMallocOOMI() {
        return sockMallocOOMI;
    }

    public final String getSockMallocOOMAsString() {
        return "Ran " + sockMallocOOM + " times out of system memory during packet sending";
    }

    public final void setTCPPureAcks(final String tCPPureAcks, final double tCPPureAcksI) {
        this.tCPPureAcks = tCPPureAcks;
        this.tCPPureAcksI = tCPPureAcksI;
    }

    public final String getTCPPureAcks() {
        return tCPPureAcks;
    }

    public final double getTCPPureAcksI() {
        return tCPPureAcksI;
    }

    public final void setTCPHPAcks(final String tCPHPAcks, final double tCPHPAcksI) {
        this.tCPHPAcks = tCPHPAcks;
        this.tCPHPAcksI = tCPHPAcksI;
    }

    public final String getTCPHPAcks() {
        return tCPHPAcks;
    }

    public final double getTCPHPAcksI() {
        return tCPHPAcksI;
    }

    public final void setTCPRenoRecovery(final String tCPRenoRecovery, final double tCPRenoRecoveryI) {
        this.tCPRenoRecovery = tCPRenoRecovery;
        this.tCPRenoRecoveryI = tCPRenoRecoveryI;
    }

    public final String getTCPRenoRecovery() {
        return tCPRenoRecovery;
    }

    public final double getTCPRenoRecoveryI() {
        return tCPRenoRecoveryI;
    }

    public final void setTCPSackRecovery(final String tCPSackRecovery, final double tCPSackRecoveryI) {
        this.tCPSackRecovery = tCPSackRecovery;
        this.tCPSackRecoveryI = tCPSackRecoveryI;
    }

    public final String getTCPSackRecovery() {
        return tCPSackRecovery;
    }

    public final double getTCPSackRecoveryI() {
        return tCPSackRecoveryI;
    }

    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("Extended TCP Statistics:\n");
        buf.append(getSyncookiesSentAsString()).append("\n");
        buf.append(getSyncookiesRecvAsString()).append("\n");
        buf.append(getSyncookiesFailedAsString()).append("\n");
        buf.append(getEmbryonicRstsAsString()).append("\n");
        buf.append(getPruneCalledAsString()).append("\n");
        buf.append(getRcvPrunedAsString()).append("\n");
        buf.append(getOfoPrunedAsString()).append("\n");
        buf.append(getTWAsString()).append("\n");
        buf.append(getTWRecycledAsString()).append("\n");
        buf.append(getTWKilledAsString()).append("\n");
        buf.append(getPAWSPassiveAsString()).append("\n");
        buf.append(getPAWSActiveAsString()).append("\n");
        buf.append(getPAWSEstabAsString()).append("\n");
        buf.append(getDelayedACKsAsString()).append("\n");
        buf.append(getDelayedACKLockedAsString()).append("\n");
        buf.append(getDelayedACKLostAsString()).append("\n");
        buf.append(getListenOverflowsAsString()).append("\n");
        buf.append(getListenDropsAsString()).append("\n");
        buf.append(getTCPPrequeuedAsString()).append("\n");
        buf.append(getTCPDirectCopyFromBacklogAsString()).append("\n");
        buf.append(getTCPDirectCopyFromPrequeueAsString()).append("\n");
        buf.append(getTCPPrequeueDroppedAsString()).append("\n");
        buf.append(getTCPHPHitsAsString()).append("\n");
        buf.append(getTCPHPHitsToUserAsString()).append("\n");
        buf.append(getSockMallocOOMAsString()).append("\n");
        return buf.toString();
    }

    public void setTCPDSACKUndo(String undo, double undoI) {
        tCPDSACKUndo = undo;
        tCPDSACKUndoI = undoI;
    }

    public String getTCPDSACKUndo() {
        return tCPDSACKUndo;
    }

    public double getTCPDSACKUndoI() {
        return tCPDSACKUndoI;
    }

    public void setTCPFACKReorder(String reorder, double reorderI) {
        tCPFACKReorder = reorder;
        tCPFACKReorderI = reorderI;
    }

    public String getTCPFACKReorder() {
        return tCPFACKReorder;
    }

    public double getTCPFACKReorderI() {
        return tCPFACKReorderI;
    }

    public void setTCPFullUndo(String fullUndo, double fullUndoI) {
        tCPFullUndo = fullUndo;
        tCPFullUndoI = fullUndoI;
    }

    public String getTCPFullUndo() {
        return tCPFullUndo;
    }

    public double getTCPFullUndoI() {
        return tCPFullUndoI;
    }

    public void setTCPLoss(String loss, double lossI) {
        tCPLoss = loss;
        tCPLossI = lossI;
    }

    public String getTCPLoss() {
        return tCPLoss;
    }

    public double getTCPLossI() {
        return tCPLossI;
    }

    public void setTCPLossFailures(String lossFailures, double lossFailuresI) {
        tCPLossFailures = lossFailures;
        tCPLossFailuresI = lossFailuresI;
    }

    public String getTCPLossFailures() {
        return tCPLossFailures;
    }

    public double getTCPLossFailuresI() {
        return tCPLossFailuresI;
    }

    public void setTCPLossUndo(String lossUndo, double lossUndoI) {
        tCPLossUndo = lossUndo;
        tCPLossUndoI = lossUndoI;
    }

    public String getTCPLossUndo() {
        return tCPLossUndo;
    }

    public double getTCPLossUndoI() {
        return tCPLossUndoI;
    }

    public void setTCPLostRetransmit(String lostRetransmit, double lostRetransmitI) {
        tCPLostRetransmit = lostRetransmit;
        tCPLostRetransmitI = lostRetransmitI;
    }

    public String getTCPLostRetransmit() {
        return tCPLostRetransmit;
    }

    public double getTCPLostRetransmitI() {
        return tCPLostRetransmitI;
    }

    public void setTCPPartialUndo(String partialUndo, double partialUndoI) {
        tCPPartialUndo = partialUndo;
        tCPPartialUndoI = partialUndoI;
    }

    public String getTCPPartialUndo() {
        return tCPPartialUndo;
    }

    public double getTCPPartialUndoI() {
        return tCPPartialUndoI;
    }

    public void setTCPRenoFailures(String renoFailures, double renoFailuresI) {
        tCPRenoFailures = renoFailures;
        tCPRenoFailuresI = renoFailuresI;
    }

    public String getTCPRenoFailures() {
        return tCPRenoFailures;
    }

    public double getTCPRenoFailuresI() {
        return tCPRenoFailuresI;
    }

    public void setTCPRenoReorder(String renoReorder, double renoReorderI) {
        tCPRenoReorder = renoReorder;
        tCPRenoReorderI = renoReorderI;
    }

    public String getTCPRenoReorder() {
        return tCPRenoReorder;
    }

    public double getTCPRenoReorderI() {
        return tCPRenoReorderI;
    }

    public void setTCPSackFailures(String sackFailures, double sackFailuresI) {
        tCPSackFailures = sackFailures;
        tCPSackFailuresI = sackFailuresI;
    }

    public String getTCPSackFailures() {
        return tCPSackFailures;
    }

    public double getTCPSackFailuresI() {
        return tCPSackFailuresI;
    }

    public void setTCPSACKReneging(String reneging, double renegingI) {
        tCPSACKReneging = reneging;
        tCPSACKRenegingI = renegingI;
    }

    public String getTCPSACKReneging() {
        return tCPSACKReneging;
    }

    public double getTCPSACKRenegingI() {
        return tCPSACKRenegingI;
    }

    public void setTCPSACKReorder(String reorder, double reorderI) {
        tCPSACKReorder = reorder;
        tCPSACKReorderI = reorderI;
    }

    public String getTCPSACKReorder() {
        return tCPSACKReorder;
    }

    public double getTCPSACKReorderI() {
        return tCPSACKReorderI;
    }

    public void setTCPTSReorder(String ptSReorder, double ptSReorderI) {
        tCPtSReorder = ptSReorder;
        tCPtSReorderI = ptSReorderI;
    }

    public String getTCPTSReorder() {
        return tCPtSReorder;
    }

    public double getTCPTSReorderI() {
        return tCPtSReorderI;
    }

    public void setTCPAbortFailed(String abortFailed, double abortFailedI) {
        tCPAbortFailed = abortFailed;
        tCPAbortFailedI = abortFailedI;
    }

    public String getTCPAbortFailed() {
        return tCPAbortFailed;
    }

    public double getTCPAbortFailedI() {
        return tCPAbortFailedI;
    }

    public void setTCPAbortOnClose(String abortOnClose, double abortOnCloseI) {
        tCPAbortOnClose = abortOnClose;
        tCPAbortOnCloseI = abortOnCloseI;
    }

    public String getTCPAbortOnClose() {
        return tCPAbortOnClose;
    }

    public double getTCPAbortOnCloseI() {
        return tCPAbortOnCloseI;
    }

    public void setTCPAbortOnData(String abortOnData, double abortOnDataI) {
        tCPAbortOnData = abortOnData;
        tCPAbortOnDataI = abortOnDataI;
    }

    public String getTCPAbortOnData() {
        return tCPAbortOnData;
    }

    public double getTCPAbortOnDataI() {
        return tCPAbortOnDataI;
    }

    public void setTCPAbortOnLinger(String abortOnLinger, double abortOnLingerI) {
        tCPAbortOnLinger = abortOnLinger;
        tCPAbortOnLingerI = abortOnLingerI;
    }

    public String getTCPAbortOnLinger() {
        return tCPAbortOnLinger;
    }

    public double getTCPAbortOnLingerI() {
        return tCPAbortOnLingerI;
    }

    public void setTCPAbortOnMemory(String abortOnMemory, double abortOnMemoryI) {
        tCPAbortOnMemory = abortOnMemory;
        tCPAbortOnMemoryI = abortOnMemoryI;
    }

    public String getTCPAbortOnMemory() {
        return tCPAbortOnMemory;
    }

    public double getTCPAbortOnMemoryI() {
        return tCPAbortOnMemoryI;
    }

    public void setTCPAbortOnSyn(String abortOnSyn, double abortOnSynI) {
        tCPAbortOnSyn = abortOnSyn;
        tCPAbortOnSynI = abortOnSynI;
    }

    public String getTCPAbortOnSyn() {
        return tCPAbortOnSyn;
    }

    public double getTCPAbortOnSynI() {
        return tCPAbortOnSynI;
    }

    public void setTCPAbortOnTimeout(String abortOntimeout, double abortOntimeoutI) {
        tCPAbortOntimeout = abortOntimeout;
        tCPAbortOntimeoutI = abortOntimeoutI;
    }

    public String getTCPAbortOnTimeout() {
        return tCPAbortOntimeout;
    }

    public double getTCPAbortOnTimeoutI() {
        return tCPAbortOntimeoutI;
    }

    public void setTCPDSACKOfoRecv(String ofoRecv, double ofoRecvI) {
        tCPDSACKOfoRecv = ofoRecv;
        tCPDSACKOfoRecvI = ofoRecvI;
    }

    public String getTCPDSACKOfoRecv() {
        return tCPDSACKOfoRecv;
    }

    public double getTCPDSACKOfoRecvI() {
        return tCPDSACKOfoRecvI;
    }

    public void setTCPDSACKOfoSent(String ofoSent, double ofoSentI) {
        tCPDSACKOfoSent = ofoSent;
        tCPDSACKOfoSentI = ofoSentI;
    }

    public String getTCPDSACKOfoSent() {
        return tCPDSACKOfoSent;
    }

    public double getTCPDSACKOfoSentI() {
        return tCPDSACKOfoSentI;
    }

    public void setTCPDSACKOldSent(String oldSent, double oldSentI) {
        tCPDSACKOldSent = oldSent;
        tCPDSACKOldSentI = oldSentI;
    }

    public String getTCPDSACKOldSent() {
        return tCPDSACKOldSent;
    }

    public double getTCPDSACKOldSentI() {
        return tCPDSACKOldSentI;
    }

    public void setTCPDSACKRecv(String recv, double recvI) {
        tCPDSACKRecv = recv;
        tCPDSACKRecvI = recvI;
    }

    public String getTCPDSACKRecv() {
        return tCPDSACKRecv;
    }

    public double getTCPDSACKRecvI() {
        return tCPDSACKRecvI;
    }

    public void setTCPFastRetrans(String fastRetrans, double fastRetransI) {
        tCPFastRetrans = fastRetrans;
        tCPFastRetransI = fastRetransI;
    }

    public String getTCPFastRetrans() {
        return tCPFastRetrans;
    }

    public double getTCPFastRetransI() {
        return tCPFastRetransI;
    }

    public void setTCPForwardRetrans(String forwardRetrans, double forwardRetransI) {
        tCPForwardRetrans = forwardRetrans;
        tCPForwardRetransI = forwardRetransI;
    }

    public String getTCPForwardRetrans() {
        return tCPForwardRetrans;
    }

    public double getTCPForwardRetransI() {
        return tCPForwardRetransI;
    }

    public void setTCPMemoryPressures(String memoryPressures, double memoryPressuresI) {
        tCPMemoryPressures = memoryPressures;
        tCPMemoryPressuresI = memoryPressuresI;
    }

    public String getTCPMemoryPressures() {
        return tCPMemoryPressures;
    }

    public double getTCPMemoryPressuresI() {
        return tCPMemoryPressuresI;
    }

    public void setTCPRcvCollapsed(String rcvCollapsed, double rcvCollapsedI) {
        tCPRcvCollapsed = rcvCollapsed;
        tCPRcvCollapsedI = rcvCollapsedI;
    }

    public String getTCPRcvCollapsed() {
        return tCPRcvCollapsed;
    }

    public double getTCPRcvCollapsedI() {
        return tCPRcvCollapsedI;
    }

    public void setTCPRenoRecoveryFail(String renoRecoveryFail, double renoRecoveryFailI) {
        tCPRenoRecoveryFail = renoRecoveryFail;
        tCPRenoRecoveryFailI = renoRecoveryFailI;
    }

    public String getTCPRenoRecoveryFail() {
        return tCPRenoRecoveryFail;
    }

    public double getTCPRenoRecoveryFailI() {
        return tCPRenoRecoveryFailI;
    }

    public void setTCPSackRecoveryFail(String sackRecoveryFail, double sackRecoveryFailI) {
        tCPSackRecoveryFail = sackRecoveryFail;
        tCPSackRecoveryFailI = sackRecoveryFailI;
    }

    public String getTCPSackRecoveryFail() {
        return tCPSackRecoveryFail;
    }

    public double getTCPSackRecoveryFailI() {
        return tCPSackRecoveryFailI;
    }

    public void setTCPSchedulerFailed(String schedulerFailed, double schedulerFailedI) {
        tCPSchedulerFailed = schedulerFailed;
        tCPSchedulerFailedI = schedulerFailedI;
    }

    public String getTCPSchedulerFailed() {
        return tCPSchedulerFailed;
    }

    public double getTCPSchedulerFailedI() {
        return tCPSchedulerFailedI;
    }

    public void setTCPSlowStartRetrans(String slowStartRetrans, double slowStartRetransI) {
        tCPSlowStartRetrans = slowStartRetrans;
        tCPSlowStartRetransI = slowStartRetransI;
    }

    public String getTCPSlowStartRetrans() {
        return tCPSlowStartRetrans;
    }

    public double getTCPSlowStartRetransI() {
        return tCPSlowStartRetransI;
    }

    public void setTCPTimeouts(String ptimeouts, double ptimeoutsI) {
        tCPtimeouts = ptimeouts;
        tCPtimeoutsI = ptimeoutsI;
    }

    public String getTCPTimeouts() {
        return tCPtimeouts;
    }

    public double getTCPTimeoutsI() {
        return tCPtimeoutsI;
    }

} // end of class TCPExtStatistics


