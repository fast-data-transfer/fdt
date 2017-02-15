
package ch.ethz.ssh2.crypto.cipher;




public class DESede extends DES
{
	private int[] key1 = null;
	private int[] key2 = null;
	private int[] key3 = null;

	private boolean encrypt;

	
	public DESede()
	{
	}

	
	public void init(boolean encrypting, byte[] key)
	{
		key1 = generateWorkingKey(encrypting, key, 0);
		key2 = generateWorkingKey(!encrypting, key, 8);
		key3 = generateWorkingKey(encrypting, key, 16);

		encrypt = encrypting;
	}

	public String getAlgorithmName()
	{
		return "DESede";
	}

	public int getBlockSize()
	{
		return 8;
	}

	public void transformBlock(byte[] in, int inOff, byte[] out, int outOff)
	{
		if (key1 == null)
		{
			throw new IllegalStateException("DESede engine not initialised!");
		}

		if (encrypt)
		{
			desFunc(key1, in, inOff, out, outOff);
			desFunc(key2, out, outOff, out, outOff);
			desFunc(key3, out, outOff, out, outOff);
		}
		else
		{
			desFunc(key3, in, inOff, out, outOff);
			desFunc(key2, out, outOff, out, outOff);
			desFunc(key1, out, outOff, out, outOff);
		}
	}

	public void reset()
	{
	}
}
