package edu.caltech.hep.dcapj.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Enumeration;

public abstract class Server implements Runnable {

    private static final Logger logger = Logger.getLogger(Server.class
            .getName());

    protected boolean accept;

    protected ServerSocket _server = null;

    private Thread serverThread = null;

    private boolean secure = true;

    protected int timeout = 5 * 60 * 1000;

    public Server() throws IOException {
        this(0);
    }

    public Server(final int port) throws IOException {
        init(port);
    }

    protected void init(final int port) throws IOException {
        _server = new ServerSocket(port);
        System.out.println("Server initialized");
        logger.log(Level.INFO, "Server initialzed to listen on port "
                + _server.getLocalPort());
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return this.timeout;
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
            serverThread.setName("Server");
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
        Socket socket = null;

        while (accept) {

            try {
                socket = _server.accept();
                if (!accept) {
                    break;
                }
                socket.setSoTimeout(getTimeout());
            } catch (final IOException e) {
                if (accept) {
                    logger.log(Level.WARNING, "Server died: " + e.getMessage(),
                            e);
                }
                break;
            }
            handleConnection(socket);
        }
    }

    protected abstract void handleConnection(Socket client);

}
