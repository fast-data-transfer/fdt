
package ch.ethz.ssh2.channel;

/**
 * RemoteForwardingData. Data about a requested remote forwarding.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: RemoteForwardingData.java,v 1.1 2005/12/07 10:25:48 cplattne Exp $
 */
public class RemoteForwardingData
{
	public String bindAddress;
	public int bindPort;

	String targetAddress;
	int targetPort;
}
