package ch.ethz.ssh2.transport;

import java.io.IOException;

/**
 * MessageHandler.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: MessageHandler.java,v 1.1 2005/05/26 14:53:29 cplattne Exp $
 */
public interface MessageHandler
{
	public void handleMessage(byte[] msg, int msglen) throws IOException;
}
