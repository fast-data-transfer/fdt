
package ch.ethz.ssh2.packets;

/**
 * PacketSessionStartShell.
 * 
 * @author Christian Plattner, plattner@inf.ethz.ch
 * @version $Id: PacketSessionStartShell.java,v 1.2 2005/08/24 17:54:09 cplattne Exp $
 */
public class PacketSessionStartShell
{
	byte[] payload;

	public int recipientChannelID;
	public boolean wantReply;

	public PacketSessionStartShell(int recipientChannelID, boolean wantReply)
	{
		this.recipientChannelID = recipientChannelID;
		this.wantReply = wantReply;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_CHANNEL_REQUEST);
			tw.writeUINT32(recipientChannelID);
			tw.writeString("shell");
			tw.writeBoolean(wantReply);
			payload = tw.getBytes();
		}
		return payload;
	}
}
