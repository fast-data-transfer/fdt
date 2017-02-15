package lia.util.net.copy.transport;

public interface ControlChannelNotifier {
    
    public void notifyCtrlMsg(ControlChannel controlChannel, Object ctrlMessage) throws FDTProcolException;
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) throws FDTProcolException;
    
}
