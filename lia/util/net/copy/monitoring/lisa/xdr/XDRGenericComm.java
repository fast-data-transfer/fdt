package lia.util.net.copy.monitoring.lisa.xdr;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class XDRGenericComm
		implements Runnable {
	
	private static final transient Logger logger = Logger.getLogger(XDRGenericComm.class.getName());

	
	String myName;
	protected XDRInputStream xdris;
	protected XDROutputStream xdros;
	protected boolean closed;
	private static long keys;
	private static Object keyLock;
	private String key;

	static {
		keyLock = new Object();
		synchronized (keyLock) {
			keys = 0;
		}
	}

	public XDRGenericComm(String myName, XDROutputStream xdros, XDRInputStream xdris, boolean closed) {
		
		setMyName(myName);
		this.xdris = xdris;
		this.xdros = xdros;
		this.closed = closed;
		key = null;
		
	}
	
	public XDRGenericComm(String myName, XDROutputStream xdros, XDRInputStream xdris) {
		this(myName,xdros,xdris,false);
	}
	
	
	public XDRGenericComm(String myName) {
		this(myName,null,null,true);
	}

	
	protected abstract void initSession() throws Exception;

	
	protected abstract void xdrSession() throws Exception;

	
	protected abstract void notifyXDRCommClosed();
	
	public void run() {
		try {
			initSession();
		} catch (Exception e) {
			if (logger.isLoggable(Level.WARNING))
				logger.log(Level.WARNING, "Session ["  + System.currentTimeMillis() + " ] " + myName + " K: [" + getKey() + "] cannot be initialized..closing", e);
			notifyXDRCommClosed();			
			close();
			return;
		}
		
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE," [ " + System.currentTimeMillis() + " ] " + getMyName() + " enter main-loop. ");
		
			try {
				
				xdrSession();
			} catch (Throwable t) {
				
				if (logger.isLoggable(Level.WARNING))
					logger.log(Level.WARNING, " [ " + System.currentTimeMillis() + " ] " + getMyName() + " is broken. Closing it.. ");
				
				StringWriter sw = new StringWriter();
				t.printStackTrace(new PrintWriter(sw));
				XDRMessage msg = XDRMessage.getErrorMessage(sw.getBuffer().toString());
				try {
					write(msg);
				} catch (Throwable tsend) {
				}				
			}		
			
		notifyXDRCommClosed();
		if (logger.isLoggable(Level.INFO))
			logger.log(Level.INFO, " [ " + System.currentTimeMillis() + " ] " + myName + " K: [" + getKey() + "] exits now .... \n\n");
		close();
	}

	public XDRMessage read()  throws IOException {
		try {
			XDRMessage msg = new XDRMessage();
			msg.xdrMessageSize = xdris.readInt();
			xdris.pad();
			msg.status = xdris.readInt();
			xdris.pad();
			msg.payload = xdris.readString();
			xdris.pad();
			return msg;
		}catch(java.io.EOFException eofe){
			logger.log(Level.INFO, " [ " + System.currentTimeMillis() + " ] " + getMyName() + ": Connection closed by remote host");
			throw new IOException (" [ " + System.currentTimeMillis() + " ] " + getMyName() + ": Connection closed by remote host");
		} catch (Throwable t) {
			if (logger.isLoggable(Level.FINEST))
				logger.log(Level.FINEST, "XDR Read Error: Cause:", t);
			else if ( logger.isLoggable(Level.WARNING) )
				logger.log(Level.WARNING, "XDR Read Error. Cause:["+t.getMessage()+"]");
			throw new IOException ("XDR Read Error: ["+t.getMessage()+"]");
		}		
	}

	public synchronized void write(XDRMessage msg) throws IOException {
		try {
			msg.xdrMessageSize = getXDRSize(msg);

			xdros.writeInt(msg.xdrMessageSize);
			xdros.pad();
			xdros.writeInt(msg.status);
			xdros.pad();
			xdros.writeString(msg.payload);
			xdros.pad();
			xdros.flush();
		} catch (Throwable t) {
			if (logger.isLoggable(Level.FINEST))
				logger.log(Level.FINEST, "XDR Write Error", t);
			else if ( logger.isLoggable(Level.WARNING) )
				logger.log(Level.WARNING, "XDR Write Error. Cause:["+t.getMessage()+"]");
			throw new IOException ("XDR Write error: ["+t.getMessage()+"]");
		}
	}

	public void close() {
		if (!closed) {
			closed = true;
			try {
				if (xdris != null)
					xdris.close();
				if (xdros != null)
					xdros.close();
			} catch (Throwable t) {

			}
		}
	}

	private static String nextKey() {
		synchronized (keyLock) {
			return "" + keys++;
		}
	}

	public String getKey() {
		if (key == null) {
			key = nextKey();
		}
		return key;
	}

	private int getXDRSize(String data) {
		int size = 0;
		if (data != null && data.length() != 0) {
			size = data.length() + 4;
			
			if (size % 4 != 0)
				size += (4 - size % 4);
		}
		return size;
	}

	private int getXDRSize(XDRMessage msg) {
		int size = 8; 
		size += getXDRSize(msg.payload);
		return size;
	}

	
	public String getMyName() {
		return this.myName;
	}

	
	public void setMyName(String myName) {
		this.myName = myName;
	}
}
