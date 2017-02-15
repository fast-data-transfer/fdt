package edu.caltech.hep.dcapj;

import edu.caltech.hep.dcapj.util.IOCallback;
import edu.caltech.hep.dcapj.util.ControlConnection;



public class dCapLayer {
    
	public static boolean _isInitialized = false;

	
    static IOCallback _dataConnectionCallback = null;

    
    static ControlConnection _controlConnection = null;

    
    static Config _conf = null;

    
    public static void initialize() throws Exception {
        if (!_isInitialized) {
            _conf = new Config();
            _dataConnectionCallback = new IOCallback();
            _controlConnection = new ControlConnection();
        }
        _isInitialized = true;
    }

    
    public static IOCallback getDataConnectionCallback() {
        return _dataConnectionCallback;
    }

    
    public static ControlConnection getControlConnection() {
        return _controlConnection;
    }

    
    public static Config getConfig() {
        return _conf;
    }

    
    public static void close() {
        _controlConnection.stop();
        _dataConnectionCallback.shutdown();
        _isInitialized = false;
        System.out.println("dCapLayer closed!");
    }
}
