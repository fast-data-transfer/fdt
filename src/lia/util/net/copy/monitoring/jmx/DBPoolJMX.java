/*
 * DBPoolJMX.java
 *
 * Created on September 22, 2008, 3:15 PM
 */
package lia.util.net.copy.monitoring.jmx;

import lia.util.net.common.DirectByteBufferPool;

import javax.management.*;
import java.util.Arrays;

/**
 * Class DBPoolJMX; just to test the JMX. Nothing intelligent, for the moment
 *
 * @author ramiro
 */
public class DBPoolJMX extends StandardMBean implements DBPoolJMXMBean {

    private final DirectByteBufferPool theRef;

    public DBPoolJMX(DirectByteBufferPool theRef) throws NotCompliantMBeanException {
        //WARNING Uncomment the following call to super() to make this class compile (see BUG ID 122377)
        super(DBPoolJMXMBean.class);
        this.theRef = theRef;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        MBeanInfo mbinfo = super.getMBeanInfo();
        return new MBeanInfo(mbinfo.getClassName(),
                mbinfo.getDescription(),
                mbinfo.getAttributes(),
                mbinfo.getConstructors(),
                mbinfo.getOperations(),
                getNotificationInfo());
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[]{};
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanInfo info) {
        return "DBPoolJMX Description";
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanAttributeInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        if (info.getName().equals("BufferSize")) {
            description = "Size (in bytes) of the payload buffer in the buffer pool";
        } else if (info.getName().equals("Capacity")) {
            description = "Total number of buffers allocated";
        } else if (info.getName().equals("Instance")) {
            description = "Attribute exposed for management";
        } else if (info.getName().equals("Size")) {
            description = "Number of buffers available in the pool. Just to check leaks in the server. Should have the same value as Capacity when no active connections.";
        }
        return description;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanParameterInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        if (op.getName().equals("totalAllocated")) {
            switch (sequence) {
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanParameterInfo.getName()
     */
    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        if (op.getName().equals("totalAllocated")) {
            switch (sequence) {
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Override customization hook:
     * You can supply a customized description for MBeanOperationInfo.getDescription()
     */
    @Override
    protected String getDescription(MBeanOperationInfo info) {
        String description = null;
        MBeanParameterInfo[] params = info.getSignature();
        String[] signature = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            signature[i] = params[i].getType();
        }
        String[] methodSignature;
        methodSignature = new String[]{};
        if (info.getName().equals("totalAllocated") && Arrays.equals(signature, methodSignature)) {
            description = "Total allocated bytes allocated in NIO buffers. Another cross check. Should be Capacity x Buffer size.";
        }
        return description;
    }

    /**
     * Get Attribute exposed for management
     */
    public int getBufferSize() {
        return theRef.getBufferSize();
    }

    /**
     * Get Attribute exposed for management
     */
    public int getCapacity() {
        return theRef.getCapacity();
    }

    /**
     * Get Attribute exposed for management
     */
    public int getSize() {
        return theRef.getSize();
    }

    /**
     * Operation exposed for management
     *
     * @return long
     */
    public long totalAllocated() {
        return theRef.totalAllocated();
    }
}


