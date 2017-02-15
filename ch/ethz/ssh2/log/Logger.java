
package ch.ethz.ssh2.log;



public class Logger
{
	private static final boolean enabled = false;
	private static final int logLevel = 99;

	private String className;

	public final static Logger getLogger(Class x)
	{
		return new Logger(x);
	}

	public Logger(Class x)
	{
		this.className = x.getName();
	}

	public final boolean isEnabled()
	{
		return enabled;
	}

	public void log(int level, String message)
	{
		long now = System.currentTimeMillis();

		if ((enabled) && (level <= logLevel))
		{
			synchronized (this)
			{
				System.err.println(now + " : " + className + ": " + message);
				
			}
		}
	}
}
