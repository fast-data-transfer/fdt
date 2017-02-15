
package ch.ethz.ssh2.crypto.digest;

/**
 * Digest.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: Digest.java,v 1.2 2005/08/11 12:47:29 cplattne Exp $
 */
public interface Digest
{
	public int getDigestLength();

	public void update(byte b);

	public void update(byte[] b);

	public void update(byte b[], int off, int len);

	public void reset();

	public void digest(byte[] out);

	public void digest(byte[] out, int off);
}
