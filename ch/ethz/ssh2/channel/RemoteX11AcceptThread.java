
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import ch.ethz.ssh2.log.Logger;


public class RemoteX11AcceptThread extends Thread
{
	private static final Logger log = Logger.getLogger(RemoteX11AcceptThread.class);

	Channel c;

	String remoteOriginatorAddress;
	int remoteOriginatorPort;

	Socket s;

	public RemoteX11AcceptThread(Channel c, String remoteOriginatorAddress, int remoteOriginatorPort)
	{
		this.c = c;
		this.remoteOriginatorAddress = remoteOriginatorAddress;
		this.remoteOriginatorPort = remoteOriginatorPort;
	}

	public void run()
	{
		try
		{
			

			c.cm.sendOpenConfirmation(c);

			

			OutputStream remote_os = c.getStdinStream();
			InputStream remote_is = c.getStdoutStream();

			

			

			

			byte[] header = new byte[6];

			if (remote_is.read(header) != 6)
				throw new IOException("Unexpected EOF on X11 startup!");

			if ((header[0] != 0x42) && (header[0] != 0x6c)) 
				throw new IOException("Unknown endian format in X11 message!");

			
			
			int idxMSB = (header[0] == 0x42) ? 0 : 1;

			

			byte[] auth_buff = new byte[6];

			if (remote_is.read(auth_buff) != 6)
				throw new IOException("Unexpected EOF on X11 startup!");

			int authProtocolNameLength = ((auth_buff[idxMSB] & 0xff) << 8) | (auth_buff[1 - idxMSB] & 0xff);
			int authProtocolDataLength = ((auth_buff[2 + idxMSB] & 0xff) << 8) | (auth_buff[3 - idxMSB] & 0xff);

			if ((authProtocolNameLength > 256) || (authProtocolDataLength > 256))
				throw new IOException("Buggy X11 authorization data");

			int authProtocolNamePadding = ((4 - (authProtocolNameLength % 4)) % 4);
			int authProtocolDataPadding = ((4 - (authProtocolDataLength % 4)) % 4);

			byte[] authProtocolName = new byte[authProtocolNameLength];
			byte[] authProtocolData = new byte[authProtocolDataLength];

			byte[] paddingBuffer = new byte[4];

			if (remote_is.read(authProtocolName) != authProtocolNameLength)
				throw new IOException("Unexpected EOF on X11 startup! (authProtocolName)");

			if (remote_is.read(paddingBuffer, 0, authProtocolNamePadding) != authProtocolNamePadding)
				throw new IOException("Unexpected EOF on X11 startup! (authProtocolNamePadding)");

			if (remote_is.read(authProtocolData) != authProtocolDataLength)
				throw new IOException("Unexpected EOF on X11 startup! (authProtocolData)");

			if (remote_is.read(paddingBuffer, 0, authProtocolDataPadding) != authProtocolDataPadding)
				throw new IOException("Unexpected EOF on X11 startup! (authProtocolDataPadding)");

			if ("MIT-MAGIC-COOKIE-1".equals(new String(authProtocolName)) == false)
				throw new IOException("Unknown X11 authorization protocol!");

			if (authProtocolDataLength != 16)
				throw new IOException("Wrong data length for X11 authorization data!");

			StringBuffer tmp = new StringBuffer(32);
			for (int i = 0; i < authProtocolData.length; i++)
			{
				String digit2 = Integer.toHexString(authProtocolData[i] & 0xff);
				tmp.append((digit2.length() == 2) ? digit2 : "0" + digit2);
			}
			String hexEncodedFakeCookie = tmp.toString();

			

			synchronized (c)
			{
				
				c.hexX11FakeCookie = hexEncodedFakeCookie;
			}

			

			X11ServerData sd = c.cm.checkX11Cookie(hexEncodedFakeCookie);

			if (sd == null)
				throw new IOException("Invalid X11 cookie received.");

			

			s = new Socket(sd.hostname, sd.port);

			OutputStream x11_os = s.getOutputStream();
			InputStream x11_is = s.getInputStream();

			

			x11_os.write(header);

			if (sd.x11_magic_cookie == null)
			{
				byte[] emptyAuthData = new byte[6];
				
				x11_os.write(emptyAuthData);
			}
			else
			{
				if (sd.x11_magic_cookie.length != 16)
					throw new IOException("The real X11 cookie has an invalid length!");

				
				x11_os.write(auth_buff);
				x11_os.write(authProtocolName); 
				x11_os.write(paddingBuffer, 0, authProtocolNamePadding);
				x11_os.write(sd.x11_magic_cookie);
				x11_os.write(paddingBuffer, 0, authProtocolDataPadding);
			}

			x11_os.flush();

			

			StreamForwarder r2l = new StreamForwarder(c, null, null, remote_is, x11_os, "RemoteToX11");
			StreamForwarder l2r = new StreamForwarder(c, null, null, x11_is, remote_os, "X11ToRemote");

			

			r2l.setDaemon(true);
			r2l.start();
			l2r.run();

			while (r2l.isAlive())
			{
				try
				{
					r2l.join();
				}
				catch (InterruptedException e)
				{
				}
			}

			

			c.cm.closeChannel(c, "EOF on both X11 streams reached.", true);
			s.close();
		}
		catch (IOException e)
		{
			log.log(50, "IOException in X11 proxy code: " + e.getMessage());

			try
			{
				c.cm.closeChannel(c, "IOException in X11 proxy code (" + e.getMessage() + ")", true);
			}
			catch (IOException e1)
			{
			}
			try
			{
				if (s != null)
					s.close();
			}
			catch (IOException e1)
			{
			}
		}
	}
}
