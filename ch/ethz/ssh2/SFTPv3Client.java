
package ch.ethz.ssh2;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Vector;

import ch.ethz.ssh2.packets.TypesReader;
import ch.ethz.ssh2.packets.TypesWriter;
import ch.ethz.ssh2.sftp.AttribFlags;
import ch.ethz.ssh2.sftp.ErrorCodes;
import ch.ethz.ssh2.sftp.Packet;


public class SFTPv3Client
{
	final Connection conn;
	final Session sess;
	final PrintStream debug;

	boolean flag_closed = false;

	InputStream is;
	OutputStream os;

	int protocol_version = 0;
	HashMap server_extensions = new HashMap();

	int next_request_id = 1000;

	String charsetName = null;

	
	public SFTPv3Client(Connection conn, PrintStream debug) throws IOException
	{
		if (conn == null)
			throw new IllegalArgumentException("Cannot accept null argument!");

		this.conn = conn;
		this.debug = debug;

		if (debug != null)
			debug.println("Opening session and starting SFTP subsystem.");

		sess = conn.openSession();
		sess.startSubSystem("sftp");

		is = sess.getStdout();
		os = new BufferedOutputStream(sess.getStdin(), 2048);

		if ((is == null) || (os == null))
			throw new IOException("There is a problem with the streams of the underlying channel.");

		init();
	}

	
	public SFTPv3Client(Connection conn) throws IOException
	{
		this(conn, null);
	}

	
	public void setCharset(String charset) throws IOException
	{
		if (charset == null)
		{
			charsetName = charset;
			return;
		}

		try
		{
			Charset.forName(charset);
		}
		catch (Exception e)
		{
			throw (IOException) new IOException("This charset is not supported").initCause(e);
		}
		charsetName = charset;
	}

	
	public String getCharset()
	{
		return charsetName;
	}

	private final void checkHandleValidAndOpen(SFTPv3FileHandle handle) throws IOException
	{
		if (handle.client != this)
			throw new IOException("The file handle was created with another SFTPv3FileHandle instance.");

		if (handle.isClosed == true)
			throw new IOException("The file handle is closed.");
	}

	private final void sendMessage(int type, int requestId, byte[] msg, int off, int len) throws IOException
	{
		int msglen = len + 1;

		if (type != Packet.SSH_FXP_INIT)
			msglen += 4;

		os.write(msglen >> 24);
		os.write(msglen >> 16);
		os.write(msglen >> 8);
		os.write(msglen);
		os.write(type);

		if (type != Packet.SSH_FXP_INIT)
		{
			os.write(requestId >> 24);
			os.write(requestId >> 16);
			os.write(requestId >> 8);
			os.write(requestId);
		}

		os.write(msg, off, len);
		os.flush();
	}

	private final void sendMessage(int type, int requestId, byte[] msg) throws IOException
	{
		sendMessage(type, requestId, msg, 0, msg.length);
	}

	private final void readBytes(byte[] buff, int pos, int len) throws IOException
	{
		while (len > 0)
		{
			int count = is.read(buff, pos, len);
			if (count < 0)
				throw new IOException("Unexpected end of sftp stream.");
			if ((count == 0) || (count > len))
				throw new IOException("Underlying stream implementation is bogus!");
			len -= count;
			pos += count;
		}
	}

	
	private final byte[] receiveMessage(int maxlen) throws IOException
	{
		byte[] msglen = new byte[4];

		readBytes(msglen, 0, 4);

		int len = (((msglen[0] & 0xff) << 24) | ((msglen[1] & 0xff) << 16) | ((msglen[2] & 0xff) << 8) | (msglen[3] & 0xff));

		if ((len > maxlen) || (len <= 0))
			throw new IOException("Illegal sftp packet len: " + len);

		byte[] msg = new byte[len];

		readBytes(msg, 0, len);

		return msg;
	}

	private final int generateNextRequestID()
	{
		synchronized (this)
		{
			return next_request_id++;
		}
	}

	private final void closeHandle(byte[] handle) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(handle, 0, handle.length);

		sendMessage(Packet.SSH_FXP_CLOSE, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	private SFTPv3FileAttributes readAttrs(TypesReader tr) throws IOException
	{
		

		SFTPv3FileAttributes fa = new SFTPv3FileAttributes();

		int flags = tr.readUINT32();

		if ((flags & AttribFlags.SSH_FILEXFER_ATTR_SIZE) != 0)
		{
			if (debug != null)
				debug.println("SSH_FILEXFER_ATTR_SIZE");
			fa.size = new Long(tr.readUINT64());
		}

		if ((flags & AttribFlags.SSH_FILEXFER_ATTR_V3_UIDGID) != 0)
		{
			if (debug != null)
				debug.println("SSH_FILEXFER_ATTR_V3_UIDGID");
			fa.uid = new Integer(tr.readUINT32());
			fa.gid = new Integer(tr.readUINT32());
		}

		if ((flags & AttribFlags.SSH_FILEXFER_ATTR_PERMISSIONS) != 0)
		{
			if (debug != null)
				debug.println("SSH_FILEXFER_ATTR_PERMISSIONS");
			fa.permissions = new Integer(tr.readUINT32());
		}

		if ((flags & AttribFlags.SSH_FILEXFER_ATTR_V3_ACMODTIME) != 0)
		{
			if (debug != null)
				debug.println("SSH_FILEXFER_ATTR_V3_ACMODTIME");
			fa.atime = new Integer(tr.readUINT32());
			fa.mtime = new Integer(tr.readUINT32());

		}

		if ((flags & AttribFlags.SSH_FILEXFER_ATTR_EXTENDED) != 0)
		{
			int count = tr.readUINT32();

			if (debug != null)
				debug.println("SSH_FILEXFER_ATTR_EXTENDED (" + count + ")");

			

			while (count > 0)
			{
				tr.readByteString();
				tr.readByteString();
				count--;
			}
		}

		return fa;
	}

	
	public SFTPv3FileAttributes fstat(SFTPv3FileHandle handle) throws IOException
	{
		checkHandleValidAndOpen(handle);

		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(handle.fileHandle, 0, handle.fileHandle.length);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_FSTAT...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_FSTAT, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		if (debug != null)
		{
			debug.println("Got REPLY.");
			debug.flush();
		}

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_ATTRS)
		{
			return readAttrs(tr);
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();

		throw new SFTPException(tr.readString(), errorCode);
	}

	private SFTPv3FileAttributes statBoth(String path, int statMethod) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(path, charsetName);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_STAT/SSH_FXP_LSTAT...");
			debug.flush();
		}

		sendMessage(statMethod, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		if (debug != null)
		{
			debug.println("Got REPLY.");
			debug.flush();
		}

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_ATTRS)
		{
			return readAttrs(tr);
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();

		throw new SFTPException(tr.readString(), errorCode);
	}

	
	public SFTPv3FileAttributes stat(String path) throws IOException
	{
		return statBoth(path, Packet.SSH_FXP_STAT);
	}

	
	public SFTPv3FileAttributes lstat(String path) throws IOException
	{
		return statBoth(path, Packet.SSH_FXP_LSTAT);
	}

	
	public String readLink(String path) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(path, charsetName);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_READLINK...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_READLINK, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		if (debug != null)
		{
			debug.println("Got REPLY.");
			debug.flush();
		}

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_NAME)
		{
			int count = tr.readUINT32();

			if (count != 1)
				throw new IOException("The server sent an invalid SSH_FXP_NAME packet.");

			return tr.readString(charsetName);
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();

		throw new SFTPException(tr.readString(), errorCode);
	}

	private void expectStatusOKMessage(int id) throws IOException
	{
		byte[] resp = receiveMessage(34000);

		if (debug != null)
		{
			debug.println("Got REPLY.");
			debug.flush();
		}

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != id)
			throw new IOException("The server sent an invalid id field.");

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();

		if (errorCode == ErrorCodes.SSH_FX_OK)
			return;

		throw new SFTPException(tr.readString(), errorCode);
	}

	
	public void setstat(String path, SFTPv3FileAttributes attr) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(path, charsetName);
		tw.writeBytes(createAttrs(attr));

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_SETSTAT...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_SETSTAT, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public void fsetstat(SFTPv3FileHandle handle, SFTPv3FileAttributes attr) throws IOException
	{
		checkHandleValidAndOpen(handle);

		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(handle.fileHandle, 0, handle.fileHandle.length);
		tw.writeBytes(createAttrs(attr));

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_FSETSTAT...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_FSETSTAT, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public void createSymlink(String src, String target) throws IOException
	{
		int req_id = generateNextRequestID();

		

		TypesWriter tw = new TypesWriter();
		tw.writeString(target, charsetName);
		tw.writeString(src, charsetName);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_SYMLINK...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_SYMLINK, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public String canonicalPath(String path) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(path, charsetName);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_REALPATH...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_REALPATH, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		if (debug != null)
		{
			debug.println("Got REPLY.");
			debug.flush();
		}

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_NAME)
		{
			int count = tr.readUINT32();

			if (count != 1)
				throw new IOException("The server sent an invalid SSH_FXP_NAME packet.");

			return tr.readString(charsetName);
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();

		throw new SFTPException(tr.readString(), errorCode);
	}

	private final Vector scanDirectory(byte[] handle) throws IOException
	{
		Vector files = new Vector();

		while (true)
		{
			int req_id = generateNextRequestID();

			TypesWriter tw = new TypesWriter();
			tw.writeString(handle, 0, handle.length);

			if (debug != null)
			{
				debug.println("Sending SSH_FXP_READDIR...");
				debug.flush();
			}

			sendMessage(Packet.SSH_FXP_READDIR, req_id, tw.getBytes());

			byte[] resp = receiveMessage(34000);

			if (debug != null)
			{
				debug.println("Got REPLY.");
				debug.flush();
			}

			TypesReader tr = new TypesReader(resp);

			int t = tr.readByte();

			int rep_id = tr.readUINT32();
			if (rep_id != req_id)
				throw new IOException("The server sent an invalid id field.");

			if (t == Packet.SSH_FXP_NAME)
			{
				int count = tr.readUINT32();

				if (debug != null)
					debug.println("Parsing " + count + " name entries...");

				while (count > 0)
				{
					SFTPv3DirectoryEntry dirEnt = new SFTPv3DirectoryEntry();

					dirEnt.filename = tr.readString(charsetName);
					dirEnt.longEntry = tr.readString(charsetName);

					dirEnt.attributes = readAttrs(tr);
					files.addElement(dirEnt);

					if (debug != null)
						debug.println("File: '" + dirEnt.filename + "'");
					count--;
				}
				continue;
			}

			if (t != Packet.SSH_FXP_STATUS)
				throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

			int errorCode = tr.readUINT32();

			if (errorCode == ErrorCodes.SSH_FX_EOF)
				return files;

			throw new SFTPException(tr.readString(), errorCode);
		}
	}

	private final byte[] openDirectory(String path) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(path, charsetName);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_OPENDIR...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_OPENDIR, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_HANDLE)
		{
			if (debug != null)
			{
				debug.println("Got SSH_FXP_HANDLE.");
				debug.flush();
			}

			byte[] handle = tr.readByteString();
			return handle;
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();
		String errorMessage = tr.readString();

		throw new SFTPException(errorMessage, errorCode);
	}

	private final String expandString(byte[] b, int off, int len)
	{
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < len; i++)
		{
			int c = b[off + i] & 0xff;

			if ((c >= 32) && (c <= 126))
			{
				sb.append((char) c);
			}
			else
			{
				sb.append("{0x" + Integer.toHexString(c) + "}");
			}
		}

		return sb.toString();
	}

	private void init() throws IOException
	{
		

		final int client_version = 3;

		if (debug != null)
			debug.println("Sending SSH_FXP_INIT (" + client_version + ")...");

		TypesWriter tw = new TypesWriter();
		tw.writeUINT32(client_version);
		sendMessage(Packet.SSH_FXP_INIT, 0, tw.getBytes());

		

		if (debug != null)
			debug.println("Waiting for SSH_FXP_VERSION...");

		TypesReader tr = new TypesReader(receiveMessage(34000)); 

		int type = tr.readByte();

		if (type != Packet.SSH_FXP_VERSION)
		{
			throw new IOException("The server did not send a SSH_FXP_VERSION packet (got " + type + ")");
		}

		protocol_version = tr.readUINT32();

		if (debug != null)
			debug.println("SSH_FXP_VERSION: protocol_version = " + protocol_version);

		if (protocol_version != 3)
			throw new IOException("Server version " + protocol_version + " is currently not supported");

		

		while (tr.remain() != 0)
		{
			String name = tr.readString();
			byte[] value = tr.readByteString();
			server_extensions.put(name, value);

			if (debug != null)
				debug.println("SSH_FXP_VERSION: extension: " + name + " = '" + expandString(value, 0, value.length)
						+ "'");
		}
	}

	
	public int getProtocolVersion()
	{
		return protocol_version;
	}

	
	public void close()
	{
		sess.close();
	}

	
	public Vector ls(String dirName) throws IOException
	{
		byte[] handle = openDirectory(dirName);
		Vector result = scanDirectory(handle);
		closeHandle(handle);
		return result;
	}

	
	public void mkdir(String dirName, int posixPermissions) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(dirName, charsetName);
		tw.writeUINT32(AttribFlags.SSH_FILEXFER_ATTR_PERMISSIONS);
		tw.writeUINT32(posixPermissions);

		sendMessage(Packet.SSH_FXP_MKDIR, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public void rm(String fileName) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(fileName, charsetName);

		sendMessage(Packet.SSH_FXP_REMOVE, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public void rmdir(String dirName) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(dirName, charsetName);

		sendMessage(Packet.SSH_FXP_RMDIR, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public void mv(String oldPath, String newPath) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(oldPath, charsetName);
		tw.writeString(newPath, charsetName);

		sendMessage(Packet.SSH_FXP_RENAME, req_id, tw.getBytes());

		expectStatusOKMessage(req_id);
	}

	
	public SFTPv3FileHandle openFileRO(String fileName) throws IOException
	{
		return openFile(fileName, 0x00000001, null); 
	}

	
	public SFTPv3FileHandle openFileRW(String fileName) throws IOException
	{
		return openFile(fileName, 0x00000003, null); 
	}

	
	
	
	
	
	

	
	public SFTPv3FileHandle createFile(String fileName) throws IOException
	{
		return createFile(fileName, null);
	}

	
	public SFTPv3FileHandle createFile(String fileName, SFTPv3FileAttributes attr) throws IOException
	{
		return openFile(fileName, 0x00000008 | 0x00000003, attr); 
	}

	
	public SFTPv3FileHandle createFileTruncate(String fileName) throws IOException
	{
		return createFileTruncate(fileName, null);
	}

	
	public SFTPv3FileHandle createFileTruncate(String fileName, SFTPv3FileAttributes attr) throws IOException
	{
		return openFile(fileName, 0x00000018 | 0x00000003, attr); 
	}

	private byte[] createAttrs(SFTPv3FileAttributes attr)
	{
		TypesWriter tw = new TypesWriter();

		int attrFlags = 0;

		if (attr == null)
		{
			tw.writeUINT32(0);
		}
		else
		{
			if (attr.size != null)
				attrFlags = attrFlags | AttribFlags.SSH_FILEXFER_ATTR_SIZE;

			if ((attr.uid != null) && (attr.gid != null))
				attrFlags = attrFlags | AttribFlags.SSH_FILEXFER_ATTR_V3_UIDGID;

			if (attr.permissions != null)
				attrFlags = attrFlags | AttribFlags.SSH_FILEXFER_ATTR_PERMISSIONS;

			if ((attr.atime != null) && (attr.mtime != null))
				attrFlags = attrFlags | AttribFlags.SSH_FILEXFER_ATTR_V3_ACMODTIME;

			tw.writeUINT32(attrFlags);

			if (attr.size != null)
				tw.writeUINT64(attr.size.longValue());

			if ((attr.uid != null) && (attr.gid != null))
			{
				tw.writeUINT32(attr.uid.intValue());
				tw.writeUINT32(attr.gid.intValue());
			}

			if (attr.permissions != null)
				tw.writeUINT32(attr.permissions.intValue());

			if ((attr.atime != null) && (attr.mtime != null))
			{
				tw.writeUINT32(attr.atime.intValue());
				tw.writeUINT32(attr.mtime.intValue());
			}
		}

		return tw.getBytes();
	}

	private SFTPv3FileHandle openFile(String fileName, int flags, SFTPv3FileAttributes attr) throws IOException
	{
		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(fileName, charsetName);
		tw.writeUINT32(flags);
		tw.writeBytes(createAttrs(attr));

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_OPEN...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_OPEN, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_HANDLE)
		{
			if (debug != null)
			{
				debug.println("Got SSH_FXP_HANDLE.");
				debug.flush();
			}

			return new SFTPv3FileHandle(this, tr.readByteString());
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();
		String errorMessage = tr.readString();

		throw new SFTPException(errorMessage, errorCode);
	}

	
	public int read(SFTPv3FileHandle handle, long fileOffset, byte[] dst, int dstoff, int len) throws IOException
	{
		checkHandleValidAndOpen(handle);

		if ((len > 32768) || (len <= 0))
			throw new IllegalArgumentException("invalid len argument");

		int req_id = generateNextRequestID();

		TypesWriter tw = new TypesWriter();
		tw.writeString(handle.fileHandle, 0, handle.fileHandle.length);
		tw.writeUINT64(fileOffset);
		tw.writeUINT32(len);

		if (debug != null)
		{
			debug.println("Sending SSH_FXP_READ...");
			debug.flush();
		}

		sendMessage(Packet.SSH_FXP_READ, req_id, tw.getBytes());

		byte[] resp = receiveMessage(34000);

		TypesReader tr = new TypesReader(resp);

		int t = tr.readByte();

		int rep_id = tr.readUINT32();
		if (rep_id != req_id)
			throw new IOException("The server sent an invalid id field.");

		if (t == Packet.SSH_FXP_DATA)
		{
			if (debug != null)
			{
				debug.println("Got SSH_FXP_DATA...");
				debug.flush();
			}

			int readLen = tr.readUINT32();

			if ((readLen < 0) || (readLen > len))
				throw new IOException("The server sent an invalid length field.");

			tr.readBytes(dst, dstoff, readLen);

			return readLen;
		}

		if (t != Packet.SSH_FXP_STATUS)
			throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

		int errorCode = tr.readUINT32();

		if (errorCode == ErrorCodes.SSH_FX_EOF)
		{
			if (debug != null)
			{
				debug.println("Got SSH_FX_EOF.");
				debug.flush();
			}

			return -1;
		}

		String errorMessage = tr.readString();

		throw new SFTPException(errorMessage, errorCode);
	}

	
	public void write(SFTPv3FileHandle handle, long fileOffset, byte[] src, int srcoff, int len) throws IOException
	{
		checkHandleValidAndOpen(handle);

		if (len < 0)

			while (len > 0)
			{
				int writeRequestLen = len;

				if (writeRequestLen > 32768)
					writeRequestLen = 32768;

				int req_id = generateNextRequestID();

				TypesWriter tw = new TypesWriter();
				tw.writeString(handle.fileHandle, 0, handle.fileHandle.length);
				tw.writeUINT64(fileOffset);
				tw.writeString(src, srcoff, writeRequestLen);

				if (debug != null)
				{
					debug.println("Sending SSH_FXP_WRITE...");
					debug.flush();
				}

				sendMessage(Packet.SSH_FXP_WRITE, req_id, tw.getBytes());

				fileOffset += writeRequestLen;

				srcoff += writeRequestLen;
				len -= writeRequestLen;

				byte[] resp = receiveMessage(34000);

				TypesReader tr = new TypesReader(resp);

				int t = tr.readByte();

				int rep_id = tr.readUINT32();
				if (rep_id != req_id)
					throw new IOException("The server sent an invalid id field.");

				if (t != Packet.SSH_FXP_STATUS)
					throw new IOException("The SFTP server sent an unexpected packet type (" + t + ")");

				int errorCode = tr.readUINT32();

				if (errorCode == ErrorCodes.SSH_FX_OK)
					continue;

				String errorMessage = tr.readString();

				throw new SFTPException(errorMessage, errorCode);
			}
	}

	
	public void closeFile(SFTPv3FileHandle handle) throws IOException
	{
		if (handle == null)
			throw new IllegalArgumentException("the handle argument may not be null");

		try
		{
			if (handle.isClosed == false)
			{
				closeHandle(handle.fileHandle);
			}
		}
		finally
		{
			handle.isClosed = true;
		}
	}
}
