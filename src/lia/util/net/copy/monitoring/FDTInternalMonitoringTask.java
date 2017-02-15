/*
 * $Id: FDTInternalMonitoringTask.java 682 2012-07-30 18:11:41Z ramiro $
 */
package lia.util.net.copy.monitoring;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.disk.DiskWriterTask;

/**
 * Used for internal monitoring
 * It will log if the logging level if -printStats is enabled
 *  
 * @author ramiro
 */
public class FDTInternalMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(FDTInternalMonitoringTask.class.getName());

    private static final class WriterAccountingContors {

        private final Lock countersRLock;

        boolean reportOk;

        long lastDtTake;

        long lastDtWrite;

        long lastDtFinishSession;

        long lastDtTotal;

        double procWrite = 0;

        double procFinish = 0;

        double procTake = 0;

        double procOther = 0;

        WriterAccountingContors(final Lock lock) {
            this.countersRLock = lock;
            reportOk = false;
        }
    }

    private static final DiskWriterManager diskWriterManager = DiskWriterManager.getInstance();

    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();

    private static final FDTInternalMonitoringTask _theInstance;

    private static boolean initialized = false;

    private static final Config config = Config.getInstance();

    private static final Level STATS_LEVEL = config.getStatsLevel();

    private static final String EOL = System.getProperty("line.separator", "\n");

    private static final String getNiceProcent(double value) {

        int aux = (int) (value * 100.0);
        if (value >= 1) {

            if ((aux % 100) != 0) return aux / 100f + "%";

            return (int) value + "%";
        } else if (value < 1 && value > 0) { return aux / 100f + "%"; }

        return value + "%";
    }

    //Params to monitor
    int dbpool_total;

    int dbpool_free;

    int hpool_total;

    int hpool_free;

    int mon_queue_count;

    int fdt_wdisk_ses_count;

    int fdt_rdisk_ses_count;

    //It's used from a single thread
    StringBuilder sb = null;

    HashMap<Integer, HashMap<Integer, WriterAccountingContors>> hmWriters;

    static {
        synchronized (FDTInternalMonitoringTask.class) {
            _theInstance = new FDTInternalMonitoringTask();
            initialized = true;
            FDTInternalMonitoringTask.class.notifyAll();
        }
    }

    //only one instance per application
    private FDTInternalMonitoringTask() {
        sb = new StringBuilder(2048);
        hmWriters = new HashMap<Integer, HashMap<Integer, WriterAccountingContors>>();
    }

    public static final FDTInternalMonitoringTask getInstance() {
        if (!initialized) {
            synchronized (FDTInternalMonitoringTask.class) {
                while (!initialized) {
                    try {
                        FDTInternalMonitoringTask.class.wait();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }//end sync
        }
        return _theInstance;
    }

    private final void printStats() {
        sb.setLength(0);

        sb.append(EOL).append(EOL).append(" *** FDT Stats @ ").append(new Date()).append(" *** ").append(EOL).append(EOL);
        sb.append(" BuffPool [ ").append(dbpool_free).append(" / ").append(dbpool_total);
        sb.append(" ] HeaderBPool [ ").append(hpool_free).append(" / ").append(hpool_total).append(" ]").append(EOL);
        sb.append(EOL).append(EOL).append(" BuffPool Identity map stats").append(DirectByteBufferPool.getInstance().identityMapStats()).append(EOL);
        sb.append(EOL).append(EOL).append(" HeaderPool Identity map stats").append(HeaderBufferPool.getInstance().identityMapStats()).append(EOL);
        sb.append(" MonitoringQueue ").append(mon_queue_count).append(EOL);
        Map<Integer, List<DiskWriterTask>> writersMap = diskWriterManager.getWritersMap();
        sb.append(" PartitionIDs: ").append(writersMap.size());
        for (final Map.Entry<Integer, List<DiskWriterTask>> entry : writersMap.entrySet()) {
            final Integer partitionID = entry.getKey();
            sb.append(" [ partitionID: ").append(partitionID).append(" workers: ").append(writersMap.get(entry.getValue().size())).append(" qSize: ").append(diskWriterManager.getQueueSize(partitionID.intValue())).append(" ] ");
        }
        sb.append(EOL);
        sb.append(" Disk Writer Sessions: ").append(fdt_wdisk_ses_count).append(" Disk Reader Sessions: ").append(fdt_rdisk_ses_count);
        sb.append(EOL);

        for (Iterator<Map.Entry<Integer, HashMap<Integer, WriterAccountingContors>>> it = hmWriters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, HashMap<Integer, WriterAccountingContors>> entry = it.next();

            Integer id = entry.getKey();
            HashMap<Integer, WriterAccountingContors> iVal = entry.getValue();

            for (Map.Entry<Integer, WriterAccountingContors> iEntry : iVal.entrySet()) {

                Integer writerID = iEntry.getKey();
                WriterAccountingContors wac = iEntry.getValue();

                if (wac.reportOk) {
                    sb.append(EOL).append(" DiskWriterStat [ PartitionID: " + id + " writerID " + writerID + " ] WOnQueue ").append(getNiceProcent(wac.procTake));
                    sb.append(" WaitOnWrite ").append(getNiceProcent(wac.procWrite));
                    sb.append(" WaitOnFinish ").append(getNiceProcent(wac.procFinish));
                    sb.append(" WaitOnOther ").append(getNiceProcent(wac.procOther));
                }
            }

            sb.append(EOL);
        }

        sb.append(EOL).append(EOL).append(" *** ==== ***").append(EOL);
        logger.log(STATS_LEVEL, sb.toString());
    }

    private void updateWritersAccounting() {
        Map<Integer, List<DiskWriterTask>> currentWriters = diskWriterManager.getWritersMap();

        for (Map.Entry<Integer, List<DiskWriterTask>> entry : currentWriters.entrySet()) {
            Integer id = entry.getKey();
            List<DiskWriterTask> partitionWritersList = entry.getValue();

            for (DiskWriterTask writerTask : partitionWritersList) {
                long diffDtTotal = 0, diffDtWrite = 0, diffDtFinishSession = 0, diffDtTake = 0, diffDtOther = 0;
                boolean init = false;

                HashMap<Integer, WriterAccountingContors> wacMap = hmWriters.get(id);

                if (wacMap == null) {
                    wacMap = new HashMap<Integer, WriterAccountingContors>();
                    hmWriters.put(id, wacMap);
                }

                WriterAccountingContors wac = wacMap.get(Integer.valueOf(writerTask.writerID()));

                if (wac == null) {
                    wac = new WriterAccountingContors(writerTask.getCountersRLock());
                    wacMap.put(Integer.valueOf(writerTask.writerID()), wac);
                    init = true;
                }

                wac.reportOk = false;

                wac.countersRLock.lock();
                try {
                    if (wac.lastDtTotal != writerTask.dtTotal) {
                        wac.reportOk = true;

                        if (!init) {
                            diffDtTotal = writerTask.dtTotal - wac.lastDtTotal;
                            diffDtTake = writerTask.dtTake - wac.lastDtTake;
                            diffDtFinishSession = writerTask.dtFinishSession - wac.lastDtFinishSession;
                            diffDtWrite = writerTask.dtWrite - wac.lastDtWrite;
                            diffDtOther = diffDtTotal - (diffDtTake + diffDtFinishSession + diffDtWrite);

                        }

                        wac.lastDtTake = writerTask.dtTake;
                        wac.lastDtTotal = writerTask.dtTotal;
                        wac.lastDtWrite = writerTask.dtWrite;
                        wac.lastDtFinishSession = writerTask.dtFinishSession;
                    } else {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, " The writer seem idle same time [ " + wac.lastDtTotal + " ] as in previous iteration ");
                        }
                    }
                }finally {
                    wac.countersRLock.unlock();
                }

                //Just for protection - if the writer was restarted by the manager
                if (diffDtTotal < 0 || diffDtTake < 0 || diffDtFinishSession < 0 || diffDtWrite < 0) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, new StringBuilder(" Report NOK smth is decreasing ").append(" diffDtTotal = ").append(diffDtTotal).append(" diffDtTake = ").append(diffDtTake).append(" diffDtFinishSession = ").append(diffDtFinishSession).append(" diffDtWrite = ").append(diffDtWrite).append(" diffDtOther = ").append(diffDtOther).toString());
                    }
                    wac.reportOk = false;
                }

                if (init) {
                    continue;
                }

                if (wac.reportOk) {

                    wac.procWrite = (diffDtWrite * 100D) / diffDtTotal;
                    wac.procFinish = (diffDtFinishSession * 100D) / diffDtTotal;
                    wac.procTake = (diffDtTake * 100D) / diffDtTotal;
                    wac.procOther = 100 - (wac.procWrite + wac.procFinish + wac.procTake);

                }
            }
        }//end for

        //check for dead writers
        for (Iterator<Integer> it = hmWriters.keySet().iterator(); it.hasNext();) {
            Integer partitionID = it.next();
            if (!currentWriters.containsKey(partitionID)) {
                it.remove();
            }
        }
    }

    public HashMap<String, Double> getLisaParams() {
        HashMap<String, Double> fdtLisaParams = new HashMap<String, Double>();

        fdtLisaParams.put("dbpool_total", (double) dbpool_total);
        fdtLisaParams.put("dbpool_free", (double) dbpool_free);

        fdtLisaParams.put("hpool_total", (double) hpool_total);
        fdtLisaParams.put("hpool_free", (double) hpool_free);
        fdtLisaParams.put("mon_queue", (double) mon_queue_count);

        fdtLisaParams.put("fdt_ses_wdisk", (double) fdt_wdisk_ses_count);
        fdtLisaParams.put("fdt_ses_rdisk", (double) fdt_rdisk_ses_count);

        //Writers accounting
        for (Iterator<Map.Entry<Integer, HashMap<Integer, WriterAccountingContors>>> it = hmWriters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, HashMap<Integer, WriterAccountingContors>> entry = it.next();

            Integer pid = entry.getKey();
            HashMap<Integer, WriterAccountingContors> wacMap = entry.getValue();

            for (Iterator<Map.Entry<Integer, WriterAccountingContors>> iti = wacMap.entrySet().iterator(); iti.hasNext();) {
                Map.Entry<Integer, WriterAccountingContors> ientry = iti.next();

                Integer wID = ientry.getKey();
                WriterAccountingContors wac = ientry.getValue();

                if (wac.reportOk) {

                    final String wPrefix = "pID_" + pid + "_wID_" + wID + "_";

                    fdtLisaParams.put(wPrefix + "w_take", wac.procTake);
                    fdtLisaParams.put(wPrefix + "w_write", wac.procWrite);
                    fdtLisaParams.put(wPrefix + "w_finish", wac.procFinish);
                    fdtLisaParams.put(wPrefix + "w_other", wac.procOther);
                }
            }

        }

        return fdtLisaParams;
    }

    public void run() {

        try {

            final long sTime = System.nanoTime();

            dbpool_total = DirectByteBufferPool.getInstance().getCapacity();
            dbpool_free = DirectByteBufferPool.getInstance().getSize();

            hpool_total = HeaderBufferPool.getInstance().getCapacity();
            hpool_free = HeaderBufferPool.getInstance().getSize();

            mon_queue_count = Utils.getMonitoringExecService().getQueue().size();

            fdt_wdisk_ses_count = diskWriterManager.getSessions().size();
            fdt_rdisk_ses_count = diskReaderManager.getSessions().size();

            updateWritersAccounting();

            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "FDTInternalMonitoring took: " + (System.nanoTime() - sTime) / 1000000D + " ms");
            }

            if (STATS_LEVEL != null && logger.isLoggable(STATS_LEVEL)) {
                printStats();
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, " [ InternalMonitoring ] Got Exception ", t);
        }
    }

}
