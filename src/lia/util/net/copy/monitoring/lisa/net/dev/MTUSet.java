/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net.dev;

import java.io.Serializable;

/**
 * Wrapper for sending request to set a new value for the mtu
 * @author Ciprian Dobre
 */
public class MTUSet implements Serializable{

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1988671591829311032L;

	public String ifName;
	
	public int mtu;
	
} // end of class MTUSet
