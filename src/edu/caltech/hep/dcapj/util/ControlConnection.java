package edu.caltech.hep.dcapj.util;

import edu.caltech.hep.dcapj.Config;
import edu.caltech.hep.dcapj.dCapLayer;
import edu.caltech.hep.dcapj.PnfsUtil;

import java.util.Hashtable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class ControlConnection implements Runnable {

    protected Hashtable<Integer, ControlCommandCallback> callbacks;

    private static final Logger _logger = Logger
            .getLogger(ControlConnection.class.getName());

    private Socket _client = null;

    private PrintWriter _clientOut = null;

    private BufferedReader _clientIn = null;

    private boolean _shouldBeRunning = true;

    private boolean _stopped = true;

    private Thread _clientThread = null;

    private Integer _nextSessionID = -1;

    public ControlConnection() throws IOException,
            InvalidConfigurationException {
        initialize();
    }

    private void initialize() throws IOException, InvalidConfigurationException {
        Config conf = dCapLayer.getConfig();

        if (conf == null) {
            String emsg = "Configuration object was null";
            throw new InvalidConfigurationException(emsg);
        }
        String dCapHost = conf.getdCapDoor(); // host:port

        if (dCapHost == null) {
            _logger
                    .fine("No defautl dCap host defined. dCapJ will try to find "
                            + " it through pnfs layer");

            // if no dcap door address specified in conf file, try to figure out
            // through the pnfs layer. If the pnfs was also not specified,
            // assume it to be /pnfs/fs/usr/data
            try {
                dCapHost = PnfsUtil
                        .getdCapDoor((conf.getPnfsDir() == null ? "/pnfs/fs/usr/data"
                                : conf.getPnfsDir()));
            } catch (Exception e) {
                _logger.finest(e.getMessage());
            }
        }

        String ip = null;
        int port = -1;

        // dcapHost can still be null
        if (dCapHost != null) {
            String tmp[] = dCapHost.split(":");
            if (tmp.length > 1) {
                ip = tmp[0];
                try {
                	System.out.println(tmp[1]);
                    port = Integer.parseInt(tmp[1]);
                } catch (NumberFormatException nfe) {
                    _logger.severe("Illegal port number in dCap host address");
                }
            }
        }

        if (ip == null || port == -1) {
            // we failed to find a valid door ip and port. Let's throw
            InvalidConfigurationException invc = new InvalidConfigurationException(
                    "dCap door host and port are not properly defined.");
            _logger.throwing("ControlConnection", "initialize", invc);
            throw invc;
        }

        _logger.fine("Connection to dCap door at " + ip + ":" + "port");
        _client = new Socket(ip, port);

        _clientOut = new PrintWriter(_client.getOutputStream());
        _clientIn = new BufferedReader(new InputStreamReader(_client
                .getInputStream()));
        _logger.finer("IO streams initialized for dCap door");

        doHelloConversation();

        callbacks = new Hashtable<Integer, ControlCommandCallback>();

        //
        start();
    }

    private void doHelloConversation() throws IOException {

        sendCommand("0 0 client hello 0 0 1 1");

        String reply = _clientIn.readLine();
        String doorReply[] = null;

        if (reply != null)
            doorReply = reply.split(" ");

        if (doorReply == null || doorReply.length < 4) {
            throw new IOException(
                    "Not a valid reply from dcap door; where reply = "
                            + doorReply);
        }

        if (doorReply[3].equals("welcome")) {
            _logger.fine("dCap door replied: " + doorReply[3]);
        } else if (doorReply[3].equals("failed")) {
            _logger.severe("dCap door rejected connection!");
            sendCommand("0 0 client byebye");
            throw new IOException("Error Code: "
                    + (doorReply.length > 5 ? doorReply[4] : "Unknown")
                    + " Error Message: "
                    + (doorReply.length > 6 ? doorReply[5] : " Unknown"));
        } else if (doorReply[3].equals("byebye")) {
            _logger.severe("dCap door closed connection.");
            throw new IOException("Failed to initialize dCap door");
        }

    }

    public void run() {

        String reply = null;

        while (_shouldBeRunning) {
            try {
                Thread.sleep(500);

                if (_clientIn.ready())
                    reply = _clientIn.readLine();
                else
                    continue;

            } catch (IOException e) {
                // TODO:recover connection if it is down
                e.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            if (reply == null) {
                _logger.warning("Received null in command loop ");
                // TODO: perhaps a pingpong to test the control connection.
                //
                break;
            }

            _logger.finer("Received command reply: \"" + reply + "\"");
            String replyParts[] = reply.split(" ");
            if (replyParts.length > 0) {
                int session = -1;
                try {
                    session = Integer.parseInt(replyParts[0]);
                } catch (NumberFormatException nfe) {
                    _logger
                            .warning("Failed to parse door reply to get session ID "
                                    + replyParts[0]);
                }

                ControlCommandCallback ccc = callbacks.get(session);
                if (ccc != null) {
                    ccc.handleDoorCommand(replyParts);
                    _logger.fine("Notified session " + session
                            + " for command callback");
                } else {
                    _logger.warning("No callback registered/found for session "
                            + session);
                }
            }
        }
        try {
            _clientIn.close();
            _clientOut.close();
            _client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        _stopped = true;
    }

    protected void start() {
        if (_clientThread == null) {
            _shouldBeRunning = true;
            _clientThread = new Thread(this);
            _clientThread.setName("Server");
            _clientThread.start();
        }
    }

    public void stop() {
        _shouldBeRunning = false;
        _logger.fine("Closting control connection");

        try {
            _clientThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isStopped() {
        return _stopped;
    }

    /**
     * Register a command callback against a give sessionID to multplex
     * in-comming control commands
     */
    public boolean registerCallback(int sessionID, ControlCommandCallback ccc) {
        if (!callbacks.contains(sessionID)) {
            callbacks.put(sessionID, ccc);
            _logger.fine("Registered command call back receiver for sessionID "
                    + sessionID);
            return true;
        }

        return false;
    }

    public boolean unregisterCallback(int sessionID) {
        if (callbacks.contains(sessionID)) {
            _logger.info("Removing callback receiver for sessionID "
                    + sessionID);
            callbacks.remove(sessionID);
            return true;
        }
        return false;
    }

    public void sendCommand(String command) throws IOException {
        _clientOut.println(command);
        _clientOut.flush();
        _logger.finer("Sent command: \"" + command + "\"");
    }

    public int getNextSessionID() {
        int temp = -1;
        synchronized (_nextSessionID) {
            temp = ++_nextSessionID;
        }
        return temp;
    }
}
