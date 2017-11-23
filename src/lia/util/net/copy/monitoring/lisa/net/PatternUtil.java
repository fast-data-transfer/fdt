/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa.net;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This module relies heavily on Pattern matching (it's safer that way), but also Pattern is a memory
 * consumer, so please use this class in order to retrieve a given Pattern. The main purpose of this
 * is to instantiate a pattern once in memory.
 *
 * @author Ciprian Dobre
 */
public class PatternUtil {

    /**
     * We only want to instantiante this class once, if necessary
     */
    protected static PatternUtil _p;
    /**
     * The mapping keys vs Patterns
     */
    protected final HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();

    /**
     * Call this method in order to retrieve the Pattern associated with a given key.
     *
     * @param key     The key to identify the Pattern
     * @param pattern The pattern to use if instantiation required
     * @return The pattern
     */
    public static synchronized Pattern getPattern(final String key, final String pattern) {
        return getPattern(key, pattern, false);
    }

    /**
     * Call this method in order to retrieve the Pattern associated with a given key.
     *
     * @param key     The key to identify the Pattern
     * @param pattern The pattern to use if instantiation required
     * @param takeEOL Should EndOfLine character be taken into consideration ?
     * @return The pattern
     */
    public static synchronized Pattern getPattern(final String key, final String pattern, final boolean takeEOL) {

        if (key == null) return null; // for null key return a null pattern
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

    /**
     * A high used pattern is the one used for recognizing of no such file pattern
     */
    protected Pattern getNoSuchCommand() {
        return Pattern.compile("([No such file or directory|Operation not permitted|bad command line argument])+", Pattern.CASE_INSENSITIVE);
    }

} // end of class PatternUtil

