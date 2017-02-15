

package org.apache.commons.cli;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public abstract class Parser implements CommandLineParser {

    
    private CommandLine cmd;
    
    private Options options;
    
    private List requiredOptions;

    
    abstract protected String[] flatten( Options opts, 
                                         String[] arguments, 
                                         boolean stopAtNonOption );

    
    public CommandLine parse( Options options, String[] arguments ) 
    throws ParseException 
    {
        return parse( options, arguments, false );
    }

    
    public CommandLine parse( Options opts, 
                              String[] arguments, 
                              boolean stopAtNonOption ) 
    throws ParseException 
    {
        
        options = opts;
        requiredOptions = options.getRequiredOptions();
        cmd = new CommandLine();

        boolean eatTheRest = false;

        List tokenList = Arrays.asList( flatten( opts, arguments, stopAtNonOption ) );
        ListIterator iterator = tokenList.listIterator();

        
        while( iterator.hasNext() ) {
            String t = (String)iterator.next();

            
            if( "--".equals( t ) ) {
                eatTheRest = true;
            }
            
            else if( "-".equals( t ) ) {
                if( stopAtNonOption ) {
                    eatTheRest = true;
                }
                else {
                    cmd.addArg(t );
                }
            }
            
            else if( t.startsWith( "-" ) ) {
                if ( stopAtNonOption && !options.hasOption( t ) ) {
                    eatTheRest = true;
                    cmd.addArg( t );
                }
                else {
                    processOption( t, iterator );
                }
            }
            
            else {
                cmd.addArg( t );
                if( stopAtNonOption ) {
                    eatTheRest = true;
                }
            }

            
            if( eatTheRest ) {
                while( iterator.hasNext() ) {
                    String str = (String)iterator.next();
                    
                    if( !"--".equals( str ) ) {
                        cmd.addArg( str );
                    }
                }
            }
        }
        checkRequiredOptions();
        return cmd;
    }

    
    private void checkRequiredOptions()
    throws MissingOptionException 
    {

        
        
        if( requiredOptions.size() > 0 ) {
            Iterator iter = requiredOptions.iterator();
            StringBuffer buff = new StringBuffer();

            
            while( iter.hasNext() ) {
                buff.append( iter.next() );
            }

            throw new MissingOptionException( buff.toString() );
        }
    }

    public void processArgs( Option opt, ListIterator iter ) 
    throws ParseException
    {
        
        while( iter.hasNext() ) {
            String var = (String)iter.next();

            
            if( options.hasOption( var ) ) {
                iter.previous();
                break;
            }
            
            else if( !opt.addValue( var ) ) {
                iter.previous();
                break;
            }
        }

        if( opt.getValues() == null && !opt.hasOptionalArg() ) {
            throw new MissingArgumentException( "no argument for:" + opt.getOpt() );
        }
    }

    private void processOption( String arg, ListIterator iter ) 
    throws ParseException
    {
        
        Option opt = null;

        boolean hasOption = options.hasOption( arg );

        
        if( !hasOption ) {
            throw new UnrecognizedOptionException("Unrecognized option: " + arg);
        }
        else {
            opt = (Option) options.getOption( arg );
        }

        
        
        if ( opt.isRequired() ) {
            requiredOptions.remove( "-" + opt.getOpt() );
        }

        
        
        if ( options.getOptionGroup( opt ) != null ) {
            OptionGroup group = ( OptionGroup ) options.getOptionGroup( opt );
            if( group.isRequired() ) {
                requiredOptions.remove( group );
            }
            group.setSelected( opt );
        }

        
        if ( opt.hasArg() ) {
            processArgs( opt, iter );
        }

        
        cmd.addOption( opt );
    }
}