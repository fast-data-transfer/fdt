package edu.caltech.hep.dcapj.util;

import java.nio.channels.SocketChannel;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface DataConnectionCallback {
    public void handleStreams(DataInputStream dataIn, DataOutputStream dataOut,
            String host, SocketChannel client);
}
