package edu.caltech.hep.dcapj;

import java.io.File;
import java.io.FileInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class contains configuration information for dCapJ. The default values
 * can be overridden by setting the environment variable
 * <code>DCAPJ_CONFIG_FILE</code> to the absolute path of the file.
 * 
 * @author Kamran Soomro
 * @author Faisal Khan
 */

public class Config {
    private Logger _logger = Logger.getLogger(Config.class.getName());

    private String _nic = null;

    // host name of form host:port
    private String _dCapDoor = null;
    private String _pnfsDir = null;

    // default in seconds
    private int _poolTimeout = 5;

    /**
     * Create an instance.
     * <p>
     * If the <code>DCAPJ_CONFIG_FILE</code> variable is defined, the values
     * defined in the file override the default values.
     * 
     */
    public Config() {
        initialize();
    }

    private void initialize() {
        String dCapConf = null;
        try {
            dCapConf = System.getenv("DCAPJ_CONFIG_FILE");
            if (dCapConf != null) {
                _logger
                        .info("DCAPJ_CONFIG_FILE environment variable is define; configuration from "
                                + " this file will take precedence over default values");
            } else {
                _logger.info("No post-configuration file for dCapJ");
                return;
            }

            File propFile = new File(dCapConf);
            if (propFile.exists()) {
                Properties prop = new Properties();
                prop.load(new FileInputStream(propFile));

                _logger.info("Loaded dCap conf file " + dCapConf);

                // extracting desired configuration
                _pnfsDir = prop.getProperty("pnfs.dir");
                _logger.fine("Config: pnfs dir = " + _pnfsDir);

                _dCapDoor = prop.getProperty("dcapdoor.hostname");
                _logger.fine("Config: dCap host = " + _dCapDoor);

                _nic = prop.getProperty("interface");
                _logger.fine("Config: local interface = " + _nic);

                String tmp = prop.getProperty("poolTimeout", "5");
                try {
                    _poolTimeout = Integer.parseInt(tmp);
                } catch (Exception e) {
                    _poolTimeout = 5;
                }
                _logger.fine("Config: pool timeout = " + _poolTimeout);

            } else {
                _logger.warning("dCap configuration file doesn't exist "
                        + dCapConf);
            }
        } catch (Exception e) {
            _logger
                    .info("Unable to load configuration file; default values will be used");
            // _logger.throwing(e);
        }
    }

    /**
     * Get the Pnfs ID of the file.
     * 
     * @return The Pnfs ID of the file.
     */
    public String getPnfsDir() {
        return _pnfsDir;
    }

    /**
     * Get the location of the dCap door.
     * 
     * @return The location of the dCap door of the form <i>hostname:port</i>
     */
    public String getdCapDoor() {
        return _dCapDoor;
    }

    /**
     * Get the interface to use for receiving callback connections from pools.
     * 
     * @return The IP address of the interface to use for callback connections.
     */
    public String getInterface() {
        if (_nic != null)
            return _nic;

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface
                    .getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                Enumeration<InetAddress> ips = nic.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress ip = ips.nextElement();
                    if (!ip.isLoopbackAddress() && ip instanceof Inet4Address) {
                        _nic = ip.getHostAddress();
                        return _nic;
                    }
                }
            }
        } catch (Exception ex) {
        }

        return null;
    }

    /**
     * Get the time to wait for a pool to respond.
     * @return The pool timeout.
     */
    public int getPoolTimeout() {
        return _poolTimeout;
    }

    /**
     * Set the time to wait for a pool to respond.
     * @param timeout The pool timeout in seconds.
     */
    public void setPoolTimeout(int timeout) {
        _poolTimeout = timeout;
    }
}
