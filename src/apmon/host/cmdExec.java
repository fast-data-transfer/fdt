package apmon.host;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;

public class cmdExec {

	public String full_cmd;
	public Process pro;
	String osname;
	String exehome = "";
	private long timeout = 60 * 1000; // 1 min
	
	protected LinkedList streams = null;
	protected LinkedList streamsReal = null;
	
	protected boolean isError = false;

	/* These varibles are set to true when we want to destroy the streams pool */
	protected boolean stopStreams = false;
	protected boolean stopStreamsReal = false;
	
	public cmdExec() {
		osname = System.getProperty("os.name");
		exehome = System.getProperty("user.dir");
		String tot = System.getProperty("iperf.timeout");
		double dd = -1.0;
		try {
			dd = Double.parseDouble(tot);
		} catch (Exception e) {
			dd = -1.0;
		}
		if (dd >= 0.0) timeout = (long)(dd * 1000);
		streams = new LinkedList();
		streamsReal = new LinkedList();
	}

	public void setCmd(String cmd) {
		osname = System.getProperty("os.name");
		full_cmd = cmd; // local
	}
	
	public void setTimeout(long timeout) {
		
		this.timeout = timeout;
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
			System.out.println("FAILED to execute cmd = " + exehome + cmd);
			Thread.currentThread().interrupt();
		}

		return null;
	}

	public BufferedReader exeHomeOutput(String cmd) {

		try {

			pro =
				Runtime.getRuntime().exec(
					new String[] { "/bin/sh", "-c", exehome + cmd});
//			System.out.println("/bin/sh -c "+exehome + cmd);
			InputStream out = pro.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(out));

			BufferedReader err = new BufferedReader(new InputStreamReader(pro.getErrorStream()));
			
			String buffer = "";
			String ret = "";
			while((buffer = err.readLine())!= null) {
				ret += buffer+"\n'";
			}
			
			if (ret.length() != 0){
//				System.out.println(ret);
				return null;
			}
			
			
			return br;

		} catch (Exception e) {
			System.out.println("FAILED to execute cmd = " + exehome + cmd);
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
	
	public boolean isError() {
		
		return isError;
	}
	
	public String executeCommand(String command, String expect) {
		
		StreamGobbler output = null;
		StreamGobbler error = null;
		
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
				long startTime = new Date().getTime();
				while (true) {
					out = error.getOutput();
					try {
						if (!out.equals("") && proc.exitValue() != 0) {
							isError = true;
							break;
						}
					} catch (IllegalThreadStateException ex) { }
					if (expect != null) {
						out = output.getOutput();
						if (out != "" && out.indexOf(expect) != -1) {
							isError = false;
							break;
						}
					}
					long endTime = new Date().getTime();
					if (endTime - startTime > timeout) {
						isError = true;
						break;
					}
					Thread.sleep(100);
				}
			
			proc.destroy();
			proc.waitFor();

			if (out.equals(""))
				out = output.getOutput();
			
//			String ret = "";
//			
//			if (!error.getOutput().equals(""))
//				ret = error.getOutput();
//			
//			ret = output.getOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamGobbler(error);
			addStreamGobbler(output);
			
			error =  null;
			output = null;
			
			return out;
			
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
			return "";
		}
	}
	

	public String executeCommand(String command, String expect, int howManyTimes) {
		
		StreamGobbler output = null;
		StreamGobbler error = null;
		int nr = 0; // how many times the expect string occured
		
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
			
			error.setInputStream(proc.getErrorStream());            
			
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			long startTime = new Date().getTime();
			while (true) {
				out = error.getOutput();
				try {
					if (!out.equals("") && proc.exitValue() != 0) {
						isError = true;
						break;
					}
				} catch (IllegalThreadStateException ex) { }
				if (expect != null) {
					out = output.getOutput();
					if (out != "" && out.indexOf(expect) != -1) {
						nr = getStringOccurences(out, expect);
						if (nr >= howManyTimes) {
							isError = false;
							break;
						}
					}
				}
				long endTime = new Date().getTime();
				if (endTime - startTime > timeout) {
					isError = true;
					break;
				}
				Thread.sleep(100);
			}
			
			proc.destroy();
			proc.waitFor();

			if (out.equals(""))
				out = output.getOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamGobbler(error);
			addStreamGobbler(output);
			
			error =  null;
			output = null;
			
			return out;
			
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
			return "";
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
	
	// cipsm ->  new execute command - it shows the output exactly as it is, by lines
	public String executeCommandReality(String command, String expect) {

		StreamRealGobbler error = null;
		StreamRealGobbler output = null;
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
				proc = Runtime.getRuntime().exec(cmd);
			} else {
				isError = true;
				return null; 
			}
			
			error = getStreamRealGobbler();
			output = getStreamRealGobbler();
			
			// any error message?
			error.setInputStream(proc.getErrorStream());            
			
			// any output?
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			// any error???
			long startTime = new Date().getTime();
			while (true) {
				out = error.forceAllOutput();
				try {
					if (!out.equals("") && proc.exitValue() != 0) {
						isError = true;
						break;
					}
				} catch (IllegalThreadStateException ex) { }
				if (expect != null) {
					out = output.forceAllOutput();
					if (out != "" && out.indexOf(expect) != -1) {
						isError = false;
						break;
					}
				}
				long endTime = new Date().getTime();
				if (endTime - startTime > timeout) {
					isError = true;
					break;
				}
				Thread.sleep(100);
			}
			
			proc.destroy();
			proc.waitFor();
			
			if (out.equals(""))
				out = output.forceAllOutput();
			
//			String ret = "";
//			
//			if (!error.getOutput().equals(""))
//				ret = error.forceAllOutput();
//			
//			ret = output.forceAllOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamRealGobbler(error);
			addStreamRealGobbler(output);
			
			error = null;
			output = null;
			
			return out;
			
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
			isError = true;
			
			return "";
		}
	}
	
	public String executeCommandReality(String command, String expect, int howManyTimes) {

		StreamRealGobbler error = null;
		StreamRealGobbler output = null;
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
				proc = Runtime.getRuntime().exec(cmd);
			} else {
				isError = true;
				return null; 
			}
			
			error = getStreamRealGobbler();
			output = getStreamRealGobbler();
			
			error.setInputStream(proc.getErrorStream());            
			
			output.setInputStream(proc.getInputStream());
			
			String out = "";
			
			long startTime = new Date().getTime();
			while (true) {
				out = error.forceAllOutput();
				try {
					if (!out.equals("") && proc.exitValue() != 0) {
						isError = true;
						break;
					}
				} catch (IllegalThreadStateException ex) { }
				if (expect != null) {
					out = output.forceAllOutput();
					if (out != "" && out.indexOf(expect) != -1) {
						int nr = getStringOccurences(out, expect);
						if (nr >= howManyTimes) {
							isError = false;
							break;
						}
					}
				}
				long endTime = new Date().getTime();
				if (endTime - startTime > timeout) {
					isError = true;
					break;
				}
				Thread.sleep(100);
			}
			
			proc.destroy();
			proc.waitFor();
			
			if (out.equals(""))
				out = output.forceAllOutput();
			
//			String ret = "";
//			
//			if (!error.getOutput().equals(""))
//				ret = error.forceAllOutput();
//			
//			ret = output.forceAllOutput();
			
			error.stopIt();
			output.stopIt();
			
			addStreamRealGobbler(error);
			addStreamRealGobbler(output);
			
			error = null;
			output = null;
			
			return out;
			
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
			isError = true;
			
			return "";
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
	
	public StreamRealGobbler getStreamRealGobbler() {
		synchronized (streamsReal) {
			if (streamsReal.size() == 0) {
				StreamRealGobbler stream = new StreamRealGobbler(null);
				stream.start();
				return stream;
			}
			return (StreamRealGobbler)streamsReal.removeFirst();
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
		//System.out.println("### cmdExec stopIt streams.size = " + streams.size());
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
		String output = "";
		boolean stop = false;
		boolean stopForever = false;
		boolean doneReading = false;
		
		public StreamGobbler(InputStream is) {
		
			super("Stream Gobler");
			this.is = is;
			this.setDaemon(true);
		}
		
		public void setInputStream(InputStream is) {
			
			this.is = is;
			output = "";
			stop = false;
			synchronized (this) {
				doneReading = false;
				notify();
			}
		}
		
		public String getOutput() {
			
			return output;
		}
		
		public synchronized String forceAllOutput() {
			
			if (!doneReading)
				return "";
			doneReading = false;
			return output;
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
						output += line;    
					}
					synchronized (this) {
						doneReading = true;
					}
					is.close();
				} catch (Exception ioe) {
					output = "";
				}
				is = null;
			}
		}
	}

	class StreamRealGobbler extends Thread {
		
		InputStream is;
		String output = "";
		boolean stop = false;
		boolean doneReading = false;
		boolean stopForever = false;
		
		public StreamRealGobbler(InputStream is) {
			
			super("Stream Real Gobbler");
			this.is = is;
			this.setDaemon(true);
		}
		
		public void setInputStream(InputStream is) {
			
			this.is = is;
			output = "";
			stop = false;
			synchronized (this) {
				doneReading = false;
				notify();
			}
		}
		
		public String getOutput() {
			
			return output;
		}
		
		public synchronized String forceAllOutput() {
			
			if (!doneReading)
				return "";
			return output;
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
						output += line+"\n";    
					}
					synchronized (this) {
						doneReading = true;
					}
				} catch (Exception ioe) {
					output = "";
				}
				is = null;
			}
		}
	}
	
}
