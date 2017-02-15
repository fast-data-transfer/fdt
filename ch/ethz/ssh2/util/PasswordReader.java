package ch.ethz.ssh2.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PasswordReader {
	public static String readPassword(String prompt) {
		System.out.print(prompt);
		try {
			return String.valueOf(System.console().readPassword());
		} catch (Throwable t) {
			
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				return in.readLine();
			} catch (IOException e) {
				return null;
			}
		}
	}
}
