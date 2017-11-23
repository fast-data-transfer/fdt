/*
 * $Id$
 */

package lia.util.net.copy.monitoring.lisa.xdr;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory used to create ssl server sockets. NO client authentication is done. Usefull when a password-based authentication over a secure channel is needed
 *
 * @author Adrian Muraru
 */

public class SocketFactory {
    /**
     * Logger name
     */
    private static final transient String COMPONENT = "lisa.comm.net.TLSSocketFactory";

    /**
     * Logger used by this class
     */
    private static final transient Logger logger = Logger.getLogger(COMPONENT);

    /**
     * maximum time to connect with the other endPoint
     */
    private static final int CONNECT_TIMEOUT = 15 * 1000; // 15s

    public static SSLServerSocket createServerSocket(int port, String keystore, String password) throws IOException {
        return createServerSocket(port, keystore, password, false);
    }

    /**
     * @param port:   port to listen on
     * @param keystore:  the path to keystore file containing server key pair (private/public key); if <code>null</code> is passed
     * @param password: password needed to access keystore file
     * @return a SSL Socket bound on port specified
     * @throws IOException
     */
    public static SSLServerSocket createServerSocket(int port, String keystore, String password, boolean needClientAuth) throws IOException {
        SSLServerSocketFactory ssf = null;
        SSLServerSocket ss = null;

        try {
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;
            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream is = new FileInputStream(keystore);
            ks.load(is, password.toCharArray());
            kmf.init(ks, password.toCharArray());
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "Server keys loaded");

            ctx.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
            ssf = ctx.getServerSocketFactory();
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Creating SSocket");
            }
            ss = (SSLServerSocket) ssf.createServerSocket();

            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "SSocket created!");
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "SSocket binding on port " + port);
            }
            ss.bind(new InetSocketAddress(port));
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "SSocket bounded on port " + port);
            }
            ss.setNeedClientAuth(needClientAuth);
            // this socket will try to authenticate clients based on X.509 Certificates
            // ss.setWantClientAuth(true);
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "SSocket FINISHED ok! Bounded on " + port);
            }

        } catch (Throwable t) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Got Exception", t);
            }
            t.printStackTrace();
            throw new IOException(t.getMessage());
        }
        return ss;
    }

    /**
     * Creates a client socket connected to a TLS capable server - no server authentication is performed, it accept any server certificate (anonymous TLS sessions) todo, host-based
     * authentication should be performed in future
     *
     * @param host
     * @param port
     * @return
     * @throws IOException
     */
    public static Socket createClientSocket(String host, int port, boolean ssl) throws IOException {

        if (!ssl) {
            // Create a socket with a timeout
            InetAddress addr = InetAddress.getByName(host);
            SocketAddress sockaddr = new InetSocketAddress(addr, port);
            // Create an unbound socket
            Socket sock = new Socket();
            // This method will block no more than timeoutMs.
            // If the timeout occurs, SocketTimeoutException is thrown.
            int timeoutMs = 2000; // 2 seconds
            sock.connect(sockaddr, timeoutMs);
            return sock;
        }

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCertsTM = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }

            public void checkClientTrusted(@SuppressWarnings("unused")
                                                   java.security.cert.X509Certificate[] certs, @SuppressWarnings("unused")
                                                   String authType) {
            }

            public void checkServerTrusted(@SuppressWarnings("unused")
                                                   java.security.cert.X509Certificate[] certs, @SuppressWarnings("unused")
                                                   String authTsype) {
            }
        }};

        SSLSocketFactory factory = null;
        SSLContext ctx;

        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAllCertsTM, null);
            factory = ctx.getSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        SSLSocket socket = (SSLSocket) factory.createSocket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
        return socket;
    }

    /**
     * Create an SSL client socket that will provide authentication for itself based on the supplied keystore
     *
     * @param host
     * @param port
     * @param keystore
     * @param password
     * @return
     * @throws IOException
     */
    public static Socket createAuthClientSocket(String host, int port, String keystore, String password) throws IOException {

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCertsTM = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }

            public void checkClientTrusted(@SuppressWarnings("unused")
                                                   java.security.cert.X509Certificate[] certs, @SuppressWarnings("unused")
                                                   String authType) {
            }

            public void checkServerTrusted(@SuppressWarnings("unused")
                                                   java.security.cert.X509Certificate[] certs, @SuppressWarnings("unused")
                                                   String authTsype) {
            }
        }};

        SSLSocketFactory factory = null;
        SSLContext ctx;

        try {
            KeyManagerFactory kmf;
            KeyStore ks;
            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream is = new FileInputStream(keystore);
            ks.load(is, password.toCharArray());
            kmf.init(ks, password.toCharArray());
            ctx.init(kmf.getKeyManagers(), trustAllCertsTM, null);
            factory = ctx.getSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        SSLSocket socket = (SSLSocket) factory.createSocket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
        return socket;
    }

}
