/*
 * $Id: TXQueueLenSet.java 352 2007-08-16 14:20:55Z ramiro $
 */
package lia.util.net.copy.monitoring.lisa.net.dev;

import java.io.Serializable;

/**
 * Wrapper for setting quelen...
 * @author Ciprian Dobre
 */
public class TXQueueLenSet implements Serializable {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1988671591829311032L;
	
	public String ifName;
	
	public int txqueuelen;
	
} // end of class TXQueueLenSet

