
package ch.ethz.ssh2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.ethz.ssh2.channel.Channel;
import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.LocalAcceptThread;


public class LocalStreamForwarder
{
	ChannelManager cm;

	String host_to_connect;
	int port_to_connect;
	LocalAcceptThread lat;

	Channel cn;

	LocalStreamForwarder(ChannelManager cm, String host_to_connect, int port_to_connect) throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		cn = cm.openDirectTCPIPChannel(host_to_connect, port_to_connect, "127.0.0.1", 0);
	}

	
	public InputStream getInputStream() throws IOException
	{
		return cn.getStdoutStream();
	}

	
	public OutputStream getOutputStream() throws IOException
	{
		return cn.getStdinStream();
	}

	
	public void close() throws IOException
	{
		cm.closeChannel(cn, "Closed due to user request.", true);
	}
}
