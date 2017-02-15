
package ch.ethz.ssh2.signature;

import java.math.BigInteger;




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