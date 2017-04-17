package edu.caltech.hep.dcapj;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.caltech.hep.dcapj.util.IOCallback;
import edu.caltech.hep.dcapj.util.ControlConnection;

/**
 * This class should be called by the application before starting any IO
 * operation using dcapJ protocol
 * 
 * @author Faisal Khan
 */

public class dCapLayer {

    /** guards _initialized flag */
    private static final Object initializedLock = new Object();

	/** The IOCallback object that handles the mapping between session IDs and sessions. */
    static IOCallback _dataConnectionCallback = null;

    /** The ControlConnection object. */
    static ControlConnection _controlConnection = null;

    /** The Config object. */
    static Config _conf = null;
    
    private static volatile boolean _isInitialized = false;
    

    /**
     * Initialize the library.
     * 
     * @throws Exception
     *             If an error occurred
     */
    public static void initialize() throws Exception {
        synchronized(initializedLock) {
            if (!_isInitialized) {
                _conf = new Config();
                _dataConnectionCallback = new IOCallback();
                _controlConnection = new ControlConnection();
            }
            _isInitialized = true;
        }
    }

    public static final boolean isInitialized() {
        synchronized(initializedLock) {
            return _isInitialized;
        }
    }
    
    /**
     * Get the IOCallback object.
     * @return The IOCallback object.
     */
    public static IOCallback getDataConnectionCallback() {
        return _dataConnectionCallback;
    }

    /**
     * Gets the ControlConnection object.
     * @return The ControlConnection object.
     */
    public static ControlConnection getControlConnection() {
        return _controlConnection;
    }

    /**
     * Gets the Config object.
     * @return The Config object.
     */
    public static Config getConfig() {
        return _conf;
    }

    /**
     * Close the library.
     * 
     */
    public static void close() {
        _controlConnection.stop();
        _dataConnectionCallback.shutdown();
        _isInitialized = false;
        System.out.println("dCapLayer closed!");
    }
}
