
package ch.ethz.ssh2.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;

import ch.ethz.ssh2.log.Logger;


public class TimeoutService
{
	private static final Logger log = Logger.getLogger(TimeoutService.class);

	public static class TimeoutToken implements Comparable
	{
		private long runTime;
		private Runnable handler;

		private TimeoutToken(long runTime, Runnable handler)
		{
			this.runTime = runTime;
			this.handler = handler;
		}

		public int compareTo(Object o)
		{
			TimeoutToken t = (TimeoutToken) o;
			if (runTime > t.runTime)
				return 1;
			if (runTime == t.runTime)
				return 0;
			return -1;
		}
	}

	private static class TimeoutThread extends Thread
	{
		public void run()
		{
			synchronized (todolist)
			{
				while (true)
				{
					if (todolist.size() == 0)
					{
						timeoutThread = null;
						return;
					}

					long now = System.currentTimeMillis();

					TimeoutToken tt = (TimeoutToken) todolist.getFirst();

					if (tt.runTime > now)
					{
						

						try
						{
							todolist.wait(tt.runTime - now);
						}
						catch (InterruptedException e)
						{
						}

						

						continue;
					}

					todolist.removeFirst();

					try
					{
						tt.handler.run();
					}
					catch (Exception e)
					{
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						log.log(20, "Exeception in Timeout handler:" + e.getMessage() + "(" + sw.toString() + ")");
					}
				}
			}
		}
	}

	
	private static final LinkedList todolist = new LinkedList();

	private static Thread timeoutThread = null;

	
	public static final TimeoutToken addTimeoutHandler(long runTime, Runnable handler)
	{
		TimeoutToken token = new TimeoutToken(runTime, handler);

		synchronized (todolist)
		{
			todolist.add(token);
			Collections.sort(todolist);

			if (timeoutThread != null)
				timeoutThread.interrupt();
			else
			{
				timeoutThread = new TimeoutThread();
				timeoutThread.setDaemon(true);
				timeoutThread.start();
			}
		}

		return token;
	}

	public static final void cancelTimeoutHandler(TimeoutToken token)
	{
		synchronized (todolist)
		{
			todolist.remove(token);

			if (timeoutThread != null)
				timeoutThread.interrupt();
		}
	}

}
