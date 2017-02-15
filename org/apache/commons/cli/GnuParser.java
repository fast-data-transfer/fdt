
package org.apache.commons.cli;

import java.util.ArrayList;


public class GnuParser extends Parser {

    
    private ArrayList tokens = new ArrayList();

    
    private void init() {
        tokens.clear();
    }

    
    protected String[] flatten( Options options, 
                                String[] arguments, 
                                boolean stopAtNonOption )
    {
        init();
        boolean eatTheRest = false;
        Option currentOption = null;

        for( int i = 0; i < arguments.length; i++ ) {
            if( "--".equals( arguments[i] ) ) {
                eatTheRest = true;
                tokens.add( "--" );
            }
            else if ( "-".equals( arguments[i] ) ) {
                tokens.add( "-" );
            }
            else if( arguments[i].startsWith( "-" ) ) {
                Option option = options.getOption( arguments[i] );

                
                if( option == null ) {
                    
                    Option specialOption = options.getOption( arguments[i].substring(0,2) );
                    if( specialOption != null ) {
                        tokens.add( arguments[i].substring(0,2) );
                        tokens.add( arguments[i].substring(2) );
                    }
                    else if( stopAtNonOption ) {
                        eatTheRest = true;
                        tokens.add( arguments[i] );
                    }
                    else {
                        tokens.add( arguments[i] );
                    }
                }
                else {
                    currentOption = option;
                    
                    Option specialOption = options.getOption( arguments[i].substring(0,2) );
                    if( specialOption != null && option == null ) {
                        tokens.add( arguments[i].substring(0,2) );
                        tokens.add( arguments[i].substring(2) );
                    }
                    else if( currentOption != null && currentOption.hasArg() ) {
                        if( currentOption.hasArg() ) {
                            tokens.add( arguments[i] );
                            currentOption= null;
                        }
                        else if ( currentOption.hasArgs() ) {
                            tokens.add( arguments[i] );
                        }
                        else if ( stopAtNonOption ) {
                            eatTheRest = true;
                            tokens.add( "--" );
                            tokens.add( arguments[i] );
                        }
                        else {
                            tokens.add( arguments[i] );
                        }
                    } 
                    else if (currentOption != null ) {
                        tokens.add( arguments[i] );
                    } 
                    else if ( stopAtNonOption ) {
                        eatTheRest = true;
                        tokens.add( "--" );
                        tokens.add( arguments[i] );
                    }
                    else {
                        tokens.add( arguments[i] );
                    }
                }
            }
            else {
                tokens.add( arguments[i] );
            }

            if( eatTheRest ) {
                for( i++; i < arguments.length; i++ ) {
                    tokens.add( arguments[i] );
                }
            }
        }
        return (String[])tokens.toArray( new String[] {} );
    }
}