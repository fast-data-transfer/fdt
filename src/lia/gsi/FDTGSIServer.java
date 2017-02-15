/*
 * $Id: FDTGSIServer.java 621 2010-09-03 14:31:24Z ramiro $
 */
package lia.gsi;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.transport.ControlChannel;

import org.ietf.jgss.GSSCredential;


/**
 * This class will handle all the FDT GSI requests 
 * It overrides handleConversation from the base class.
 * 
 * @author ramiro
 */
public class FDTGSIServer extends GSIServer {

    private static final FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();
    private static final Logger logger = Logger.getLogger(FDTGSIServer.class.getName());

    public FDTGSIServer() throws Exception {
        super();
    }

    public FDTGSIServer(int port) throws Exception {
        this((String) null, (String) null, port);
    }
   
    public FDTGSIServer(GSSCredential cred, int port) throws Exception {
        super(cred, port);
    }

    public FDTGSIServer(String serverKey, String serverCert, int port) throws Exception {
        super(generateGSSCredential(serverKey, serverCert), port);
    }

    public void start() {
        super.start();
    }
    
    protected void handleConversation(GSIServer parent, Socket client, Subject peerSubject) {
        ControlChannel ct = null;
        
        try {
            ct = new ControlChannel(parent, client, peerSubject, fdtSessionManager);
            fdtSessionManager.addFDTClientSession(ct);
        } catch (Throwable t) {
            ct = null;
            throw new IllegalStateException("[ FDTGSIServer ] Cannot instantiate ControlChannel for client: " + client, t);
        }

        if (ct != null) {
            new Thread(ct, "ControlChannel thread for [ " + client.getInetAddress() + ":" + client.getPort() + " ]").start();
        }

        logger.log(Level.INFO, "[ FDTGSIServer ] ControlChannel for client: " + client + " started!");
    }
}
