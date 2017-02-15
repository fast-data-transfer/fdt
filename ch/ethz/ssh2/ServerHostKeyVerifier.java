
package ch.ethz.ssh2;



public interface ServerHostKeyVerifier
{
	
	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
			throws Exception;
}
