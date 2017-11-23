package edu.caltech.hep.dcapj.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ServerNIO implements Runnable {

    private static final Logger logger = Logger.getLogger(ServerNIO.class
            .getName());

    protected boolean accept;

    protected ServerSocketChannel _serverChannel = null;

    protected ServerSocket _server = null;
    protected int timeout = 5 * 60 * 1000;
    private Thread serverThread = null;
    private boolean secure = true;

    public ServerNIO() throws IOException {
        this(0);
    }

    public ServerNIO(final int port) throws IOException {
        init(port);
    }

    protected void init(final int port) throws IOException {
        _serverChannel = ServerSocketChannel.open();
        // _serverChannel.configureBlocking(false);

        _server = _serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        _server.bind(address);

        logger.log(Level.INFO, "ServerNIO initialzed to listen on port "
                + _server.getLocalPort());
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public void shutdown() {
        accept = false;
        try {
            _server.close();
        } catch (final Exception e) {
        }

        /*
         * final SocketFactory factory = SocketFactory.getDefault(); Socket s =
         * null; try { s = factory.createSocket(InetAddress.getLocalHost(),
         * getPort()); s.getInputStream(); } catch (final Exception e) {
         *  } finally { if (s != null) { try { s.close(); } catch (final
         * Exception e) { } } }
         */
        try {
            serverThread.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        serverThread = null;
        _server = null;
    }

    protected void start() {
        if (serverThread == null) {
            accept = true;
            serverThread = new Thread(this);
            serverThread.setName("dCapIOCallbackServerNIO");
            serverThread.start();
        }
    }

    public int getPort() {
        return _server.getLocalPort();
    }

    // public String getHostAddress() {
    // return hostAddress;

    // }

    public void run() {
        SocketChannel socketChannel = null;

        while (accept) {

            try {
                socketChannel = _serverChannel.accept();
                if (!accept) {
                    break;
                }
                // socket.setSoTimeout(getTimeout());
            } catch (final IOException e) {
                if (accept) {
                    logger.log(Level.WARNING, "ServerNIO died: "
                            + e.getMessage(), e);
                }
                break;
            }
            handleConnection(socketChannel);
        }
    }

    protected abstract void handleConnection(SocketChannel client);

}
