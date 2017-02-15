/*
 * $Id$
 */
package lia.util.net.jiperf.control;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import lia.util.net.common.LocalHost;

/**
 * 
 * This will be kept for history :). 
 * The entire package lia.util.net.jiperf is the very first version of FDT. It
 * started as an Iperf-like test for Java.
 * 
 * @author ramiro
 */
public class ControlStream implements StreamConsumer {
	// consume stderr lines

	public BufferedReader stdout;

	public PrintWriter stdin;

	public Process proc;

	public Thread stderr;

	public void consumeLine(String line) {
		System.err.println(" [Server] DEBUG:" + line);
		// System.exit(1);
	}

	public void startServer(String host, String username, String command) throws IOException {
		this.proc = Runtime.getRuntime().exec("ssh -l " + username + " " + host + " " + command);
		this.stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		StreamPumper errorPumper = new StreamPumper(proc.getErrorStream(), null, this);
		this.stderr = new Thread(errorPumper);
		this.stderr.start();
		this.stdin = new PrintWriter(proc.getOutputStream(), true);
	}

	public void sendInitCommands(String myIp, int port, int threadsNumber, int windowSize) {
		if (myIp == null || myIp.length() == 0)
			myIp=LocalHost.getPublicIP4();
			this.stdin.println(myIp);
		this.stdin.println(port);
		this.stdin.println(threadsNumber);
		this.stdin.println(windowSize);
	}
	//	 read the ACK from server (blocking the thread)
	public void waitAck() throws IOException {
		String sAck = this.stdout.readLine();
		if (sAck == null) {
			throw new IOException("Invalid ack message");
		}
	}
	
	public void destroy(){
		proc.destroy();
	}

	public void awaitTermination() {
		try {
			proc.waitFor();
			// cleanup
			stderr.join();
			proc.getInputStream().close();
			proc.getOutputStream().close();
			proc.getErrorStream().close();
		} catch (Exception e) {
			System.err.println("Thread was interrupted while executing command \"" + "\"." + e);
		}
	}

	public static boolean isStdinOpen(){
		return FileDescriptor.in.valid();
	}
	public static void main(String[] args) throws IOException {
		// Execute the command using the specified environment
		System.out.println(LocalHost.getPublicIP4());
		ControlStream c = new ControlStream();
		System.out.println(args[1]);
		c.startServer(args[0], args[1], args[2]);
		c.stdin.println("LINE");
		System.out.println(" WAIT");
		// Parse the stdout of the command
		String line;
		StringBuffer buff = new StringBuffer();
		while ((line = c.stdout.readLine()) != null) {
			buff.append(line).append('\n');
		}
		String sStdout = buff.toString();
		// Set the exit code
		c.awaitTermination();
		int exitCode = c.proc.exitValue();
		System.out.println("Stdout (" + exitCode + "): " + sStdout);
	}
}