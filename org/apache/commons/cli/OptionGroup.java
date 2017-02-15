

package org.apache.commons.cli;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class OptionGroup {

    
    private HashMap optionMap = new HashMap();

    
    private String selected;

    
    private boolean required;

    
    public OptionGroup addOption(Option opt) {
        
        
        optionMap.put( "-" + opt.getOpt(), opt );
        return this;
    }

    
    public Collection getNames() {
        
        return optionMap.keySet();
    }

    
    public Collection getOptions() {
        
        return optionMap.values();
    }

    
    public void setSelected(Option opt) throws AlreadySelectedException {
        
        
        

        if ( this.selected == null || this.selected.equals( opt.getOpt() ) ) {
            this.selected = opt.getOpt();
        }
        else {
            throw new AlreadySelectedException( "an option from this group has " + 
                                                "already been selected: '" + 
                                                selected + "'");
        }
    }

    
    public String getSelected() {
        return selected;
    }

    
    public void setRequired( boolean required ) {
        this.required = required;
    }

    
    public boolean isRequired() {
        return this.required;
    }

    
    public String toString() {
        StringBuffer buff = new StringBuffer();

        Iterator iter = getOptions().iterator();

        buff.append( "[" );
        while( iter.hasNext() ) {
            Option option = (Option)iter.next();

            buff.append( "-" );
            buff.append( option.getOpt() );
            buff.append( " " );
            buff.append( option.getDescription( ) );

            if( iter.hasNext() ) {
                buff.append( ", " );
            }
        }
        buff.append( "]" );

        return buff.toString();
    }
}
