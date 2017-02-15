
package lia.util.net.common;

import java.io.IOException;
import java.io.InputStream;


public interface ControlStream {

	public void startProgram(String cmd) throws IOException;

	public InputStream getProgramStdOut() throws IOException;

	public InputStream getProgramStdErr() throws IOException;

	
	public void waitForControlMessage(String expect, boolean allowEOF, boolean grabRemainingLog) throws IOException;

	
	public void waitForControlMessage(String expect, boolean allowEOF) throws IOException;

	
	public void waitForControlMessage(String expect) throws IOException;

	
	public void saveStdErr() throws IOException;

	public int getExitCode();

	public void close();

}