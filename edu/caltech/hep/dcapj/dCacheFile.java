package edu.caltech.hep.dcapj;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.caltech.hep.dcapj.util.ControlCommandCallback;
import edu.caltech.hep.dcapj.util.ControlConnection;
import edu.caltech.hep.dcapj.util.DataConnectionCallback;
import edu.caltech.hep.dcapj.util.IOCallback;
import edu.caltech.hep.dcapj.util.InvalidConfigurationException;


public class dCacheFile extends File implements DataConnectionCallback,
        ControlCommandCallback {

    private Logger _logger = Logger.getLogger(dCacheFile.class.getName());

    private boolean _connected = false;
    private boolean _poolReplied = false;
    private boolean _doorReplied = false;
    private ByteBuffer _commandBuffer = null;

    private IOCallback _poolCallback = null;
    private ControlConnection _controlConnection = null;

    protected SocketChannel _clientChannel = null;

    private String _pnfsID;
    private int _sessionID = -1;
    private int _commandID = -1;

    
    public enum Mode {
        READ_ONLY, WRITE_ONLY
    };

    private Mode _openMode;

    private long _max_bytes = Long.MAX_VALUE;
    private boolean _writable = false;

    private long _filesize;

    private int _poolID = 0;
    private int _mode;
    
    private SelectionKey _selectionKey;

    

    private String[] _doorReply = null;
    
    public dCacheFile(String name, String openMode) throws FileNotFoundException,
    IOException, InvalidConfigurationException {
        this(name, openMode.equals("w")?Mode.WRITE_ONLY:Mode.READ_ONLY);
    }
    
    
    public dCacheFile(String name, Mode openMode) throws FileNotFoundException,
                   IOException, InvalidConfigurationException {
        super(name);

        
        if (!dCapLayer._isInitialized) {
            throw new InvalidConfigurationException(
                    "dCap layer should be initialized by "
                    + "  making a call to dCapLayer.initialze()");
        }

        
        _openMode = openMode;
        _logger.fine("[" + name + "] mode " + _openMode);

        _poolCallback = dCapLayer.getDataConnectionCallback();
        _controlConnection = dCapLayer.getControlConnection();

        _sessionID = _controlConnection.getNextSessionID();
        _logger.fine("[" + name + "] sessionID = " + _sessionID);

        _controlConnection.registerCallback(_sessionID, this);
        _logger.finer("[" + name + "] registered callback with sessionID "
                + _sessionID);

        if (!super.exists()) {
            if (_openMode == Mode.WRITE_ONLY) {
                if (super.createNewFile())
                    _logger.fine("Created new file " + super.getName());
            } else 
            {
                FileNotFoundException ex = new FileNotFoundException(
                        "File does not exist " + name);
                _logger.throwing("dCacheFile", "dCacheFile(String)", ex);
                throw ex;
            }
        } else 
        {
            if (_openMode == Mode.WRITE_ONLY) {
                IOException ex = new IOException("File already exists");
                _logger.throwing("dCacheFile", "dCacheFile(String)", ex);
                throw ex;
            }
        } 

        _pnfsID = PnfsUtil.getPnfsID(super.getAbsolutePath()); 
        if (_openMode == Mode.READ_ONLY)
        {
        	this.open();
            this.tell();
            _connected = true;
            getPoolID();
        }
    }

    private void open() throws IOException {
        _logger.log(Level.FINE, "Opening file " + this.getAbsolutePath());

        boolean result = false;

        result = _poolCallback.registerCallback(_sessionID, this);

        if (!result) {
            String emsg = "Open failed on file " + getName()
            + "; Unable to register " + " pool call back for session "
            + _sessionID;
            _logger.severe(emsg);
            throw new IOException(emsg);
        }

        String mode = (_openMode == Mode.READ_ONLY) ? "\"r\"" : "\"w\"";

        String ip = dCapLayer.getConfig().getInterface();
        if (ip == null) {
            IOException ex = new IOException(
            "Cannot get ip address for system.");
            _logger.severe(ex.getMessage());
            _logger.throwing("dCacheFile", "open", ex);
            throw ex;
        }

        _doorReplied = false;
        String openCommand = _sessionID + " " + (++_commandID)
        + " client open " + _pnfsID + " " + mode + " " + ip + " "
        + _poolCallback.getPort();
        _controlConnection.sendCommand(openCommand);

        long t0 = System.currentTimeMillis();
        int poolTimeout = dCapLayer.getConfig().getPoolTimeout();
        _logger.finest("Pool timeout value is set as " + poolTimeout + " secs");

        
        
        
        
        while (!_doorReplied
                && ((System.currentTimeMillis() - t0) < (poolTimeout * 1000))
                && !_poolReplied) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                _logger.finer("Thread was interrupted while"
                        + " waiting for door reply");
            }
            _logger.finest("Waiting for a reply from pool for"
                    + super.getName());
        }

        if (_doorReplied) {
        	String reply = "";
        	for (String replyPart : _doorReply)
        		reply += replyPart + " ";
            String errormsg = "dCapJ " + reply;
            _logger.severe(errormsg);
            throw new IOException(errormsg);
        }
        
        _doorReplied = false;

        _logger.fine("Waiting for a data call back connection");
        long t1 = System.currentTimeMillis();
        while (!_poolReplied) {	

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                _logger.finer("Thread was interrupted "
                        + "while waiting for pool reply");
            }

            _logger.finest("Waiting for a reply from pool for"
                    + super.getName());

            if ((System.currentTimeMillis() - t1) > (poolTimeout * 1000)) {
                _logger.severe("Pool wait timedout!");
                throw new IOException("No reply from pool within give time");
            }
        }

        _logger.info("Pool connected for file: " + super.getName());

        
        ByteBuffer commandBuffer = ByteBuffer.allocate(20);
        commandBuffer.limit(4);
        
        _clientChannel.read(commandBuffer);

        commandBuffer.rewind();
        int nBytes = commandBuffer.getInt();
        if (nBytes > 0) {
            commandBuffer.limit(nBytes);
            commandBuffer.rewind();
            _clientChannel.read(commandBuffer);

            byte utf[] = new byte[commandBuffer.remaining()];
            commandBuffer.get(utf);
            String reply = new String(utf);
            _logger.severe("Error receiving HELLO BLOCK." + reply);
            throw new IOException("Unknown error receiving HELLO BLOCK");
        }

        _logger.fine("Received HELLO BLOCK [sessionID = " + _sessionID
                + ", nBytes = " + nBytes);
    }

    
    

    
    public long seek(long offset, boolean relative) throws IOException {
    	if (_openMode == Mode.WRITE_ONLY)
    	{
    		IOException ex = new IOException("Cannot seek in WRITE mode.");
    		_logger.throwing("dCacheFile", "seek", ex);
    	}
    		
        _logger.fine("Sending SEEK command to pool [offset = " + offset
                + ", relative = " + relative + "]");

        ByteBuffer _commandBuffer = ByteBuffer.allocate(30);

        _commandBuffer.putInt(16);
        _commandBuffer.putInt(3);
        _commandBuffer.putLong(offset);

        _commandBuffer.flip();
        _clientChannel.write(_commandBuffer);

        _commandBuffer.rewind();
        _commandBuffer.limit(4);
        if (offset >= 0)
            if (relative) 
                _commandBuffer.putInt(1);
            else
                _commandBuffer.putInt(0);
        else if (offset < 0) 
            _commandBuffer.putInt(2);

        _clientChannel.write(_commandBuffer);

        _commandBuffer.clear();
        _commandBuffer.limit(16);
        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        int nBytes = _commandBuffer.getInt();
        int ack = _commandBuffer.getInt();
        int cmdCode = _commandBuffer.getInt();
        int success = _commandBuffer.getInt();

        if (success != 0) 
        {
            byte utf[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(utf);
            String detail = new String(utf);

            _logger.severe("SEEK command failed [nBytes = " + nBytes
                    + ", ack = " + ack + ", cmdCode = " + cmdCode
                    + ", success = " + success + "]\n" + "Pool replied: "
                    + detail);

            IOException ex = new IOException(detail);
            _logger.throwing("dCacheFile", "seek", ex);
            throw ex;
        }

        
        _logger.fine("SEEK succeeded [nBytes = " + nBytes + ", ack = " + ack
                + ", cmdCode = " + cmdCode + ", success = " + success + "]");

        _commandBuffer.rewind();
        _commandBuffer.limit(8);
        _clientChannel.read(_commandBuffer);

        return _commandBuffer.getLong();
    }

    
    public long tell() throws IOException {
    	if (_openMode == Mode.WRITE_ONLY)
    	{
    		IOException ex = new IOException("Cannot get cursor position in WRITE mode");
    		_logger.throwing("dCacheFiles", "tell", ex);
    		throw ex;
    	}
    	
        _logger.fine("Sending LOCATE command to pool");

        ByteBuffer _commandBuffer = ByteBuffer.allocate(30);

        _commandBuffer.putInt(4);
        _commandBuffer.putInt(9);

        _commandBuffer.flip();
        _clientChannel.write(_commandBuffer);

        _commandBuffer.clear();
        _commandBuffer.limit(4);
        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        int nBytes = _commandBuffer.getInt();

        _commandBuffer.rewind();
        _commandBuffer.limit(nBytes);
        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        int ack = _commandBuffer.getInt();
        int cmdCode = _commandBuffer.getInt(); 
        int success = _commandBuffer.getInt();

        if (success != 0) 
        {
            byte utf[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(utf);
            String detail = new String(utf);

            _logger.severe("LOCATE command failed [nBytes = " + nBytes
                    + ", ack = " + ack + ", cmdCode = " + cmdCode
                    + ", success = " + success + "]\n" + "Pool replied: "
                    + detail);

            IOException ex = new IOException(detail);
            _logger.throwing("dCacheFile", "tell", ex);
            throw ex;
        }

        
        _logger.fine("LOCATE succeeded [nBytes = " + nBytes + ", ack = " + ack
                + ", cmdCode = " + cmdCode + ", success = " + success + "]");

        _filesize = _commandBuffer.getLong();

        return _commandBuffer.getLong();
    }

    
    public int read(ByteBuffer bytes, long off) throws IOException {
        if (_openMode != Mode.READ_ONLY) {
            IOException ex = new IOException("File not opened for reading");
            _logger.throwing("dCacheFile", "read(byte[], long)", ex);
            throw ex;
        }
        ByteBuffer _commandBuffer = ByteBuffer.allocate(30);

        if (off == 0) {
            _commandBuffer.putInt(12);
            _commandBuffer.putInt(2);
        } else {
            _commandBuffer.putInt(24);
            _commandBuffer.putInt(11);
            _commandBuffer.putLong(off);
            _commandBuffer.putInt(0);
        }

        _commandBuffer.putLong(bytes.remaining());
        _commandBuffer.flip();

        _commandBuffer.rewind();
        _clientChannel.write(_commandBuffer);

        _logger.fine("Sending READ request for " + bytes.remaining()
                + " bytes from " + super.getName() + ".");

        _commandBuffer.clear();
        _commandBuffer.limit(4);
        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        int num_bytes = 0;
        while (num_bytes < 12)
        	num_bytes += _commandBuffer.getInt(); 

        _commandBuffer.rewind();
        _commandBuffer.limit(num_bytes);

        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        int ack = _commandBuffer.getInt();
        int cmdCode = _commandBuffer.getInt(); 
        int success = _commandBuffer.getInt();

        if (success == 0) 
        {
            _logger.fine("READ request successful [num_bytes = " + num_bytes
                    + ", ack = " + ack + ", cmdCode = " + cmdCode
                    + ", success = " + success +
            "]");
        } else {
            
            byte utf[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(utf);
            String detail = new String(utf);
            _logger.fine("READ request successful [num_bytes = " + num_bytes
                    + ", ack = " + ack + ", cmdCode = " + cmdCode
                    + ", success = " + success + "]\n" + "Server replied: ");

            IOException ex = new IOException(detail);
            _logger.throwing("dCacheFile", "read(byte[])", ex);
            throw ex;
        }

        _commandBuffer.rewind();
        _commandBuffer.limit(8);
        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        num_bytes = _commandBuffer.getInt();
        int data = _commandBuffer.getInt();

        _logger.fine("READ: Data header received [num_bytes = " + num_bytes
                + " data " + data + "]");

        int bytes_read = 0;
        int position = 0;

        while (true) {
            _commandBuffer.rewind();
            _commandBuffer.limit(4);
            _clientChannel.read(_commandBuffer);

            _commandBuffer.rewind();
            num_bytes = _commandBuffer.getInt();
            _logger.finest("READ: Number of bytes available from pool "
                    + num_bytes);

            if (num_bytes < 0)
                break;

            int restPacket = num_bytes;

            
            

            while (restPacket > 0) {
                int block = restPacket;

                for (int rest = block; rest > 0;) {
                    bytes.position(position);
                    bytes.limit(position + rest);
                    int rc = _clientChannel.read(bytes);
                    if (rc < 0)
                        throw new IOException(
                        "Read operation terminted prematurely");

                    rest -= rc;
                    position += rc;
                }
                bytes_read += block;
                restPacket -= block;
            }
        }

        _commandBuffer.rewind();
        _commandBuffer.limit(16);
        _clientChannel.read(_commandBuffer);

        _commandBuffer.rewind();
        num_bytes = _commandBuffer.getInt();
        int fin = _commandBuffer.getInt();
        cmdCode = _commandBuffer.getInt();
        success = _commandBuffer.getInt();

        if (success != 0) 
        {
            byte utf[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(utf);
            String detail = new String(utf);

            _logger.severe("Failed while reading " + super.getName() + "["
                    + "num_bytes = " + num_bytes + ", fin = " + fin
                    + ", cmdCode = " + cmdCode + ", success = " + success
                    + "]\n" + "Server replied: " + detail);
            IOException ex = new IOException(detail);
            _logger.throwing("dCacheFile", "read(byte[])", ex);
            throw ex;
        }

        _logger.fine("Read " + bytes_read + " bytes from " + super.getName());
        return (bytes_read == 0 ? -1 : bytes_read);
    }
    

    
    public int read(byte bytes[]) throws IOException {
        
        throw new IOException("Not Implemented Yet");
    }
    
    

    public int getPoolID() {
    	if (!_connected) 
    	{
    		try
	    	{
	    		open();
	    		tell();
	    		_connected = true;
	    	}
	    	catch(Exception ex)
	    	{
	    		ex.printStackTrace();
	    	}
    	}
    	
    	_logger.info("PoolID: " + _poolID);
        return _poolID;
    }
    
    
    

    private long makeWritable(long position) throws IOException {
    	_commandBuffer.clear();

        if (position == -1) {
        	_commandBuffer.putInt(4);
        	_commandBuffer.putInt(1);
        } else {
        	_commandBuffer.putInt(16);
        	_commandBuffer.putInt(12);
        	_commandBuffer.putLong(position);
        	_commandBuffer.putInt(0);
        }

        _commandBuffer.flip();
        _clientChannel.write(_commandBuffer);

        _logger.fine("Sent WRITE request [" + "position = " + position + "]");
        _logger.fine("Receiving reply from pool");

        _commandBuffer.clear();
        int bytes_read = 0;
        while (bytes_read < 12)
        	bytes_read += _clientChannel.read(_commandBuffer);

        _commandBuffer.flip();
        int num_bytes = _commandBuffer.getInt();

        int ack = _commandBuffer.getInt();
        int cmdCode = _commandBuffer.getInt();
        int success = _commandBuffer.getInt();

        if (success != 0) {
            
            byte utf[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(utf);
            String detail = new String(utf);
            _logger.severe("WRITE failed for " + super.getName() + "["
                    + "num_bytes = " + num_bytes + ", ack = " + ack
                    + ", cmdCode = " + cmdCode + ", success = " + success
                    + "]\n" + "Server replied: " + detail + ".");
            IOException ex = new IOException(detail);
            _logger.throwing("dCacheFile", "write(byte[], int, int, long)", ex);
            throw ex;
        }

        if (num_bytes != 12) {
            
            if (num_bytes == 24) {
            	_commandBuffer.position(_commandBuffer.position() + 4);
                _max_bytes = _commandBuffer.getLong();
                _logger.fine("WRITE request succeeded for " + super.getName()
                        + ". Server limited write size. [" + "num_bytes = "
                        + num_bytes + ", ack = " + ack + ", cmdCode = "
                        + cmdCode + ", success = " + success + ", max_bytes = "
                        + _max_bytes + "]");
            } else if (num_bytes == 16) {
            	_commandBuffer.position(_commandBuffer.position() + 4);

                _logger.fine("WRITE request succeeded for " + super.getName()
                        + ". Server replied EXPECT_NEW_CONNECTION. ["
                        + "num_bytes = " + num_bytes + ", ack = " + ack
                        + ", cmdCode = " + cmdCode + ", success = " + success
                        + "]");
            }
        } else {
            _logger
            .fine("WRITE request succeeded for " + super.getName()
                    + " [" + "num_bytes = " + num_bytes + ", ack = "
                    + ack + ", cmdCode = " + cmdCode + ", success = "
                    + success + "]");
        }

        _writable = true;
        return _max_bytes;
    }

    
    
    public int write(ByteBuffer buffer, long position)
    throws IOException {
    	if (!_connected)
    	{
    		open();
    		tell();
    		_connected = true;
    		_logger.fine("PoolID: " + getPoolID());
    	}
    	
        try {
            if (_commandBuffer == null)
            	_commandBuffer = ByteBuffer.allocate(128);
            else
            	_commandBuffer.clear();

            if (_openMode != Mode.WRITE_ONLY) {
                IOException ex = new IOException("File not opened for writing");
                _logger.throwing("dCacheFile", "write(byte[], int, int, long)",
                        ex);
                throw ex;
            }

            if ( _max_bytes < buffer.remaining() ) {
                IOException ex = new IOException("Cannot write more than "
                        + _max_bytes + " to " + super.getName());
                _logger.throwing("dCacheFile", "write(byte[], int, int, long)",
                        ex);
                throw ex;
            }

            if (!_writable) {
                makeWritable(position);


            }

            _commandBuffer.clear();
            _commandBuffer.putInt(4);
            _commandBuffer.putInt(8);
            
          	_commandBuffer.putInt(buffer.remaining());
           	_logger.finest("Writing " + buffer.remaining() + " bytes to " + super.getName() + " starting...");

           	_commandBuffer.flip();
            
            while (_commandBuffer.hasRemaining())
            	_clientChannel.write(_commandBuffer);

            int bytes_written = 0;
            while (buffer.hasRemaining()) {
                bytes_written += _clientChannel.write(buffer);
            }
                        
            _logger.finest("File was opened write only, telling pool transfer is complete");

            finishedWriting();

            _logger.finest("[" + super.getName() + "] Bytes written = " + bytes_written);
            _max_bytes -= bytes_written;
            return bytes_written;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void finishedWriting() throws IOException {
    	_commandBuffer.clear();
    	
    	_commandBuffer.putInt(-1);
    	_commandBuffer.flip();
    	_clientChannel.write(_commandBuffer);
    	_commandBuffer.clear();

    	int num_bytes = 0;
    	while (num_bytes < 12)
    		num_bytes +=  _clientChannel.read(_commandBuffer);
        _logger.finest("Bytes read: " + num_bytes);
        _commandBuffer.flip();
        num_bytes = _commandBuffer.getInt();

        int fin = _commandBuffer.getInt();
        int cmdCode = _commandBuffer.getInt();
        int success = _commandBuffer.getInt();

        if (success != 0) 
        {
            byte utf[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(utf);
            String detail = new String(utf);

            IOException ex = new IOException(detail);
            _logger.info("Error writing file to pool [" + "num_bytes = "
                    + num_bytes + ", fin = " + fin + ", cmdCode = " + cmdCode
                    + ", success = " + success + "]\n" + "Server replied: "
                    + detail);
            _logger.throwing("dCacheFile", "close", ex);
            throw ex;
        } else {
            _logger.fine("Success code received from the pool");
            _writable = false;
        }
    }

    





    public void close() throws IOException {
        ByteBuffer _commandBuffer = ByteBuffer.allocate(30);
        _logger.info("Close on " + super.getName());
        _connected = false;

        int num_bytes = 0, ack = 0, cmdCode = 0, success = -1;

        try {
            _commandBuffer.putInt(4);
            _commandBuffer.putInt(4);
            _commandBuffer.limit(8);
            _commandBuffer.rewind();

            _logger.fine("Sending close command to pool");
            _clientChannel.write(_commandBuffer);

            _commandBuffer.clear();
            _commandBuffer.limit(16);
            int numread = 0;
            while (numread < 16)
            	numread += _clientChannel.read(_commandBuffer);

            if (numread >= 16) {
                _commandBuffer.rewind();
                num_bytes = _commandBuffer.getInt();
                ack = _commandBuffer.getInt();
                cmdCode = _commandBuffer.getInt();
                success = _commandBuffer.getInt();
            } else {
                _logger.warning("Close command read " + numread
                        + " bytes expected 16");
            }

        } catch (BufferOverflowException of) {
            _logger.fine("Closed() " + of.getMessage());
            if (_logger.isLoggable(Level.FINE))
                _logger.throwing("dCacheFile", "close", of);
        } catch (BufferUnderflowException uf) {
            _logger.fine("Closed() " + uf.getMessage());
            if (_logger.isLoggable(Level.FINE))
                _logger.throwing("dCacheFile", "close", uf);
        }

        if (success != 0) {
            
            byte detail[] = new byte[_commandBuffer.remaining()];
            _commandBuffer.get(detail);

            IOException ex = new IOException(new String(detail));
            _logger.info("Error writing file to pool [" + "num_bytes = "
                    + num_bytes + ", ack = " + ack + ", cmdCode = " + cmdCode
                    + ", success = " + success + "]\n" + "Server replied: "
                    + new String(detail));
            _logger.throwing("dCacheFile", "close", ex);
            throw ex;
        }

        _poolCallback.unregisterCallback(_sessionID);
        _controlConnection.unregisterCallback(_sessionID);

        if (_logger.isLoggable(Level.FINE))
            Thread.dumpStack();

        _logger.info(super.getName() + " closed");
    }


    
    public long length() {
        return _filesize;
    }
    
    

    public int available() throws IOException {
        return Integer.MAX_VALUE;
    }
    
    

    public Mode mode() {
        return _openMode;
    }
    

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    

    public void handleStreams(DataInputStream dataIn, DataOutputStream dataOut,
            String host, SocketChannel client) {
        _clientChannel = client;
        try
        {
        	_clientChannel.socket().setSendBufferSize(800000);
        }
        catch (java.net.SocketException ex)
        {
        	
        }
        
        _logger.info("Data connection callback for " + super.getAbsolutePath());

        if (host != null)
            _poolID = host.hashCode();

        _poolReplied = true;
    }

    public void handleDoorCommand(String input[]) {
        _doorReply = input;
        _doorReplied = true;
    }
}
