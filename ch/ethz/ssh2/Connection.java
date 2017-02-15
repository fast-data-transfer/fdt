
package ch.ethz.ssh2;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Vector;

import ch.ethz.ssh2.auth.AuthenticationManager;
import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.crypto.CryptoWishList;
import ch.ethz.ssh2.crypto.cipher.BlockCipherFactory;
import ch.ethz.ssh2.crypto.digest.MAC;
import ch.ethz.ssh2.transport.KexManager;
import ch.ethz.ssh2.transport.TransportManager;
import ch.ethz.ssh2.util.TimeoutService;
import ch.ethz.ssh2.util.TimeoutService.TimeoutToken;



public class Connection
{
	
	public final static String identification = "Ganymed Build_210beta8";

	

	private SecureRandom generator;

	
	public static synchronized String[] getAvailableCiphers()
	{
		return BlockCipherFactory.getDefaultCipherList();
	}

	
	public static synchronized String[] getAvailableMACs()
	{
		return MAC.getMacList();
	}

	
	public static synchronized String[] getAvailableServerHostKeyAlgorithms()
	{
		return KexManager.getDefaultServerHostkeyAlgorithmList();
	}

	private AuthenticationManager am;

	private boolean authenticated = false;
	private ChannelManager cm;

	private CryptoWishList cryptoWishList = new CryptoWishList();

	private DHGexParameters dhgexpara = new DHGexParameters();

	private final String hostname;

	private final int port;

	private TransportManager tm;

	private boolean tcpNoDelay = false;

	private ProxyData proxyData = null;

	private Vector connectionMonitors = new Vector();

	
	public Connection(String hostname)
	{
		this(hostname, 22);
	}

	
	public Connection(String hostname, int port)
	{
		this.hostname = hostname;
		this.port = port;
	}

	
	public synchronized boolean authenticateWithDSA(String user, String pem, String password) throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Connection is not established!");

		if (authenticated)
			throw new IllegalStateException("Connection is already authenticated!");

		if (am == null)
			am = new AuthenticationManager(tm);

		if (cm == null)
			cm = new ChannelManager(tm);

		if (user == null)
			throw new IllegalArgumentException("user argument is null");

		if (pem == null)
			throw new IllegalArgumentException("pem argument is null");

		authenticated = am.authenticatePublicKey(user, pem.toCharArray(), password, getOrCreateSecureRND());

		return authenticated;
	}

	
	public synchronized boolean authenticateWithKeyboardInteractive(String user, InteractiveCallback cb)
			throws IOException
	{
		return authenticateWithKeyboardInteractive(user, null, cb);
	}

	
	public synchronized boolean authenticateWithKeyboardInteractive(String user, String[] submethods,
			InteractiveCallback cb) throws IOException
	{
		if (cb == null)
			throw new IllegalArgumentException("Callback may not ne NULL!");

		if (tm == null)
			throw new IllegalStateException("Connection is not established!");

		if (authenticated)
			throw new IllegalStateException("Connection is already authenticated!");

		if (am == null)
			am = new AuthenticationManager(tm);

		if (cm == null)
			cm = new ChannelManager(tm);

		if (user == null)
			throw new IllegalArgumentException("user argument is null");

		authenticated = am.authenticateInteractive(user, submethods, cb);

		return authenticated;
	}

	
	public synchronized boolean authenticateWithPassword(String user, String password) throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Connection is not established!");

		if (authenticated)
			throw new IllegalStateException("Connection is already authenticated!");

		if (am == null)
			am = new AuthenticationManager(tm);

		if (cm == null)
			cm = new ChannelManager(tm);

		if (user == null)
			throw new IllegalArgumentException("user argument is null");

		if (password == null)
			throw new IllegalArgumentException("password argument is null");

		authenticated = am.authenticatePassword(user, password);

		return authenticated;
	}

	
	public synchronized boolean authenticateWithPublicKey(String user, char[] pemPrivateKey, String password)
			throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Connection is not established!");

		if (authenticated)
			throw new IllegalStateException("Connection is already authenticated!");

		if (am == null)
			am = new AuthenticationManager(tm);

		if (cm == null)
			cm = new ChannelManager(tm);

		if (user == null)
			throw new IllegalArgumentException("user argument is null");

		if (pemPrivateKey == null)
			throw new IllegalArgumentException("pemPrivateKey argument is null");

		authenticated = am.authenticatePublicKey(user, pemPrivateKey, password, getOrCreateSecureRND());

		return authenticated;
	}

	
	public synchronized boolean authenticateWithPublicKey(String user, File pemFile, String password)
			throws IOException
	{
		if (pemFile == null)
			throw new IllegalArgumentException("pemFile argument is null");

		char[] buff = new char[256];

		CharArrayWriter cw = new CharArrayWriter();

		FileReader fr = new FileReader(pemFile);

		while (true)
		{
			int len = fr.read(buff);
			if (len < 0)
				break;
			cw.write(buff, 0, len);
		}

		fr.close();

		return authenticateWithPublicKey(user, cw.toCharArray(), password);
	}

	
	public synchronized void addConnectionMonitor(ConnectionMonitor cmon)
	{
		if (cmon == null)
			throw new IllegalArgumentException("cmon argument is null");

		connectionMonitors.addElement(cmon);

		if (tm != null)
			tm.setConnectionMonitors(connectionMonitors);
	}

	
	public synchronized void close()
	{
		Throwable t = new Throwable("Closed due to user request.");
		close(t, false);
	}

	private void close(Throwable t, boolean hard)
	{
		if (cm != null)
			cm.closeAllChannels();

		if (tm != null)
		{
			tm.close(t, hard == false);
			tm = null;
		}
		am = null;
		cm = null;
		authenticated = false;
	}

	
	public synchronized ConnectionInfo connect() throws IOException
	{
		return connect(null, 0, 0);
	}

	
	public synchronized ConnectionInfo connect(ServerHostKeyVerifier verifier) throws IOException
	{
		return connect(verifier, 0, 0);
	}

	
	public synchronized ConnectionInfo connect(ServerHostKeyVerifier verifier, int connectTimeout, int kexTimeout)
			throws IOException
	{
		final class TimeoutState
		{
			boolean isCancelled = false;
			boolean timeoutSocketClosed = false;
		}

		if (tm != null)
			throw new IOException("Connection to " + hostname + " is already in connected state!");

		if (connectTimeout < 0)
			throw new IllegalArgumentException("connectTimeout must be non-negative!");

		if (kexTimeout < 0)
			throw new IllegalArgumentException("kexTimeout must be non-negative!");

		final TimeoutState state = new TimeoutState();

		tm = new TransportManager(hostname, port);

		tm.setConnectionMonitors(connectionMonitors);

		

		synchronized (tm)
		{
			
		}

		try
		{
			TimeoutToken token = null;

			if (kexTimeout > 0)
			{
				final Runnable timeoutHandler = new Runnable()
				{
					public void run()
					{
						synchronized (state)
						{
							if (state.isCancelled)
								return;
							state.timeoutSocketClosed = true;
							tm.close(new SocketTimeoutException("The connect timeout expired"), false);
						}
					}
				};

				long timeoutHorizont = System.currentTimeMillis() + kexTimeout;

				token = TimeoutService.addTimeoutHandler(timeoutHorizont, timeoutHandler);
			}

			try
			{
				tm.initialize(cryptoWishList, verifier, dhgexpara, connectTimeout, getOrCreateSecureRND(), proxyData);
			}
			catch (SocketTimeoutException se)
			{
				throw (SocketTimeoutException) new SocketTimeoutException(
						"The connect() operation on the socket timed out.").initCause(se);
			}

			tm.setTcpNoDelay(tcpNoDelay);

			

			ConnectionInfo ci = tm.getConnectionInfo(1);

			

			if (token != null)
			{
				TimeoutService.cancelTimeoutHandler(token);

				

				synchronized (state)
				{
					if (state.timeoutSocketClosed)
						throw new IOException("This exception will be replaced by the one below =)");
					
					state.isCancelled = true;
				}
			}

			return ci;
		}
		catch (SocketTimeoutException ste)
		{
			throw ste;
		}
		catch (IOException e1)
		{
			
			close(new Throwable("There was a problem during connect."), false);

			synchronized (state)
			{
				
				if (state.timeoutSocketClosed)
					throw new SocketTimeoutException("The kexTimeout (" + kexTimeout + " ms) expired.");
			}

			
			if (e1 instanceof HTTPProxyException)
				throw e1;

			throw (IOException) new IOException("There was a problem while connecting to " + hostname + ":" + port)
					.initCause(e1);
		}
	}

	
	public synchronized LocalPortForwarder createLocalPortForwarder(int local_port, String host_to_connect,
			int port_to_connect) throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Cannot forward ports, you need to establish a connection first.");

		if (!authenticated)
			throw new IllegalStateException("Cannot forward ports, connection is not authenticated.");

		return new LocalPortForwarder(cm, local_port, host_to_connect, port_to_connect);
	}

	
	public synchronized LocalStreamForwarder createLocalStreamForwarder(String host_to_connect, int port_to_connect)
			throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Cannot forward, you need to establish a connection first.");

		if (!authenticated)
			throw new IllegalStateException("Cannot forward, connection is not authenticated.");

		return new LocalStreamForwarder(cm, host_to_connect, port_to_connect);
	}

	
	public synchronized SCPClient createSCPClient() throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Cannot create SCP client, you need to establish a connection first.");

		if (!authenticated)
			throw new IllegalStateException("Cannot create SCP client, connection is not authenticated.");

		return new SCPClient(this);
	}

	
	public synchronized void forceKeyExchange() throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("You need to establish a connection first.");

		tm.forceKeyExchange(cryptoWishList, dhgexpara);
	}

	
	public synchronized String getHostname()
	{
		return hostname;
	}

	
	public synchronized int getPort()
	{
		return port;
	}

	
	public synchronized ConnectionInfo getConnectionInfo() throws IOException
	{
		if (tm == null)
			throw new IllegalStateException(
					"Cannot get details of connection, you need to establish a connection first.");
		return tm.getConnectionInfo(1);
	}

	
	public synchronized String[] getRemainingAuthMethods(String user) throws IOException
	{
		if (user == null)
			throw new IllegalArgumentException("user argument may not be NULL!");

		if (tm == null)
			throw new IllegalStateException("Connection is not established!");

		if (authenticated)
			throw new IllegalStateException("Connection is already authenticated!");

		if (am == null)
			am = new AuthenticationManager(tm);

		if (cm == null)
			cm = new ChannelManager(tm);

		return am.getRemainingMethods(user);
	}

	
	public synchronized boolean isAuthenticationComplete()
	{
		return authenticated;
	}

	
	public synchronized boolean isAuthenticationPartialSuccess()
	{
		if (am == null)
			return false;

		return am.getPartialSuccess();
	}

	
	public synchronized boolean isAuthMethodAvailable(String user, String method) throws IOException
	{
		if (method == null)
			throw new IllegalArgumentException("method argument may not be NULL!");

		String methods[] = getRemainingAuthMethods(user);

		for (int i = 0; i < methods.length; i++)
		{
			if (methods[i].compareTo(method) == 0)
				return true;
		}

		return false;
	}

	private final SecureRandom getOrCreateSecureRND()
	{
		if (generator == null)
			generator = new SecureRandom();
		
		return generator;
	}
	
	
	public synchronized Session openSession() throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("Cannot open session, you need to establish a connection first.");

		if (!authenticated)
			throw new IllegalStateException("Cannot open session, connection is not authenticated.");

		return new Session(cm, getOrCreateSecureRND());
	}

	
	private String[] removeDuplicates(String[] list)
	{
		if ((list == null) || (list.length < 2))
			return list;

		String[] list2 = new String[list.length];

		int count = 0;

		for (int i = 0; i < list.length; i++)
		{
			boolean duplicate = false;

			String element = list[i];

			for (int j = 0; j < count; j++)
			{
				if (((element == null) && (list2[j] == null)) || ((element != null) && (element.equals(list2[j]))))
				{
					duplicate = true;
					break;
				}
			}

			if (duplicate)
				continue;

			list2[count++] = list[i];
		}

		if (count == list2.length)
			return list2;

		String[] tmp = new String[count];
		System.arraycopy(list2, 0, tmp, 0, count);

		return tmp;
	}

	
	public synchronized void setClient2ServerCiphers(String[] ciphers)
	{
		if ((ciphers == null) || (ciphers.length == 0))
			throw new IllegalArgumentException();
		ciphers = removeDuplicates(ciphers);
		BlockCipherFactory.checkCipherList(ciphers);
		cryptoWishList.c2s_enc_algos = ciphers;
	}

	
	public synchronized void setClient2ServerMACs(String[] macs)
	{
		if ((macs == null) || (macs.length == 0))
			throw new IllegalArgumentException();
		macs = removeDuplicates(macs);
		MAC.checkMacList(macs);
		cryptoWishList.c2s_mac_algos = macs;
	}

	
	public synchronized void setDHGexParameters(DHGexParameters dgp)
	{
		if (dgp == null)
			throw new IllegalArgumentException();

		dhgexpara = dgp;
	}

	
	public synchronized void setServer2ClientCiphers(String[] ciphers)
	{
		if ((ciphers == null) || (ciphers.length == 0))
			throw new IllegalArgumentException();
		ciphers = removeDuplicates(ciphers);
		BlockCipherFactory.checkCipherList(ciphers);
		cryptoWishList.s2c_enc_algos = ciphers;
	}

	
	public synchronized void setServer2ClientMACs(String[] macs)
	{
		if ((macs == null) || (macs.length == 0))
			throw new IllegalArgumentException();

		macs = removeDuplicates(macs);
		MAC.checkMacList(macs);
		cryptoWishList.s2c_mac_algos = macs;
	}

	
	public synchronized void setServerHostKeyAlgorithms(String[] algos)
	{
		if ((algos == null) || (algos.length == 0))
			throw new IllegalArgumentException();

		algos = removeDuplicates(algos);
		KexManager.checkServerHostkeyAlgorithmsList(algos);
		cryptoWishList.serverHostKeyAlgorithms = algos;
	}

	
	public synchronized void setTCPNoDelay(boolean enable) throws IOException
	{
		tcpNoDelay = enable;

		if (tm != null)
			tm.setTcpNoDelay(enable);
	}

	
	public synchronized void setProxyData(ProxyData proxyData)
	{
		this.proxyData = proxyData;
	}

	
	public synchronized void requestRemotePortForwarding(String bindAddress, int bindPort, String targetAddress,
			int targetPort) throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("You need to establish a connection first.");

		if (!authenticated)
			throw new IllegalStateException("The connection is not authenticated.");

		if ((bindAddress == null) || (targetAddress == null) || (bindPort <= 0) || (targetPort <= 0))
			throw new IllegalArgumentException();

		cm.requestGlobalForward(bindAddress, bindPort, targetAddress, targetPort);
	}

	
	public synchronized void cancelRemotePortForwarding(int bindPort) throws IOException
	{
		if (tm == null)
			throw new IllegalStateException("You need to establish a connection first.");

		if (!authenticated)
			throw new IllegalStateException("The connection is not authenticated.");

		cm.requestCancelGlobalForward(bindPort);
	}
	
	
	public synchronized void setSecureRandom(SecureRandom rnd)
	{
		if (rnd == null)
			throw new IllegalArgumentException();
		
		this.generator = rnd;
	}
}
