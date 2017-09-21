/*
 * $Id$
 */
package lia.gsi.net;

import lia.gsi.authz.LocalMappingAuthorization;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.net.GssSocket;
import org.globus.gsi.gssapi.net.GssSocketFactory;
import org.globus.net.ServerSocketFactory;
import org.globus.net.SocketFactory;
import org.globus.util.Util;
import org.globus.util.deactivator.DeactivationHandler;
import org.globus.util.deactivator.Deactivator;
import org.gridforum.jgss.ExtendedGSSContext;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;

import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides the basics for writing various servers. <b>Note:</b> Sockets created by this server have a 5
 * minute default timeout. The timeout can be changed using the {@link #setTimeout(int) setTimeout()} function.
 *
 * @author Adrian Muraru
 */
public abstract class GSIBaseServer implements Runnable {

    private static final Logger logger = Logger.getLogger(GSIBaseServer.class.getName());

    /**
     * Socket timeout in milliseconds.
     */
    private static final int SO_TIMEOUT = Integer.getInteger("GSI_SO_TIMEOUT", 5 * 60 * 1000);

    protected volatile boolean accept;

    protected ServerSocket _server = null;
    protected String url = null;
    protected GSSCredential credentials = null;
    protected Integer gssMode = GSIConstants.MODE_SSL;
    protected int timeout = SO_TIMEOUT;
    /**
     * A handler for the deactivation framework.
     */
    protected AbstractServerDeactivator deactivator = null;
    /**
     * This method should be called by all subclasses.
     */
    String authzClassName;
    private Thread serverThread = null;
    private boolean secure = true;

    public GSIBaseServer() throws IOException {
        this(null, 0);
    }

    public GSIBaseServer(final int port) throws IOException {
        this(null, port);
    }

    public GSIBaseServer(final GSSCredential cred, final int port) throws IOException {
        this.credentials = cred;
        this._server = ServerSocketFactory.getDefault().createServerSocket(port);
        this.secure = true;
        initialize();
    }

    public GSIBaseServer(final boolean secure, final int port) throws IOException {
        this.credentials = null;
        this._server = ServerSocketFactory.getDefault().createServerSocket(port);
        this.secure = secure;
        initialize();
    }

    protected void initialize() {

        authzClassName = System.getProperty("gsi.authz.Authorization");
        if (authzClassName == null) {
            authzClassName = "lia.gsi.authz.GridMapAuthorization";
        }
    }

    private LocalMappingAuthorization createAuthorizer() {
        try {
            final Class clazz = Class.forName(authzClassName);
            if (!LocalMappingAuthorization.class.isAssignableFrom(clazz)) {
                throw new RuntimeException("Invalid Server Authorization class");
            }
            return (LocalMappingAuthorization) clazz.newInstance();
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Unable to load '" + authzClassName + "' authorization plugin class. Cause: " + e.getMessage());
        } catch (final InstantiationException e) {
            throw new RuntimeException("Unable to instantiate '" + authzClassName + "'authorization plugin class. Cause: " + e.getMessage());
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate '" + authzClassName + "'authorization plugin class. Cause: " + e.getMessage());
        } catch (Throwable t) {
            throw new RuntimeException("Unable to instantiate '" + authzClassName + "'authorization plugin class. Cause: " + t.getMessage());
        }

    }

    public int getTimeout() {
        return this.timeout;
    }

    /**
     * Sets timeout for the created sockets. By default if not set, 5 minute timeout is used.
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * Stops the server but does not stop all the client threads
     */
    public void shutdown() {
        accept = false;
        try {
            _server.close();
        } catch (final Exception e) {
        }
        // this is a hack to ensue the server socket is
        // unblocked from accpet()
        // but this is not guaranteed to work still
        final SocketFactory factory = SocketFactory.getDefault();
        Socket s = null;
        try {
            s = factory.createSocket(InetAddress.getLocalHost(), getPort());
            s.getInputStream();
        } catch (final Exception e) {
            // can be ignored
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (final Exception e) {
                }
            }
        }

        // reset everything
        serverThread = null;
        _server = null;
    }

    /**
     * Starts the server.
     */
    protected void start() {
        if (serverThread == null) {
            accept = true;
            serverThread = new Thread(this);
            serverThread.setName("GSIServer");
            serverThread.start();
        }
    }

    public GSSCredential getCredentials() {
        return this.credentials;
    }

    public String getProtocol() {
        return (secure) ? "https" : "http";
    }

    /**
     * Returns url of this server
     *
     * @return url of this server
     */
    public String getURL() {
        if (url == null) {
            final StringBuffer buf = new StringBuffer();
            buf.append(getProtocol()).append("://").append(getHost()).append(":").append(String.valueOf(getPort()));
            url = buf.toString();
        }
        return url;
    }

    /**
     * Returns port of this server
     *
     * @return port number
     */
    public int getPort() {
        return _server.getLocalPort();
    }

    /**
     * Returns hostname of this server
     *
     * @return hostname
     */
    public String getHostname() {
        return Util.getLocalHostAddress();
    }

    /**
     * Returns hostname of this server. The format of the host conforms to RFC 2732, i.e. for a literal IPv6 address,
     * this method will return the IPv6 address enclosed in square
     * brackets ('[' and ']').
     *
     * @return hostname
     */
    public String getHost() {
        final String host = Util.getLocalHostAddress();
        try {
            final URL u = new URL("http", host, 80, "/");
            return u.getHost();
        } catch (final MalformedURLException e) {
            return host;
        }
    }

    public void run() {
        Socket socket = null;
        boolean error = false;

        while (accept) {
            error = false;
            socket = null;

            try {
                try {
                    socket = _server.accept();
                    if (!accept) {
                        break;
                    }
                } catch (final IOException e) {
                    if (accept) { // display error message
                        logger.log(Level.WARNING, "Server died: " + e.getMessage(), e);
                    }
                    error = true;
                    break;
                }

                if (socket == null) {
                    continue;
                }

                try {
                    socket.setSoTimeout(getTimeout());

                    Peer peer = null;
                    if (this.secure) {
                        try {
                            peer = wrapSocket(socket);
                        } catch (final GSSException e) {
                            logger.log(Level.WARNING, "Failed to secure the socket", e);
                        }
                    } else {
                        peer = new Peer(socket, null);
                    }

                    handleConnection(peer);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " [ GSIBaseServer ] Exception while trying to secure GSI socket: " + socket, t);
                    error = true;
                }

            } finally {
                if (error && socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable ignore) {
                    }
                }
            }

        }

        logger.log(Level.WARNING, "server thread stopped");
    }

    protected Peer wrapSocket(final Socket socket) throws GSSException {

        final GSSManager manager = ExtendedGSSManager.getInstance();

        final ExtendedGSSContext context = (ExtendedGSSContext) manager.createContext(credentials);

        context.setOption(GSSConstants.GSS_MODE, gssMode);

        final GssSocketFactory factory = GssSocketFactory.getDefault();

        final GssSocket gsiSocket = (GssSocket) factory.createSocket(socket, null, 0, context);
        // server socket
        gsiSocket.setUseClientMode(false);

        final LocalMappingAuthorization authorizer = createAuthorizer();
        gsiSocket.setAuthorization(authorizer);

        return new Peer(gsiSocket, authorizer);
    }

    public void setGssMode(final Integer mode) {
        this.gssMode = mode;
    }

    public void setAuthorizationClass(final String auth) {
        authzClassName = auth;
    }

    /**
     * This method needs to be implemented by subclasses. <br>
     * Optimmaly, it should be a non-blocking call starting a separate thread to handle the client. <br>
     * Note that to start an SSL handshake, you need to call socket.getInput(Output) stream().
     */
    protected abstract void handleConnection(Peer peer);

    /**
     * Registers a default deactivation handler. It is used to shutdown the server without having a reference to the
     * server. <br>
     * Call Deactivate.deactivateAll() to shutdown all registered servers.
     */
    public void registerDefaultDeactivator() {
        if (deactivator == null) {
            deactivator = new AbstractServerDeactivator(this);
        }
        Deactivator.registerDeactivation(deactivator);
    }

    /**
     * Unregisters a default deactivation handler.
     */
    public void unregisterDefaultDeactivator() {
        if (deactivator == null)
            return;
        Deactivator.unregisterDeactivation(deactivator);
    }

}

class AbstractServerDeactivator implements DeactivationHandler {

    private GSIBaseServer server = null;

    public AbstractServerDeactivator(GSIBaseServer server) {
        this.server = server;
    }

    public void deactivate() {
        if (server != null)
            server.shutdown();
    }

}
