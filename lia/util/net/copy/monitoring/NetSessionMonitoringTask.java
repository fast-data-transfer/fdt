
package lia.util.net.copy.monitoring;

import lia.util.net.copy.Accountable;
import lia.util.net.copy.monitoring.base.AbstractAccountableMonitoringTask;
import lia.util.net.copy.transport.TCPTransportProvider;


public class NetSessionMonitoringTask extends AbstractAccountableMonitoringTask {
    private final TCPTransportProvider transportProvider;
    public NetSessionMonitoringTask(TCPTransportProvider transportProvider) {
        super(new Accountable[] {transportProvider});
        this.transportProvider = transportProvider;
    }

    public double getTotalRate() {
        return getTotalRate(transportProvider);
    }
    
    @Override
    public void rateComputed() {
        
        
    }

}
