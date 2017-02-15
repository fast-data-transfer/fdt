
package lia.util.net.copy.monitoring.jmx;


public interface DBPoolJMXMBean {

    
    public int getBufferSize();

    
    public int getCapacity();

    
    public int getSize();

    
    public long totalAllocated();
}


