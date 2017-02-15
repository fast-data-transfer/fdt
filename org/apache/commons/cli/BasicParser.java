
package org.apache.commons.cli;


public class BasicParser extends Parser {

    
    protected String[] flatten( Options options, 
                                String[] arguments, 
                                boolean stopAtNonOption )
    {
        
        return arguments;
    }
}