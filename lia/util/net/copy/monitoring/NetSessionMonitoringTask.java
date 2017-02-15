
package lia.util.net.copy.monitoring;

import lia.util.net.copy.transport.TCPTransportProvider;


public class NetSessionMonitoringTask extends AbstractAccountableMonitoringTask {

    public NetSessionMonitoringTask(TCPTransportProvider transportProvider) {
        super(transportProvider);
    }

}
