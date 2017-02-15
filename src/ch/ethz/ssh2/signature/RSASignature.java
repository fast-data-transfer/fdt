
package ch.ethz.ssh2.signature;

import java.math.BigInteger;


/**
 * RSASignature.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: RSASignature.java,v 1.1 2005/08/11 12:47:29 cplattne Exp $
 */

public class RSASignature
{
	BigInteger s;

	public BigInteger getS()
	{
		return s;
	}

	public RSASignature(BigInteger s)
	{
		this.s = s;
	}
}