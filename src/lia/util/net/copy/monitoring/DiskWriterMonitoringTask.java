/*
 * $Id$
 */
package lia.util.net.copy.monitoring;

import apmon.ApMon;
import lia.util.net.common.Utils;
import lia.util.net.copy.disk.DiskWriterTask;

import java.util.Date;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

/**
 * Monitors disk activity per writer
 *
 * @author ramiro
 */
public class DiskWriterMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(DiskWriterManagerMonitoringTask.class.getName());

    private final Lock countersRLock;

    long lastDtTake;
    long lastDtWrite;
    long lastDtFinishSession;
    long lastDtTotal;

    boolean initialized = false;
    StringBuilder sb = new StringBuilder();

    DiskWriterTask writerTask;
    Vector<String> paramNames = new Vector<String>();
    Vector<Double> paramValues = new Vector<Double>();
    Vector<Integer> valueTypes = new Vector<Integer>();

    public DiskWriterMonitoringTask(DiskWriterTask writerTask) {
        this.writerTask = writerTask;
        countersRLock = writerTask.getCountersRLock();
    }

    String getNiceProcent(double value) {

        int aux = (int) (value * 100.0);
        if (value >= 1) {

            if ((aux % 100) != 0)
                return aux / 100f + "%";

            return (int) value + "%";
        } else if (value < 1 && value > 0) {
            return aux / 100f + "%";
        }

        return value + "%";
    }

    public void run() {

        sb.setLength(0);
        sb.append("\n[ FileWriterMonitorTask Status @ ").append(new Date().toString()).append(" ]\n");
        sb.append("\n******************************************************\n");

        long diffDtTotal = 0, diffDtWrite = 0, diffDtFinishSession = 0, diffDtTake = 0, diffDtOther = 0;
        double procWrite = 0, procFinish = 0, procTake = 0, procOther = 0;

        boolean reportOk = false;

        countersRLock.lock();
        try {
            if (this.lastDtTotal != writerTask.dtTotal) {
                reportOk = true;
                if (initialized) {
                    diffDtTotal = writerTask.dtTotal - this.lastDtTotal;
                    diffDtTake = writerTask.dtTake - this.lastDtTake;
                    diffDtFinishSession = writerTask.dtFinishSession - this.lastDtFinishSession;
                    diffDtWrite = writerTask.dtWrite - this.lastDtWrite;
                    diffDtOther = diffDtTotal - (diffDtTake + diffDtFinishSession + diffDtWrite);

                }
                lastDtTake = writerTask.dtTake;
                lastDtTotal = writerTask.dtTotal;
                lastDtWrite = writerTask.dtWrite;
                lastDtFinishSession = writerTask.dtFinishSession;
            }
        } finally {
            countersRLock.unlock();
        }

        if (!initialized) {
            initialized = true;
            initParams();
            return;
        }

        sb.append("PoolStats:");
//        sb.append("\nPayload Pool: [ ").append(bufferPool.getSize()).append(" / ").append(bufferPool.getCapacity()).append(" ]");
//        sb.append("\nPacket Header Pool: [ ").append(Utils.getHeaderBufferPool().getSize()).append(" / ").append(Utils.getHeaderBufferPool().getCapacity()).append(" ]");
        if (reportOk) {

            procWrite = (diffDtWrite * 100D) / diffDtTotal;
            procFinish = (diffDtFinishSession * 100D) / diffDtTotal;
            procTake = (diffDtTake * 100D) / diffDtTotal;
            procOther = 100 - (procWrite + procFinish + procTake);

            sb.append("\n DT = ").append(diffDtTotal);
            sb.append(" DtTake = ").append(diffDtTake).append(" ( ").append(getNiceProcent(procTake)).append(" ) ");
            sb.append(" DtWrite = ").append(diffDtWrite).append(" ( ").append(getNiceProcent(procWrite)).append(" )");
            sb.append(" DtFinish = ").append(diffDtFinishSession).append(" ( ").append(getNiceProcent(procFinish)).append(" )");
            sb.append(" DtOther = ").append(diffDtOther).append(" ( ").append(getNiceProcent(procOther)).append(" )");
        }

        sb.append("\n******************************************************\n");

        System.out.println(sb.toString());
        //use the apmon monitoring
        if (Utils.getApMon() != null) {
            try {
//                Utils.getApMon().sendParameter( null, null, "PoolStat_Available", bufferPool.getSize());
//                Utils.getApMon().sendParameter( null, null, "PoolStat_Total", bufferPool.getCapacity());
                if (reportOk) {
                    paramValues.set(0, new Double(procTake));
                    paramValues.set(1, new Double(procWrite));
                    paramValues.set(2, new Double(procFinish));
                    paramValues.set(3, new Double(procOther));
                    Utils.getApMon().sendParameters(null, null, paramNames.size(), paramNames, valueTypes, paramValues);
                }
            } catch (Exception ex) {
                logger.warning("Could not send monitoring information to MonALISA.");
                ex.printStackTrace();
            }
        }
    }//run()

    public void initParams() {
        String[] names = {"Network_GET", "Disk_PUT", "File_CLOSE", "Other"};
        for (int i = 0; i < names.length; i++) {
            paramNames.add(names[i]);
            paramValues.add(null);
            valueTypes.add(ApMon.XDR_REAL64);
        }
        ;
    }

}
