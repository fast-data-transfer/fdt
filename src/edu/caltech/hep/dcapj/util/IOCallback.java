package edu.caltech.hep.dcapj.util;

import java.util.Hashtable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.net.Socket;
import java.util.logging.Logger;

public class IOCallback extends ServerNIO {

    protected Hashtable<Integer, DataConnectionCallback> callbacks;

    private static final Logger logger = Logger.getLogger(IOCallback.class
            .getName());

    public IOCallback() throws IOException {
        callbacks = new Hashtable<Integer, DataConnectionCallback>();
        start();
        logger.info("Data connecation callback is now active");
    }

    public void handleConnection(SocketChannel client) {
        DataInputStream poolIn = null;
        DataOutputStream poolOut = null;
        if (client == null) {
            logger.severe("IO callback channel failed for unknown file");
            return;
        }

        try {
            logger.fine("Handling connection from "
                    + client.socket().getInetAddress().getHostAddress() + ":"
                    + client.socket().getPort());
            poolIn = new DataInputStream(client.socket().getInputStream());
            poolOut = new DataOutputStream(client.socket().getOutputStream());

            ByteBuffer buffer = ByteBuffer.allocate(4);
            client.read(buffer);

            buffer.flip();
            int sessionID = buffer.getInt();

            logger.info("Received callback for session id " + sessionID);

            // handover the connection to appropriate dCache-file block
            DataConnectionCallback connection = callbacks.get(sessionID);

            if (connection != null) {
                logger.fine("Information callback reciever for sessionID "
                        + sessionID);
                connection.handleStreams(poolIn, poolOut, client.socket()
                        .getInetAddress().getHostAddress(), client);
            } else {
                logger.warning("No callback receiver registred for sessionID "
                        + sessionID);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
        }
    }

    /**
     * Register a connection callback against a given sessionID to multplex
     * in-comming pool connection
     */
    public boolean registerCallback(int sessionID,
            DataConnectionCallback connection) {
        if (!callbacks.contains(sessionID)) {
            callbacks.put(sessionID, connection);
            logger.fine("Registered data call back receiver for sessionID "
                    + sessionID);
            return true;
        }

        return false;
    }

    public boolean unregisterCallback(int sessionID) {
        if (callbacks.contains(sessionID)) {
            logger.info("Removing callback receiver for sessionID "
                            + sessionID);
            callbacks.remove(sessionID);
            return true;
        }
        return false;
    }
}
