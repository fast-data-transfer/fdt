package edu.caltech.hep.dcapj.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.channels.SocketChannel;

public interface DataConnectionCallback {
    public void handleStreams(DataInputStream dataIn, DataOutputStream dataOut,
                              String host, SocketChannel client);
}
