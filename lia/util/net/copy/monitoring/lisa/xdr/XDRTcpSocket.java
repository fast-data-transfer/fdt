
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;


public abstract class  XDRTcpSocket
		extends XDRGenericComm {
	
	
	private static final transient Logger logger = Logger.getLogger("lisa.comm.XDRTcpSocket");

	private Socket rawSocket;
	protected boolean closed;


	
	

	public XDRTcpSocket(Socket s) throws IOException {
		super("XDRTcpSocket for [ " + s.getInetAddress() + ":" + s.getPort() + " ] ", new XDROutputStream(s.getOutputStream()), new XDRInputStream(s.getInputStream()));
		this.rawSocket = s;
		closed = false;
		
	}

	
		
	public int getPort() {
		return rawSocket.getPort();
	}

	public int getLocalPort() {
		return rawSocket.getLocalPort();
	}

	public InetAddress getInetAddress() {
		return rawSocket.getInetAddress();
	}

	public InetAddress getLocalAddress() {
		return rawSocket.getLocalAddress();
	}

	public void close() {
		try {
			super.close();
			if (!closed) {
				closed = true;
				if (rawSocket != null) {
					rawSocket.close();
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


}
