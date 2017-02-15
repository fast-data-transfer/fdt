
package lia.util.net.copy.transport;

import lia.util.net.common.FDTCloseable;


public interface ControlChannelNotifier extends FDTCloseable {
    
    public void notifyCtrlMsg(ControlChannel controlChannel, Object ctrlMessage) throws FDTProcolException;
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause);
    
}
