
package lia.util.net.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import ch.ethz.ssh2.util.PasswordReader;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SSHControlStream implements ControlStream {

    private static final Logger logger = Logger.getLogger(SSHControlStream.class.getName());
    static final String knownHostPath = System.getProperty("user.home") + "/.ssh/known_hosts";
    static final String idDSAPath = System.getProperty("user.home") + "/.ssh/id_dsa.fdt";
    static final String idRSAPath = System.getProperty("user.home") + "/.ssh/id_rsa.fdt";
    
    private Connection conn;
    private Session sess;
    private String cmd;

    
    public SSHControlStream(String hostname, String username) throws IOException {
        this.conn = new Connection(hostname);
        conn.connect();

        

        boolean enableKeyboardInteractive = true;
        boolean enableDSA = true;
        boolean enableRSA = true;
        boolean enableKEY = true;

        String lastError = null;
        int passwordRetry = 0;

        
        
        
        final String configSshKeyPath = Config.getInstance().getSshKeyPath();
        File sshKeyPath = null;
        if (configSshKeyPath != null) {
            try {
                sshKeyPath = new File(configSshKeyPath);
                if (!sshKeyPath.exists() || !sshKeyPath.canRead()) {
                    sshKeyPath = null;
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, " [ SSHControlStream ] Unable to check configSshKeyPath", t);
            }
        }

        if (sshKeyPath == null) {
            enableKEY = false;
        }
        
        while (true) {
            if ((enableKEY || enableDSA || enableRSA) && conn.isAuthMethodAvailable(username, "publickey")) {

                if (enableKEY && sshKeyPath != null) {
                    boolean res = false;
                    String password = getPassword("\n [" + username + "@" + hostname + "] [Public key authentication] Enter password for SSH private key[" + configSshKeyPath + "]: ");
                    try {
                        res = conn.authenticateWithPublicKey(username, sshKeyPath, password);
                    } catch (Exception e) {
                        res = false;
                    }
                    if (res == true) {
                        break;
                    }

                    enableKEY = false;
                }

                if (enableDSA) {
                    File key = new File(idDSAPath);

                    if (key.exists() && key.canRead()) {
                        boolean res = false;
                        String password = getPassword("\n [" + username + "@" + hostname + "] [Public key authentication] Enter password for DSA private key[" + idDSAPath + "]: ");
                        try {
                            res = conn.authenticateWithPublicKey(username, key, password);
                        } catch (Exception e) {
                            res = false;
                        }
                        if (res == true) {
                            break;
                        }
                        lastError = "DSA authentication failed.";
                    }
                    enableDSA = false; 

                }

                if (enableRSA) {
                    File key = new File(idRSAPath);

                    if (key.exists() && key.exists()) {
                        boolean res = false;
                        String password = getPassword("\n[" + username + "@" + hostname + "] [Public key authentication] Enter password for RSA private key[" + idRSAPath + "]: ");
                        try {
                            res = conn.authenticateWithPublicKey(username, key, password);
                        } catch (Exception e) {
                            res = false;
                        }
                        if (res == true) {
                            break;
                        }
                        lastError = "RSA authentication failed.";
                    }
                    enableRSA = false; 

                }

                continue;
            }

            if (enableKeyboardInteractive && conn.isAuthMethodAvailable(username, "keyboard-interactive")) {
                InteractiveLogic il = new InteractiveLogic(lastError, "\n[" + username + "@" + hostname + "]");

                boolean res = false;
                try {
                    res = conn.authenticateWithKeyboardInteractive(username, il);
                } catch (Exception e) {
                    res = false;
                    lastError = "Keyboard-interactive auth failed. " + e.getMessage();
                }

                if (res == true) {
                    break;
                }
                if (il.getPromptCount() == 0) {
                    
                    
                    
                    

                    lastError = "Keyboard-interactive does not work.";

                    enableKeyboardInteractive = false; 

                } else {
                    lastError = "Keyboard-interactive auth failed."; 

                }

                continue;
            }

            if (conn.isAuthMethodAvailable(username, "password")) {
                boolean res = false;

                String password = getPassword("\n[" + username + "@" + hostname + "] [Password Authentication] Enter password: ");
                if (password == null || password.length() == 0) {
                    res = false;
                } else {
                    res = conn.authenticateWithPassword(username, password);
                }
                if (res == true) {
                    break;
                }
                lastError = "Password authentication failed."; 

                if (++passwordRetry == 3) {
                    throw new IOException("Too many tries. Give up.");
                }
                continue;
            }

            throw new IOException("No supported authentication methods available.");
        }
        
        this.sess = this.conn.openSession();
        this.sess.requestPTY("javash", 0, 0, 0, 0, null);
    }

    public String getPassword(String message) throws IOException {
        return PasswordReader.readPassword(message);
    }


    
    public void startProgram(String cmd) throws IOException {
        this.cmd = "/bin/bash --login -c '" + cmd + " 2>&1'";
        this.sess.execCommand(this.cmd);
    }

    
    public InputStream getProgramStdOut() {
        return this.sess.getStdout();
    }

    
    public InputStream getProgramStdErr() {
        return this.sess.getStderr();
    }

    
    public void waitForControlMessage(String expect, boolean allowEOF, boolean grabRemainingLog) throws IOException {
        
        
        BufferedReader br = new BufferedReader(new InputStreamReader(getProgramStdOut()));
        final String outputPrefix = "[" + this.conn.getHostname() + "]$ ";
        while (true) {
            String line = br.readLine();
            if (line == null) {
                if (allowEOF) {
                    return;
                
                }
                throw new IOException("[" + this.cmd + "] exited. No control message received]");
            }
            System.err.println(outputPrefix + line);
            if (line.trim().equalsIgnoreCase(expect)) {
                LogWriter lw = grabRemainingLog ? new LogWriter(br, "fdt_" + this.conn.getHostname() + ".log") : new LogWriter(br);
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
        LogWriter lw = new LogWriter(br, "fdt_" + this.conn.getHostname() + ".err");
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
                        if (out != null) {
                            out.close();
                        }
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
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e1) {
                }
                return;
            }
        }
    }

    
    public int getExitCode() {
        return this.sess.getExitStatus();
    }

    
    public void close() {
        this.sess.close();
        this.conn.close();
    }

    
    class InteractiveLogic implements InteractiveCallback {

        int promptCount = 0;
        String lastError;
        String prefix;

        public InteractiveLogic(String lastError, String prefix) {
            this.lastError = lastError;
            this.prefix = prefix;
        }

        
        public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws IOException {
            String[] result = new String[numPrompts];

            for (int i = 0; i < numPrompts; i++) {
                

                if (lastError != null) {
                    
                    lastError = null;
                }

                StringBuilder sContent = new StringBuilder();
                
                if (name != null && name.trim().length() > 0) {
                    sContent.append(name).append(' ');
                }
                if (instruction != null && instruction.trim().length() > 0) {
                    sContent.append(instruction).append('\n');
                }
                sContent.append(prefix).append("[Keyboard Interactive Authentication] ").append(prompt[i]);

                String userResponse = getPassword(sContent.toString());

                if (userResponse == null) {
                    throw new IOException("Login aborted by user");
                }
                result[i] = userResponse;
                promptCount++;
            }

            return result;
        }

        
        public int getPromptCount() {
            return promptCount;
        }
    }

    
    public static void main(String[] args) throws IOException {
        ControlStream cs = new SSHControlStream(args[0], args[1]);
        cs.startProgram(args[2]);

        
        InputStream stdout = new StreamGobbler(cs.getProgramStdOut());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);
        }
        System.out.println("ExitCode:" + cs.getExitCode());
        cs.close();
    }
}
