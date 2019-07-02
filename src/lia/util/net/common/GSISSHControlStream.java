/*
 * $Id$
 */
package lia.util.net.common;

import ch.ethz.ssh2.StreamGobbler;
import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.session.SessionChannelClient;
import org.ietf.jgss.GSSException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Adrian Muraru
 */
public class GSISSHControlStream implements ControlStream {

    // configuration parameters
    private final String hostname;
    private final String username;
    private final int port;

    /**
     * the SSH connection & session
     */
    private SshClient conn;
    private SessionChannelClient sess;
    private String cmd;
    private String customShell;

    /**
     * Creates a new GSI SSH control connection on the default ssh port.
     * <p>
     * Same as {@link #GSISSHControlStream(String, String, int) GSISSHControlStream(hostname, username, 22)}
     *
     * @param hostname: remote host
     * @param username: remote account
     * @throws IOException in case of failure
     */
    public GSISSHControlStream(String hostname, String username) {
        this(hostname, username, 22);
    }

    /**
     * Creates a new SSH control connection on the specified remote GSI sshd server port
     *
     * @param port:     remote GSI-sshd port
     * @param hostname: remote host
     * @param username: remote account
     * @throws IOException in case of failure
     */
    public GSISSHControlStream(String hostname, String username, int port) {
        this.hostname = hostname;
        this.username = username;
        this.port = port;
    }

    // TEST
    public static void main(String[] args) throws IOException {
        ControlStream cs = new GSISSHControlStream(args[0], args[1]);
        cs.startProgram(args[2]);

		/* read stdout */
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

    public void connect() throws IOException {
        lia.gsi.ssh.GSIAuthenticationClient gsiAuth = null;
        try {
            gsiAuth = new lia.gsi.ssh.GSIAuthenticationClient();
            gsiAuth.setUsername(username);
        } catch (GSSException e) {
            throw new IOException("Cannot load grid credentials.");
        }
        conn = new SshClient();
        SshToolsConnectionProfile properties = new SshToolsConnectionProfile();
        // TODO: add new "port" parameter
        properties.setPort(port);
        properties.setForwardingAutoStartMode(false);
        properties.setHost(hostname);
        properties.setUsername(username);
        conn.setUseDefaultForwarding(false);
        conn.connect(properties);
        try {
            // Authenticate the user
            int result = conn.authenticate(gsiAuth, hostname);
            if (result != AuthenticationProtocolState.COMPLETE) {
                throw new IOException("GSI authentication failed");
            }
            // Open a session channel
            sess = conn.openSessionChannel();
            sess.requestPseudoTerminal("javash", 0, 0, 0, 0, "");
        } catch (Throwable t) {
            throw new IOException(t.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#startProgram(java.lang.String)
     */
    public void startProgram(String cmd, String customShell) throws IOException {
        this.cmd = customShell + " --login -c '" + cmd + " 2>&1'";
        this.sess.executeCommand(this.cmd);
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#getProgramStdOut()
     */
    public InputStream getProgramStdOut() {
        return this.sess.getInputStream();
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#getProgramStdErr()
     */
    public InputStream getProgramStdErr() throws IOException {
        return this.sess.getStderrInputStream();
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#waitForControlMessage(java.lang.String, boolean, boolean)
     */
    public void waitForControlMessage(String expect, boolean allowEOF, boolean grabRemainingLog) throws IOException {
        /* read stdout */
        // InputStream stdout = new StreamGobbler(getProgramStdOut());
        BufferedReader br = new BufferedReader(new InputStreamReader(getProgramStdOut()));
        final String outputPrefix = "[" + this.hostname + "]$ ";
        while (true) {
            String line = br.readLine();
            if (line == null) {
                if (allowEOF)
                    return;
                // else
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

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#waitForControlMessage(java.lang.String, boolean)
     */
    public void waitForControlMessage(String expect, boolean allowEOF) throws IOException {
        this.waitForControlMessage(expect, allowEOF, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#waitForControlMessage(java.lang.String)
     */
    public void waitForControlMessage(String expect) throws IOException {
        this.waitForControlMessage(expect, false, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#saveStdErr()
     */
    public void saveStdErr() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getProgramStdErr()));
        LogWriter lw = new LogWriter(br, "fdt_" + this.hostname + ".err");
        lw.setDaemon(true);
        lw.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#getExitCode()
     */
    public int getExitCode() {
        return this.sess.getExitCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see lia.util.net.common.ControlStream#close()
     */
    public void close() {
        try {
            if (this.sess != null) {
                this.sess.close();
            }
        } catch (IOException e) {

        }

        if (this.conn != null) {
            this.conn.disconnect();
        }
    }

    /**
     * asynch write in a local log file of the remote stderr stream
     */
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

}
