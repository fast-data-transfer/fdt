/*
 * $Id: XDRGenericComm.java 356 2007-08-16 14:31:17Z ramiro $
 */
package lia.util.net.copy.monitoring.lisa.xdr;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Adrian Muraru
 */
public abstract class XDRGenericComm
		implements Runnable {
	/** Logger used by this class */
	private static final transient Logger logger = Logger.getLogger(XDRGenericComm.class.getName());

	//XDRSessionHandler sessionHandler;
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
		//this.sessionHandler = sessionHandler;
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

	/**
	 * Called just before the session start
	 * Can be used for authentication
	 * 
	 * @throws {@link Exception}
	 *             if comm. session could not be established
	 */
	protected abstract void initSession() throws Exception;

	/**
	 * XDR Session protocol
	 * 
	 * @throws {@link Exception}
	 *             if session is broken and it should be closed
	 */
	protected abstract void xdrSession() throws Exception;

	/**
	 * Called just before the session closing. Should be used to do upper-layers
	 * cleanups
	 */
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
		// else: session successfully init'ed ...enter main loop
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE," [ " + System.currentTimeMillis() + " ] " + getMyName() + " enter main-loop. ");
		
			try {
				/*
				 * XDRMessage xdrMsg = read(); if (xdrMsg == null) continue;
				 * notifier.notifyXDRMessage(xdrMsg, this);
				 */
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
		if (!closed) {// allow multiple invocation for close()
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
			/*
			 * the length of the XDR representation must be a multiple of 4, so
			 * there might be some extra bytes added
			 */
			if (size % 4 != 0)
				size += (4 - size % 4);
		}
		return size;
	}

	private int getXDRSize(XDRMessage msg) {
		int size = 8; // two integers (size and status)
		size += getXDRSize(msg.payload);
		return size;
	}

	/**
	 * @return the friendly name of this communication endpoint
	 */
	public String getMyName() {
		return this.myName;
	}

	/**
	 * @param myName:
	 *            communication endpoint friendly name
	 */
	public void setMyName(String myName) {
		this.myName = myName;
	}
}
