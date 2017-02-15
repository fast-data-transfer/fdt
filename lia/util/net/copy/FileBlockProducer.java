package lia.util.net.copy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


public interface FileBlockProducer {

    public FileBlock take() throws InterruptedException;
    public FileBlock poll();
    public FileBlock poll(long delay, TimeUnit unit) throws InterruptedException;
    public void transportWorkerDown() throws Exception;
    
}
