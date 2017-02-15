
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class LocalAcceptThread extends Thread implements IChannelWorkerThread
{
	ChannelManager cm;
	int local_port;
	String host_to_connect;
	int port_to_connect;

	final ServerSocket ss;

	public LocalAcceptThread(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect)
			throws IOException
	{
		this.cm = cm;
		this.local_port = local_port;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		ss = new ServerSocket(local_port);
	}

	public void run()
	{
		try
		{
			cm.registerThread(this);
		}
		catch (IOException e)
		{
			stopWorking();
			return;
		}

		while (true)
		{
			Socket s = null;

			try
			{
				s = ss.accept();
			}
			catch (IOException e)
			{
				stopWorking();
				return;
			}

			Channel cn = null;
			StreamForwarder r2l = null;
			StreamForwarder l2r = null;

			try
			{
				

				cn = cm.openDirectTCPIPChannel(host_to_connect, port_to_connect, s.getInetAddress().getHostAddress(), s
						.getPort());

			}
			catch (IOException e)
			{
				

				try
				{
					s.close();
				}
				catch (IOException ignore)
				{
				}

				continue;
			}

			try
			{
				r2l = new StreamForwarder(cn, null, null, cn.stdoutStream, s.getOutputStream(), "RemoteToLocal");
				l2r = new StreamForwarder(cn, r2l, s, s.getInputStream(), cn.stdinStream, "LocalToRemote");
			}
			catch (IOException e)
			{
				try
				{
					
					cn.cm.closeChannel(cn, "Weird error during creation of StreamForwarder (" + e.getMessage() + ")",
							true);
				}
				catch (IOException ignore)
				{
				}

				continue;
			}

			r2l.setDaemon(true);
			l2r.setDaemon(true);
			r2l.start();
			l2r.start();
		}
	}

	public void stopWorking()
	{
		try
		{
			
			ss.close();
		}
		catch (IOException e)
		{
		}
	}
}
