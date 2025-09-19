package lia.util.net.common;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OperatingSystem;

public class SystemLoadMonitor implements Runnable {

    private static final long MONITOR_INTERVAL_MS = 1000;

    private volatile double cpuLoad = 0.0;
    private volatile double[] perCoreLoads = new double[0];
    private volatile double[] systemLoadAverage = new double[3]; // 1, 5, 15-minute averages
    private volatile long contextSwitches = 0;

    private final SystemInfo systemInfo;
    private final CentralProcessor processor;
    private final OperatingSystem os;
    private Thread monitorThread;

    private long[] prevTicks;
    private long[][] prevProcTicks;
    private long prevContextSwitches = 0;

    private static SystemLoadMonitor instance = new SystemLoadMonitor();

    private SystemLoadMonitor() {
        systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        os = systemInfo.getOperatingSystem();
        prevTicks = processor.getSystemCpuLoadTicks();
        prevProcTicks = processor.getProcessorCpuLoadTicks();
        prevContextSwitches = getRawContextSwitches();
        startMonitoring();
    }

    public static SystemLoadMonitor getInstance() {
        return instance;
    }

    private void startMonitoring() {
        monitorThread = new Thread(this, "SystemLoadMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    public void run() {
        while (true) {
            cpuLoad = calculateCpuLoad();
            perCoreLoads = calculatePerCoreCpuLoad();
            systemLoadAverage = getSystemLoadAverage();
            contextSwitches = calculateContextSwitches();

            try {
                Thread.sleep(MONITOR_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private double calculateCpuLoad() {
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks();
        return load; // Value between 0.0 and 1.0
    }

    private double[] calculatePerCoreCpuLoad() {
        double[] loads = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
        prevProcTicks = processor.getProcessorCpuLoadTicks();
        return loads; // Per-core loads
    }

    public double[] getSystemLoadAverage() {
        double[] loadAverages = processor.getSystemLoadAverage(1);
        if (loadAverages[0] < 0) {
            // Load averages not available
            java.util.Arrays.fill(loadAverages, 0.0);
        }
        return loadAverages;
    }

    private long getRawContextSwitches() {
        // Use CentralProcessor's method to get context switches
        long contextSwitches = processor.getContextSwitches();
        return contextSwitches;
    }

    private long calculateContextSwitches() {
        long currentContextSwitches = getRawContextSwitches();
        long delta = currentContextSwitches - prevContextSwitches;
        prevContextSwitches = currentContextSwitches;
        return delta; // Context switches since last check
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public double[] getPerCoreLoads() {
        return perCoreLoads;
    }

    public double[] getSystemLoadAverages() {
        return systemLoadAverage;
    }

    public long getContextSwitches() {
        return contextSwitches;
    }
}
