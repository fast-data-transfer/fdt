package ch.ethz.ssh2.signature;

import java.math.BigInteger;


public class DSASignature
{
	private BigInteger r;
	private BigInteger s;

	public DSASignature(BigInteger r, BigInteger s)
	{
		this.r = r;
		this.s = s;
	}

	public BigInteger getR()
	{
		return r;
	}

	public BigInteger getS()
	{
		return s;
	}
}
