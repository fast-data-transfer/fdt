
package lia.util.net.common;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class DDCopy {
    
    private static final long KILO   =   1024;
    private static final long MEGA   =   KILO * 1024;
    private static final long GIGA   =   MEGA * 1024;
    private static final long TERA   =   GIGA * 1024;
    private static final long PETA   =   TERA * 1024;
    
    
    private static final String USAGE_MESSAGE =
            "\nUsage: java -cp fdt.jar " + DDCopy.class.getName() + " [ OPTIONS ] ARGS\n"+
            "\nARGS: if=<sourceFile> of=<destinationFile>\n" +
            "\n\nWhere OPTIONS can be:\n" +
            "\n   bs=<BufferSize>[K|M]\t size of the buffer used for read/write." +
            "\n   \t\t\t [K(ilo) | M(ega)] may be used as suffixes. Default 4K" +
            "\n   bn=<NoOfBuffers>\t Number of buffers used to readv()/writev() at once." +
            "\n   \t\t\t If this parameter is 1, or is missing the program will " +
            "\n   \t\t\t read()/write() a single buffer at a time, otherwise " +
            "\n   \t\t\t the readv()/writev() will be used. Default is 1" +
            "\n   count=<count>\t Number of \"blocks\" to write." +
            "\n   \t\t\t A \"block\" is represents how much data is read/write" +
            "\n   \t\t\t The size of a \"block\" is: <BufferSize>*<BuffersNumber>" +
            "\n   \t\t\t If <count> <= 0 the copy stops when EOF is reached" +
            "\n   \t\t\t reading the <SourceFile>. The default is 0" +
            "\n   statsdelay=<seconds>\t Number of seconds between reports." +
            "\n   \t\t\t Default is 2 seconds. If <seconds> <= 0 no reports " +
            "\n   \t\t\t will be printed" +
            "\n   flags=<flag>\t\t The <flag> field can have of the following values: " +
            "\n   \t\t\t    SYNC   For every write both data and metadata are" +
            "\n   \t\t\t           written synchronously" +
            "\n   \t\t\t    DSYCN  Same as SYNC, but only the data is written" +
            "\n   \t\t\t           synchronously." +
            "\n   \t\t\t    NOSYNC The sync() is left to be done by the" +
            "\n   \t\t\t           underlying OS" +
            "\n   \t\t\t The default value is DSYNC" +
            "\n   rformat=<rformat>\t Report format. Possible values are:" +
            "\n   \t\t\t    K - KiloBytes" +
            "\n   \t\t\t    M - MegaBytes" +
            "\n   \t\t\t    G - GigaBytes" +
            "\n   \t\t\t    T - TeraBytes" +
            "\n   \t\t\t    P - PetaBytes" +
            "\n   \t\t\t The default value is self adjusted. If the factor " +
            "\n   \t\t\t is too big only 0s will be displayed" +
            "\n";
    
    
    
    private static final class ReportingThread extends Thread {
        
        long lastTime;
        long lastCount;
        long now;
        long cCount;
        
        
        public ReportingThread() {
            setDaemon(true);
            setName("DDCopy reporting thread");
        }
        
        public void run() {
            
            
            lastCount = bytesNo.get();
            lastTime = System.currentTimeMillis();
            
            for(;;) {
                
                try {
                    Thread.sleep(delay);
                } catch(Throwable t1) {}
                
                if(!hasToRun.get()) return;
                
                now = System.currentTimeMillis();
                cCount = bytesNo.get();
                
                double speed = (cCount - lastCount)/((now - lastTime)/1000D);
                double avgSpeed = cCount/((now - START_TIME)/1000D);
                
                lastTime = now;
                lastCount = cCount;
                
                System.out.println("[" + new Date().toString() + "] Current Speed = " + format(speed, reportingFactor, "B/s") +
                        " Avg Speed: " + format(avgSpeed, reportingFactor, "B/s") +
                        " Total Transfer: " + format(cCount, reportingFactor, "B")
                        );
            }
        }
    }
    
    
    private static final class ShutdownHook extends Thread {
        public void run() {
            setName("Shutdown Hook Thread");
            
            if(verbose) {
                System.out.println("\n\n Entering shutdown hook \n\n");
            }
            
            hasToRun.set(false);
            if(reportingThread != null) {
                reportingThread.interrupt();
            }
            
            final long totalTime = System.currentTimeMillis() - START_TIME;
            final long totalBytes = bytesNo.get();
            final double avgSpeed = totalBytes / (totalTime/1000D);
            
            System.out.println("\n" +
                    "\n Total Transfer: " + format(totalBytes, reportingFactor, "Bytes") + " ( " + totalBytes + " bytes )"+
                    "\n Time: " + totalTime/1000 + " seconds" +
                    "\n Avg Speed: " + format(avgSpeed, reportingFactor, "B/s") +
                    "\n");
            
            System.out.flush();
            System.err.flush();
        }
    }
    
    
    private static String sourceName;
    
    private static String destinationName;
    
    
    private static AtomicLong bytesNo = new AtomicLong(0);
    
    
    private static int BUFF_NO = 1;
    
    
    private static int BUFF_SIZE = 4 * (int)KILO;
    
    
    private static int COUNT = 0;
    
    
    private static boolean verbose = false;
    
    
    private static long delay = 2 * 1000;
    
    
    private static long reportingFactor = 0;
    
    
    private static Thread reportingThread;
    
    
    private static AtomicBoolean hasToRun = new AtomicBoolean(true);
    
    
    private static String wrFlags = "rw";
    
    
    private static long START_TIME;
    
    
    private static final String format(final double number, final long factor, final String append) {
        String appendUM;
        double fNo = number;
        
        if(factor == 0) {
            if(number > PETA) {
                fNo /= PETA;
                appendUM = "P" + append;
            } else if(number > TERA) {
                fNo /= TERA;
                appendUM = "T" + append;
            } else if(number > GIGA) {
                fNo /= GIGA;
                appendUM = "G" + append;
            } else if(number > MEGA) {
                fNo /= MEGA;
                appendUM = "M" + append;
            } else if(number > KILO) {
                fNo /= KILO;
                appendUM = "K" + append;
            } else {
                appendUM = append;
            }
        } else {
            if (factor == PETA) {
                fNo /= PETA;
                appendUM = "P" + append;
            } else if (factor == TERA) {
                fNo /= TERA;
                appendUM = "T" + append;
            } else if (factor == GIGA) {
                fNo /= GIGA;
                appendUM = "G" + append;
            } else if (factor == MEGA) {
                fNo /= MEGA;
                appendUM = "M" + append;
            } else if (factor == KILO) {
                fNo /= KILO;
                appendUM = "K" + append;
            } else {
                appendUM = append;
            }
        }
        
        return DecimalFormat.getNumberInstance().format(fNo) + " " + appendUM;
    }
    
    
    
    
    private static final void printHelp() {
        System.out.println(USAGE_MESSAGE);
    }
    
    public static void main(String[] args) throws Exception {
        try {
            
            for(int i=0; i<args.length; i++) {
                if(args[i].startsWith("-h")) {
                    printHelp();
                    System.exit(0);
                }
            }
            
            
            for(int i=0; i<args.length; i++) {
                if(args[i].startsWith("-v")) {
                    verbose = true;
                    break;
                }
            }
            
            
            for(int i=0; i<args.length; i++) {
                if(args[i].startsWith("if=")) {
                    sourceName = args[i].substring("if=".length());
                } else if(args[i].startsWith("of=")) {
                    destinationName = args[i].substring("of=".length());
                } else if (args[i].startsWith("bs=")) {
                    String bSParam = args[i].substring("bs=".length());
                    int factor = 1;
                    try {
                        if(bSParam.endsWith("k") || bSParam.endsWith("K")) {
                            factor = (int)KILO;
                            bSParam = bSParam.substring(0, bSParam.length() - 1);
                        } else if(bSParam.endsWith("m") || bSParam.endsWith("M")) {
                            factor = (int)MEGA;
                            bSParam = bSParam.substring(0, bSParam.length() - 1);
                        }
                        
                        BUFF_SIZE = Integer.parseInt(bSParam) * factor;
                        
                    } catch(Throwable t) {
                        if(verbose) {
                            System.err.println("Cannot parse bsParam " + args[i] + " Cause: ");
                            t.printStackTrace();
                        } else {
                            System.err.println("Cannot parse bs param: " + args[i] + " . Try to run DDCopy with -v for further details");
                        }
                        
                        printHelp();
                        System.err.flush();
                        System.out.flush();
                        
                        System.exit(1);
                    }
                } else if (args[i].startsWith("bn=")) {
                    try {
                        BUFF_NO = Integer.parseInt(args[i].substring("bn=".length()));
                    } catch(Throwable t) {
                        BUFF_NO = 1;
                        if(verbose) {
                            System.err.println("Cannot parse bn param " + args[i] + " Cause: ");
                            t.printStackTrace();
                        } else {
                            System.err.println("Cannot parse bn param " + args[i] + ". Will use the default value: " + BUFF_NO);
                        }
                    }
                } else if(args[i].startsWith("count=")) {
                    try {
                        COUNT = Integer.parseInt(args[i].substring("count=".length()));
                    } catch(Throwable t) {
                        COUNT = -1;
                        if(verbose) {
                            System.err.println("Cannot parse count param " + args[i] + " Cause: ");
                            t.printStackTrace();
                        } else {
                            System.err.println("Cannot parse count param " + args[i] + ". Will use the default value: " + COUNT);
                        }
                    }
                } else if(args[i].startsWith("statsdelay=")) {
                    try {
                        delay = Long.parseLong(args[i].substring("count=".length())) * 1000L;
                    } catch(Throwable t) {
                        delay = 2 * 1000;
                        if(verbose) {
                            System.err.println("Cannot parse statsdelay param " + args[i] + " Cause: ");
                            t.printStackTrace();
                        } else {
                            System.err.println("Cannot parse statsdelay param " + args[i] + ". Will use the default value: " + delay/1000 + " seconds");
                        }
                    }
                } else if(args[i].startsWith("flags=")) {
                    final String wFlag = args[i].substring("flags=".length());
                    if(wFlag.equalsIgnoreCase("NOSYNC")) {
                        wrFlags = "rw";
                    } else if(wFlag.equalsIgnoreCase("SYNC")) {
                        wrFlags = "rws";
                    } else if(wFlag.equalsIgnoreCase("DSYNC")) {
                        wrFlags = "rwd";
                    }
                } else if(args[i].startsWith("rformat=")) {
                    final String rFlag = args[i].substring("rformat=".length());
                    if(rFlag.equalsIgnoreCase("K")) {
                        reportingFactor = KILO;
                    } else if(rFlag.equalsIgnoreCase("M")) {
                        reportingFactor = MEGA;
                    } else if(rFlag.equalsIgnoreCase("G")) {
                        reportingFactor = GIGA;
                    } else if(rFlag.equalsIgnoreCase("T")) {
                        reportingFactor = TERA;
                    } else if(rFlag.equalsIgnoreCase("P")) {
                        reportingFactor = PETA;
                    }
                }
            }
            
            if(sourceName == null || sourceName.trim().length() == 0) {
                System.out.println("\n No source specified ( if=<SourceFile> parameter ). Use -h for help.\n");
                System.exit(1);
            }
            
            if(destinationName == null || destinationName.trim().length() == 0) {
                System.out.println("\n No destination specified ( 'of=<DestinationFile>' parameter ). Use -h for help.\n");
                System.exit(1);
            }
            
            
            if(verbose) {
                StringBuilder sb = new StringBuilder();
                sb.append("Source: ").append(sourceName);
                sb.append(" Destination: ").append(destinationName);
                sb.append("");
            }
            
            final FileChannel sourceChannel = new RandomAccessFile(sourceName, "r").getChannel();
            final FileChannel destinationChannel = new RandomAccessFile(destinationName, wrFlags).getChannel();
            
            
            ByteBuffer[] bbuff = new ByteBuffer[BUFF_NO];
            
            for(int i=0; i<BUFF_NO; i++) {
                try {
                    bbuff[i] = ByteBuffer.allocateDirect(BUFF_SIZE);
                } catch(OutOfMemoryError oomError) {
                    System.err.println("ByteBuffer reached max limit. The copy may be slow. You may consider to increase to -XX:MaxDirectMemorySize=256m, or decrease the buffer number (bn) parameter");
                    System.err.flush();
                    System.exit(1);
                }
            }
            
            
            if(delay > 0) {
                reportingThread = new ReportingThread();
                reportingThread.start();
            }
            
            
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
            
            long count = 0;
            START_TIME = System.currentTimeMillis();
            
            for(int j=0; (COUNT > 0)?j<COUNT:true; j++) {
                count = sourceChannel.read(bbuff);
                
                if(count == -1) {
                    
                    break;
                }
                
                for(int i=0; i<BUFF_NO; i++) {
                    bbuff[i].flip();
                }
                
                if(BUFF_NO == 1) {
                    count = destinationChannel.write(bbuff[0]);
                } else {
                    count = destinationChannel.write(bbuff);
                }
                
                if(verbose) {
                    System.out.println("Current transfer count =  " + count + " Total: " + bytesNo.get());
                }
                
                if(count < 0) {
                    break;
                }
                
                bytesNo.addAndGet(count);
                
                for(int i=0; i<BUFF_NO; i++) {
                    bbuff[i].clear();
                }
                
            }
        } catch(Throwable t) {
            System.err.println("Got exception: ");
            t.printStackTrace();
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }
    
}
