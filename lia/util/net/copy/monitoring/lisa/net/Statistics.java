
package lia.util.net.copy.monitoring.lisa.net;

import java.io.Serializable;


public class Statistics implements Serializable {

	
	private static final long serialVersionUID = 1988671591829311032L;

	
	
	protected long time;
	
	public Statistics() {
		time = System.currentTimeMillis();
	}
	
	public void updateTime() {
		time = System.currentTimeMillis();
	}
	
	public final long getTime() {
		return time;
	}
	
} 
