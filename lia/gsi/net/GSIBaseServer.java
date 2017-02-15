
package lia.gsi.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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


public abstract class GSIBaseServer implements Runnable {

	private static final Logger logger = Logger.getLogger(GSIBaseServer.class.getName());

	
	public static final int SO_TIMEOUT = 5 * 60 * 1000;

	protected boolean accept;
	protected ServerSocket _server = null;
	private Thread serverThread = null;
	private boolean secure = true;
	protected String url = null;

	protected GSSCredential credentials = null;
	protected Integer gssMode = GSIConstants.MODE_SSL;

	protected int timeout = SO_TIMEOUT;

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

	
	String authzClassName;

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
		}

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
		
		
		
		final SocketFactory factory = SocketFactory.getDefault();
		Socket s = null;
		try {
			s = factory.createSocket(InetAddress.getLocalHost(), getPort());
			s.getInputStream();
		} catch (final Exception e) {
			
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (final Exception e) {
				}
			}
		}

		
		serverThread = null;
		_server = null;
	}

	
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

	
	public String getURL() {
		if (url == null) {
			final StringBuffer buf = new StringBuffer();
			buf.append(getProtocol()).append("://").append(getHost()).append(":").append(String.valueOf(getPort()));
			url = buf.toString();
		}
		return url;
	}

	
	public int getPort() {
		return _server.getLocalPort();
	}

	
	public String getHostname() {
		return Util.getLocalHostAddress();
	}

	
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

		while (accept) {

			try {
				socket = _server.accept();
				if (!accept) {
					break;
				}
				socket.setSoTimeout(getTimeout());
			} catch (final IOException e) {
				if (accept) { 
					logger.log(Level.WARNING, "Server died: " + e.getMessage(), e);
				}
				break;
			}

			Peer peer = null;
			if (this.secure) {
				try {
					peer = wrapSocket(socket);
				} catch (final GSSException e) {
					logger.log(Level.WARNING, "Failed to secure the socket", e);
					break;
				}
			} else
				peer = new Peer(socket, null);

			handleConnection(peer);
		}

		logger.log(Level.WARNING, "server thread stopped");
	}

	protected Peer wrapSocket(final Socket socket) throws GSSException {

		final GSSManager manager = ExtendedGSSManager.getInstance();

		final ExtendedGSSContext context = (ExtendedGSSContext) manager.createContext(credentials);

		context.setOption(GSSConstants.GSS_MODE, gssMode);

		final GssSocketFactory factory = GssSocketFactory.getDefault();

		final GssSocket gsiSocket = (GssSocket) factory.createSocket(socket, null, 0, context);
		
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

	
	protected abstract void handleConnection(Peer peer);

	
	public void registerDefaultDeactivator() {
		if (deactivator == null) {
			deactivator = new AbstractServerDeactivator(this);
		}
		Deactivator.registerDeactivation(deactivator);
	}

	
	public void unregisterDefaultDeactivator() {
		if (deactivator == null)
			return;
		Deactivator.unregisterDeactivation(deactivator);
	}

	
	protected AbstractServerDeactivator deactivator = null;

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
