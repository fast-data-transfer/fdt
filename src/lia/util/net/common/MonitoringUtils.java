package lia.util.net.common;

import lia.util.net.copy.FDT;
import lia.util.net.copy.FDTSession;
import org.opentsdb.client.ExpectResponse;
import org.opentsdb.client.HttpClient;
import org.opentsdb.client.builder.Metric;
import org.opentsdb.client.builder.MetricBuilder;
import org.opentsdb.client.response.Response;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Raimondas Sirvinskas
 * @version 1.0
 */
public class MonitoringUtils {

    private static Logger logger = Logger.getLogger(MonitoringUtils.class.getName());

    private Config config;
    private HttpClient client;
    private String hostName;
    private FDTSession session;

    public MonitoringUtils(Config config, FDTSession session) {
        this.session = session;
        this.config = config;
        this.client = config.getOpenTSDBMonitorClient();
        this.hostName = getHostName();
    }

    public MonitoringUtils(Config config) {
        this.config = config;
        this.client = config.getOpenTSDBMonitorClient();
        this.hostName = getHostName();
    }

    public void monitorStart(long timeRequested, String clusterName) {
        MetricBuilder builder = MetricBuilder.getInstance();
        Metric metric = builder.addMetric("fdt.transferstatus.timeStarted")
                .setDataPoint(timeRequested, timeRequested);
        addTags(metric, clusterName, null);
        monitorStartSession(builder, timeRequested, clusterName);
        sendMetricsToServer(builder);
    }

    public void monitorFinish(long timeRequested, String clusterName) {
        MetricBuilder builder = MetricBuilder.getInstance();
        Metric metric = builder.addMetric("fdt.transferstatus.timeFinished")
                .setDataPoint(timeRequested, timeRequested);
        addTags(metric, clusterName, null);
        monitorFinishSession(builder, timeRequested, clusterName);
        sendMetricsToServer(builder);
    }

    private synchronized void sendMetricsToServer(MetricBuilder builder) {
        if (builder.getMetrics().size() > 0) {
            if (logger.isLoggable(Level.FINE)) {
                Iterator iterator = builder.getMetrics().listIterator();
                while (iterator.hasNext()) {
                    try {
                        Metric metric = (Metric) iterator.next();
                        logger.log(Level.FINE, "metric    " + metric.getName() + " " + metric.stringValue());
                        logger.log(Level.FINE, "metric tag names: " + builder.getMetrics().listIterator().next().getTags().keySet().toString());
                        logger.log(Level.FINE, "metric tag values: " + builder.getMetrics().listIterator().next().getTags().values().toString());
                    } catch (Exception e) {
                        //do nothing
                    }
                }
            }
            try {
                if (client != null) {
                    Response response = client.pushMetrics(builder, ExpectResponse.SUMMARY);
                    logger.log(Level.FINE, "Response from OpenTSDB server: " + response.toString());
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to send metrics to OpenTSDB server", e);
            }
        }

    }

    private void monitorStartSession(MetricBuilder builder, long timeStarted, String clusterName) {
        monitorStartConfiguration(builder, timeStarted, clusterName, "debugLevel", Level.parse(config.getLogLevel()).intValue());
        monitorStartConfiguration(builder, timeStarted, clusterName, "bio", config.isBlocking() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "notmp", config.isNoTmpFlagSet() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "nolock", config.isNoLockFlagSet() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "nolocks", config.getConfigMap().get("-nolocks") != null ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "nettest", config.isNetTest() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "bs", config.getByteBufferSize());
        monitorStartConfiguration(builder, timeStarted, clusterName, "P", config.getSockNum());
        monitorStartConfiguration(builder, timeStarted, clusterName, "ss", config.getSockBufSize());
        monitorStartConfiguration(builder, timeStarted, clusterName, "limit", config.getRateLimit());
        monitorStartConfiguration(builder, timeStarted, clusterName, "iof", config.getRetryIOCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "rCount", config.getReadersCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "wCount", config.getWritersCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "pCount", config.getMaxPartitionCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "sourcePort", config.getPort());
        monitorStartConfiguration(builder, timeStarted, clusterName, "destPort", session.getRemotePort());
        monitorStartConfiguration(builder, timeStarted, clusterName, "transferPort", session.getTransferPort());
        monitorStartConfiguration(builder, timeStarted, clusterName, "totalBytes", session.getTotalBytes());
        monitorStartConfiguration(builder, timeStarted, clusterName, "status", 1);
        monitorStartConfiguration(builder, timeStarted, clusterName, "recursive", config.isRecursive() ? 1 : 0);
    }

    private void monitorFinishSession(MetricBuilder builder, long timeStarted, String clusterName) {
        monitorStartConfiguration(builder, timeStarted, clusterName, "debugLevel", Level.parse(config.getLogLevel()).intValue());
        monitorStartConfiguration(builder, timeStarted, clusterName, "bio", config.isBlocking() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "notmp", config.isNoTmpFlagSet() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "nolock", config.isNoLockFlagSet() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "nolocks", config.getConfigMap().get("-nolocks") != null ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "nettest", config.isNetTest() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "bs", config.getByteBufferSize());
        monitorStartConfiguration(builder, timeStarted, clusterName, "P", config.getSockNum());
        monitorStartConfiguration(builder, timeStarted, clusterName, "ss", config.getSockBufSize());
        monitorStartConfiguration(builder, timeStarted, clusterName, "limit", config.getRateLimit());
        monitorStartConfiguration(builder, timeStarted, clusterName, "iof", config.getRetryIOCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "rCount", config.getReadersCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "wCount", config.getWritersCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "pCount", config.getMaxPartitionCount());
        monitorStartConfiguration(builder, timeStarted, clusterName, "sourcePort", config.getPort());
        monitorStartConfiguration(builder, timeStarted, clusterName, "destPort", session.getRemotePort());
        monitorStartConfiguration(builder, timeStarted, clusterName, "transferPort", session.getTransferPort());
        monitorStartConfiguration(builder, timeStarted, clusterName, "totalBytes", session.getTotalBytes());
        monitorStartConfiguration(builder, timeStarted, clusterName, "status", 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "recursive", config.isRecursive() ? 1 : 0);
        monitorStartConfiguration(builder, timeStarted, clusterName, "exitStatus", session.getCurrentStatus());
    }

    public void monitorStartConfiguration(MetricBuilder builder, long timeStarted, String clusterName, String configParam, long configValue) {
        Metric metric = builder.addMetric("fdt.transferstatus." + configParam)
                .setDataPoint(timeStarted, configValue);
        addTags(metric, clusterName, null);
    }

    private void addTags(Metric metric, String clusterName, String destIP) {
        metric.addTag("fdthost", hostName != null ? hostName : "none")
                .addTag("fdtversion", FDT.FDT_FULL_VERSION)
                .addTag("fdtsourceIP", getHostIP())
                .addTag("fdtdestIP", destIP == null ? session != null ? session.getRemoteAddress().getHostAddress() : "0.0.0.0" : destIP)
                .addTag("fdtclusterName", clusterName)
                .addTag("fdtcustomFDTTag", config.getFDTTag());
    }

    public synchronized void shareMetrics(String clusterName, String nodeName, Vector paramNames, Vector paramValues) {
        final String hostName = getHostName();

        MetricBuilder builder = MetricBuilder.getInstance();
        Iterator nameIterator = paramNames.iterator();
        Iterator valueIterator = paramValues.iterator();
        String[] node = nodeName.split(":");

        while (nameIterator.hasNext() && valueIterator.hasNext() && paramNames.size() == paramValues.size()) {
            try {
                Object nameObject = nameIterator.next();
                Object valueObject = valueIterator.next();
                double value = Double.valueOf(valueObject.toString());
                Metric metric = builder.addMetric("fdt.transferstatus." + String.valueOf(nameObject))
                        .setDataPoint(System.currentTimeMillis(), value);
                addTags(metric, clusterName, node[0]);

                logger.log(Level.FINE, "metric    " + metric.getName() + " " + metric.stringValue());
                logger.log(Level.FINE, "metric tag names: " + metric.getTags().keySet().toString());
                logger.log(Level.FINE, "metric tag values: " + metric.getTags().values().toString());
            } catch (Exception e) {
                logger.log(Level.FINEST, "Failed to parse metric value ", e);
            }
        }

        sendMetricsToServer(builder);
    }

    private String getHostName() {
        String host = null;
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            host = localhost.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (host == null) {
            host = System.getenv("HOSTNAME");
        }
        return host;
    }

    private String getHostIP() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet6Address) continue;
                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ip != null ? ip : "0.0.0.0";
    }

    public void monitorEndStats(boolean finishStatus, long totalBytes, long utilBytes, long startTimeMillis, long endTime, long period, String clusterName) {
        MetricBuilder builder = MetricBuilder.getInstance();
        monitorStartConfiguration(builder, endTime, clusterName, "destPort", session.getRemotePort());
        monitorStartConfiguration(builder, endTime, clusterName, "transferTime", period);
        monitorStartConfiguration(builder, endTime, clusterName, "totalFileBytes", totalBytes);
        monitorStartConfiguration(builder, endTime, clusterName, "totalNetworkBytes", utilBytes);
        monitorStartConfiguration(builder, endTime, clusterName, "finishStatus", finishStatus ? 1 : 0);
        monitorStartConfiguration(builder, endTime, clusterName, "startTime", startTimeMillis);
        monitorStartConfiguration(builder, endTime, clusterName, "endTime", endTime);
        sendMetricsToServer(builder);
    }
}
