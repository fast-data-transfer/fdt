
package lia.util.net.copy.monitoring.lisa.net;

import java.util.HashMap;
import java.util.regex.Pattern;


public class PatternUtil {

	
	protected final HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();
	
	
	protected static PatternUtil _p;
	
	
	public static synchronized Pattern getPattern(final String key, final String pattern) {
		return getPattern(key, pattern, false);
	}
	
	
	public static synchronized Pattern getPattern(final String key, final String pattern, final boolean takeEOL) {
		
		if (key == null) return null; 
		if (_p == null) {
			_p = new PatternUtil();
			_p.patterns.put("Unknown command", _p.getNoSuchCommand());
		}
		if (!_p.patterns.containsKey(key)) {
			final Pattern p = takeEOL ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL) : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			_p.patterns.put(key, p);
			return p;
		}
		return _p.patterns.get(key);
	}
	
	
	protected Pattern getNoSuchCommand() {
		return Pattern.compile("([No such file or directory|Operation not permitted|bad command line argument])+", Pattern.CASE_INSENSITIVE);
	}
	
} 

