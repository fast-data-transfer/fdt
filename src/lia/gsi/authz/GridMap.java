/*
 * Portions of this file Copyright 1999-2005 University of Chicago
 * Portions of this file Copyright 1999-2005 The University of Southern California.
 *
 * This file or a portion of this file is licensed under the
 * terms of the Globus Toolkit Public License, found at
 * http://www.globus.org/toolkit/download/license.html.
 * If you redistribute this file, with or without
 * modifications, you must include this notice in the file.
 */
package lia.gsi.authz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.globus.util.QuotedStringTokenizer;

public class GridMap implements Serializable {

	public static final String DEFAULT_GRID_MAP = "/etc/grid-security/grid-mapfile";
	private static Map gridMaps = new HashMap();

	private static final String COMMENT_CHARS = "#";
	// keywords that need to be replaced
	private static final char[] EMAIL_KEYWORD_1 = { 'e', '=' };
	private static final char[] EMAIL_KEYWORD_2 = { 'e', 'm', 'a', 'i', 'l', '=' };
	private static final char[] UID_KEYWORD = { 'u', 'i', 'd', '=' };
	// Length of key words that need to be replaced
	private static final int EMAIL_KEYWORD_1_L = 2;
	private static final int EMAIL_KEYWORD_2_L = 6;
	private static final int UID_KEYWORD_L = 4;
	// Keywords to be replaced with.
	private static final String EMAIL_KEYWORD = "emailaddress=";
	private static final String USERID_KEYWORD = "userid=";

	protected Map map;

	// the file the grim map was loaded from
	private File file;
	// last time the file was modified
	private long lastModified;

	/**
	 * Returns an instance of the default grid map. If an existing instance is already loaded, it is refreshed. The meaning of default grid map depends on the <i>grid.mapfile</i>
	 * system property. If the property is defined, this method attempts to load the map file from the file pointed to by the the property. If the "grid.mapfile" system property is
	 * not defined, the method attempts to load the map file from <code>/etc/grid-security/grid-mapfile.
	 * 
	 * @exception IOException if an error occurs while loading the default map file.
	 * @return The default map file instance.
	 */
	public static GridMap getGridMap() throws IOException {
		// java property?
		String mapFile = System.getProperty("GRIDMAP");
		// env var?
		if (mapFile == null)
			mapFile = System.getenv("GRIDMAP");
		// default one
		if (mapFile == null)
			mapFile = DEFAULT_GRID_MAP;

		return getGridMap(mapFile);
	}

	/**
	 * Returns an instance of the grid map loaded from a specific file. If an instance was loaded previously (by calling this method with the same argument), it is refreshed and
	 * returned.
	 * 
	 * @param mapFile
	 *            the file from which the grid map is to be loaded
	 * @exception IOException
	 *                if an error occurs while loading the grid map file
	 * @return The grid map file instance loaded from the specified file
	 */
	public static synchronized GridMap getGridMap(String mapFile) throws IOException {
		GridMap gridMap = (GridMap) gridMaps.get(mapFile);
		if (gridMap == null) {
			gridMap = new GridMap();
			gridMap.load(mapFile);
			gridMaps.put(mapFile, gridMap);
		} else {
			gridMap.refresh();
		}
		return gridMap;
	}

	/**
	 * Loads grid map definition from a given file.
	 * 
	 * @param file
	 *            grid map file
	 * @exception IOException
	 *                in case of I/O or parsing error.
	 */
	public void load(String file) throws IOException {
		load(new File(file));
	}

	/**
	 * Loads grid map definition from a given file.
	 * 
	 * @param file
	 *            grid map file
	 * @exception IOException
	 *                in case of I/O or parsing error.
	 */
	public synchronized void load(File file) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			this.file = file;
			this.lastModified = file.lastModified();
			load(in);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Reloads the gridmap from a file only if the gridmap was initially loaded using the {@link #load(File)  load} or {@link #load(String) load} functions. The file will only be
	 * reloaded if it has changed since the last time.
	 * 
	 * @exception IOException
	 *                in case of I/O error during reload.
	 */
	public void refresh() throws IOException {
		if (this.file == null) {
			return;
		}
		if (this.file.lastModified() != this.lastModified) {
			load(this.file);
		}
	}

	/**
	 * Loads grid map file definition from a given input stream. The input stream is not closed in case of an error.
	 * 
	 * @param input
	 *            input stream containing grid map definitions.
	 * @exception IOException
	 *                in case of I/O error or parsing error.
	 */
	public void load(InputStream input) throws IOException {
		String line;

		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		Map localMap = new HashMap();
		GridMapEntry entry;
		QuotedStringTokenizer tokenizer;
		StringTokenizer idTokenizer;

		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if ((line.length() == 0) || (COMMENT_CHARS.indexOf(line.charAt(0)) != -1)) {
				continue;
			}

			tokenizer = new QuotedStringTokenizer(line);

			String globusID = null;

			if (tokenizer.hasMoreTokens()) {
				globusID = tokenizer.nextToken();
			} else {
				throw new IOException("Globus ID not defined: " + line);
			}

			String userIDs = null;

			if (tokenizer.hasMoreTokens()) {
				userIDs = tokenizer.nextToken();
			} else {
				throw new IOException("User ID mapping missing: " + line);
			}

			idTokenizer = new StringTokenizer(userIDs, ",");
			String[] ids = new String[idTokenizer.countTokens()];
			int i = 0;
			while (idTokenizer.hasMoreTokens()) {
				ids[i++] = idTokenizer.nextToken();
			}

			String normalizedDN = normalizeDN(globusID);
			entry = (GridMapEntry) localMap.get(normalizedDN);
			if (entry == null) {
				entry = new GridMapEntry();
				entry.setGlobusID(globusID);
				entry.setUserIDs(ids);
				localMap.put(normalizedDN, entry);
			} else {
				entry.addUserIDs(ids);
			}
		}

		map = localMap;
	}

	/**
	 * Returns first local user name mapped to the specified globusID.
	 * 
	 * @param globusID
	 *            globusID
	 * @return local user name for the specified globusID. Null if the globusID is not mapped to a local user name.
	 */
	public synchronized String getUserID(String globusID) {
		String[] ids = getUserIDs(globusID);
		if (ids != null && ids.length > 0) {
			return ids[0];
		} else {
			return null;
		}
	}

	/**
	 * Returns local user names mapped to the specified globusID.
	 * 
	 * @param globusID
	 *            globusID
	 * @return array of local user names for the specified globusID. Null if the globusID is not mapped to any local user name.
	 */
	public synchronized String[] getUserIDs(String globusID) {
		if (globusID == null) {
			throw new IllegalArgumentException("globusID is null");
		}

		if (map == null) {
			// not initialized
			// if not running as root, then return own username
			// this allows someone to run as themselves without
			// having to have a gridmap file.
			String user = System.getProperty("user.name");
			if (user == null)
				return null;
			String tmpUser = user.toLowerCase();
			if (tmpUser.equals("root") || tmpUser.equals("administrator")) {
				return null;
			} else {
				return new String[] { user };
			}
		}

		GridMapEntry entry = (GridMapEntry) map.get(normalizeDN(globusID));
		return (entry == null)
				? null
				: entry.getUserIDs();
	}

	/**
	 * Checks if a given globus ID is associated with given local user account.
	 * 
	 * @param globusID
	 *            globus ID
	 * @param userID
	 *            userID
	 * @return true if globus ID is associated with given local user account, false, otherwise.
	 */
	public synchronized boolean checkUser(String globusID, String userID) {
		if (globusID == null) {
			throw new IllegalArgumentException("globusID is null");
		}
		if (userID == null) {
			throw new IllegalArgumentException("userID is null");
		}

		if (map == null) {
			// not initialized
			// if not running as root, then return own username
			// this allows someone to run as themselves without
			// having to have a gridmap file.
			String user = System.getProperty("user.name");
			if (user == null)
				return false;
			String tmpUser = user.toLowerCase();
			if (tmpUser.equals("root") || tmpUser.equals("administrator")) {
				return false;
			} else {
				return user.equalsIgnoreCase(userID);
			}
		}

		GridMapEntry entry = (GridMapEntry) map.get(normalizeDN(globusID));
		return (entry == null)
				? false
				: entry.containsUserID(userID);
	}

	/**
	 * Returns globus ID associated with the specified local user name.
	 * 
	 * @param userID
	 *            local user name
	 * @return associated globus ID, null if there is not any.
	 */
	public synchronized String getGlobusID(String userID) {
		if (userID == null) {
			throw new IllegalArgumentException("userID is null");
		}

		if (map == null) {
			return null;
		}

		Iterator iter = map.entrySet().iterator();
		Map.Entry mapEntry;
		GridMapEntry entry;
		while (iter.hasNext()) {
			mapEntry = (Map.Entry) iter.next();
			entry = (GridMapEntry) mapEntry.getValue();
			if (entry.containsUserID(userID)) {
				return entry.getGlobusID();
			}
		}
		return null;
	}

	/**
	 * Returns all globus IDs associated with the specified local user name.
	 * 
	 * @param userID
	 *            local user name
	 * @return associated globus ID, null if there is not any.
	 */
	public synchronized String[] getAllGlobusID(String userID) {
		if (userID == null) {
			throw new IllegalArgumentException("userID is null");
		}

		if (map == null) {
			return null;
		}

		Vector v = new Vector();

		Iterator iter = map.entrySet().iterator();
		Map.Entry mapEntry;
		GridMapEntry entry;
		while (iter.hasNext()) {
			mapEntry = (Map.Entry) iter.next();
			entry = (GridMapEntry) mapEntry.getValue();
			if (entry.containsUserID(userID)) {
				v.add(entry.getGlobusID());
			}
		}

		// create array of strings and add values back in
		if (v.size() == 0) {
			return null;
		}

		String idS[] = new String[v.size()];
		for (int ctr = 0; ctr < v.size(); ctr++) {
			idS[ctr] = (String) v.elementAt(ctr);
		}

		return idS;
	}

	public synchronized void map(String globusID, String userID) {
		if (globusID == null) {
			throw new IllegalArgumentException("globusID is null");
		}
		if (userID == null) {
			throw new IllegalArgumentException("userID is null");
		}

		if (map == null) {
			map = new HashMap();
		}

		String normalizedDN = normalizeDN(globusID);

		GridMapEntry entry = (GridMapEntry) map.get(normalizedDN);
		if (entry == null) {
			entry = new GridMapEntry();
			entry.setGlobusID(globusID);
			entry.setUserIDs(new String[] { userID });
			map.put(normalizedDN, entry);
		} else {
			entry.addUserID(userID);
		}
	}

	static class GridMapEntry implements Serializable {
		String globusID;
		String[] userIDs;

		public String getFirstUserID() {
			return userIDs[0];
		}

		public String[] getUserIDs() {
			return userIDs;
		}

		public String getGlobusID() {
			return globusID;
		}

		public void setGlobusID(String globusID) {
			this.globusID = globusID;
		}

		public void setUserIDs(String[] userIDs) {
			this.userIDs = userIDs;
		}

		public boolean containsUserID(String userID) {
			if (userID == null) {
				return false;
			}
			for (int i = 0; i < userIDs.length; i++) {
				if (userIDs[i].equalsIgnoreCase(userID)) {
					return true;
				}
			}
			return false;
		}

		public void addUserID(String userID) {
			if (containsUserID(userID))
				return;
			String[] ids = new String[userIDs.length + 1];
			System.arraycopy(userIDs, 0, ids, 0, userIDs.length);
			ids[userIDs.length] = userID;
			userIDs = ids;
		}

		public void addUserIDs(String[] userIDs) {
			for (int i = 0; i < userIDs.length; i++) {
				addUserID(userIDs[i]);
			}
		}

	}

	private static boolean keyWordPresent(char[] args, int startIndex, char[] keyword, int length) {

		if (startIndex + length > args.length) {
			return false;
		}

		int j = startIndex;
		for (int i = 0; i < length; i++) {
			if (args[j] != keyword[i]) {
				return false;
			}
			j++;
		}
		return true;
	}

	public static String normalizeDN(String globusID) {

		if (globusID == null) {
			return null;
		}

		globusID = globusID.toLowerCase();
		char[] globusIdChars = globusID.toCharArray();

		StringBuffer normalizedDN = new StringBuffer();

		int i = 0;

		while (i < globusIdChars.length) {

			if (globusIdChars[i] == '/') {

				normalizedDN.append("/");

				if (keyWordPresent(globusIdChars, i + 1, EMAIL_KEYWORD_1, EMAIL_KEYWORD_1_L)) {
					normalizedDN.append(EMAIL_KEYWORD);
					i = i + EMAIL_KEYWORD_1_L;
				} else if (keyWordPresent(globusIdChars, i + 1, EMAIL_KEYWORD_2, EMAIL_KEYWORD_2_L)) {
					normalizedDN.append(EMAIL_KEYWORD);
					i = i + EMAIL_KEYWORD_2_L;
				} else if (keyWordPresent(globusIdChars, i + 1, UID_KEYWORD, UID_KEYWORD_L)) {
					normalizedDN.append(USERID_KEYWORD);
					i = i + UID_KEYWORD_L;
				}
				i++;
			} else {
				normalizedDN.append(globusIdChars[i]);
				i++;
			}
		}

		return normalizedDN.toString();
	}
}
