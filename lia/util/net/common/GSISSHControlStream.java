package lia.util.net.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.ietf.jgss.GSSException;

import ch.ethz.ssh2.StreamGobbler;

import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.session.SessionChannelClient;

public class GSISSHControlStream implements ControlStream {

	
	private SshClient conn;

	private SessionChannelClient sess;

	private String cmd;

	private String hostname;

	
	public GSISSHControlStream(String hostname, String username) throws IOException {
		this.hostname = hostname;
		lia.gsi.ssh.GSIAuthenticationClient gsiAuth = null;
		try {
			gsiAuth = new lia.gsi.ssh.GSIAuthenticationClient();
			gsiAuth.setUsername(username);
		} catch (GSSException e) {
			throw new IOException("Cannot load grid credentials.");
		}
		conn = new SshClient();
		SshToolsConnectionProfile properties = new SshToolsConnectionProfile();
		
		properties.setPort(1975);
		properties.setForwardingAutoStartMode(false);
		properties.setHost(hostname);
		properties.setUsername(username);
		conn.setUseDefaultForwarding(false);
		conn.connect(properties);
		try {
			
			int result = conn.authenticate(gsiAuth, hostname);
			if (result != AuthenticationProtocolState.COMPLETE) {
				throw new IOException("GSI authentication failed");
			}
			
			sess = conn.openSessionChannel();
			sess.requestPseudoTerminal("javash", 0, 0, 0, 0, "");
		} catch (Throwable t) {
			throw new IOException(t.getMessage());
		}
	}

	
	public void startProgram(String cmd) throws IOException {
		this.cmd = "/bin/bash --login -c '" + cmd + " 2>&1'";
		this.sess.executeCommand(this.cmd);
	}

	
	public InputStream getProgramStdOut() {
		return this.sess.getInputStream();
	}

	
	public InputStream getProgramStdErr() throws IOException {
		return this.sess.getStderrInputStream();
	}

	
	public void waitForControlMessage(String expect, boolean allowEOF, boolean grabRemainingLog) throws IOException {
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(getProgramStdOut()));
		final String outputPrefix = "[" + this.hostname + "]$ ";
		while (true) {
			String line = br.readLine();
			if (line == null) {
				if (allowEOF)
					return;
				
				throw new IOException("[" + this.cmd + "] exited. No control message received]");
			}
			System.err.println(outputPrefix + line);
			if (line.trim().equalsIgnoreCase(expect)) {
				LogWriter lw = grabRemainingLog
						? new LogWriter(br, "fdt_" + this.hostname + ".log")
						: new LogWriter(br);
				lw.setDaemon(true);
				lw.start();
				return;
			}
		}
	}

	
	public void waitForControlMessage(String expect, boolean allowEOF) throws IOException {
		this.waitForControlMessage(expect, allowEOF, false);
	}

	
	public void waitForControlMessage(String expect) throws IOException {
		this.waitForControlMessage(expect, false, true);
	}

	
	public void saveStdErr() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(getProgramStdErr()));
		LogWriter lw = new LogWriter(br, "fdt_" + this.hostname + ".err");
		lw.setDaemon(true);
		lw.start();
	}

	
	static class LogWriter extends Thread {
		BufferedReader br;
		String logFile;

		public LogWriter(BufferedReader br) {
			this.br = br;
			this.logFile = null;
		}

		public LogWriter(BufferedReader br, String fileName) {
			this.br = br;
			this.logFile = fileName;
		}

		public void run() {
			BufferedWriter out = null;
			try {
				if (this.logFile != null) {
					out = new BufferedWriter(new FileWriter(logFile, false));
					final Date date = new Date();
					out.write("==============" + new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z").format(date) + "================\n");
				}
				while (true) {
					String line = br.readLine();

					if (line == null) {
						if (out != null)
							out.close();
						return;
					}
					if (out != null) {
						out.write(line + "\n");
						out.flush();
					}
				}
			} catch (IOException e) {
				System.err.println("Cannot write remote log:" + logFile);
				try {
					if (out != null)
						out.close();
				} catch (IOException e1) {
				}
				return;
			}
		}
	}

	
	public int getExitCode() {
		return this.sess.getExitCode();
	}

	
	public void close() {
		try {
			this.sess.close();
		} catch (IOException e) {
		}
		this.conn.disconnect();
	}

	
	public static void main(String[] args) throws IOException {
		ControlStream cs = new GSISSHControlStream(args[0], args[1]);
		cs.startProgram(args[2]);

		
		InputStream stdout = new StreamGobbler(cs.getProgramStdOut());
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			System.out.println(line);
		}
		System.out.println("ExitCode:" + cs.getExitCode());
		cs.close();
	}

}
