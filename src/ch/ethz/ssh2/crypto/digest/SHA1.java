
package ch.ethz.ssh2.crypto.digest;

/**
 * 
 * SHA-1 implementation based on FIPS PUB 180-1.
 * 
 * (http://www.itl.nist.gov/fipspubs/fip180-1.htm)
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: SHA1.java,v 1.4 2006/02/02 09:11:03 cplattne Exp $
 */
public final class SHA1 implements Digest
{
	private int H0, H1, H2, H3, H4;

	private final byte msg[] = new byte[64];
	private final int[] w = new int[80];
	private int currentPos;
	private long currentLen;

	public SHA1()
	{
		reset();
	}

	public final int getDigestLength()
	{
		return 20;
	}

	public final void reset()
	{
		H0 = 0x67452301;
		H1 = 0xEFCDAB89;
		H2 = 0x98BADCFE;
		H3 = 0x10325476;
		H4 = 0xC3D2E1F0;

		currentPos = 0;
		currentLen = 0;
	}

	public final void update(byte b[], int off, int len)
	{
		for (int i = off; i < (off + len); i++)
			update(b[i]);
	}

	public final void update(byte b[])
	{
		for (int i = 0; i < b.length; i++)
			update(b[i]);
	}

	public final void update(byte b)
	{
		// System.out.println(pos + "->" + b);
		msg[currentPos++] = b;
		currentLen += 8;
		if (currentPos == 64)
		{
			perform();
			currentPos = 0;
		}
	}

	private static final String toHexString(byte[] b)
	{
		final String hexChar = "0123456789ABCDEF";

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
		{
			sb.append(hexChar.charAt((b[i] >> 4) & 0x0f));
			sb.append(hexChar.charAt(b[i] & 0x0f));
		}
		return sb.toString();
	}

	private final void putInt(byte[] b, int pos, int val)
	{
		b[pos] = (byte) (val >> 24);
		b[pos + 1] = (byte) (val >> 16);
		b[pos + 2] = (byte) (val >> 8);
		b[pos + 3] = (byte) val;
	}

	public final void digest(byte[] out)
	{
		digest(out, 0);
	}

	public final void digest(byte[] out, int off)
	{
		long l = currentLen;

		update((byte) 0x80);

		// padding could be done more efficiently...
		while (currentPos != 56)
			update((byte) 0);

		update((byte) (l >> 56));
		update((byte) (l >> 48));
		update((byte) (l >> 40));
		update((byte) (l >> 32));

		update((byte) (l >> 24));
		update((byte) (l >> 16));
		update((byte) (l >> 8));
		update((byte) (l));

		// debug(80, H0, H1, H2, H3, H4);

		putInt(out, off, H0);
		putInt(out, off + 4, H1);
		putInt(out, off + 8, H2);
		putInt(out, off + 12, H3);
		putInt(out, off + 16, H4);

		reset();
	}

	/*
	 * private void debug(int t, int A, int B, int C, int D, int E) {
	 * System.out.println(t + ": " + Integer.toHexString(A).toUpperCase() + ", " +
	 * Integer.toHexString(B).toUpperCase() + ", " +
	 * Integer.toHexString(C).toUpperCase() + "," +
	 * Integer.toHexString(D).toUpperCase() + ", " +
	 * Integer.toHexString(E).toUpperCase()); }
	 */
	private final void perform()
	{
		for (int i = 0; i < 16; i++)
			w[i] = ((msg[i * 4] & 0xff) << 24) | ((msg[i * 4 + 1] & 0xff) << 16) | ((msg[i * 4 + 2] & 0xff) << 8)
					| ((msg[i * 4 + 3] & 0xff));

		for (int t = 16; t < 80; t++)
		{
			int x = w[t - 3] ^ w[t - 8] ^ w[t - 14] ^ w[t - 16];
			w[t] = ((x << 1) | (x >>> 31));
		}

		int A = H0;
		int B = H1;
		int C = H2;
		int D = H3;
		int E = H4;

		int T;

		for (int t = 0; t <= 19; t++)
		{
			T = ((A << 5) | (A >>> 27)) + ((B & C) | ((~B) & D)) + E + w[t] + 0x5A827999;
			E = D;
			D = C;
			C = ((B << 30) | (B >>> 2));
			B = A;
			A = T;
			// debug(t, A, B, C, D, E);
		}

		for (int t = 20; t <= 39; t++)
		{
			T = ((A << 5) | (A >>> 27)) + (B ^ C ^ D) + E + w[t] + 0x6ED9EBA1;
			E = D;
			D = C;
			C = ((B << 30) | (B >>> 2));
			B = A;
			A = T;
			// debug(t, A, B, C, D, E);
		}

		for (int t = 40; t <= 59; t++)
		{
			T = ((A << 5) | (A >>> 27)) + ((B & C) | (B & D) | (C & D)) + E + w[t] + 0x8F1BBCDC;
			E = D;
			D = C;
			C = ((B << 30) | (B >>> 2));
			B = A;
			A = T;
			// debug(t, A, B, C, D, E);
		}

		for (int t = 60; t <= 79; t++)
		{
			T = ((A << 5) | (A >>> 27)) + (B ^ C ^ D) + E + w[t] + 0xCA62C1D6;
			E = D;
			D = C;
			C = ((B << 30) | (B >>> 2));
			B = A;
			A = T;
			// debug(t, A, B, C, D, E);
		}

		H0 = H0 + A;
		H1 = H1 + B;
		H2 = H2 + C;
		H3 = H3 + D;
		H4 = H4 + E;

		// debug(80, H0, H1, H2, H3, H4);
	}

	public static void main(String[] args)
	{
		SHA1 sha = new SHA1();

		byte[] dig1 = new byte[20];
		byte[] dig2 = new byte[20];
		byte[] dig3 = new byte[20];

		/*
		 * We do not specify a charset name for getBytes(), since we assume that
		 * the JVM's default encoder maps the _used_ ASCII characters exactly as
		 * getBytes("US-ASCII") would do. (Ah, yes, too lazy to catch the
		 * exception that can be thrown by getBytes("US-ASCII")). Note: This has
		 * no effect on the SHA-1 implementation, this is just for the following
		 * test code.
		 */

		sha.update("abc".getBytes());
		sha.digest(dig1);

		sha.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes());
		sha.digest(dig2);

		for (int i = 0; i < 1000000; i++)
			sha.update((byte) 'a');
		sha.digest(dig3);

		String dig1_res = toHexString(dig1);
		String dig2_res = toHexString(dig2);
		String dig3_res = toHexString(dig3);

		String dig1_ref = "A9993E364706816ABA3E25717850C26C9CD0D89D";
		String dig2_ref = "84983E441C3BD26EBAAE4AA1F95129E5E54670F1";
		String dig3_ref = "34AA973CD4C4DAA4F61EEB2BDBAD27316534016F";

		if (dig1_res.equals(dig1_ref))
			System.out.println("SHA-1 Test 1 OK.");
		else
			System.out.println("SHA-1 Test 1 FAILED.");

		if (dig2_res.equals(dig2_ref))
			System.out.println("SHA-1 Test 2 OK.");
		else
			System.out.println("SHA-1 Test 2 FAILED.");

		if (dig3_res.equals(dig3_ref))
			System.out.println("SHA-1 Test 3 OK.");
		else
			System.out.println("SHA-1 Test 3 FAILED.");

	}
}
