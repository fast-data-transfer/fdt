package ch.ethz.ssh2.signature;

import java.math.BigInteger;

/**
 * RSAPublicKey.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: RSAPublicKey.java,v 1.2 2005/08/11 12:47:29 cplattne Exp $
 */
public class RSAPublicKey
{
	BigInteger e;
	BigInteger n;

	public RSAPublicKey(BigInteger e, BigInteger n)
	{
		this.e = e;
		this.n = n;
	}

	public BigInteger getE()
	{
		return e;
	}

	public BigInteger getN()
	{
		return n;
	}
}