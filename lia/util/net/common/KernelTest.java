

package lia.util.net.common;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class KernelTest extends Thread{
    
    public static final int BUFF_SIZE = 512 * 1024;
    
    static ByteBuffer _theBuffer;
    static int NUMBER_OF_THREADS = 5;
    
    
    int id;
    
    
    public KernelTest(int id) {
        this.id = id;
        setName("KernelTest worker id: " + id);
        setDaemon(true);
    }
    
    public void run() {
        FileChannel channel = null;
        
        try {
            channel = new FileInputStream("/dev/zero").getChannel();
        }catch(Throwable t) {
            t.printStackTrace();
        }
        
	if(channel == null) {
		return;
	}
	
	System.out.println("KernelTest thread " + id + " started");

        for(;;) {
            try {
                _theBuffer.clear();
                channel.read(_theBuffer);
                
            } catch(Throwable t){
              t.printStackTrace();  
            }
        }
    }
    
    public static final void main(String[] args) throws Exception {
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-d")) {
                _theBuffer = ByteBuffer.allocateDirect(BUFF_SIZE);
            } else if(args[i].equals("-n")) {
                if(i + 1 < args.length) {
                    int tNo  = NUMBER_OF_THREADS;
                    try {
                        tNo = Integer.parseInt(args[i+1]);
                        if(tNo < 0) {
                            tNo = NUMBER_OF_THREADS;
                        }
                    } catch(Throwable t) {
                        System.out.println("Cannot parse -n arg" + t.getCause());
                        tNo = NUMBER_OF_THREADS;
                    }
                    
                    NUMBER_OF_THREADS = tNo;
                }
            }
            
        }
        
        if(_theBuffer == null) {
            _theBuffer = ByteBuffer.allocate(BUFF_SIZE);
        }

        System.out.println("Using: " + NUMBER_OF_THREADS + " threads," +
                " BUFF_SIZE: " + BUFF_SIZE + " Bytes," +
                " " + ((_theBuffer.isDirect())?"Direct":"Heap") + " buffer" );
        
        for(int i = 0; i< NUMBER_OF_THREADS; i++) {
            new KernelTest(i).start();
        }
        
        Object waiter = new Object();
        synchronized (waiter) {
            waiter.wait(10 * 1000);

        }
        
    }
}
