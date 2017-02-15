
package ch.ethz.ssh2.sftp;


public class AttribFlags
{
	
	public static final int SSH_FILEXFER_ATTR_SIZE = 0x00000001;

	
	public static final int SSH_FILEXFER_ATTR_V3_UIDGID = 0x00000002;

	
	public static final int SSH_FILEXFER_ATTR_PERMISSIONS = 0x00000004;

	
	public static final int SSH_FILEXFER_ATTR_V3_ACMODTIME = 0x00000008;

	
	public static final int SSH_FILEXFER_ATTR_ACCESSTIME = 0x00000008;

	
	public static final int SSH_FILEXFER_ATTR_CREATETIME = 0x00000010;

	
	public static final int SSH_FILEXFER_ATTR_MODIFYTIME = 0x00000020;

	
	public static final int SSH_FILEXFER_ATTR_ACL = 0x00000040;

	
	public static final int SSH_FILEXFER_ATTR_OWNERGROUP = 0x00000080;

	
	public static final int SSH_FILEXFER_ATTR_SUBSECOND_TIMES = 0x00000100;

	
	public static final int SSH_FILEXFER_ATTR_BITS = 0x00000200;

	
	public static final int SSH_FILEXFER_ATTR_ALLOCATION_SIZE = 0x00000400;

	
	public static final int SSH_FILEXFER_ATTR_TEXT_HINT = 0x00000800;

	
	public static final int SSH_FILEXFER_ATTR_MIME_TYPE = 0x00001000;

	
	public static final int SSH_FILEXFER_ATTR_LINK_COUNT = 0x00002000;

	
	public static final int SSH_FILEXFER_ATTR_UNTRANSLATED_NAME = 0x00004000;

	
	public static final int SSH_FILEXFER_ATTR_CTIME = 0x00008000;

	
	public static final int SSH_FILEXFER_ATTR_EXTENDED = 0x80000000;
}
