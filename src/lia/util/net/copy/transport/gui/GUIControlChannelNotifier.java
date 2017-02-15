package lia.util.net.copy.transport.gui;

import lia.util.net.copy.transport.FDTProcolException;

/**
 * An interface to be used for signaling between session and gui
 * @author Ciprian Dobre
 */
public interface GUIControlChannelNotifier {
    
    public void notifyCtrlMsg(GUIControlChannel controlChannel, Object ctrlMessage) throws FDTProcolException;
    public void notifyCtrlSessionDown(GUIControlChannel controlChannel, Throwable cause) throws FDTProcolException;
    
}
