
package org.apache.commons.cli;


public interface CommandLineParser {
    
    
    public CommandLine parse( Options options, String[] arguments )
    throws ParseException;

    
    public CommandLine parse( Options options, String[] arguments, boolean stopAtNonOption )
    throws ParseException;
}