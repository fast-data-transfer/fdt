package ch.ethz.ssh2.signature;

import java.math.BigInteger;

/**
 * DSASignature.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: DSASignature.java,v 1.1 2005/05/26 14:53:30 cplattne Exp $
 */
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
