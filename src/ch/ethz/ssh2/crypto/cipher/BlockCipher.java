package ch.ethz.ssh2.crypto.cipher;

/**
 * BlockCipher.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: BlockCipher.java,v 1.1 2005/05/26 14:53:27 cplattne Exp $
 */
public interface BlockCipher
{
	public void init(boolean forEncryption, byte[] key);

	public int getBlockSize();

	public void transformBlock(byte[] src, int srcoff, byte[] dst, int dstoff);
}
