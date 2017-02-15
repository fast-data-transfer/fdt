
package lia.util.net.copy.transport.gui;

import lia.util.net.copy.transport.FDTProcolException;

public interface GUIControlChannelNotifier {
    
    public void notifyCtrlMsg(GUIControlChannel controlChannel, Object ctrlMessage) throws FDTProcolException;
    public void notifyCtrlSessionDown(GUIControlChannel controlChannel, Throwable cause) throws FDTProcolException;
    
}
