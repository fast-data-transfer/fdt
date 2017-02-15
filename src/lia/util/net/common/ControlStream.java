/*
 * $Id$
 */
package lia.util.net.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @author Adrian Muraru
 * 
 */
public interface ControlStream {

	/** Start the connection with the configured parameters */
	void connect() throws IOException;
	
	public void startProgram(String cmd) throws IOException;

	public InputStream getProgramStdOut() throws IOException;

	public InputStream getProgramStdErr() throws IOException;

	/**
	 * Wait for the control message <expect> and log the remaining ouput asynchronously in the fdt_<hostname>.log file (optional)
	 * 
	 * @param expect:
	 *            the control message we are looking for
	 * @param allowEOF:
	 *            if this is true it means that the EOF is accepted as a *control message* in the protocol
	 * @param grabRemainingLog:
	 *            if true start a backround thread and save the output in fdt_<hostname>.log file
	 * @throws IOException
	 */
	public void waitForControlMessage(String expect, boolean allowEOF, boolean grabRemainingLog) throws IOException;

	/**
	 * @see #waitForControlMessage(String, boolean, boolean) Wait for the control message but do save the remaining log in a file (thow it away /dev/null)
	 * @param expect
	 * @param allowEOF
	 * @throws IOException
	 */
	public void waitForControlMessage(String expect, boolean allowEOF) throws IOException;

	/**
	 * @see SSHControlStream#waitForControlMessage(String, boolean,boolean)
	 * @param expect
	 * @throws IOException
	 */
	public void waitForControlMessage(String expect) throws IOException;

	/**
	 * save the remote stderr stream in a local file BUG: for some reason this SSH library streams the program stdout and stderr on the same stream (stdout) back to the client
	 * 
	 * @unused : see BUG
	 * @param localFileName
	 */
	public void saveStdErr() throws IOException;

	public int getExitCode();

	public void close();

}