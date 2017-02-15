
package ch.ethz.ssh2.sftp;


public class OpenFlags
{
	
	public static final int SSH_FXF_ACCESS_DISPOSITION = 0x00000007;

	
	public static final int SSH_FXF_CREATE_NEW = 0x00000000;

	
	public static final int SSH_FXF_CREATE_TRUNCATE = 0x00000001;

	
	public static final int SSH_FXF_OPEN_EXISTING = 0x00000002;

	
	public static final int SSH_FXF_OPEN_OR_CREATE = 0x00000003;

	
	public static final int SSH_FXF_TRUNCATE_EXISTING = 0x00000004;

	
	public static final int SSH_FXF_ACCESS_APPEND_DATA = 0x00000008;

	
	public static final int SSH_FXF_ACCESS_APPEND_DATA_ATOMIC = 0x00000010;

	
	public static final int SSH_FXF_ACCESS_TEXT_MODE = 0x00000020;

	
	public static final int SSH_FXF_ACCESS_BLOCK_READ = 0x00000040;

	
	public static final int SSH_FXF_ACCESS_BLOCK_WRITE = 0x00000080;

	
	public static final int SSH_FXF_ACCESS_BLOCK_DELETE = 0x00000100;

	
	public static final int SSH_FXF_ACCESS_BLOCK_ADVISORY = 0x00000200;

	
	public static final int SSH_FXF_ACCESS_NOFOLLOW = 0x00000400;

	
	public static final int SSH_FXF_ACCESS_DELETE_ON_CLOSE = 0x00000800;
}
