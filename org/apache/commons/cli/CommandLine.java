
package org.apache.commons.cli;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class CommandLine {
    
    
    private List args    = new LinkedList();

    
    private Map options = new HashMap();

    
    private Map hashcodeMap = new HashMap();

    
    private Option[] optionsArray;

    
    CommandLine() {
    }
    
    
    public boolean hasOption(String opt) {
        return options.containsKey( opt );
    }

    
    public boolean hasOption( char opt ) {
        return hasOption( String.valueOf( opt ) );
    }

    
    public Object getOptionObject( String opt ) {
        String res = getOptionValue( opt );
        
        Object type = ((Option)((List)options.get(opt)).iterator().next()).getType();
        return res == null ? null : TypeHandler.createValue(res, type);
    }

    
    public Object getOptionObject( char opt ) {
        return getOptionObject( String.valueOf( opt ) );
    }

    
    public String getOptionValue( String opt ) {
        String[] values = getOptionValues(opt);
        return (values == null) ? null : values[0];
    }

    
    public String getOptionValue( char opt ) {
        return getOptionValue( String.valueOf( opt ) );
    }

    
    public String[] getOptionValues( String opt ) {
        List values = new java.util.ArrayList();

        if( options.containsKey( opt ) ) {
            List opts = (List)options.get( opt );
            Iterator iter = opts.iterator();

            while( iter.hasNext() ) {
                Option optt = (Option)iter.next();
                values.addAll( optt.getValuesList() );
            }
        }
        return (values.size() == 0) ? null : (String[])values.toArray(new String[]{});
    }

    
    public String[] getOptionValues( char opt ) {
        return getOptionValues( String.valueOf( opt ) );
    }
    
    
    public String getOptionValue( String opt, String defaultValue ) {
        String answer = getOptionValue( opt );
        return ( answer != null ) ? answer : defaultValue;
    }
    
    
    public String getOptionValue( char opt, String defaultValue ) {
        return getOptionValue( String.valueOf( opt ), defaultValue );
    }

    
    public String[] getArgs() {
        String[] answer = new String[ args.size() ];
        args.toArray( answer );
        return answer;
    }
    
    
    public List getArgList() {
        return args;
    }
    
    
    

    
    void addArg(String arg) {
        args.add( arg );
    }
        
    
    void addOption( Option opt ) {
        hashcodeMap.put( new Integer( opt.hashCode() ), opt );

        String key = opt.getOpt();
        if( " ".equals(key) ) {
            key = opt.getLongOpt();
        }

        if( options.get( key ) != null ) {
            ((java.util.List)options.get( key )).add( opt );
        }
        else {
            options.put( key, new java.util.ArrayList() );
            ((java.util.List)options.get( key ) ).add( opt );
        }
    }

    
    public Iterator iterator( ) {
        return hashcodeMap.values().iterator();
    }

    
    public Option[] getOptions( ) {
        Collection processed = hashcodeMap.values();

        
        optionsArray = new Option[ processed.size() ];

        
        return (Option[]) processed.toArray( optionsArray );
    }

}
