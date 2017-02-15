/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.xdr;

/**
 * 
 * @author Adrian Muraru
 */
public interface XDRMessageNotifier {
    public void notifyXDRMessage(XDRMessage message, XDRGenericComm comm);
    public void notifyXDRCommClosed(XDRGenericComm comm);
}
