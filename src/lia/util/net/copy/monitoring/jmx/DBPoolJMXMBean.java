/*
 * DBPoolJMXMBean.java
 *
 * Created on September 22, 2008, 3:15 PM
 */
package lia.util.net.copy.monitoring.jmx;

/**
 * Interface DBPoolJMXMBean
 *
 * @author ramiro
 */
public interface DBPoolJMXMBean {

    /**
     * Get Attribute exposed for management
     */
    public int getBufferSize();

    /**
     * Get Attribute exposed for management
     */
    public int getCapacity();

    /**
     * Get Attribute exposed for management
     */
    public int getSize();

    /**
     * Operation exposed for management
     *
     * @return long
     */
    public long totalAllocated();
}


