
package ch.ethz.ssh2.crypto.digest;





public final class MD5 implements Digest
{
	private int state0, state1, state2, state3;
	private long count;
	private final byte[] block = new byte[64];
	private final int x[] = new int[16];

	private static final byte[] padding = new byte[] { (byte) 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	public MD5()
	{
		reset();
	}

	private static final int FF(int a, int b, int c, int d, int x, int s, int ac)
	{
		a += ((b & c) | ((~b) & d)) + x + ac;
		return ((a << s) | (a >>> (32 - s))) + b;
	}

	private static final int GG(int a, int b, int c, int d, int x, int s, int ac)
	{
		a += ((b & d) | (c & (~d))) + x + ac;
		return ((a << s) | (a >>> (32 - s))) + b;
	}

	private static final int HH(int a, int b, int c, int d, int x, int s, int ac)
	{
		a += (b ^ c ^ d) + x + ac;
		return ((a << s) | (a >>> (32 - s))) + b;
	}

	private static final int II(int a, int b, int c, int d, int x, int s, int ac)
	{
		a += (c ^ (b | (~d))) + x + ac;
		return ((a << s) | (a >>> (32 - s))) + b;
	}

	private static final void encode(byte[] dst, int dstoff, int word)
	{
		dst[dstoff] = (byte) (word);
		dst[dstoff + 1] = (byte) (word >> 8);
		dst[dstoff + 2] = (byte) (word >> 16);
		dst[dstoff + 3] = (byte) (word >> 24);
	}

	private final void transform(byte[] src, int pos)
	{
		int a = state0;
		int b = state1;
		int c = state2;
		int d = state3;

		for (int i = 0; i < 16; i++, pos += 4)
		{
			x[i] = (src[pos] & 0xff) | ((src[pos + 1] & 0xff) << 8) | ((src[pos + 2] & 0xff) << 16)
					| ((src[pos + 3] & 0xff) << 24);
		}

		

		a = FF(a, b, c, d, x[0], 7, 0xd76aa478); 
		d = FF(d, a, b, c, x[1], 12, 0xe8c7b756); 
		c = FF(c, d, a, b, x[2], 17, 0x242070db); 
		b = FF(b, c, d, a, x[3], 22, 0xc1bdceee); 
		a = FF(a, b, c, d, x[4], 7, 0xf57c0faf); 
		d = FF(d, a, b, c, x[5], 12, 0x4787c62a); 
		c = FF(c, d, a, b, x[6], 17, 0xa8304613); 
		b = FF(b, c, d, a, x[7], 22, 0xfd469501); 
		a = FF(a, b, c, d, x[8], 7, 0x698098d8); 
		d = FF(d, a, b, c, x[9], 12, 0x8b44f7af); 
		c = FF(c, d, a, b, x[10], 17, 0xffff5bb1); 
		b = FF(b, c, d, a, x[11], 22, 0x895cd7be); 
		a = FF(a, b, c, d, x[12], 7, 0x6b901122); 
		d = FF(d, a, b, c, x[13], 12, 0xfd987193); 
		c = FF(c, d, a, b, x[14], 17, 0xa679438e); 
		b = FF(b, c, d, a, x[15], 22, 0x49b40821); 

		
		a = GG(a, b, c, d, x[1], 5, 0xf61e2562); 
		d = GG(d, a, b, c, x[6], 9, 0xc040b340); 
		c = GG(c, d, a, b, x[11], 14, 0x265e5a51); 
		b = GG(b, c, d, a, x[0], 20, 0xe9b6c7aa); 
		a = GG(a, b, c, d, x[5], 5, 0xd62f105d); 
		d = GG(d, a, b, c, x[10], 9, 0x2441453); 
		c = GG(c, d, a, b, x[15], 14, 0xd8a1e681); 
		b = GG(b, c, d, a, x[4], 20, 0xe7d3fbc8); 
		a = GG(a, b, c, d, x[9], 5, 0x21e1cde6); 
		d = GG(d, a, b, c, x[14], 9, 0xc33707d6); 
		c = GG(c, d, a, b, x[3], 14, 0xf4d50d87); 
		b = GG(b, c, d, a, x[8], 20, 0x455a14ed); 
		a = GG(a, b, c, d, x[13], 5, 0xa9e3e905); 
		d = GG(d, a, b, c, x[2], 9, 0xfcefa3f8); 
		c = GG(c, d, a, b, x[7], 14, 0x676f02d9); 
		b = GG(b, c, d, a, x[12], 20, 0x8d2a4c8a); 

		
		a = HH(a, b, c, d, x[5], 4, 0xfffa3942); 
		d = HH(d, a, b, c, x[8], 11, 0x8771f681); 
		c = HH(c, d, a, b, x[11], 16, 0x6d9d6122); 
		b = HH(b, c, d, a, x[14], 23, 0xfde5380c); 
		a = HH(a, b, c, d, x[1], 4, 0xa4beea44); 
		d = HH(d, a, b, c, x[4], 11, 0x4bdecfa9); 
		c = HH(c, d, a, b, x[7], 16, 0xf6bb4b60); 
		b = HH(b, c, d, a, x[10], 23, 0xbebfbc70); 
		a = HH(a, b, c, d, x[13], 4, 0x289b7ec6); 
		d = HH(d, a, b, c, x[0], 11, 0xeaa127fa); 
		c = HH(c, d, a, b, x[3], 16, 0xd4ef3085); 
		b = HH(b, c, d, a, x[6], 23, 0x4881d05); 
		a = HH(a, b, c, d, x[9], 4, 0xd9d4d039); 
		d = HH(d, a, b, c, x[12], 11, 0xe6db99e5); 
		c = HH(c, d, a, b, x[15], 16, 0x1fa27cf8); 
		b = HH(b, c, d, a, x[2], 23, 0xc4ac5665); 

		
		a = II(a, b, c, d, x[0], 6, 0xf4292244); 
		d = II(d, a, b, c, x[7], 10, 0x432aff97); 
		c = II(c, d, a, b, x[14], 15, 0xab9423a7); 
		b = II(b, c, d, a, x[5], 21, 0xfc93a039); 
		a = II(a, b, c, d, x[12], 6, 0x655b59c3); 
		d = II(d, a, b, c, x[3], 10, 0x8f0ccc92); 
		c = II(c, d, a, b, x[10], 15, 0xffeff47d); 
		b = II(b, c, d, a, x[1], 21, 0x85845dd1); 
		a = II(a, b, c, d, x[8], 6, 0x6fa87e4f); 
		d = II(d, a, b, c, x[15], 10, 0xfe2ce6e0); 
		c = II(c, d, a, b, x[6], 15, 0xa3014314); 
		b = II(b, c, d, a, x[13], 21, 0x4e0811a1); 
		a = II(a, b, c, d, x[4], 6, 0xf7537e82); 
		d = II(d, a, b, c, x[11], 10, 0xbd3af235); 
		c = II(c, d, a, b, x[2], 15, 0x2ad7d2bb); 
		b = II(b, c, d, a, x[9], 21, 0xeb86d391); 

		state0 += a;
		state1 += b;
		state2 += c;
		state3 += d;
	}

	public final void reset()
	{
		count = 0;

		state0 = 0x67452301;
		state1 = 0xefcdab89;
		state2 = 0x98badcfe;
		state3 = 0x10325476;

		

		for (int i = 0; i < 16; i++)
			x[i] = 0;
	}

	public final void update(byte b)
	{
		final int space = 64 - ((int) (count & 0x3f));

		count++;

		block[64 - space] = b;

		if (space == 1)
			transform(block, 0);
	}

	public final void update(byte[] buff, int pos, int len)
	{
		int space = 64 - ((int) (count & 0x3f));

		count += len;

		while (len > 0)
		{
			if (len < space)
			{
				System.arraycopy(buff, pos, block, 64 - space, len);
				break;
			}

			if (space == 64)
			{
				transform(buff, pos);
			}
			else
			{
				System.arraycopy(buff, pos, block, 64 - space, space);
				transform(block, 0);
			}

			pos += space;
			len -= space;
			space = 64;
		}
	}

	public final void update(byte[] b)
	{
		update(b, 0, b.length);
	}

	public final void digest(byte[] dst, int pos)
	{
		byte[] bits = new byte[8];

		encode(bits, 0, (int) (count << 3));
		encode(bits, 4, (int) (count >> 29));

		int idx = (int) count & 0x3f;
		int padLen = (idx < 56) ? (56 - idx) : (120 - idx);

		update(padding, 0, padLen);
		update(bits, 0, 8);

		encode(dst, pos, state0);
		encode(dst, pos + 4, state1);
		encode(dst, pos + 8, state2);
		encode(dst, pos + 12, state3);

		reset();
	}

	public final void digest(byte[] dst)
	{
		digest(dst, 0);
	}

	public final int getDigestLength()
	{
		return 16;
	}
}
