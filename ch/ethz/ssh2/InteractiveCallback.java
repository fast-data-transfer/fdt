
package ch.ethz.ssh2;



public interface InteractiveCallback
{
	
	public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception;
}
