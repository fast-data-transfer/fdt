
package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketIgnore.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketIgnore.java,v 1.2 2005/08/24 17:54:09 cplattne Exp $
 */
public class PacketIgnore
{
	byte[] payload;

	byte[] body;

	public void setBody(byte[] body)
	{
		this.body = body;
		payload = null;
	}

	public PacketIgnore(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_IGNORE)
			throw new IOException("This is not a SSH_MSG_IGNORE packet! (" + packet_type + ")");

		/* Could parse String body */
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_IGNORE);
			tw.writeString(body, 0, body.length);
			payload = tw.getBytes();
		}
		return payload;
	}
}
