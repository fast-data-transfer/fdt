/*
 * $Id$
 */
package lia.util.net.jiperf;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * 
 * This will be kept for history :). 
 * The entire package lia.util.net.jiperf is the very first version of FDT. It
 * started as an Iperf-like test for Java.
 * 
 * @author ramiro
 */
public class JIperf {

	/** Logger used by this class */
	private static final transient Logger logger = Logger.getLogger(JIperf.class.getName());

	/**
	 * The executor used to perform I/O tasks
	 */
	private static final ExecutorService executor;

	static {
		ThreadPoolExecutor texecutor = new ThreadPoolExecutor(5, 20, 2 * 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
			AtomicLong l = new AtomicLong(0);

			public Thread newThread(Runnable r) {
				return new Thread(r, " JIperf Worker Task " + l.getAndIncrement());
			}
		});
		texecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					// slow down a little bit
					final long SLEEP_TIME = Math.round(Math.random() * 1000D + 1);
					try {
						Thread.sleep(SLEEP_TIME);
					} catch (Throwable ignore) {
					}
					System.err.println("\n\n [ RejectedExecutionHandler ] slept for " + SLEEP_TIME);
					// resubmit the task!
					executor.execute(r);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});

		// it will be added in 1.6
		// texecutor.allowCoreThreadTimeOut(true);
		texecutor.prestartAllCoreThreads();
		executor = texecutor;
	}

	public static final ExecutorService getExecutor() {
		return executor;
	}

	public static final void shutdownExecutor() {
        executor.shutdown();
        try {
            if(!executor.awaitTermination(10L,TimeUnit.SECONDS))
                executor.shutdownNow();
        } catch (Exception e) {
            //nothing to do
        }
	}

	private static HashMap<String, String> parseArguments(final String args[]) {
		if (args == null || args.length == 0)
			return null;
		HashMap<String, String> rHM = new HashMap<String, String>();

		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].startsWith("-")) {
				if (args[i + 1].startsWith("-")) {
					rHM.put(args[i], "");
				} else {
					rHM.put(args[i], args[i + 1]);
					i++;
				}
			}
		}
		if (args[args.length-1].startsWith("-"))
			rHM.put(args[args.length-1], "");

		return rHM;
	}

	public static void printHelp() {
		System.err.println("Usage:");
		System.err.println("\tServer");
		System.err.println("\t\tstandalone: java JIperf -s -p portNumer -P numberOfThreads -w windowSize");
		System.err.println("\t\tUse a SSH control connection: java JIperf -s -ssh");
		System.err.println("\tClient");
		System.err.println("\t\tstandalone: java JIperf -c host -p portNumer -P numberOfThreads -w windowSize");
		System.err.println("\t\tremotely start a jiperf server: java JIperf -c host -ssh [-u user] [-E command] -p portNumer -P numberOfThreads -w windowSize");

	}

	public static void main(String[] args) {
		try {
			HashMap<String, String> argsMap = parseArguments(args);
			if (argsMap.containsKey("-c")) {
				JIperfClient client = new JIperfClient(argsMap);
				client.flood();
			} else if (argsMap.containsKey("-s")) {
				JIperfServer server = new JIperfServer(argsMap);
				server.doWork();

			} else {
				printHelp();
				System.exit(0);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
}
