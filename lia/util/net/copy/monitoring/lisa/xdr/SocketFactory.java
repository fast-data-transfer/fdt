package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;



public class SocketFactory {
	
	private static final transient String COMPONENT = "lisa.comm.net.TLSSocketFactory";

	
	private static final transient Logger logger = Logger.getLogger(COMPONENT);

	
	private static final int CONNECT_TIMEOUT = 15 * 1000; 

	public static SSLServerSocket createServerSocket(int port, String keystore, String password) throws IOException {
		return createServerSocket(port, keystore, password, false);
	}

	
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

	
	public static Socket createClientSocket(String host, int port, boolean ssl) throws IOException {

		if (!ssl) {
			
			InetAddress addr = InetAddress.getByName(host);
			SocketAddress sockaddr = new InetSocketAddress(addr, port);
			
			Socket sock = new Socket();
			
			
			int timeoutMs = 2000; 
			sock.connect(sockaddr, timeoutMs);
			return sock;
		}

		
		TrustManager[] trustAllCertsTM = new TrustManager[] { new X509TrustManager() {
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
		} };

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

	
	public static Socket createAuthClientSocket(String host, int port, String keystore, String password) throws IOException {

		
		TrustManager[] trustAllCertsTM = new TrustManager[] { new X509TrustManager() {
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
		} };

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
