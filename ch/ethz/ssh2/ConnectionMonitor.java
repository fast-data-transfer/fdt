
package ch.ethz.ssh2;



public interface ConnectionMonitor
{
	
	public void connectionLost(Throwable reason);
}