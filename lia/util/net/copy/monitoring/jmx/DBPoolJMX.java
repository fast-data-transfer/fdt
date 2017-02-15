
package lia.util.net.copy.monitoring.jmx;

import javax.management.*;
import java.util.Arrays;
import lia.util.net.common.DirectByteBufferPool;


public class DBPoolJMX extends StandardMBean implements DBPoolJMXMBean {

    private DirectByteBufferPool theRef;

    public DBPoolJMX(DirectByteBufferPool theRef) throws NotCompliantMBeanException {
        
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

    
    @Override
    protected String getDescription(MBeanInfo info) {
        return "DBPoolJMX Description";
    }

    
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        if (info.getName().equals("BufferSize")) {
            description = "Attribute exposed for management";
        } else if (info.getName().equals("Capacity")) {
            description = "Attribute exposed for management";
        } else if (info.getName().equals("Instance")) {
            description = "Attribute exposed for management";
        } else if (info.getName().equals("Size")) {
            description = "Attribute exposed for management";
        }
        return description;
    }

    
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
            description = "Operation exposed for management";
        }
        return description;
    }

    
    public int getBufferSize() {
        return theRef.getBufferSize();
    }

    
    public int getCapacity() {
        return theRef.getCapacity();
    }

    
    public int getSize() {
        return theRef.getSize();
    }

    
    public long totalAllocated() {
        return theRef.totalAllocated();
    }
}


