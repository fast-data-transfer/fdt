/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * XDR message used by LisaDaemon and different API libraries
 *
 * @author Adrian Muraru
 */
public class XDRMessage {

    public static final int SUCCESS = 0;
    public static final int ERROR = 1;
    public int status;
    public String payload;
    protected int xdrMessageSize;

    /**
     * constructs a xdr message containing a text and default status of SUCCESS xdrMessageSize remains uninitialized
     *
     * @param msg
     * @return
     */
    public static final XDRMessage getSuccessMessage(String msg) {
        XDRMessage m = new XDRMessage();
        m.payload = msg;
        m.status = SUCCESS;

        return m;
    }

    /**
     * constructs a xdr message containing a text and default message tag xdrMessageSize remains uninitialized
     *
     * @param msg
     * @return
     */
    public static final XDRMessage getMessage(String msg, int tag) {
        XDRMessage m = new XDRMessage();
        m.payload = msg;
        m.status = tag;
        return m;
    }

    public static final XDRMessage getErrorMessage(String cause) {
        XDRMessage retMsg = new XDRMessage();
        retMsg.payload = cause;
        retMsg.status = ERROR;
        return retMsg;
    }

    public static final XDRMessage getErrorMessage(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return getErrorMessage(sw.getBuffer().toString());
    }

    public String toString() {
        return "[" + (status == SUCCESS ? "SUCCESS" : status == ERROR ? "ERROR" : String.valueOf(status)) + "] " + payload;
    }
}
