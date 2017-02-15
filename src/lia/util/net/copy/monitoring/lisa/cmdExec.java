/*
 * $Id: cmdExec.java 357 2007-08-16 14:35:18Z ramiro $
 */
package lia.util.net.copy.monitoring.lisa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class was taken more or less from MonALISA
 * @author Ciprian Dobre
 * @author ramiro
 */

// FIXME - UPDATE!!! this class with latest version from ML!!! 
public class cmdExec {

	public final transient Logger logger = Logger.getLogger("monalisa.util.cmdExec");

	public String full_cmd;
	public Process pro;
	String osname;
	String exehome = "";
	
	protected LinkedList streams = null;
	protected LinkedList streamsReal = null;
	
	//protected boolean isError = false;

	/* These varibles are set to true when we want to destroy the streams pool */
	protected boolean stopStreams = false;
	protected boolean stopStreamsReal = false;

	private static cmdExec _instance = null;
	
	/**
	 * structure for command output
	 */
	public static CommandResult NullCommandResult = new CommandResult(null,true);
	public static class CommandResult{
		private final String output;
		private final boolean failed;
		public CommandResult(String output,boolean wasError) {
			this.output = output;
			this.failed = wasError;
		}
		/**
		 * @return Returns the output.
		 */
		public String getOutput() {
			return output==null?"":output;
		}
		/**
		 * @return Returns the failed.
		 */
		public boolean failed() {
			return failed;
		}
		
	}
	
	private cmdExec() {
		osname = System.getProperty("os.name");
		exehome = System.getProperty("user.home");
		streams = new LinkedList();
		streamsReal = new LinkedList();
	}
	
	public static synchronized cmdExec getInstance() {
		if (_instance == null)
			_instance = new cmdExec();
		return _instance;
	}

	public void setCmd(String cmd) {
		osname = System.getProperty("os.name");
		full_cmd = cmd; // local
	}
	
	public BufferedReader procOutput(String cmd) {
		try {

			if (osname.startsWith("Linux") || osname.startsWith("Mac")) {
				pro =
					Runtime.getRuntime().exec(
						new String[] { "/bin/sh", "-c", cmd });
			} else if (osname.startsWith("Windows")) {
				pro = Runtime.getRuntime().exec(exehome + cmd);
			}

			InputStream out = pro.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(out));
			BufferedReader err = new BufferedReader(new InputStreamReader(pro.getErrorStream()));
			
			String buffer = "";
			String ret = "";
			while((buffer = err.readLine())!= null) {
				ret += buffer+"\n'";
			}
			
			if (ret.length() != 0){
				return null;
			}
			
			return br;

		} catch (Exception e) {
			logger.warning("FAILED to execute cmd = " + exehome + cmd);
			Thread.currentThread().interrupt();
		}

		return null;
	}

	public BufferedReader exeHomeOutput(String cmd) {

		try {
			pro =
				Runtime.getRuntime().exec(
					new String[] { "/bin/sh", "-c", exehome + cmd});
			InputStream out = pro.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(out));

			BufferedReader err = new BufferedReader(new InputStreamReader(pro.getErrorStream()));
			
			String buffer = "";
			String ret = "";
			while((buffer = err.readLine())!= null) {
				ret += buffer+"\n'";
			}
			
			if (ret.length() != 0){
				return null;
			}
			return br;
		} catch (Exception e) {
			logger.warning("FAILED to execute cmd = " + exehome + cmd);
			Thread.currentThread().interrupt();
		}
		return null;
	}

	public void stopModule() {
		if (this.pro != null)
			this.pro.destroy();
	}

	public BufferedReader readProc(String filePath) {
		try {
			return new BufferedReader(new FileReader(filePath));
		} catch (Exception e) {

			return null;
		}
	}
	public CommandResult executeCommand(String command, String expect) {
		return executeCommand(command, expect, 60 * 1000);
	}
	
	public CommandResult executeCommand(String command, String expect, long timeout) {
		
		StreamGobbler output = null;
		StreamGobbler error = null;
		boolean isError = false;
		try {            
			String osName = System.getProperty("os.name" );
			Process proc = null;

			if (osName.indexOf("Win") != -1) {
				proc = Runtime.getRuntime().exec(command);
			} else if (osName.indexOf("Linux") != -1 || osName.indexOf("Mac") != -1) {
				String[] cmd = new String[3];
				cmd[0] = "/bin/sh";
				cmd[1] = "-c";
				cmd[2] = command;
				proc = Runtime.getRuntime().exec(cmd);
			} else {
				isError = true;
				return null; 
			}
			
			error = getStreamGobbler();
			output = getStreamGobbler();
			
			// any error message?
			error.setInputStream(proc.getErrorStream());            
			
			// any output?
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			// any error???
				long startTime = System.currentTimeMillis();
				while (true) {
					try {
						out = error.getOutput();
						if (out!=null && out.length() != 0 && proc.exitValue() != 0) {
							isError = true;
							break;
						}
					} catch (IllegalThreadStateException ex) { }
					if (expect != null) {
						out = output.getOutput();
						if (out != null && out.length() != 0 && out.indexOf(expect) != -1) {
							isError = false;
							break;
						}
					}
					long endTime = System.currentTimeMillis();
					if (endTime - startTime > timeout) {
						isError = true;
						break;
					}
					Thread.sleep(100);
				}
			
			proc.destroy();
			proc.waitFor();

			if (out.length() == 0 || proc.exitValue() == 0)
				out = output.getOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamGobbler(error);
			addStreamGobbler(output);
			
			error =  null;
			output = null;
			
			return new CommandResult(out,isError);
			
		} catch (Exception e) {
			e.printStackTrace();
			
			if (error != null) {
				addStreamGobbler(error);
				error.stopIt();
				error = null;
			}
			
			if (output != null) {
				addStreamGobbler(output);
				output.stopIt();
				output = null;
			}
			isError = true;
			return new CommandResult("",true);
		}
	}

	public CommandResult executeCommand(String command, Pattern expect) {
		return executeCommand(command, expect, 60*1000);
	}
	
	public CommandResult executeCommand(String command, Pattern expect, long timeout) {
		
		StreamGobbler output = null;
		StreamGobbler error = null;
		boolean isError = false;
		try
		{            
			String osName = System.getProperty("os.name" );
			Process proc = null;

			if (osName.indexOf("Win") != -1) {
				proc = Runtime.getRuntime().exec(command);
			} else if (osName.indexOf("Linux") != -1 || osName.indexOf("Mac") != -1) {
				String[] cmd = new String[3];
				cmd[0] = "/bin/sh";
				cmd[1] = "-c";
				cmd[2] = command;
				proc = Runtime.getRuntime().exec(cmd);
			} else {
				isError = true;
				return null; 
			}
			
			error = getStreamGobbler();
			output = getStreamGobbler();
			
			// any error message?
			error.setInputStream(proc.getErrorStream());            
			
			// any output?
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			// any error???
				long startTime = System.currentTimeMillis();
				while (true) {
					try {
						out = error.getOutput();
						if (out!=null && out.length() != 0 && proc.exitValue() != 0) {
							isError = true;
							break;
						}
					} catch (IllegalThreadStateException ex) { }
					if (expect != null) {
						out = output.getOutput();
						if (out != null && out.length() != 0) {
							if (expect.matcher(out).matches()) {
								isError = false;
								break;
							}
						}
					}
					long endTime = System.currentTimeMillis();
					if (endTime - startTime > timeout) {
						isError = true;
						break;
					}
					Thread.sleep(100);
				}
			
			proc.destroy();
			proc.waitFor();

			if (out.length() == 0 || proc.exitValue() == 0)
				out = output.getOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamGobbler(error);
			addStreamGobbler(output);
			
			error =  null;
			output = null;
			
			return new CommandResult(out,isError);
			
		} catch (Exception e) {
			e.printStackTrace();
			
			if (error != null) {
				addStreamGobbler(error);
				error.stopIt();
				error = null;
			}
			
			if (output != null) {
				addStreamGobbler(output);
				output.stopIt();
				output = null;
			}
			return new CommandResult("",true);
		}
	}

	public CommandResult executeCommand(String command, String expect, int howManyTimes) {
		return executeCommand(command, expect, howManyTimes, 60*1000);
	}	
	
	public CommandResult executeCommand(String command, String expect, int howManyTimes, long timeout) {
		
		StreamGobbler output = null;
		StreamGobbler error = null;
		int nr = 0; // how many times the expect string occured
		boolean isError = false;
		try
		{            
			String osName = System.getProperty("os.name" );
			Process proc = null;

			if (osName.indexOf("Win") != -1) {
				proc = Runtime.getRuntime().exec(command);
			} else if (osName.indexOf("Linux") != -1 || osName.indexOf("Mac") != -1) {
				String[] cmd = new String[3];
				cmd[0] = "/bin/sh";
				cmd[1] = "-c";
				cmd[2] = command;
				proc = Runtime.getRuntime().exec(cmd);
			} else {
				return NullCommandResult;
			}
			
			error = getStreamGobbler();
			output = getStreamGobbler();
			
			error.setInputStream(proc.getErrorStream());            
			
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			long startTime = System.currentTimeMillis();
			while (true) {
				try {
					out = error.getOutput();
					if (out!=null && out.length() != 0 && proc.exitValue() != 0) {
						isError = true;
						break;
					}
				} catch (IllegalThreadStateException ex) { }
				if (expect != null) {
					out = output.getOutput();
					if (out!=null && out.length() != 0 && out.indexOf(expect) != -1) {
						nr = getStringOccurences(out, expect);
						if (nr >= howManyTimes) {
							isError = false;
							break;
						}
					}
				}
				long endTime = System.currentTimeMillis();
				if (endTime - startTime > timeout) {
					isError = true;
					break;
				}
				Thread.sleep(100);
			}
			
			proc.destroy();
			proc.waitFor();

			if (out.length() == 0 || proc.exitValue() == 0)
				out = output.getOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamGobbler(error);
			addStreamGobbler(output);
			
			error =  null;
			output = null;
			
			return new CommandResult(out,isError);
			
		} catch (Exception e) {
			e.printStackTrace();
			
			if (error != null) {
				addStreamGobbler(error);
				error.stopIt();
				error = null;
			}
			
			if (output != null) {
				addStreamGobbler(output);
				output.stopIt();
				output = null;
			}
			return NullCommandResult;
		}
	}
	
	protected int getStringOccurences(String text, String token) {
		
		if (text.indexOf(token) < 0) return 0;
		int nr = 0;
		String str = text;
		while (str.indexOf(token) >= 0) {
			str = str.substring(str.indexOf(token)+token.length());
			nr++;
		}
		return nr;
	}

	public CommandResult executeCommandReality(String command, String expect, String path) {
		return executeCommandReality(command, expect, 60*1000, path);
	}
	
	public CommandResult executeCommandReality(String command, String expect, long timeout, String path) {
		
		StreamRealGobbler error = null;
		StreamRealGobbler output = null;
		boolean isError = false;
		try {            
			String osName = System.getProperty("os.name" );
			Process proc = null;

			if (osName.indexOf("Win") != -1) {
				proc = Runtime.getRuntime().exec(command);
			} else if (osName.indexOf("Linux") != -1) {
				String[] cmd = new String[3];
				cmd[0] = "/bin/sh";
				cmd[1] = "-c";
				cmd[2] = command;
				if (path != null && path.length() != 0) 
					proc = Runtime.getRuntime().exec(cmd, new String[] { "PATH="+path });
				else
					proc = Runtime.getRuntime().exec(cmd);
			} else {
				return NullCommandResult;
			}
			
			error = getStreamRealGobbler(timeout);
			output = getStreamRealGobbler(timeout);
			
			// any error message?
			error.setInputStream(proc.getErrorStream());            
			
			// any output?
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			// any error???
			long startTime = System.currentTimeMillis();
			boolean timeoutOccured = false;
			while (true) {
				try {
					out = error.forceAllOutput();
					if (proc.exitValue() != 0) {
						isError = true;
					}
					break; // also if exitValue did not throw exception than we're done running
				} catch (IllegalThreadStateException ex) { }
				if (expect != null) {
					out = output.forceAllOutput();
					if (out!=null && out.length() != 0 && out.indexOf(expect) != -1) {
						isError = false;
						proc.destroy();
						break;
					}
				}
				long endTime = System.currentTimeMillis();
				if (endTime - startTime > timeout) {
					isError = true;
					timeoutOccured = true;
					proc.destroy();
					break;
				}
				Thread.sleep(100);
			}
			
			if (!timeoutOccured) {
				proc.waitFor();
			} 
			else {
				try {
					Thread.sleep(2000);
					proc.getOutputStream().close();
					proc.getInputStream().close();
					proc.getErrorStream().close();
				} catch (Exception ex) { 
				}
			}
			
			if (out!=null && out.length() == 0 || proc.exitValue() == 0) {
				out = output.forceAllOutput();
			}
	
			if (timeoutOccured)
				out += "...Timeout";
			
			error.stopIt();
			output.stopIt();
			
			addStreamRealGobbler(error);
			addStreamRealGobbler(output);
			
			error = null;
			output = null;
			
			return new CommandResult(out,isError);
			
		} catch (Exception e) {
			e.printStackTrace();
			
			if (error != null) {
				addStreamRealGobbler(error);
				error.stopIt();
				error = null;
			}
			
			if (output != null) {
				addStreamRealGobbler(output);
				output.stopIt();
				output = null;
			}
			return NullCommandResult;
		}
	}
	
	public void executeCommandRealityForFinish(String command, String path) {
		executeCommandRealityForFinish(command, false, path);
	}

	public void executeCommandRealityForFinish(String command, final boolean showOutput, String path) {
		executeCommandRealityForFinish(command, showOutput, 60*60*1000, path);
	}
	
	public boolean executeCommandRealityForFinish(String command, final boolean showOutput, long timeout, String path) {

		StreamRealGobbler error = null;
		StreamRealGobbler output = null;
		boolean isError = false;
		try {            
			String osName = System.getProperty("os.name" );
			Process proc = null;
			if (osName.indexOf("Win") != -1) {
				proc = Runtime.getRuntime().exec(command);
			} else if (osName.indexOf("Linux") != -1) {
				String[] cmd = new String[3];
				cmd[0] = "/bin/sh";
				cmd[1] = "-c";
				cmd[2] = command;
				if (path != null && path.length() != 0)
					proc = Runtime.getRuntime().exec(cmd, new String[] { "PATH="+path });
				else
					proc = Runtime.getRuntime().exec(cmd);
			} else {
				return  true;
			}
			
			if (showOutput) {
				error = getStreamRealGobbler(timeout);
				output = getStreamRealGobbler(timeout);
				error.setProgress(true);
				output.setProgress(true);
				// any error message?
				error.setInputStream(proc.getErrorStream());            
				// any output?
				output.setInputStream(proc.getInputStream());
				error.setProgress(false);
				output.setProgress(false);
			}
			
			long startTime = System.currentTimeMillis();
			boolean timeoutOccured = false;
			while (true) {
				try {
					if (proc.exitValue() != 0) {
						isError = true;
					}
					break; // also if exitValue did not throw exception than we're done running
				} catch (IllegalThreadStateException ex) { }
				long endTime = System.currentTimeMillis();
				if (endTime - startTime > timeout) {
					isError = true;
					timeoutOccured = true;
					proc.destroy();
					break;
				}
				Thread.sleep(100);
			}

			if (!timeoutOccured) {
				proc.waitFor();
			} 
			else {
				try {
					Thread.sleep(2000);
					proc.getOutputStream().close();
					proc.getInputStream().close();
					proc.getErrorStream().close();
				} catch (Exception ex) { 
				}
			}
			
			if (showOutput) {
				error.setProgress(false);
				output.setProgress(false);
				error.stopIt();
				output.stopIt();
				addStreamRealGobbler(error);
				addStreamRealGobbler(output);
				error = null;
				output = null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			isError = true;
			if (error != null) {
				addStreamRealGobbler(error);
				error.stopIt();
				error = null;
			}
			
			if (output != null) {
				addStreamRealGobbler(output);
				output.stopIt();
				output = null;
			}

		}
		return isError;
	}

	public CommandResult executeCommandReality(String command, String expect, int howManyTimes, String path) {
		return executeCommandReality(command, expect, howManyTimes, 60*1000, path);
	}
	
	public CommandResult executeCommandReality(String command, String expect, int howManyTimes, long timeout, String path) {

		StreamRealGobbler error = null;
		StreamRealGobbler output = null;
		boolean isError = false;
		try
		{            
			String osName = System.getProperty("os.name" );
			Process proc = null;

			if (osName.indexOf("Win") != -1) {
				proc = Runtime.getRuntime().exec(command);
			} else if (osName.indexOf("Linux") != -1) {
				String[] cmd = new String[3];
				cmd[0] = "/bin/sh";
				cmd[1] = "-c";
				cmd[2] = command;
				if (path != null && path.length() != 0)
					proc = Runtime.getRuntime().exec(cmd, new String[] { "PATH="+path });
				else
					proc = Runtime.getRuntime().exec(cmd);
			} else {
			 return NullCommandResult;
			}
			
			error = getStreamRealGobbler(timeout);
			output = getStreamRealGobbler(timeout);
			
			error.setInputStream(proc.getErrorStream());            
			
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			long startTime = System.currentTimeMillis();
			boolean timeoutOccured = false;
			while (true) {
				try {
					out = error.forceAllOutput();
					if (out!=null && out.length() != 0 && proc.exitValue() != 0) {
						isError = true;
					}
					break;
				} catch (IllegalThreadStateException ex) { }
				if (expect != null) {
					out = output.forceAllOutput();
					if (out!=null && out.length() != 0 && out.indexOf(expect) != -1) {
						int nr = getStringOccurences(out, expect);
						if (nr >= howManyTimes) {
							isError = false;
							proc.destroy();
							break;
						}
					}
				}
				long endTime = System.currentTimeMillis();
				if (endTime - startTime > timeout) {
					isError = true;
					timeoutOccured = true;
					proc.destroy();
					break;
				}
				Thread.sleep(100);
			}
			
			if (!timeoutOccured) {
				proc.waitFor();
			} else {
				try {
					Thread.sleep(2000);
					proc.getOutputStream().close();
					proc.getInputStream().close();
					proc.getErrorStream().close();
				} catch (Exception ex) { }
			}
			
			if (out!=null && out.length() == 0 || proc.exitValue() == 0)
				out = output.forceAllOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamRealGobbler(error);
			addStreamRealGobbler(output);
			
			error = null;
			output = null;
			return new CommandResult(out,isError);
			
		} catch (Exception e) {
			e.printStackTrace();
			
			if (error != null) {
				addStreamRealGobbler(error);
				error.stopIt();
				error = null;
			}
			
			if (output != null) {
				addStreamRealGobbler(output);
				output.stopIt();
				output = null;
			}
		return NullCommandResult;
		}
	}
	
	public StreamGobbler getStreamGobbler() {
		
		synchronized (streams) {
			if (streams.size() == 0) {
				StreamGobbler stream = new StreamGobbler(null);
				stream.start();
				return stream;
			}
			return (StreamGobbler)streams.removeFirst();
		}
	}
	
	public void addStreamGobbler(StreamGobbler stream) {
		
		synchronized (streams) {
		    if (!stopStreams)
		    	streams.addLast(stream);
		    else
		    	stream.stopItForever();
		}
	}
	
	public StreamRealGobbler getStreamRealGobbler(long timeout) {
		
		synchronized (streamsReal) {
			if (streamsReal.size() == 0) {
				StreamRealGobbler stream = new StreamRealGobbler(null, timeout);
				stream.start();
				return stream;
			}
			StreamRealGobbler st = (StreamRealGobbler)streamsReal.removeFirst();
			st.setTimeout(timeout);
			return st;
		}
	}
	
	public void addStreamRealGobbler(StreamRealGobbler stream) {
		
		synchronized (streamsReal) {
			if (!stopStreamsReal)
		    	streamsReal.addLast(stream);
		    else
		    	stream.stopItForever();
		}
	}
	
	public void stopIt() {
		synchronized(streams) {
			stopStreams = true;
			
			while (streams.size() > 0) {
				StreamGobbler sg = (StreamGobbler)(streams.removeFirst());
				sg.stopItForever();
			}
		}
		synchronized(streamsReal) {
			stopStreamsReal = true;
			
			while (streamsReal.size() > 0) {
				StreamRealGobbler sg = (StreamRealGobbler)(streamsReal.removeFirst());
				sg.stopItForever();
			}
		}
	}

	class StreamGobbler extends Thread {
		
		InputStream is;
		StringBuilder output;
		boolean stop = false;
		boolean stopForever = false;
		boolean doneReading = false;
		
		public StreamGobbler(InputStream is) {
		
			super("Stream Gobler");
			this.is = is;
			this.output = new StringBuilder();
			this.setDaemon(true);
		}
		
		public void setInputStream(InputStream is) {
			
			this.is = is;
			output = new StringBuilder();
			stop = false;
			synchronized (this) {
				doneReading = false;
				notify();
			}
		}
		
		public String getOutput() {
			return output==null?null:output.toString();
		}
		
		public synchronized String forceAllOutput() {
			
			if (!doneReading)
				return null;
			doneReading = false;
			return output==null?null:output.toString();
		}
		
		public void stopIt() {
			
			stop = true;
		}
		
		public void stopItForever() {
			synchronized(this) {
				stopForever = true;
				notify();
			}
		}

		public void run() {
			
			while (true) {
				
				synchronized (this) {
					while (is == null && !stopForever) {
						try {
							wait();
						} catch (Exception e) { }
					}
				}
				
				if (stopForever) {
					break;
				}

				try {
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String line=null;
					while (!stop && (line = br.readLine()) != null) {
						output.append(line);    
					}
					synchronized (this) {
						doneReading = true;
					}
					is.close();
				} catch (Exception ioe) {
					output =  new StringBuilder();
				}
				is = null;
			}
		}
	}

	class StreamRealGobbler extends Thread {
		
		InputStream is;
		InputStreamReader isr; 
		final char[] buf;
		StringBuilder output = new StringBuilder();
		boolean stop = false;
		boolean doneReading = false;
		boolean stopForever = false;

		private Thread thread = null;
		private boolean showProgress = false;
		
		private long timeout;
		
		public StreamRealGobbler(InputStream is, long timeout) {
			
			super("Stream Real Gobler");
			this.timeout = timeout;
			this.is = is;
			this.setDaemon(true);
			buf = new char[32]; 
		}
		
		public void setTimeout(long timeout) {
			this.timeout = timeout;
		}
		
		public void setInputStream(InputStream is) {
			
			this.is = is;
			output = new StringBuilder();
			stop = false;
			synchronized (buf) {
				doneReading = false;
				buf.notifyAll();
			}
		}
		
		public void setProgress(boolean showProgress) {
			this.showProgress = showProgress;
		}
		
		public String getOutput() {
			
			return output==null?null:output.toString();
		}
		
		public String forceAllOutput() {
			
			if (!doneReading) {
				try {
					if (!isr.ready()) {
						return null;
					}
				} catch (Exception ex) { }
				
				try {
					thread.interrupt(); // force the thread out of sleep
				} catch (Exception ex) { }
				synchronized (buf) {
					buf.notifyAll();
				}
				// otherwise let's give the output a chance to complete
				long start = System.currentTimeMillis();
				while (!doneReading) {
					try {
						Thread.sleep(200);
					} catch (Exception ex) { }
					if (doneReading) {
						return output==null?null:output.toString();
					}
					long now = System.currentTimeMillis();
					if ((now - start) >= timeout) {
						return null; // last chance
					}
				}
			}
			return output==null?null:output.toString();
		}
		
		public void stopIt() {
			try {
				is.close();
			} catch (Exception ex) { }
			try {
				isr.close();
			} catch (Exception ex) { }
			stop = true;
		}
		
		public void stopItForever() {
			synchronized(buf) {
				stopIt();
				stopForever = true;
				buf.notifyAll();
			}
		}

		public void run() {
			
			thread = Thread.currentThread();
			
			while (true) {
				
				synchronized (buf) {
					while (is == null && !stopForever) {
						try {
							buf.wait();
						} catch (Exception e) { }
					}
				}
				
				if (stopForever) {
					synchronized (buf) {
						doneReading = true;
					}
					break;
				}

				try {
					isr = new InputStreamReader(is);
					while (!stop) {
						try {
							final int ret = isr.read(buf);
							if (ret > 0) {
								final String nstr = new String(buf, 0, ret);
								if (showProgress) {
									logger.info(nstr);
								}
								output.append(nstr);
							} else {
								break; // and of stream
							}
						} catch (Exception ex) {
							break;
						}
					}
					
					synchronized (buf) {
						doneReading = true;
					}
				} catch (Exception ioe) {
					output = new StringBuilder();
				}
				try {
					is.close();
				} catch (Exception ex) { }
				is = null;
			}
		}
	}
	
}
