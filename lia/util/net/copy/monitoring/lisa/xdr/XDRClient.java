package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;


public final class XDRClient extends XDRTcpSocket {

	
	
	
	
	
	
	
	private Object waitObj = new Object();
	
	private volatile boolean bWait = true;

	protected XDRClient(String sHost, int nPort) throws IOException {
		super(SocketFactory.createClientSocket(sHost, nPort, false));
	}

	protected XDRClient(String sHost, int nPort, boolean ssl) throws IOException {
		super(SocketFactory.createClientSocket(sHost, nPort, ssl));
	}

	
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
		
		System.out.println("Init connection");
		
	}

	@Override
	protected void xdrSession() throws Exception {
		
		System.out.println("XDR session started");
		
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
		
		
		bWait = false;
		synchronized (waitObj) {
			waitObj.notify();
		}
	}

	@Override
	protected void notifyXDRCommClosed() {
		
		System.out.println("Connection closed");
		
	}

	public String sendCommand(String sCommand) throws Exception {
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
