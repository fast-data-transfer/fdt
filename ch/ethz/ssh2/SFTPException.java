
package ch.ethz.ssh2;

import java.io.IOException;

import ch.ethz.ssh2.sftp.ErrorCodes;



public class SFTPException extends IOException
{
	private static final long serialVersionUID = 578654644222421811L;

	private final String sftpErrorMessage;
	private final int sftpErrorCode;

	private static String constructMessage(String s, int errorCode)
	{
		String[] detail = ErrorCodes.getDescription(errorCode);

		if (detail == null)
			return s + " (UNKNOW SFTP ERROR CODE)";

		return s + " (" + detail[0] + ": " + detail[1] + ")";
	}

	SFTPException(String msg, int errorCode)
	{
		super(constructMessage(msg, errorCode));
		sftpErrorMessage = msg;
		sftpErrorCode = errorCode;
	}

	
	public String getServerErrorMessage()
	{
		return sftpErrorMessage;
	}

	
	public int getServerErrorCode()
	{
		return sftpErrorCode;
	}

	
	public String getServerErrorCodeSymbol()
	{
		String[] detail = ErrorCodes.getDescription(sftpErrorCode);

		if (detail == null)
			return "UNKNOW SFTP ERROR CODE " + sftpErrorCode;

		return detail[0];
	}

	
	public String getServerErrorCodeVerbose()
	{
		String[] detail = ErrorCodes.getDescription(sftpErrorCode);

		if (detail == null)
			return "The error code " + sftpErrorCode + " is unknown.";

		return detail[1];
	}
}
