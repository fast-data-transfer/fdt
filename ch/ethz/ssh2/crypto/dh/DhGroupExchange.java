
package ch.ethz.ssh2.crypto.dh;

import java.math.BigInteger;
import java.security.SecureRandom;

import ch.ethz.ssh2.DHGexParameters;
import ch.ethz.ssh2.crypto.digest.HashForSSH2Types;


public class DhGroupExchange
{
	

	private BigInteger p;
	private BigInteger g;

	

	private BigInteger e;
	private BigInteger x;

	

	private BigInteger f;

	

	private BigInteger k;

	public DhGroupExchange(BigInteger p, BigInteger g)
	{
		this.p = p;
		this.g = g;
	}

	public void init(SecureRandom rnd)
	{
		k = null;

		x = new BigInteger(p.bitLength() - 1, rnd);
		e = g.modPow(x, p);
	}

	
	public BigInteger getE()
	{
		if (e == null)
			throw new IllegalStateException("Not initialized!");

		return e;
	}

	
	public BigInteger getK()
	{
		if (k == null)
			throw new IllegalStateException("Shared secret not yet known, need f first!");

		return k;
	}

	
	public void setF(BigInteger f)
	{
		if (e == null)
			throw new IllegalStateException("Not initialized!");

		BigInteger zero = BigInteger.valueOf(0);

		if (zero.compareTo(f) >= 0 || p.compareTo(f) <= 0)
			throw new IllegalArgumentException("Invalid f specified!");

		this.f = f;
		this.k = f.modPow(x, p);
	}

	public byte[] calculateH(byte[] clientversion, byte[] serverversion, byte[] clientKexPayload,
			byte[] serverKexPayload, byte[] hostKey, DHGexParameters para)
	{
		HashForSSH2Types hash = new HashForSSH2Types("SHA1");

		hash.updateByteString(clientversion);
		hash.updateByteString(serverversion);
		hash.updateByteString(clientKexPayload);
		hash.updateByteString(serverKexPayload);
		hash.updateByteString(hostKey);
		if (para.getMin_group_len() > 0)
			hash.updateUINT32(para.getMin_group_len());
		hash.updateUINT32(para.getPref_group_len());
		if (para.getMax_group_len() > 0)
			hash.updateUINT32(para.getMax_group_len());
		hash.updateBigInt(p);
		hash.updateBigInt(g);
		hash.updateBigInt(e);
		hash.updateBigInt(f);
		hash.updateBigInt(k);

		return hash.getDigest();
	}
}
