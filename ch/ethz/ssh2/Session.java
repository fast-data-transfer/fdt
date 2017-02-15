
package ch.ethz.ssh2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import ch.ethz.ssh2.channel.Channel;
import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.X11ServerData;


public class Session
{
	ChannelManager cm;
	Channel cn;

	boolean flag_pty_requested = false;
	boolean flag_x11_requested = false;
	boolean flag_execution_started = false;
	boolean flag_closed = false;

	String x11FakeCookie = null;

	final SecureRandom rnd;
	
	Session(ChannelManager cm, SecureRandom rnd) throws IOException
	{
		this.cm = cm;
		this.cn = cm.openSessionChannel();
		this.rnd = rnd;
	}

	
	public void requestDumbPTY() throws IOException
	{
		requestPTY("dumb", 0, 0, 0, 0, null);
	}

	
	public void requestPTY(String term) throws IOException
	{
		requestPTY(term, 0, 0, 0, 0, null);
	}

	
	public void requestPTY(String term, int term_width_characters, int term_height_characters, int term_width_pixels,
			int term_height_pixels, byte[] terminal_modes) throws IOException
	{
		if (term == null)
			throw new IllegalArgumentException("TERM cannot be null.");

		if ((terminal_modes != null) && (terminal_modes.length > 0))
		{
			if (terminal_modes[terminal_modes.length - 1] != 0)
				throw new IOException("Illegal terminal modes description, does not end in zero byte");
		}
		else
			terminal_modes = new byte[] { 0 };

		synchronized (this)
		{
			
			if (flag_closed)
				throw new IOException("This session is closed.");

			if (flag_pty_requested)
				throw new IOException("A PTY was already requested.");

			if (flag_execution_started)
				throw new IOException(
						"Cannot request PTY at this stage anymore, a remote execution has already started.");

			flag_pty_requested = true;
		}

		cm.requestPTY(cn, term, term_width_characters, term_height_characters, term_width_pixels, term_height_pixels,
				terminal_modes);
	}

	
	public void requestX11Forwarding(String hostname, int port, byte[] cookie, boolean singleConnection)
			throws IOException
	{
		if (hostname == null)
			throw new IllegalArgumentException("hostname argument may not be null");

		synchronized (this)
		{
			
			if (flag_closed)
				throw new IOException("This session is closed.");

			if (flag_x11_requested)
				throw new IOException("X11 forwarding was already requested.");

			if (flag_execution_started)
				throw new IOException(
						"Cannot request X11 forwarding at this stage anymore, a remote execution has already started.");

			flag_x11_requested = true;
		}

		

		X11ServerData x11data = new X11ServerData();

		x11data.hostname = hostname;
		x11data.port = port;
		x11data.x11_magic_cookie = cookie; 

		

		byte[] fakeCookie = new byte[16];
		String hexEncodedFakeCookie;

		

		while (true)
		{
			rnd.nextBytes(fakeCookie);

			

			StringBuffer tmp = new StringBuffer(32);
			for (int i = 0; i < fakeCookie.length; i++)
			{
				String digit2 = Integer.toHexString(fakeCookie[i] & 0xff);
				tmp.append((digit2.length() == 2) ? digit2 : "0" + digit2);
			}
			hexEncodedFakeCookie = tmp.toString();

			

			if (cm.checkX11Cookie(hexEncodedFakeCookie) == null)
				break;
		}

		

		cm.requestX11(cn, singleConnection, "MIT-MAGIC-COOKIE-1", hexEncodedFakeCookie, 0);

		
		

		synchronized (this)
		{
			if (flag_closed == false)
			{
				this.x11FakeCookie = hexEncodedFakeCookie;
				cm.registerX11Cookie(hexEncodedFakeCookie, x11data);
			}
		}

		
	}

	
	public void execCommand(String cmd) throws IOException
	{
		if (cmd == null)
			throw new IllegalArgumentException("cmd argument may not be null");

		synchronized (this)
		{
			
			if (flag_closed)
				throw new IOException("This session is closed.");

			if (flag_execution_started)
				throw new IOException("A remote execution has already started.");

			flag_execution_started = true;
		}

		cm.requestExecCommand(cn, cmd);
	}

	
	public void startShell() throws IOException
	{
		synchronized (this)
		{
			
			if (flag_closed)
				throw new IOException("This session is closed.");

			if (flag_execution_started)
				throw new IOException("A remote execution has already started.");

			flag_execution_started = true;
		}

		cm.requestShell(cn);
	}

	
	public void startSubSystem(String name) throws IOException
	{
		if (name == null)
			throw new IllegalArgumentException("name argument may not be null");

		synchronized (this)
		{
			
			if (flag_closed)
				throw new IOException("This session is closed.");

			if (flag_execution_started)
				throw new IOException("A remote execution has already started.");

			flag_execution_started = true;
		}

		cm.requestSubSystem(cn, name);
	}

	public InputStream getStdout()
	{
		return cn.getStdoutStream();
	}

	public InputStream getStderr()
	{
		return cn.getStderrStream();
	}

	public OutputStream getStdin()
	{
		return cn.getStdinStream();
	}

	
	public int waitUntilDataAvailable(long timeout) throws IOException
	{
		if (timeout < 0)
			throw new IllegalArgumentException("timeout must not be negative!");

		int conditions = cm.waitForCondition(cn, timeout, ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA
				| ChannelCondition.EOF);

		if ((conditions & ChannelCondition.TIMEOUT) != 0)
			return -1;

		if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) != 0)
			return 1;

		

		if ((conditions & ChannelCondition.EOF) != 0)
			return 0;

		throw new IllegalStateException("Unexpected condition result (" + conditions + ")");
	}

	

	public int waitForCondition(int condition_set, long timeout)
	{
		if (timeout < 0)
			throw new IllegalArgumentException("timeout must be non-negative!");

		return cm.waitForCondition(cn, timeout, condition_set);
	}

	
	public Integer getExitStatus()
	{
		return cn.getExitStatus();
	}

	
	public String getExitSignal()
	{
		return cn.getExitSignal();
	}

	
	public void close()
	{
		synchronized (this)
		{
			if (flag_closed)
				return;

			flag_closed = true;

			if (x11FakeCookie != null)
				cm.unRegisterX11Cookie(x11FakeCookie, true);

			try
			{
				cm.closeChannel(cn, "Closed due to user request", true);
			}
			catch (IOException ignored)
			{
			}
		}
	}
}
