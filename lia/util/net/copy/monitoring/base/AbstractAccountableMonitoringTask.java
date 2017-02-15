
package lia.util.net.copy.monitoring.base;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.Accountable;


public abstract class AbstractAccountableMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(AbstractAccountableMonitoringTask.class.getName());

    private final ConcurrentHashMap<Accountable, AccountableEntry> accMap = new ConcurrentHashMap<Accountable, AccountableEntry>();

    private static class AccountableEntry {
        final boolean debug;

        long monCount;

        protected long startTime;
        protected long lastTimeCalled;

        protected long lastUtilBytes;
        protected long currentUtilBytes;
        protected long lastTotalBytes;
        protected long currentTotalBytes;

        protected long startUtilBytes;
        protected long startTotalBytes;

        protected double utilRate;
        protected double totalRate;

        protected double avgUtilRate;
        protected double avgTotalRate;

        AccountableEntry() {
            this(false);
        }

        AccountableEntry(boolean debug) {
            this.debug = debug;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(" startTime: ").append(startTime).append(", ");
            sb.append(" startUtilBytes: ").append(startUtilBytes).append(", ");
            sb.append(" startTotalBytes: ").append(startTotalBytes).append(", ");
            sb.append(" lastTimeCalled: ").append(lastTimeCalled).append(", ");
            sb.append(" lastUtilBytes: ").append(lastUtilBytes).append(", ");
            sb.append(" currentUtilBytes: ").append(currentUtilBytes).append(", ");
            sb.append(" lastTotalBytes: ").append(lastTotalBytes).append(", ");
            sb.append(" currentTotalBytes: ").append(currentTotalBytes).append(", ");
            sb.append(" utilRate: ").append(utilRate).append(", ");
            sb.append(" totalRate: ").append(totalRate).append(", ");
            sb.append(" avgUtilRate: ").append(avgUtilRate).append(", ");
            sb.append(" avgUtilRate: ").append(avgUtilRate);
            return sb.toString();
        }
    }

    public AbstractAccountableMonitoringTask(Accountable[] accountableList) {
        
        if(accountableList == null) return;
        int pos = 0;
        for(Accountable accountable: accountableList) {
            try {
                addIfAbsent(accountable);
                pos++;
            } catch(NullPointerException npe) {
                throw new NullPointerException(" accountable is null on pos: " + pos);
            }
        }
    }

    private final void iComputeRate(final Accountable accountable, final AccountableEntry accEntry, final long now) {
        try {
            if(accEntry.debug) {
                logger.log(Level.INFO, " AbstractAccountableMonitoringTask debug for : " + accountable + " BEFORE: \n:" + accEntry);
            }
            accEntry.currentUtilBytes = accountable.getUtilBytes();
            accEntry.currentTotalBytes = accountable.getTotalBytes();

            if(accEntry.lastTimeCalled != 0) {
                computeRate(accEntry, now);

            }

            if(accEntry.debug) {
                logger.log(Level.INFO, " AbstractAccountableMonitoringTask debug for : " + accountable + " AFTER: \n:" + accEntry);
            }
        } catch(Throwable t) {
            logger.log(Level.WARNING, " [ AbstractAccountableMonitoringTask ] got exception computing rate for " + accountable, t);
        } finally {

            if(accEntry.lastTimeCalled == 0) {
                accEntry.startTime = now;

                accEntry.startUtilBytes = accEntry.currentUtilBytes;
                accEntry.startTotalBytes = accEntry.currentTotalBytes;
            } else {
            }

            accEntry.lastTimeCalled = now;
            accEntry.lastUtilBytes = accEntry.currentUtilBytes;
            accEntry.lastTotalBytes = accEntry.currentTotalBytes;
        }
    }
    public void run() {
        final long now = System.currentTimeMillis();

        try {
            for(final Map.Entry<Accountable, AccountableEntry> entry: accMap.entrySet()) 
                iComputeRate(entry.getKey(), entry.getValue(), now);
        } catch (Throwable t) {
            logger.log(Level.WARNING, " [ AbstractAccountableMonitoringTask ] got exception main loop ", t);
        }

        try {
            rateComputed();
        }catch(Throwable t) {
            logger.log(Level.WARNING, " [ AbstractAccountableMonitoringTask ] calling rateComputed", t);
        }
    }

    public abstract void rateComputed();

    protected void resetAllCounters() {
        for(final AccountableEntry accEntry: accMap.values()) {
            accEntry.startTime = accEntry.lastTimeCalled = 0;

            accEntry.lastUtilBytes = accEntry.startUtilBytes = accEntry.lastTotalBytes = accEntry.startTotalBytes = 0;

            accEntry.utilRate = accEntry.totalRate = 0;

            accEntry.avgUtilRate =  accEntry.avgTotalRate = 0;
        }

    }

    protected void resetAllCounters(final Accountable accountable) {
        final AccountableEntry accEntry = getEntry(accountable);

        accEntry.startTime = accEntry.lastTimeCalled = 0;

        accEntry.lastUtilBytes = accEntry.startUtilBytes = accEntry.lastTotalBytes = accEntry.startTotalBytes = 0;

        accEntry.utilRate = accEntry.totalRate = 0;

        accEntry.avgUtilRate =  accEntry.avgTotalRate = 0;
    }

    
    private void computeRate(final AccountableEntry accEntry, final long now) throws Exception {
        long dt = now - accEntry.lastTimeCalled;

        accEntry.monCount++;

        if(dt <= 0) {
            logger.log(Level.WARNING, " Going back in the future ? The count the average from now on ...");
            accEntry.startTime = now;
            accEntry.startUtilBytes = accEntry.currentUtilBytes;
            accEntry.startTotalBytes = accEntry.currentTotalBytes;
            throw new Exception(" [ AbstractAccountableMonitoringTask ] Going back in the future ? lastTime: " + accEntry.lastTimeCalled + " now: " + now);
        }

        accEntry.utilRate  = ( accEntry.currentUtilBytes - accEntry.lastUtilBytes ) * 1000D / dt;
        accEntry.totalRate = ( accEntry.currentTotalBytes - accEntry.lastTotalBytes ) * 1000D / dt;

        dt = (now - accEntry.startTime);

        if(dt <= 0) {
            logger.log(Level.WARNING, " Going back in the future ? The count the average from now on ...");
            accEntry.startTime = now;
            accEntry.startUtilBytes = accEntry.currentUtilBytes;
            accEntry.startTotalBytes = accEntry.currentTotalBytes;
            return;
        }

        accEntry.avgUtilRate = ( accEntry.currentUtilBytes - accEntry.startUtilBytes ) * 1000D / dt; 
        accEntry.avgTotalRate = ( accEntry.currentTotalBytes - accEntry.startTotalBytes ) * 1000D / dt; 
    }

    protected double getUtilRate(final Accountable accountable) {
        return getEntry(accountable).utilRate;
    }

    protected double getTotalRate(final Accountable accountable) {
        return getEntry(accountable).totalRate;
    }

    protected double getAvgTotalRate(final Accountable accountable) {
        return getEntry(accountable).avgTotalRate;
    }

    protected double getAvgUtilRate(final Accountable accountable) {
        return getEntry(accountable).avgUtilRate;
    }

    protected long getMonCount(final Accountable accountable) {
        return getEntry(accountable).monCount;
    }

    private AccountableEntry getEntry(final Accountable accountable) {
        final AccountableEntry accEntry = accMap.get(accountable);
        if(accEntry == null) {
            throw new NoSuchElementException("No entry for " + accountable);
        }
        return accEntry;
    }

    protected boolean addIfAbsent(final Accountable accountable) {
        if(accountable == null) throw new NullPointerException(" Accountable cannot be null ");
        return ( accMap.putIfAbsent(accountable, new AccountableEntry()) == null);
    }

    protected boolean addIfAbsent(final Accountable accountable, boolean debug) {
        final AccountableEntry accEntry = new AccountableEntry(debug);
        
        final boolean bRet = ( accMap.putIfAbsent(accountable, accEntry) == null);
        if(bRet) {
            iComputeRate(accountable, accEntry, System.currentTimeMillis());
        }
        return bRet;
    }

    protected boolean remove(final Accountable accountable) {
        return (accMap.remove(accountable) != null);
    }
}
