package lia.util.net.copy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


public interface FileBlockConsumer {
    
    public boolean offer(final FileBlock fileBlock, long delay, TimeUnit unit) throws InterruptedException;
    public void put(FileBlock fileBlock) throws InterruptedException;
    
}
