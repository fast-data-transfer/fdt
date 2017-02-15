
package ch.ethz.ssh2;



public class DHGexParameters
{
	private final int min_group_len;
	private final int pref_group_len;
	private final int max_group_len;

	private static final int MIN_ALLOWED = 1024;
	private static final int MAX_ALLOWED = 8192;

	
	public DHGexParameters()
	{
		this(1024, 1024, 4096);
	}

	
	public DHGexParameters(int pref_group_len)
	{
		if ((pref_group_len < MIN_ALLOWED) || (pref_group_len > MAX_ALLOWED))
			throw new IllegalArgumentException("pref_group_len out of range!");

		this.pref_group_len = pref_group_len;
		this.min_group_len = 0;
		this.max_group_len = 0;
	}

	
	public DHGexParameters(int min_group_len, int pref_group_len, int max_group_len)
	{
		if ((min_group_len < MIN_ALLOWED) || (min_group_len > MAX_ALLOWED))
			throw new IllegalArgumentException("min_group_len out of range!");

		if ((pref_group_len < MIN_ALLOWED) || (pref_group_len > MAX_ALLOWED))
			throw new IllegalArgumentException("pref_group_len out of range!");

		if ((max_group_len < MIN_ALLOWED) || (max_group_len > MAX_ALLOWED))
			throw new IllegalArgumentException("max_group_len out of range!");

		if ((pref_group_len < min_group_len) || (pref_group_len > max_group_len))
			throw new IllegalArgumentException("pref_group_len is incompatible with min and max!");

		if (max_group_len < min_group_len)
			throw new IllegalArgumentException("max_group_len must not be smaller than min_group_len!");

		this.min_group_len = min_group_len;
		this.pref_group_len = pref_group_len;
		this.max_group_len = max_group_len;
	}

	
	public int getMax_group_len()
	{
		return max_group_len;
	}

	
	public int getMin_group_len()
	{
		return min_group_len;
	}

	
	public int getPref_group_len()
	{
		return pref_group_len;
	}
}
