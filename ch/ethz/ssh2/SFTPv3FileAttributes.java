
package ch.ethz.ssh2;



public class SFTPv3FileAttributes
{
	
	public Long size = null;

	
	public Integer uid = null;

	
	public Integer gid = null;

	
	public Integer permissions = null;

	
	public Integer atime = null;

	
	public Integer mtime = null;

	
	public boolean isDirectory()
	{
		if (permissions == null)
			return false;
		
		return ((permissions.intValue() & 0040000) != 0);
	}
	
	
	public boolean isRegularFile()
	{
		if (permissions == null)
			return false;
		
		return ((permissions.intValue() & 0100000) != 0);
	}
	
	
	public boolean isSymlink()
	{
		if (permissions == null)
			return false;
		
		return ((permissions.intValue() & 0120000) != 0);
	}
	
	
	public String getOctalPermissions()
	{
		if (permissions == null)
			return null;

		String res = Integer.toString(permissions.intValue() & 0177777, 8);

		StringBuffer sb = new StringBuffer();

		int leadingZeros = 7 - res.length();

		while (leadingZeros > 0)
		{
			sb.append('0');
			leadingZeros--;
		}

		sb.append(res);

		return sb.toString();
	}
}
