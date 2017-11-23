/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;

/**
 * An interface to be implemented by objects than
 * can be read and written using XDR
 *
 * @author Tony Johnson (tonyj@slac.stanford.edu)
 */
public interface XDRSerializable {
    public void read(XDRDataInput in) throws IOException;

    public void write(XDRDataOutput out) throws IOException;
}
