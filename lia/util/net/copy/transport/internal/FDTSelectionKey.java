
package lia.util.net.copy.transport.internal;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import lia.util.net.common.Utils;


public class FDTSelectionKey {

    private static final SelectionManager selectionManager = SelectionManager.getInstance();

    public static final FDTSelectionKey END_PROCESSING_NOTIF_KEY = new FDTSelectionKey(UUID.randomUUID(), null, 0, null);
    
    protected Selector selector;
    protected SocketChannel channel;
    
    SelectionKey selectionKey;
    SelectionHandler handler;
    
    final int interests;
    
    AtomicBoolean canceled;
    AtomicBoolean registered;
    AtomicBoolean renewed;
    UUID fdtSessionID;

    public int opCount;
    private Object attachment;
    
    int MSS = -1;
    
    public FDTSelectionKey(UUID fdtSessionID, SocketChannel channel, int interests, SelectionHandler handler, Object attachement) {
        this.channel = channel;
        this.interests = interests;
        this.handler = handler;
        
        renewed = new AtomicBoolean(false);
        canceled = new AtomicBoolean(false);
        registered = new AtomicBoolean(false);
        
        this.fdtSessionID = fdtSessionID;
        this.attachment = attachement;
    }
    
    public FDTSelectionKey(UUID fdtSessionID, SocketChannel channel, int interests, SelectionHandler handler) {
        this(fdtSessionID, channel, interests, handler, null);
    }
    
    public boolean registerInterest() {
        
        if(registered.compareAndSet(false, true)) {
            selectionManager.initialKeyRegister(this);
            return true;
        }
        
        return false;
    }
    
    
    public boolean renewInterest() {
        
        if(renewed.compareAndSet(false, true)) {
            opCount = 0;
            selectionManager.renewInterest(this);
            return true;
        }
        
        return false;
    }
    
    
    
    public boolean cancel() {
        if(canceled.compareAndSet(false, true)) {
            if(handler != null) {
                try {
                    handler.canceled(this);
                }catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            
            try {
                
                if(selectionKey != null) {
                    try {
                        selectionKey.cancel();
                    }catch(Throwable t){
                    }
                }
                
                
            }catch(Throwable t){
                t.printStackTrace();
            }
            return true;
        }
        return false;
    }
    
    public boolean isValid() {
        return !canceled.get();
    }
    
    public SocketChannel channel() {
        return channel;
    }
    
    
    public final Object attach(Object o) {
       Object ret = this.attachment;
       this.attachment = o;
       return ret;
    }
    
    public UUID fdtSessionID() {
        return fdtSessionID;
    }
    
    
    public final Object attachment() {
        return attachment;
    }
    
    public int getMSS() {
        return MSS;
    }
    
    public void setMSS(int MSS) {
        this.MSS = MSS;
    }
    
    public Selector selector() {
        return selector;
    }
    
    public String toString() {
        return Utils.toStringSelectionKey(this);
    }
}
