package lia.util.net.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import lia.util.net.copy.FDT;
import apmon.ApMon;

public final class Utils {
    
    private static final ScheduledThreadPoolExecutor scheduledExecutor = getSchedExecService("FDT Monitoring ThPool", 5, Thread.MIN_PRIORITY);
    
    
    private static ApMon apmon = null;
    private static boolean apmonInitied = false;
    
    public static final int VALUE_2_STRING_NO_UNIT = 1;
    
    public static final int VALUE_2_STRING_UNIT = 2;
    
    public static final int VALUE_2_STRING_SHORT_UNIT = 3;
    
    private static final int AV_PROCS;
    
    public static final long KILO  = 1024;
    public static final long MEGA  = KILO*1024;
    public static final long GIGA  = MEGA*1024;
    public static final long TERA  = GIGA*1024;
    public static final long PETA  = TERA*1024;
    

    public static final int URL_CONNECTION_TIMEOUT = 20 * 1000;
    
    static {
        
        int avProcs = Runtime.getRuntime().availableProcessors();
        
        if (avProcs <= 0) {
            avProcs = 1;
        }
        
        AV_PROCS = avProcs;
        
    }
    
    public static final String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static final ScheduledThreadPoolExecutor getSchedExecService(final String name, final int corePoolSize, final int threadPriority) {
        return new ScheduledThreadPoolExecutor(corePoolSize, new ThreadFactory() {
            AtomicLong l = new AtomicLong(0);
            
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name + " - WorkerTask " + l.getAndIncrement());
                t.setPriority(threadPriority);
                t.setDaemon(true);
                return t;
            }
        },
                new RejectedExecutionHandler() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
                        return;
                    }
                    
                    final long SLEEP_TIME = Math.round(Math.random() * 1000D + 1);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (Throwable ignore) {
                    }
                    System.err.println("\n\n [ RejectedExecutionHandler ] for " + name + " WorkerTask slept for " + SLEEP_TIME);
                    executor.execute(r);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }
    
    public static final ExecutorService getStandardExecService(final String name, final int corePoolSize, final int maxPoolSize,
            BlockingQueue<Runnable> taskQueue, final int threadPriority) {
        ThreadPoolExecutor texecutor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize,
                2 * 60, TimeUnit.SECONDS,
                taskQueue,
                new ThreadFactory() {
            AtomicLong l = new AtomicLong(0);
            
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name + " - WorkerTask " + l.getAndIncrement());
                t.setPriority(threadPriority);
                t.setDaemon(true);
                return t;
            }
        });
        texecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
                        return;
                    }
                    
                    final long SLEEP_TIME = Math.round(Math.random() * 400D + 1);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (Throwable ignore) {
                    }
                    System.err.println("\n\n [ RejectedExecutionHandler ] [ Full Throttle ] for " + name + " WorkerTask slept for " + SLEEP_TIME);
                    executor.getQueue().put(r);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
        
        
        texecutor.prestartAllCoreThreads();
        
        return texecutor;
    }
    
    public static final ExecutorService getStandardExecService(final String name, final int corePoolSize, final int maxPoolSize,
            final int threadPriority) {
        return getStandardExecService(name, corePoolSize, maxPoolSize, new SynchronousQueue(), threadPriority);
    }
    
    public static final String format(final double number, final long factor, final String append) {
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
    
    public static final String valToString(final double value, final int options) {
        String text;
        long val = (long) (value * 100);
        String addedText = "";
        if ((options & VALUE_2_STRING_UNIT) > 0) {
            if (val > 102400) {
                val /= 1024;
                if ((options & VALUE_2_STRING_SHORT_UNIT) > 0)
                    addedText = "K";
                else
                    addedText = "Kilo";
            }
            if (val > 102400) {
                val /= 1024;
                if ((options & VALUE_2_STRING_SHORT_UNIT) > 0)
                    addedText = "M";
                else
                    addedText = "Mega";
            }
            if (val > 102400) {
                val /= 1024;
                if ((options & VALUE_2_STRING_SHORT_UNIT) > 0)
                    addedText = "G";
                else
                    addedText = "Giga";
            }
        }
        ;
        long rest = val % 100;
        text = (val / 100) + "." + (rest < 10 ? "0" : "") + rest + " " + addedText;
        return text;
    }
    
    public static final ScheduledThreadPoolExecutor getMonitoringExecService() {
        return scheduledExecutor;
    }
    
    public static final DirectByteBufferPool getDirectBufferPool() {
        return DirectByteBufferPool.getInstance();
    }
    
    public static final int availableProcessors() {
        return AV_PROCS;
    }
    
    public static final HeaderBufferPool getHeaderBufferPool() {
        return HeaderBufferPool.getInstance();
    }
    
    public static final void initApMonInstance(ApMon apmon) throws Exception {
        synchronized(Utils.class) {
            if(apmonInitied) {
                return;
            }
            
            Utils.apmon = apmon;
            
            Utils.class.notifyAll();
        }
    }
    
    public static final ApMon getApMon() {
        synchronized(Utils.class) {
            while(!apmonInitied) {
                try {
                    Utils.class.wait();
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            return apmon;
        }
    }
    
    
    public static final HashMap<String, Object> parseArguments(final String args[], final String[] singleArgs) {
        
        List<String> sArgs = Arrays.asList(singleArgs);
        
        HashMap<String, Object> rHM = new HashMap<String, Object>();
        if (args == null || args.length == 0)
            return rHM;
        
                
        String lParams = "";
        
        
        
        
        
        
        
        
        
        
        
        
                
        
        
        
        
        
        
        ArrayList<String> sshUsers = new ArrayList<String>();
        ArrayList<String> sshHosts = new ArrayList<String>();
        ArrayList<String> sshFiles = new ArrayList<String>();
        

        int i = 0;
        for (i = 0; i < args.length; i++) {

            if (args[i].startsWith("-")) {
                if (i == args.length - 1 || args[i + 1].startsWith("-") || sArgs.contains(args[i])) {
                    rHM.put(args[i], "");
                } else {
                    if(sshUsers.size() > 0 && ( args[i].equals("-c") || args[i].equals("-d"))) {
                        throw new IllegalArgumentException("Illegal syntax! You can use either either Client/Server (-c/-d) syntax, either SCP syntax");
                    }
                    
                    rHM.put(args[i], args[i + 1]);
                    i++;
                }
            } else if (args[i].indexOf(":") >= 0) {

                if(sshUsers.size() == 0 && ( rHM.get("-d") != null || rHM.get("-c") != null)) {
                    throw new IllegalArgumentException("Illegal syntax! You can use either Client/Server (-c/-d) syntax, either SCP syntax");
                }
                
                int idx = args[i].indexOf(":");
                String userHost = null;
                if(idx > 0) {
                    userHost = args[i].substring(0, idx);
                }
                
                String user = null;
                String host = null;
                String path = null;
                if(userHost != null) {
                    int idx1 = userHost.indexOf("@");
                    if(idx1 >= 0 ) { 
                        if(idx1 == 0) {
                            throw new IllegalArgumentException("Invalid scp syntax for " + args[i]);
                        }
                        
                        user = userHost.substring(0, idx1);
                        if(idx1 + 1 < userHost.length()) {
                            host = userHost.substring(idx1 + 1);
                        } else {
                            throw new IllegalArgumentException("Invalid scp syntax for " + args[i]);
                        }
                    } else {
                        host = userHost;
                    }
                }

                if(idx + 1 == args[i].length()) {
                    path = ".";
                } else {
                    path = args[i].substring(idx + 1);
                }
                
                
                
                
                sshUsers.add(user);
                sshHosts.add(host);
                sshFiles.add(path);
            } else {
                if(sshUsers.size() > 0) {
                    
                    rHM.put("destinationDir", args[i]);
                    rHM.put("-d", rHM.get("destinationDir"));
                    break;
                }
                
                lParams += args[i] + " ";
            }
        }
        
        int sshHostsNo = sshUsers.size();
        if(sshHostsNo > 0) {
            rHM.put("SCPSyntaxUsed", true);
            
            if(rHM.get("destinationDir") == null) {
                
                
                rHM.put("destinationUser", sshUsers.get(sshHostsNo - 1));
                rHM.put("destinationHost", sshHosts.get(sshHostsNo - 1));
                rHM.put("destinationDir", sshFiles.get(sshHostsNo - 1));
                rHM.put("-d", rHM.get("destinationDir"));
                rHM.put("-c", rHM.get("destinationHost"));
                
                
                sshUsers.remove(sshHostsNo - 1);
                sshHosts.remove(sshHostsNo - 1);
                sshFiles.remove(sshHostsNo - 1);
                
            }
            
            sshHostsNo = sshUsers.size();
            
            if(sshHostsNo > 0 ) {
                
                final String[] sUsers = sshUsers.toArray(new String[sshHostsNo]);
                final String[] sHosts = sshHosts.toArray(new String[sshHostsNo]);
                final String[] sFiles = sshFiles.toArray(new String[sshHostsNo]);
                
                rHM.put("sourceUsers", sUsers);
                rHM.put("sourceHosts", sHosts);
                rHM.put("sourceFiles", sFiles);
                
                rHM.put("Files", rHM.get("sourceFiles"));
                



            }
        }
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        rHM.put("LastParams", lParams);
        
        if(rHM.get("-c") != null) {
            if(rHM.get("-d") == null) {
                throw new IllegalArgumentException("No destination specified");
            } else {
                if(rHM.get("-fl") == null && lParams.length() == 0 && rHM.get("Files") == null) {
                    throw new IllegalArgumentException("No source specified");
                }
            }
        }
        

        return rHM;
    }
    
    
    
    public static final boolean updateFDT(final String currentVersion, final String updateURL) throws Exception {
        
        final URL urlDownJar = new URL(updateURL + (updateURL.endsWith("/")?"":"/")+"fdt.jar");
        final URLConnection urlConnection = urlDownJar.openConnection();
        final String finalPath = FDT.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        
        if(finalPath == null || finalPath.length() == 0) {
            throw new IOException("Cannot determine the path to current fdtJar");
        }
        
        final File currentJar = new File(finalPath);
        
        if(!currentJar.exists()) {
            
            throw new IOException("Current fdt.jar path seems to be [ " + finalPath + " ] but the JVM cannot access it!");
        }
        
        if(currentJar.isFile() && currentJar.canWrite()) {
            System.out.println("\nCurrent fdt.jar path is: " + finalPath);
        } else {
            throw new IOException("Current fdt.jar path seems to be [ " + finalPath + " ] but it does not have write access!");
        }
        
        
        File tmpUpdateFile = null;
        FileOutputStream fos = null;
        JarFile jf = null;
        InputStream connInputStream = null;
        try {
            
            tmpUpdateFile = File.createTempFile("fdt_update_tmp", ".jar");
            fos = new FileOutputStream(tmpUpdateFile);
            
            urlConnection.setUseCaches(false);
            
            urlConnection.setDefaultUseCaches(false);
            urlConnection.setConnectTimeout(URL_CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(URL_CONNECTION_TIMEOUT);
            
            System.out.print("Checking remote fdt.jar [ " + urlDownJar.toString() + " ] ... ");
            urlConnection.connect();
            connInputStream = urlConnection.getInputStream();
            System.out.println("OK");
            byte[] buff = new byte[8192];
            
            int count = 0;
            while ((count = connInputStream.read(buff)) > 0) {
                fos.write(buff, 0, count);
                fos.flush();
            }
            
            fos.flush();
            fos.close();
            
            
            jf = new JarFile(tmpUpdateFile);
            final Manifest mf = jf.getManifest();
            final Attributes attr = mf.getMainAttributes();
            final String remoteVersion = attr.getValue("Implementation-Version");
            
            jf.close();
            
            if(remoteVersion == null || remoteVersion.trim().length() == 0){
                throw new Exception("Cannot read the version from the downloaded jar...The current jar will not be updated");
            }
            
            if(currentVersion.equals(remoteVersion.trim())) {
                
                return false;
            }
            
            System.out.println("Remote version: " + remoteVersion + " Local version: " + currentVersion + " ... will update");
            
            copyFile2File(tmpUpdateFile, currentJar);
            
            return true;
        } finally {
            
            if(connInputStream != null) {
                try {
                    connInputStream.close();
                }catch(Throwable ignore){}
            }
            
            if(fos != null) {
                try {
                    fos.close();
                }catch(Throwable ignore){}
            }
            
            if(tmpUpdateFile != null) {
                try {
                    tmpUpdateFile.delete();
                }catch(Throwable ignore){}
            }
            
            if(jf != null) {
                try {
                    jf.close();
                }catch(Throwable ignore){}
            }
        }
        
    }
    
    public static final String md5ToString(byte[] md5sum) {
        StringBuilder sb = new StringBuilder();
        
        for(int i=0; i<md5sum.length; i++) {
            sb.append(Integer.toString( ( md5sum[i] & 0xff ) + 0x100, 16).substring( 1 ));
        }
        
        return sb.toString();
    }

    
    public static final boolean checkForUpdate(final String currentVersion, final boolean shouldUpdate, final long updatePeriod, final String updateURL) {
        String updateFile = System.getProperty("user.home") + File.separatorChar + ".fdt" + File.separatorChar + "fdt_update";
        File f = new File(updateFile);
        long lTime = -1;
        if(!f.exists()) {
            new File(f.getParent()).mkdirs();
            try {
                f.createNewFile();
                
                
                
                
            } catch (IOException ex) {
                System.out.println("Could not create update checking file. Information about new updates will not be available.");
            }
        } else {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
                String line = br.readLine();
                fr.close();
                if ( line!=null )
                    lTime = Long.parseLong(line);
            } catch(Throwable t) {
                System.out.println("Could not read update checking file. Information about new updates will not be available.");
                
            } finally {
                if(br != null) {
                    try {
                        br.close();
                    }catch(Throwable ignore){}
                }
                
                if(fr != null) {
                    try {
                        fr.close();
                    }catch(Throwable ignore){}
                }
            }
        }
        long currentTime = System.currentTimeMillis();
        if ( lTime==-1 || currentTime-lTime > updatePeriod || shouldUpdate ) {
            System.out.println("Checking for updates... ");
            
            BufferedWriter bw = null;
            BufferedReader brDown = null;
            InputStream isDown = null;
            InputStreamReader isr = null;
            
            try {
                bw = new BufferedWriter(new FileWriter(f));
                if ( bw != null )
                    bw.write(""+currentTime);
                bw.close();
                URL urlDown;
                urlDown = new URL(updateURL+"version");
                URLConnection connection = urlDown.openConnection();
                isDown = connection.getInputStream();
                isr = new InputStreamReader(isDown);
                brDown = new BufferedReader(isr);
                String new_version = brDown.readLine();
                brDown.close();
                
                if(new_version == null) {
                    System.out.println("Unable to check for remote version ... got null response from the web server");
                    return false;
                }
                
                if ( !new_version.equals(currentVersion) ) {
                    System.out.println("There is a new version ("+new_version+") available at "+updateURL);
                    System.out.println("Would you like to update? [Y/n] ");
                    char car = (char)System.in.read();
                    if ( car=='Y' || car=='y' || car=='\n' || car=='\r' ) {
                        
                        
                        URL urlDownJar = new URL(updateURL+"fdt.jar");
                        URLConnection connectionJar = urlDownJar.openConnection();
                        isDown = connectionJar.getInputStream();
                        File tempFile = File.createTempFile("update", "new_version.jar");
                        FileOutputStream fos = new FileOutputStream( tempFile);
                        byte[] buf = new byte[10240];
                        int read=-1;
                        while ( (read=isDown.read(buf))!=-1 ) {
                            fos.write(buf,0, read);
                        };
                        fos.close();
                        
                        
                        String finalPath = FDT.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                        File thisFile = new File(finalPath);
                        
                        
                        
                        copyFile2File( tempFile, thisFile);
                        System.out.println("Application updated successfully. Exiting.");
                        return true;
                        
                        
                        
                        
                        
                    }
                } else {
                    System.out.println("You have the lastest version.");
                }
            } catch( Exception ex) {
                System.out.println("Error. Please check manually the site for new updates: "+updateURL);
                ex.printStackTrace();
            } finally {
                if(bw != null) {
                    try {
                        bw.close();
                    }catch(Throwable ignore){}
                }
                if(brDown != null) {
                    try {
                        brDown.close();
                    }catch(Throwable ignore){}
                }
                if(isDown != null) {
                    try {
                        isDown.close();
                    }catch(Throwable ignore){}
                }
                if(isr != null) {
                    try {
                        isr.close();
                    }catch(Throwable ignore){}
                }
            }
        }
        
        return false;
    }
    
    public static final void copyFile2File(File s, File d) throws Exception {
        
        
        
        
        
        final FileChannel srcChannel = new RandomAccessFile(s, "rw").getChannel();
        
        
        final FileChannel dstChannel = new FileOutputStream(d).getChannel();

        srcChannel.lock();
        dstChannel.lock();
        
        
        final long tr = dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        
        long ss = srcChannel.size();
        long ds = dstChannel.size();
        
        if ( ss != ds || ss != tr ) {
            
            throw new Exception("Cannot copy SourceFileSize [ " + ss +" ] DestinationFileSize [ " +ds+" ] Transferred [ " +tr +" ] ");
        }
        
        
        srcChannel.close();
        dstChannel.close();
        
        
        
    }
    
    
    public static final void getRecursiveFiles(String fileName, List<String> allFiles) throws Exception {
        
        if(allFiles == null) throw new NullPointerException("File list is null");
        
        File file = new File(fileName);
        if ( file.exists() && file.canRead() ) {
            if ( file.isFile() ) {
                allFiles.add(fileName);
            } else if ( file.isDirectory() ) {
                String[] listContents = file.list();
                if ( listContents!=null && listContents.length > 0)
                    for( String subFile: listContents )
                        getRecursiveFiles( fileName + File.separator + subFile, allFiles);
            } else {
                allFiles.add(fileName);
            }
        }
    }
    
}
