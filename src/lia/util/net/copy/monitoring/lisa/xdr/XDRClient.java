/*
 * $Id: XDRClient.java 492 2008-02-20 15:03:54Z catac $
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;

/**
 * java implementation of XDR Client. extends
 * 
 * @author Lucian Musat
 */
public final class XDRClient extends XDRTcpSocket {

	/** state of xdr connection * */
	// private int state = NO_STATE;
	// public static final int NO_STATE = 0;
	// public static final int STATE_INIT = 1;
	// public static final int STATE_SESSION = 2;
	// public static final int STATE_CLOSED = 3;
	/** puts the xdr session in wait mode because it is not used, at least not yet */
	private Object waitObj = new Object();
	/** if should wait on the wait object or not */
	private volatile boolean bWait = true;

	protected XDRClient(String sHost, int nPort) throws IOException {
		super(SocketFactory.createClientSocket(sHost, nPort, false));
	}

	protected XDRClient(String sHost, int nPort, boolean ssl) throws IOException {
		super(SocketFactory.createClientSocket(sHost, nPort, ssl));
	}

	/**
	 * creates a new client connection with the xdr lisa server
	 * 
	 * @param sHost
	 *            hostname of lisa
	 * @param nPort
	 *            port on which the xdr daemon listens
	 * @return new xdr client or null if it could not be created or started
	 */
	public static XDRClient getClient(String sHost, int nPort, boolean ssl) {
		XDRClient cl = null;
		try {
			cl = new XDRClient(sHost, nPort,ssl);
			new Thread(cl).start();
		} catch (IOException e) {
			e.printStackTrace();
			cl = null;
		}
		return cl;
	}

	public static XDRClient getClient(String sHost, int nPort) {
		return getClient(sHost, nPort, false);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few parameters.\nUsage: <xdr_client> <host> <port>");
			System.exit(-1);
		}
		int nPort = 0;
		try {
			nPort = Integer.parseInt(args[1]);
		} catch (Exception ex) {
			System.err.println("Invalid port number.\nUsage: <xdr_client> <host> <port>");
			System.exit(-1);
		}
		try {
			new Thread(new XDRClient(args[0], nPort)).start();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	protected void initSession() throws Exception {
		// TODO do authentication
		System.out.println("Init connection");
		// state = STATE_INIT;
	}

	@Override
	protected void xdrSession() throws Exception {
		// TODO do xdr session: exchange of information, main loop
		System.out.println("XDR session started");
		// state = STATE_SESSION;
		if (bWait) {
			try {
				synchronized (waitObj) {
					waitObj.wait();
				}
			} catch (InterruptedException ex) {

			}
		}
		System.out.println("XDR session ended");
	}

	public void close() {
		super.close();
		// notify the xdr session that should finish
		// why here and not in notify... is because the notify will be called after xdrSession ends
		bWait = false;
		synchronized (waitObj) {
			waitObj.notify();
		}
	}

	@Override
	protected void notifyXDRCommClosed() {
		// TODO this end was just notified of connection closed
		System.out.println("Connection closed");
		// state = STATE_CLOSED;
	}

	synchronized public String sendCommand(String sCommand) throws Exception {
		XDRMessage msg = XDRMessage.getSuccessMessage(sCommand);
		XDRMessage resMsg = null;
		write(msg);
		resMsg = read();

		if (resMsg == null)
			throw new Exception("no connection!!!");
		if (resMsg.status == XDRMessage.ERROR)
			throw new Exception(resMsg.payload);

		return resMsg.payload;
	}

	public boolean isClosed() {
		return super.closed;
	}

}
