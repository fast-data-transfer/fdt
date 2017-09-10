/*
 * $Id$
 */
package lia.util.net.copy.transport;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FileBlockConsumer;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import lia.util.net.copy.transport.internal.SelectionManager;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides the TransportProvider for a FDTSession which expects data from the wire.
 * It is responsible to stop all the sockets and notify it's session if something goes wrong
 * It also registers every worker in the SelectionManager with OP_READ interest
 *
 * @author ramiro
 */
public class TCPSessionReader extends TCPTransportProvider {

    private static final Logger logger = Logger.getLogger(TCPSessionReader.class.getName());
    private static final SelectionManager selectionManager = SelectionManager.getInstance();
    private static final Config config = Config.getInstance();
    private FileBlockConsumer fileBlockConsumer;

    public TCPSessionReader(FDTSession fdtSession, FileBlockConsumer fileBlockConsumer) throws Exception {
        super(fdtSession);
        this.fileBlockConsumer = fileBlockConsumer;
    }

    public TCPSessionReader(FDTSession fdtSession,
                            FileBlockConsumer fileBlockConsumer,
                            InetAddress endPointAddress, int port,
                            int numberOfStreams
    ) throws Exception {

        super(fdtSession, endPointAddress, port, numberOfStreams);
        this.fileBlockConsumer = fileBlockConsumer;
    }


    public void addWorkerStream(SocketChannel sc, boolean sentCookie) throws Exception {
        synchronized (this.closeLock) {
            super.addWorkerStream(sc, sentCookie);
            FDTSelectionKey fsk = null;

            if (config.isBlocking()) {
                fsk = new FDTSelectionKey(fdtSession.sessionID(), sc, SelectionKey.OP_READ, this, null);
                fsk.attach(new FDTReaderKeyAttachement(fsk, fdtSession.useFixedBlockSize()));
                selectionQueue.add(fsk);
                SocketTask socketTask = new SocketReaderTask(selectionQueue, fileBlockConsumer, this);

                if (addSocketTask(socketTask)) {
                    executor.submit(socketTask);
                } else {
                    close("Unable to add a new SocketTask. OOM?", null);
                }

            } else {
                fsk = selectionManager.register(fdtSession.sessionID(), sc, SelectionKey.OP_READ, this);
                fsk.attach(new FDTReaderKeyAttachement(fsk, fdtSession.useFixedBlockSize()));
                if (!fsk.registerInterest()) {
                    logger.log(Level.WARNING, " \n\n Smth went terrible wrong ?? \n\n fsk.registerInterest() returned false \n\n");
                }
            }

            channels.put(sc, fsk);
        }
    }


    //
    //TODO - can we recover if downCause != null
    //     - implement a timeout retry ? ... for the moment it just finishes the entire session
    //     - this behavior should be changed when dynamic creation of workers will be added

    /**
     * @param fdtSelectionKey
     * @param downCause
     */
    public void workerDown(FDTSelectionKey fdtSelectionKey, Throwable downCause) {
        //smth gone wrong ... or maybe the session finished already
        //I do not know if it should take other action ... for the moment the session will go down

//        if(downCause != null) {
//            logger.log(Level.WARNING, " [ TCPSessionReader ] for fdtSession [ " + fdtSession + " ] got an error on a worker", downCause);
//        }
//        
//        close(downCause);
    }

    public void startTransport(boolean sendCookie) throws Exception {
        super.startTransport(sendCookie);
        synchronized (this.closeLock) {
            if (!config.isBlocking()) {
                for (int i = 0; i <= Utils.availableProcessors() * 2; i++) {
                    SocketTask socketTask = new SocketReaderTask(selectionQueue, fileBlockConsumer, this);
                    if (addSocketTask(socketTask)) {
                        executor.submit(socketTask);
                    } else {
                        close("Unable to add a new SocketTask. OOM?", null);
                    }
                }
            }
        }
    }

}
