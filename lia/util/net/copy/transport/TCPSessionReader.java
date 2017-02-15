package lia.util.net.copy.transport;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FileBlockConsumer;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import lia.util.net.copy.transport.internal.SelectionManager;


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

    
    
    public void addWorkerStream(SocketChannel sc) throws Exception {
        synchronized(this.channels) {
            super.addWorkerStream(sc);
            FDTSelectionKey fsk = null;

            if(config.isBlocking()) {
                fsk = new FDTSelectionKey(fdtSession.sessionID(), sc, SelectionKey.OP_READ, this);
                fsk.attach(new FDTReaderKeyAttachement(fsk, fdtSession.useFixedBlockSize()));
                selectionQueue.add(fsk);
                initExecutor();
                SocketTask socketTask = new SocketReaderTask(selectionQueue, fileBlockConsumer, this);
                
                if(addSocketTask(socketTask)) {
                    executor.submit(socketTask);
                } else {
                    close("Unable to add a new SocketTask. OOM?", null);
                }
                
            } else {
                fsk = selectionManager.register(fdtSession.sessionID(), sc, SelectionKey.OP_READ, this);
                fsk.attach(new FDTReaderKeyAttachement(fsk, fdtSession.useFixedBlockSize()));
                if(!fsk.registerInterest()) {
                    logger.log(Level.WARNING, " \n\n Smth went terrible wrong ?? \n\n fsk.registerInterest() returned false \n\n");
                }
            }
            
            channels.put(sc, fsk);
        }
    }

    
    
    
    
    
    public void workerDown(FDTSelectionKey fdtSelectionKey, Throwable downCause) {
        
        
        





    }

    public void startTransport() throws Exception {
        super.startTransport();
        synchronized(channels) {
            if(!config.isBlocking()) {
                for(int i =0; i <= Utils.availableProcessors()*2; i++) {
                    SocketTask socketTask = new SocketReaderTask(selectionQueue, fileBlockConsumer, this);
                    if(addSocketTask(socketTask)) {
                        executor.submit(socketTask);
                    } else {
                        close("Unable to add a new SocketTask. OOM?", null);
                    }
                }
            }
        }
    }

}
