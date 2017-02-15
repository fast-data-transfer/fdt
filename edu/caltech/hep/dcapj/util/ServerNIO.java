package edu.caltech.hep.dcapj.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Inet4Address;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.NetworkInterface;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Enumeration;

public abstract class ServerNIO implements Runnable {

    private static final Logger logger = Logger.getLogger(ServerNIO.class
            .getName());

    protected boolean accept;

    protected ServerSocketChannel _serverChannel = null;

    protected ServerSocket _server = null;

    private Thread serverThread = null;

    private boolean secure = true;

    protected int timeout = 5 * 60 * 1000;

    public ServerNIO() throws IOException {
        this(0);
    }

    public ServerNIO(final int port) throws IOException {
        init(port);
    }

    protected void init(final int port) throws IOException {
        _serverChannel = ServerSocketChannel.open();
        

        _server = _serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        _server.bind(address);

        logger.log(Level.INFO, "ServerNIO initialzed to listen on port "
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

    
    

    

    public void run() {
        SocketChannel socketChannel = null;

        while (accept) {

            try {
                socketChannel = _serverChannel.accept();
                if (!accept) {
                    break;
                }
                
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
