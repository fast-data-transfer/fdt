
package lia.util.net.copy.monitoring.lisa.net.dev;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lia.util.net.copy.monitoring.lisa.cmdExec;
import lia.util.net.copy.monitoring.lisa.cmdExec.CommandResult;
import lia.util.net.copy.monitoring.lisa.net.PatternUtil;


public class InterfaceHandler {

	
	protected final cmdExec exec;
	
	
	protected final PrintStream out;
	
	protected final Hashtable<String, String> lastRXPackets = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastRXErrors = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastRXDropped = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastRXOverruns = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastRXFrame = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastRXBytes = new Hashtable<String, String>();

	protected final Hashtable<String, String> lastTXPackets = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastTXErrors = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastTXDropped = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastTXOverruns = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastTXCarrier = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastTXBytes = new Hashtable<String, String>();
	
	protected final Hashtable<String, String> lastCollisions = new Hashtable<String, String>();
	protected final Hashtable<String, String> lastCompressed = new Hashtable<String, String>();

	protected long lastCall = 0;
	
	
	protected final HashMap<String, InterfaceStatistics> ifs = new HashMap<String, InterfaceStatistics>();

	protected final HashMap<String, InterfaceStatisticsStatic> lastGoodStatic = new HashMap<String, InterfaceStatisticsStatic>();

	protected final HashMap<String, InterfaceStatisticsStatic> staticifs = new HashMap<String, InterfaceStatisticsStatic>();
	
	
	
	protected static final String ifNamePattern = "^(\\S+)\\s+";
	protected static final String hwAddrPattern = "HWaddr\\s+(\\S+)";
	protected static final String ipv4Pattern = "inet addr:(\\S+)";
	protected static final String mask4Pattern = "Mask:(\\S+)";
	protected static final String bcast4Pattern = "Bcast:(\\S+)";
	protected static final String mtuPattern = "MTU:(\\S+)";
	
	protected static final String rxPackets = "RX[\\s\\S]+packets:(\\S+)";
	protected static final String rxErrors = "RX[\\s\\S]+errors:(\\S+)";
	protected static final String rxDropped = "RX[\\s\\S]+dropped:(\\S+)";
	protected static final String rxOverruns = "RX[\\s\\S]+overruns:(\\S+)";
	protected static final String rxFrame = "RX[\\s\\S]+frame:(\\S+)";
	protected static final String rxBytes = "RX\\s+bytes:(\\S+)";

	protected static final String txPackets = "TX[\\s\\S]+packets:(\\S+)";
	protected static final String txErrors = "TX[\\s\\S]+errors:(\\S+)";
	protected static final String txDropped = "TX[\\s\\S]+dropped:(\\S+)";
	protected static final String txOverruns = "TX[\\s\\S]+overruns:(\\S+)";
	protected static final String txCarrier = "TX[\\s\\S]+carrier:(\\S+)";
	protected static final String txBytes = "TX\\s+bytes:(\\S+)";

	protected static final String collisions = "\\s+collisions:(\\S+)";
	protected static final String compressed = "\\s+compressed:(\\S+)";
	protected static final String txqueuelen = "\\s+txqueuelen:(\\S+)";
	
	protected static final String netDevPattern = "(\\S+):\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+";
	
	protected static final String supportedPortsPattern = "Supported ports:\\s*\\[(.*)\\]";
	protected static final String supportedLinkModes = "Supported link modes:\\s*(.*?)Supports";
	protected static final String supportsAutoNegotiation = "Supports auto-negotiation:\\s*(\\S+)";
	protected static final String speed = "Speed:\\s*(.*)";
	protected static final String duplex = "Duplex:\\s*(.*)";
	protected static final String port = "Port:\\s*(.*)";
	
	protected final Logger logger;
	
	
	private boolean is64BitArch = false;

	final static NumberFormat nf = NumberFormat.getInstance();
	
	static {
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(4);
	}
	
	
	public InterfaceHandler(final PrintStream out, final Logger logger) {
		this.logger = logger;
		this.out = out;
		exec = cmdExec.getInstance();
	}
	
	
	private synchronized final String getIfconfigPath() {
		String path = System.getProperty("net.ifconfig.path", "/bin,/sbin,/usr/bin,/usr/sbin");
		if (path == null || path.length() == 0) {
			logger.warning("[Net - iconfig can not be found in " + path+ "]");
			return null;
		}
		return path.replace(',', ':').trim();
	}
	
	
	private final boolean checkLinkEncap(final InterfaceStatisticsStatic props, final String ifOutput) {
		
		if (ifOutput == null || ifOutput.length() == 0) return false;
		if (ifOutput.contains("Link encap:Ethernet")) {
			if (props != null) props.setEncap(InterfaceStatisticsStatic.TYPE_ETHERNET);
			return true;
		}
		if (ifOutput.contains("Link encap:Fiber Distributed Data Interface")) {
			if (props != null) props.setEncap(InterfaceStatisticsStatic.TYPE_FIBER);
			return true;
		}
		
		
		
		return false;
	}
	
	
	final List<String> ifArray = new ArrayList<String>();
	final List<String> toRemove = new ArrayList<String>();

	
	private final void checkIfConfig() {
		final String ifc = getIfconfigPath();
		final String command = "ifconfig";
		CommandResult cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		final String ret = cmdRes.getOutput();
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
			return;
		}

		double diff = 0D;
		long now = System.currentTimeMillis();
		if (lastCall != 0) {
			diff = (now - lastCall) / 1000D;
		}
		lastCall = now;

		
		final String interfaces[] = ret.split("\n\n");
		ifArray.clear();
		for (int i=0; i<interfaces.length; i++) {
			Pattern pattern = PatternUtil.getPattern("ifname", ifNamePattern);
			Matcher matcher = pattern.matcher(interfaces[i]);
			InterfaceStatistics ifProp = null;
			InterfaceStatisticsStatic ifPropStatic = null;
			String name = null;
			if (matcher.find()) {
				name = matcher.group(1);
				ifArray.add(name);
				if (!ifs.containsKey(name)) {
					ifProp = new InterfaceStatistics(name);
					ifs.put(name, ifProp);
				} else {
					ifProp = ifs.get(name);
					ifProp.updateTime();
				}
				if (!staticifs.containsKey(name)) {
					ifPropStatic = new InterfaceStatisticsStatic(name);
					staticifs.put(name, ifPropStatic);
				} else {
					ifPropStatic = staticifs.get(name);
					ifPropStatic.updateTime();
				}
				if (!checkLinkEncap(ifPropStatic, interfaces[i])) { 
					ifArray.remove(name); 
					continue; 
				}
			} else {
				continue; 
			}
			pattern = PatternUtil.getPattern("hwaddr", hwAddrPattern);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				ifPropStatic.setHwAddr(matcher.group(1));
			} else
				ifPropStatic.setHwAddr("unknown");
			pattern = PatternUtil.getPattern("ipv4", ipv4Pattern);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				ifProp.setIPv4(matcher.group(1));
			} else
				ifProp.setIPv4("unknown");
			pattern = PatternUtil.getPattern("mask4", mask4Pattern);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				ifProp.setMaskv4(matcher.group(1));
			} else
				ifProp.setMaskv4("unknown");
			pattern = PatternUtil.getPattern("bcast4", bcast4Pattern);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				ifProp.setBcastv4(matcher.group(1));
			} else 
				ifProp.setBcastv4("unknown");
			
			pattern = PatternUtil.getPattern("mtu", mtuPattern);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				try {
					ifProp.setMTU(Integer.parseInt(matcher.group(1)));
				} catch (Exception ex) {
					ifProp.setMTU(-1);
				}
			} else
				ifProp.setMTU(-1);
			pattern = PatternUtil.getPattern("rxpackets", rxPackets);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastRXPackets.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setRXPackets((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setRXPackets(-1D);
					}
				}
				lastRXPackets.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("rxerrors", rxErrors);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastRXErrors.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setRXErrors((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setRXErrors(-1D);
					}
				}
				lastRXErrors.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("rxdropped", rxDropped);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastRXDropped.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setRXDropped((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setRXDropped(-1D);
					}
				}
				lastRXDropped.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("rxoverruns", rxOverruns);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastRXOverruns.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setRXOverruns((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setRXOverruns(-1D);
					}
				}
				lastRXOverruns.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("rxframe", rxFrame);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastRXFrame.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setRXFrame((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setRXFrame(-1D);
					}
				}
				lastRXFrame.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("rxBytes", rxBytes);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastRXBytes.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setRX((double)difRes * 8D / diff);
					} catch (Throwable t) { 
						ifProp.setRX(-1D);
					}
				}
				lastRXBytes.put(name, newVal);
			}

			pattern = PatternUtil.getPattern("txpackets", txPackets);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastTXPackets.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setTXPackets((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setTXPackets(-1D);
					}
				}
				lastTXPackets.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("txerrors", txErrors);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastTXErrors.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setTXErrors((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setTXErrors(-1D);
					}
				}
				lastTXErrors.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("txdropped", txDropped);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastTXDropped.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setTXDropped((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setTXDropped(-1D);
					}
				}
				lastTXDropped.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("txoverruns", txOverruns);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastTXOverruns.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setTXOverruns((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setTXOverruns(-1D);
					}
				}
				lastTXOverruns.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("txcarrier", txCarrier);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastTXCarrier.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setTXCarrier((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setTXCarrier(-1D);
					}
				}
				lastTXCarrier.put(name, newVal);
			}
			pattern = PatternUtil.getPattern("txBytes", txBytes);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);

				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastTXBytes.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setTX((double)difRes * 8D / diff);
					} catch (Throwable t) { 
						ifProp.setTX(-1D);
					}
				}
				lastTXBytes.put(name, newVal);
			}

			pattern = PatternUtil.getPattern("collisions", collisions);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastCollisions.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setCollisions((double)difRes / diff);
					} catch (Throwable t) { 
						ifProp.setCollisions(-1D);
					}
				}
				lastCollisions.put(name, newVal);
			}

			pattern = PatternUtil.getPattern("compressed", compressed);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				if (diff > 0) {
					String res = diffWithOverflowCheck(newVal, lastCompressed.get(name));
					try {
						double difRes = Double.parseDouble(res);
						ifProp.setCompressed((double)difRes / diff);
						ifProp.setCanCompress(true);
					} catch (Throwable t) { 
						ifProp.setCompressed(-1D);
					}
				}
				lastCompressed.put(name, newVal);
			} else
				ifProp.setCanCompress(false);

			pattern = PatternUtil.getPattern("txqueuelen", txqueuelen);
			matcher = pattern.matcher(interfaces[i]);
			if (matcher.find()) {
				String newVal = matcher.group(1);
				try {
					ifProp.setTXQueueLen(Integer.parseInt(newVal));
				} catch (Exception e) {
					ifProp.setTXQueueLen(-1);
				}
			}

		}
		
		toRemove.clear();
		for (Iterator it = ifs.keySet().iterator(); it.hasNext(); ) {
			String ifn = (String)it.next();
			if (!ifArray.contains(ifn)) toRemove.add(ifn);
		}
		for (String ifn : toRemove) {
			ifs.remove(ifn);
			staticifs.remove(ifn);
		}
		



	}
	
	public void modify(TXQueueLenSet set) {

		String ifc = getIfconfigPath();

		
		String previoustx = null;
		String command = "ifconfig "+set.ifName;
		CommandResult cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		String ret =cmdRes.getOutput();
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
		} else {
			final Pattern pattern = PatternUtil.getPattern("txqueuelen", txqueuelen);
			final Matcher matcher = pattern.matcher(ret);
			if (matcher.find()) {
				previoustx = matcher.group(1);
			}
		}
		
		
		command = "ifconfig "+set.ifName+" txqueuelen "+set.txqueuelen;
		cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		ret = cmdRes.getOutput(); 
		boolean done = true;
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
			done = false;
		}
		if (done) {
			
			command = "ifconfig "+set.ifName;
			cmdRes = exec.executeCommandReality(command, (String)null, ifc);
			ret = cmdRes.getOutput(); 
			if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
				if (out != null) 
					out.println(ret);
				else
					logger.info(ret);
				done = false;
			}
			if (done) {
				final Pattern pattern = PatternUtil.getPattern("txqueuelen", txqueuelen);
				final Matcher matcher = pattern.matcher(ret);
				if (matcher.find()) {
					String newVal = matcher.group(1);
					if (previoustx == null) return; 
					done = !previoustx.equals(newVal);
				}
			}
		}
		if (done)
			return;

		
		doIfDown(set.ifName);
		command = "ifconfig "+set.ifName+" txqueuelen "+set.txqueuelen;
		cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		ret = cmdRes.getOutput(); 
		if (cmdRes.failed()|| ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
		}
		doIfUp(set.ifName);
	}
	
	public void modify(MTUSet set) {

		String ifc = getIfconfigPath();

		
		String previousmtu = null;
		String command = "ifconfig "+set.ifName;
		CommandResult cmdRes =exec.executeCommandReality(command, (String)null, ifc); 
		String ret = cmdRes.getOutput();
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
		} else {
			final Pattern pattern = PatternUtil.getPattern("mtu", mtuPattern);
			final Matcher matcher = pattern.matcher(ret);
			if (matcher.find()) {
				previousmtu = matcher.group(1);
			}
		}
		
		
		command = "ifconfig "+set.ifName+" mtu "+set.mtu;
		cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		ret = cmdRes.getOutput(); 
		boolean done = true;
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
			done = false;
		}
		if (done) {
			
			command = "ifconfig "+set.ifName;
			cmdRes =exec.executeCommandReality(command, (String)null, ifc);
			ret = cmdRes.getOutput(); 
			if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
				if (out != null) 
					out.println(ret);
				else
					logger.info(ret);
				done = false;
			}
			if (done) {
				final Pattern pattern = PatternUtil.getPattern("mtu", mtuPattern);
				final Matcher matcher = pattern.matcher(ret);
				if (matcher.find()) {
					String newVal = matcher.group(1);
					if (previousmtu == null) return; 
					done = !previousmtu.equals(newVal);
				}
			}
		}
		if (done)
			return;

		
		doIfDown(set.ifName);
		command = "ifconfig "+set.ifName+" mtu "+set.mtu;
		cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		ret = cmdRes.getOutput(); 
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
		}
		doIfUp(set.ifName);
	}
	
	private void doIfDown(String ifName) {

		final String ifc = getIfconfigPath();
		final String command = "ifconfig down";
		final CommandResult cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		final String ret = cmdRes.getOutput(); 
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
			
			return;
		}
	}

	private void doIfUp(String ifName) {

		final String ifc = getIfconfigPath();
		final String command = "ifconfig up";
		final CommandResult cmdRes =exec.executeCommandReality(command, (String)null, ifc);
		final String ret = cmdRes.getOutput(); 
		if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
			if (out != null) 
				out.println(ret);
			else
				logger.info(ret);
			
			return;
		}
	}

	
	private final boolean shouldRunEthTool() {
		String prop = System.getProperty("net.ethtool.use", null);
		if (prop == null)
			return true;
		try {
			return Boolean.getBoolean(prop);
		} catch (Throwable t) { }
		return true;
	}
	
	private synchronized final String getEthtoolPath() {
		String path = System.getProperty("net.ethtool.path", "/bin,/sbin,/usr/bin,/usr/sbin");
		if (path == null || path.length() == 0) {
			logger
			.warning("[Net - ethtool can not be found in " + path
					+ "]");
			return null;
		}
		return path.replace(',', ':').trim();
	}
	
	
	private final void checkEthtool() {
		
		final String eth = getEthtoolPath();
		final String command = "ethtool ";
		for (Map.Entry<String, InterfaceStatisticsStatic> entry : staticifs.entrySet()) {
			final String ifName = entry.getKey();
			final InterfaceStatisticsStatic stat = entry.getValue();
			if (ifName == null || ifName.length() == 0 || stat == null) continue;
			final CommandResult cmdRes= exec.executeCommandReality(command+ifName, null, eth);
			final String ret= cmdRes.getOutput();
			if (cmdRes.failed() || ret == null || ret.length() == 0 || PatternUtil.getPattern("Unknown command", null).matcher(ret).matches()) {
				if (out != null) {
					out.println(ret);
				} else {
					if (logger.isLoggable(Level.FINER))
						logger.log(Level.FINE, ret);
				}
				continue;
			}
			stat.setSupportedPorts((byte)0);
			
			Pattern pattern = PatternUtil.getPattern("supportedPorts", supportedPortsPattern);
			Matcher matcher = pattern.matcher(ret);
			if (matcher.find()) {
				final String mm = matcher.group(1);
				if (mm.trim().length() != 0) {
					final String sp[] = mm.trim().split(" ");
					for (int k=0; k<sp.length; k++) {
						if (sp[k].equals("TP"))
							stat.setSupportedPorts(InterfaceStatisticsStatic.PORT_TP);
						if (sp[k].equals("AUI"))
							stat.setSupportedPorts(InterfaceStatisticsStatic.PORT_AUI);
						if (sp[k].equals("BNC"))
							stat.setSupportedPorts(InterfaceStatisticsStatic.PORT_BNC);
						if (sp[k].equals("MII"))
							stat.setSupportedPorts(InterfaceStatisticsStatic.PORT_MII);
						if (sp[k].equals("FIBRE"))
							stat.setSupportedPorts(InterfaceStatisticsStatic.PORT_FIBRE);
					}
				}
			}
			pattern = PatternUtil.getPattern("supportedLinkModes", supportedLinkModes, true);
			matcher = pattern.matcher(ret);
			if (matcher.find()) {
				final String mm = matcher.group(1);
				if (mm.trim().length() != 0) {
					final String sp[] = mm.trim().split("[ \n]+");
					for (int k=0; k<sp.length; k++) {
						if (sp[k].trim().length() == 0) continue;
						final String ss = sp[k].trim();
						if (ss.equals("10baseT/Half"))
							stat.setSupportedLinkModes(InterfaceStatisticsStatic.SUPPORTED_10BaseT_Half);
						if (ss.equals("10baseT/Full"))
							stat.setSupportedLinkModes(InterfaceStatisticsStatic.SUPPORTED_10BaseT_Full);
						if (ss.equals("100baseT/Half"))
							stat.setSupportedLinkModes(InterfaceStatisticsStatic.SUPPORTED_100BaseT_Half);
						if (ss.equals("100baseT/Full"))
							stat.setSupportedLinkModes(InterfaceStatisticsStatic.SUPPORTED_100BaseT_Full);
						if (ss.equals("1000baseT/Half"))
							stat.setSupportedLinkModes(InterfaceStatisticsStatic.SUPPORTED_1000BaseT_Half);
						if (ss.equals("1000baseT/Full"))
							stat.setSupportedLinkModes(InterfaceStatisticsStatic.SUPPORTED_1000BaseT_Full);
					}
				}
			}
			pattern = PatternUtil.getPattern("supportsAutoNegotiation", supportsAutoNegotiation);
			matcher = pattern.matcher(ret);
			if (matcher.find()) {
				final String mm = matcher.group(1);
				if (mm.trim().length() != 0) {
					if (mm.trim().equals("Yes"))
						stat.setSupportsAutoNegotiation(true);
				}
			}
			pattern = PatternUtil.getPattern("linkSpeed",  speed);
			matcher = pattern.matcher(ret);
			if (matcher.find()) {
				final String mm = matcher.group(1);
				if (mm != null && mm.trim().length() != 0) {
					final String ss = mm.trim();
					if (ss.equals("10Mb/s"))
						stat.setMaxSpeed(10);
					else if (ss.equals("100Mb/s"))
						stat.setMaxSpeed(100);
					else if (ss.equals("1000Mb/s"))
						stat.setMaxSpeed(1000);
					else if (ss.equals("10000Mb/s"))
						stat.setMaxSpeed(10000);
					else if (ss.startsWith("Unknown")) {
						final String sp = ss.substring(ss.indexOf('(')+1, ss.lastIndexOf(')'));
						try { stat.setMaxSpeed(Integer.parseInt(sp)); } catch (Exception ex) {
							logger.warning("Can not determine speed "+ss+" for "+ifName);
						}
					}
				}
			}
			pattern = PatternUtil.getPattern("linkDuplex", duplex);
			matcher = pattern.matcher(ret);
			if (matcher.find()) {
				final String mm = matcher.group(1);
				if (mm != null && mm.trim().length() != 0) {
					final String ss = mm.trim();
					if (ss.compareToIgnoreCase("Half") == 0) {
						stat.setDuplex(InterfaceStatisticsStatic.LINK_HALF);
					} else if (ss.compareToIgnoreCase("Full") == 0) {
						stat.setDuplex(InterfaceStatisticsStatic.LINK_DUPLEX);
					}
				}
			}
			pattern = PatternUtil.getPattern("linkPort", port);
			matcher = pattern.matcher(ret);
			if (matcher.find()) {
				final String mm = matcher.group(1);
				if (mm != null && mm.trim().length() != 0) {
					final String ss = mm.trim();
					if (ss.compareTo("Twisted Pair") == 0) {
						stat.setPort(InterfaceStatisticsStatic.PORT_TP);
					} else if (ss.compareToIgnoreCase("FIBRE") == 0) {
						stat.setPort(InterfaceStatisticsStatic.PORT_FIBRE);
					} else if (ss.compareToIgnoreCase("AUI") == 0) {
						stat.setPort(InterfaceStatisticsStatic.PORT_AUI);
					} else if (ss.compareToIgnoreCase("BNC") == 0) {
						stat.setPort(InterfaceStatisticsStatic.PORT_BNC);
					} else if (ss.compareToIgnoreCase("MII") == 0) {
						stat.setPort(InterfaceStatisticsStatic.PORT_MII);
					}
				}
			}
		}
	}

	
	private final void getFromProcDev() {
		ifArray.clear();
		final Pattern pattern = PatternUtil.getPattern("/proc/net/dev", netDevPattern);
		try {
			BufferedReader reader = new BufferedReader(new FileReader("/proc/net/dev"));
			String line;
			while ((line = reader.readLine()) != null) {
				final Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					final String ifName = matcher.group(1);
					ifArray.add(ifName);
					if (!ifs.containsKey(ifName)) {
						ifs.put(ifName, new InterfaceStatistics(ifName));
					} else
						ifs.get(ifName).updateTime();
				}
			}
			reader.close();
		} catch (Exception e) { }
		
		
		toRemove.clear();
		for (Iterator it = ifs.keySet().iterator(); it.hasNext(); ) {
			String ifn = (String)it.next();
			if (!ifArray.contains(ifn)) toRemove.add(ifn);
		}
		for (String ifn : toRemove) {
			ifs.remove(ifn);
		}
	}
	
	private final boolean compare(InterfaceStatisticsStatic olds, InterfaceStatisticsStatic news) {
		if (olds.getEncap() != news.getEncap()) return false;
		if (olds.getHwAddr() == null && news.getHwAddr() != null) return false;
		if (olds.getHwAddr() != null && news.getHwAddr() == null) return false;
		if (olds.getHwAddr() != null && news.getHwAddr() != null)
			if (!olds.getHwAddr().equals(news.getHwAddr())) return false;
		if (olds.getSupportedPorts() != news.getSupportedPorts()) return false;
		if (olds.getSupportedLinkModes() != news.getSupportedLinkModes()) return false;
		if (olds.supportsAutoNegotiation() != news.supportsAutoNegotiation()) return false;
		if (olds.getMaxSpeed() != news.getMaxSpeed()) return false;
		if (olds.getDuplex() != news.getDuplex()) return false;
		if (olds.getPort() != news.getPort()) return false;
		return true;
	}
	
	
	public final void check() {
		checkIfConfig();
		if (ifs.size() == 0) { 
			getFromProcDev();
		}
		if (ifs.size() == 0) return; 
		if (shouldRunEthTool())
			checkEthtool();
		
		final LinkedList<String> names = new LinkedList<String>();
		for (Map.Entry<String, InterfaceStatisticsStatic> entry : lastGoodStatic.entrySet()) {
			names.addLast(entry.getKey());
		}
		for (String name : names) {
			if (!staticifs.containsKey(name))
				lastGoodStatic.remove(name);
		}
		names.clear();
		for (Map.Entry<String, InterfaceStatisticsStatic> entry : staticifs.entrySet()) {
			names.addLast(entry.getKey());
		}			
		for (String name : names) {
			if (!lastGoodStatic.containsKey(name)) {
				lastGoodStatic.put(name, staticifs.get(name));
				continue;
			}
			final InterfaceStatisticsStatic olds = lastGoodStatic.get(name);
			final InterfaceStatisticsStatic news = staticifs.get(name);
			
			if (!compare(olds, news)) {
				lastGoodStatic.put(name, news);
			} else {
				staticifs.remove(name);
			}
		}
	}
	
	public final HashMap<String, InterfaceStatistics> getIfStatistics() {
		return ifs;
	}
	
	public final HashMap<String, InterfaceStatisticsStatic> getOldIfStatisticsStatic() {
		return lastGoodStatic;
	}
	
	public final HashMap<String, InterfaceStatisticsStatic> getIfStatisticsStatic() {
		return staticifs;
	}

	public String diffWithOverflowCheck(String newVal, String oldVal) throws NumberFormatException {
		if (is64BitArch) {
			String str = prepareString(newVal);
			BigDecimal newv = null;
			try {
				newv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception " + t + " for " + str);
			}
			str = prepareString(oldVal);
			BigDecimal oldv = null;
			try {
				oldv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception " + t + " for " + str);
			}
			if (newv.compareTo(oldv) >= 0)
				return newv.subtract(oldv).toString();
			BigInteger overflow = new BigInteger("1").shiftLeft(64);
			BigDecimal d = new BigDecimal(overflow.toString());
			return newv.add(d).subtract(oldv).toString();
		}
		
		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return diffWithOverflowCheck(newVal, oldVal);
		}
		
		if (newv >= oldv) {
			return "" + (newv - oldv);
		}
		long vmax = 1L << 32; 
		return "" + (newv - oldv + vmax);
	}

	public String divideWithOverflowCheck(String newVal, String oldVal) throws NumberFormatException {

		if (is64BitArch) {
			String str = prepareString(newVal);
			BigDecimal newv = null;
			try {
				newv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception " + t + " for " + str);
			}
			str = prepareString(oldVal);
			BigDecimal oldv = null;
			try {
				oldv = new BigDecimal(str);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Got exception " + t + " for " + str);
			}
			return newv.divide(oldv, BigDecimal.ROUND_FLOOR).toString();
		}
		
		double toCompare = 1L << 32;
		double newv = Double.parseDouble(newVal);
		double oldv = Double.parseDouble(oldVal);
		if (newv >= toCompare || oldv >= toCompare) {
			is64BitArch = true;
			return divideWithOverflowCheck(newVal, oldVal);
		}
		
		return "" + (newv / oldv);
	}
	
	private final String prepareString(String str) {
		
		
		try {
			double d = Double.parseDouble(str);
			if (!Double.isInfinite(d) && !Double.isNaN(d)) {
				String n = nf.format(d);
				n = n.replaceAll(",", "");
				return n;
			}
		} catch (Throwable t) { }
		
		if (!str.contains(".")) {
			return str+".0000";
		}
		int nr = str.lastIndexOf('.')+1;
		nr = str.length() - nr;
		for (int i=nr; i<4; i++)
			str += "0";
		return str;
	}
	
	
	public void clear() {
		ifs.clear();
		staticifs.clear();
	}
	
	public static void main(String args[]) {
		
		String line = "Speed: Unknown (100000)\nTest\n";
		Pattern pattern = PatternUtil.getPattern("supportedLinkModes", speed);
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			System.out.println(matcher.group(1));
		} else
			System.out.println("no match");
	}

} 

