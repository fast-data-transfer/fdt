
package org.apache.commons.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


public class PosixParser extends Parser {

    
    private ArrayList tokens = new ArrayList();
    
    private boolean eatTheRest;
    
    private Option currentOption;
    
    private Options options;

    
    private void init() {
        eatTheRest = false;
        tokens.clear();
        currentOption = null;
    }

    
    protected String[] flatten( Options options, 
                                String[] arguments, 
                                boolean stopAtNonOption )
    {
        init();
        this.options = options;

        
        Iterator iter = Arrays.asList( arguments ).iterator();
        String token = null;
        
        
        while ( iter.hasNext() ) {

            
            token = (String) iter.next();

            
            if( token.startsWith( "--" ) ) {
                if( token.indexOf( '=' ) != -1 ) {
                    tokens.add( token.substring( 0, token.indexOf( '=' ) ) );
                    tokens.add( token.substring( token.indexOf( '=' ) + 1,
                                                 token.length() ) );
                }
                else {
                    tokens.add( token );
                }	
            }
            
            else if( "-".equals( token ) ) {
                processSingleHyphen( token );
            }
            else if( token.startsWith( "-" ) ) {
                int tokenLength = token.length();
                if( tokenLength == 2 ) {
                    processOptionToken( token, stopAtNonOption );
                }
                
                else {
                    burstToken( token, stopAtNonOption );
                }
            }
            else {
                if( stopAtNonOption ) {
                    process( token );
                }
                else {
                    tokens.add( token );
                }
            }

            gobble( iter );
        }

        return (String[])tokens.toArray( new String[] {} );
    }

    
    private void gobble( Iterator iter ) {
        if( eatTheRest ) {
            while( iter.hasNext() ) {
                tokens.add( iter.next() );
            }
        }
    }

    
    private void process( String value ) {
        if( currentOption != null && currentOption.hasArg() ) {
            if( currentOption.hasArg() ) {
                tokens.add( value );
                currentOption = null;
            }
            else if (currentOption.hasArgs() ) {
                tokens.add( value );
            }
        }
        else {
            eatTheRest = true;
            tokens.add( "--" );
            tokens.add( value );
        }
    }

    
    private void processSingleHyphen( String hyphen ) {
        tokens.add( hyphen );
    }

    
    private void processOptionToken( String token, boolean stopAtNonOption ) {
        if( this.options.hasOption( token ) ) {
            currentOption = this.options.getOption( token );
            tokens.add( token );
        }
        else if( stopAtNonOption ) {
            eatTheRest = true;
        }
    }

    
    protected void burstToken( String token, boolean stopAtNonOption ) {
        int tokenLength = token.length();

        for( int i = 1; i < tokenLength; i++) {
            String ch = String.valueOf( token.charAt( i ) );
            boolean hasOption = options.hasOption( ch );

            if( hasOption ) {
                tokens.add( "-" + ch );
                currentOption = options.getOption( ch );
                if( currentOption.hasArg() && token.length()!=i+1 ) {
                    tokens.add( token.substring( i+1 ) );
                    break;
                }
            }
            else if( stopAtNonOption ) {
                process( token.substring( i ) );
            }
            else {
                tokens.add( "-" + ch );
            }
        }
    }
}