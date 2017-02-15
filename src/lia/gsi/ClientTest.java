/*
 * $Id$
 */
package lia.gsi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import lia.gsi.net.GSIGssSocketFactory;

/**
 * 
 * @author Adrian Muraru
 *
 */
public class ClientTest {

	public static void main(String[] args) throws UnknownHostException, IOException {
		GSIGssSocketFactory factory = new GSIGssSocketFactory();
		Socket socket = factory.createSocket(InetAddress.getByName(args[0]), 54320, false, false);
		OutputStream outputStream = socket.getOutputStream();
		InputStream inputStream = socket.getInputStream();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		System.out.println("Received:"+br.readLine());
		PrintWriter pw = new PrintWriter(outputStream,true);
		pw.println("Hello");
	}
}
