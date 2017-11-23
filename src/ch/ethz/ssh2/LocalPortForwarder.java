package ch.ethz.ssh2;

import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.LocalAcceptThread;

import java.io.IOException;

/**
 * A <code>LocalPortForwarder</code> forwards TCP/IP connections to a local
 * port via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server).
 *
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: LocalPortForwarder.java,v 1.5 2006/02/14 19:43:16 cplattne Exp $
 */
public class LocalPortForwarder {
    ChannelManager cm;

    int local_port;

    String host_to_connect;

    int port_to_connect;

    LocalAcceptThread lat;

    LocalPortForwarder(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect)
            throws IOException {
        this.cm = cm;
        this.local_port = local_port;
        this.host_to_connect = host_to_connect;
        this.port_to_connect = port_to_connect;

        lat = new LocalAcceptThread(cm, local_port, host_to_connect, port_to_connect);
        lat.setDaemon(true);
        lat.start();
    }

    /**
     * Stop TCP/IP forwarding of newly arriving connections.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        lat.stopWorking();
    }
}
