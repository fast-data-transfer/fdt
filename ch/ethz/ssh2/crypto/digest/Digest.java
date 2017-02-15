
package ch.ethz.ssh2.crypto.digest;


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
