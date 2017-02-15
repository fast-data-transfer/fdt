

package org.apache.commons.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class Options {

    
    private Map  shortOpts    = new HashMap();

    
    private Map  longOpts     = new HashMap();

    
    private List requiredOpts = new ArrayList();
    
    
    private Map optionGroups  = new HashMap();

    
    public Options() {        
    }

    
    public Options addOptionGroup( OptionGroup group ) {
        Iterator options = group.getOptions().iterator();

        if( group.isRequired() ) {
            requiredOpts.add( group );
        }

        while( options.hasNext() ) {
            Option option = (Option)options.next();
            
            
            
            option.setRequired( false );
            addOption( option );

            optionGroups.put( option.getOpt(), group );
        }

        return this;
    }

    
    public Options addOption(String opt, boolean hasArg, String description) {
        addOption( opt, null, hasArg, description );
        return this;
    }
    
    
    public Options addOption(String opt, String longOpt, boolean hasArg, String description) {
        addOption( new Option( opt, longOpt, hasArg, description ) );        
        return this;
    }

    
    public Options addOption(Option opt)  {
        String shortOpt = "-" + opt.getOpt();
        
        
        if ( opt.hasLongOpt() ) {
            longOpts.put( "--" + opt.getLongOpt(), opt );
        }
        
        
        if ( opt.isRequired() ) {
            requiredOpts.add( shortOpt );
        }

        shortOpts.put( shortOpt, opt );
        
        return this;
    }
    
    
    public Collection getOptions() {
        List opts = new ArrayList( shortOpts.values() );

        
        
        Iterator iter = longOpts.values().iterator();
        while (iter.hasNext())
        {
            Object item = iter.next();
            if (!opts.contains(item))
            {
                opts.add(item);
            }
        }
        return Collections.unmodifiableCollection( opts );
    }

    
    List helpOptions() {
        return new ArrayList( shortOpts.values() );
    }

    
    public List getRequiredOptions() {
        return requiredOpts;
    }
    
    
    public Option getOption( String opt ) {

        Option option = null;

        
        if( opt.length() == 1 ) {
            option = (Option)shortOpts.get( "-" + opt );
        }
        
        else if( opt.startsWith( "--" ) ) {
            option = (Option)longOpts.get( opt );
        }
        
        else {
            option = (Option)shortOpts.get( opt );
        }

        return (option == null) ? null : (Option)option.clone();
    }

    
    public boolean hasOption( String opt ) {

        
        if( opt.length() == 1 ) {
            return shortOpts.containsKey( "-" + opt );
        }
        
        else if( opt.startsWith( "--" ) ) {
            return longOpts.containsKey( opt );
        }
        
        else {
            return shortOpts.containsKey( opt );
        }
    }

    
    public OptionGroup getOptionGroup( Option opt ) {
        return (OptionGroup)optionGroups.get( opt.getOpt() );
    }
    
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        buf.append("[ Options: [ short ");
        buf.append( shortOpts.toString() );
        buf.append( " ] [ long " );
        buf.append( longOpts );
        buf.append( " ]");
        
        return buf.toString();
    }
}
