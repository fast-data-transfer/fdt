/*
 * $Id$
 * Created on Jul 12, 2005
 *
 */
package lia.util.net.common;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Adrian Muraru
 */
public class LocalHost {

    static public List<String> getPublicIPs4() {
        List<String> ips4 = new ArrayList<String>();
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            return ips4;
        }
        while (ifs.hasMoreElements()) {
            NetworkInterface iface = ifs.nextElement();
            Enumeration<InetAddress> iad = iface.getInetAddresses();
            while (iad.hasMoreElements()) {
                InetAddress localIP = iad.nextElement();
                if (!localIP.isSiteLocalAddress() && !localIP.isLoopbackAddress()) {
                    // found an IPv4 address and proxyAddress not set yet
                    if (localIP instanceof java.net.Inet4Address)
                        ips4.add(localIP.getHostAddress());
                }
            }
        }
        return ips4;
    }

    /**
     * @return A list of colon separated list of local public ips
     * If no public address is found an empty string is returned
     */
    static public String getStringPublicIPs4() {
        final List<String> ip4s = LocalHost.getPublicIPs4();
        if (ip4s.size() <= 0) return "";
        final StringBuilder sb = new StringBuilder();
        for (String ip : ip4s) {
            sb.append(ip).append(':');
        }
        return sb.length() >= 1 ? sb.deleteCharAt(sb.length() - 1).toString() : "";
    }

    static public List<String> getPublicIPs6() {
        List<String> ips6 = new ArrayList<String>();
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            return ips6;
        }
        while (ifs.hasMoreElements()) {
            NetworkInterface iface = ifs.nextElement();
            Enumeration<InetAddress> iad = iface.getInetAddresses();
            while (iad.hasMoreElements()) {
                InetAddress localIP = iad.nextElement();
                if (!localIP.isSiteLocalAddress() && !localIP.isLinkLocalAddress() && !localIP.isLoopbackAddress()) {
                    if (localIP instanceof java.net.Inet6Address)
                        ips6.add(localIP.getHostAddress());
                }
            }
        }
        return ips6;
    }

    static public String getPublicIP4() {

        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        while (ifs.hasMoreElements()) {
            NetworkInterface iface = ifs.nextElement();
            Enumeration<InetAddress> iad = iface.getInetAddresses();
            while (iad.hasMoreElements()) {
                InetAddress localIP = iad.nextElement();
                if (!localIP.isSiteLocalAddress() && !localIP.isLoopbackAddress()) {
                    // found an IPv4 address
                    if (localIP instanceof java.net.Inet4Address)
                        return localIP.getHostAddress();
                }
            }
        }
        return null;
    }

    static public String getPublicIP6() {

        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        while (ifs.hasMoreElements()) {
            NetworkInterface iface = ifs.nextElement();
            Enumeration<InetAddress> iad = iface.getInetAddresses();
            while (iad.hasMoreElements()) {
                InetAddress localIP = iad.nextElement();
                if (!localIP.isSiteLocalAddress() && !localIP.isLinkLocalAddress() && !localIP.isLoopbackAddress()) {
                    if (localIP instanceof java.net.Inet6Address)
                        return localIP.getHostAddress();
                }
            }
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(getPublicIP4());
        System.out.println(getPublicIP6());
        System.out.println(getPublicIPs4());
        System.out.println(getPublicIPs6());


    }

}
