package ch.ethz.ssh2.transport;

import java.io.IOException;


public interface MessageHandler
{
	public void handleMessage(byte[] msg, int msglen) throws IOException;
}
