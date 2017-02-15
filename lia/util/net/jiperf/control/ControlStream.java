package lia.util.net.jiperf.control;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import lia.util.net.common.LocalHost;

public class ControlStream implements StreamConsumer {
	

	public BufferedReader stdout;

	public PrintWriter stdin;

	public Process proc;

	public Thread stderr;

	public void consumeLine(String line) {
		System.err.println(" [Server] DEBUG:" + line);
		
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
		
		System.out.println(LocalHost.getPublicIP4());
		ControlStream c = new ControlStream();
		System.out.println(args[1]);
		c.startServer(args[0], args[1], args[2]);
		c.stdin.println("LINE");
		System.out.println(" WAIT");
		
		String line;
		StringBuffer buff = new StringBuffer();
		while ((line = c.stdout.readLine()) != null) {
			buff.append(line).append('\n');
		}
		String sStdout = buff.toString();
		
		c.awaitTermination();
		int exitCode = c.proc.exitValue();
		System.out.println("Stdout (" + exitCode + "): " + sStdout);
	}
}