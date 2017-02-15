



package org.apache.commons.cli;

import java.util.ArrayList;



public class Option implements Cloneable {

    
    public final static int UNINITIALIZED = -1;
    
    
    public final static int UNLIMITED_VALUES = -2;
    
    
    private String opt;

    
    private String longOpt;

    
    private boolean hasArg;

    
    private String argName;

    
    private String description;

    
    private boolean required;

    
    private boolean optionalArg;

    
    private int numberOfArgs = UNINITIALIZED;   

    
    private Object type;

    
    private ArrayList values = new ArrayList();
    
    
    private char id;

    
    private char valuesep;

    
    private void validateOption( String opt ) 
    throws IllegalArgumentException
    {
        
        if( opt == null ) {
            throw new IllegalArgumentException( "opt is null" );
        }
        
        else if( opt.length() == 1 ) {
            char ch = opt.charAt( 0 );
            if ( !isValidOpt( ch ) ) {
                throw new IllegalArgumentException( "illegal option value '" 
                                                    + ch + "'" );
            }
            id = ch;
        }
        
        else {
            char[] chars = opt.toCharArray();
            for( int i = 0; i < chars.length; i++ ) {
                if( !isValidChar( chars[i] ) ) {
                    throw new IllegalArgumentException( "opt contains illegal character value '" + chars[i] + "'" );
                }
            }
        }
    }

    
    private boolean isValidOpt( char c ) {
        return ( isValidChar( c ) || c == ' ' || c == '?' || c == '@' );
    }

    
    private boolean isValidChar( char c ) {
        return Character.isJavaIdentifierPart( c );
    }

    
    public int getId( ) {
        return id;
    }

    
    public Option( String opt, String description ) 
    throws IllegalArgumentException
    {
        this( opt, null, false, description );
    }

    
    public Option( String opt, boolean hasArg, String description ) 
    throws IllegalArgumentException
    {
        this( opt, null, hasArg, description );
    }
    
    
    public Option( String opt, String longOpt, boolean hasArg, String description ) 
    throws IllegalArgumentException
    {
        
        validateOption( opt );

        this.opt          = opt;
        this.longOpt      = longOpt;

        
        if( hasArg ) {
            this.numberOfArgs = 1;
        }

        this.hasArg       = hasArg;
        this.description  = description;
    }
    
    
    public String getOpt() {
        return this.opt;
    }

    
    public Object getType() {
        return this.type;
    }

    
    public void setType( Object type ) {
        this.type = type;
    }
    
    
    public String getLongOpt() {
        return this.longOpt;
    }

    
    public void setLongOpt( String longOpt ) {
        this.longOpt = longOpt;
    }

    
    public void setOptionalArg( boolean optionalArg ) {
        this.optionalArg = optionalArg;
    }

    
    public boolean hasOptionalArg( ) {
        return this.optionalArg;
    }
    
    
    public boolean hasLongOpt() {
        return ( this.longOpt != null );
    }
    
    
    public boolean hasArg() {
        return this.numberOfArgs > 0 || numberOfArgs == UNLIMITED_VALUES;
    }
    
    
    public String getDescription() {
        return this.description;
    }

     
     public boolean isRequired() {
         return this.required;
     }

     
     public void setRequired( boolean required ) {
         this.required = required;
     }

     
     public void setArgName( String argName ) {
         this.argName = argName;
     }

     
     public String getArgName() {
         return this.argName;
     }

     
     public boolean hasArgName() {
         return (this.argName != null && this.argName.length() > 0 );
     }

     
     public boolean hasArgs() {
         return ( this.numberOfArgs > 1 || this.numberOfArgs == UNLIMITED_VALUES );
     }

     
     public void setArgs( int num ) {
         this.numberOfArgs = num;
     }

     
     public void setValueSeparator( char sep ) {
         this.valuesep = sep;
     }

     
     public char getValueSeparator() {
         return this.valuesep;
     }

     
     public int getArgs( ) {
         return this.numberOfArgs;
     }

    
    public String toString() {
        StringBuffer buf = new StringBuffer().append("[ option: ");
        
        buf.append( this.opt );
        
        if ( this.longOpt != null ) {
            buf.append(" ")
            .append(this.longOpt);
        }
        
        buf.append(" ");
        
        if ( hasArg ) {
            buf.append( "+ARG" );
        }
        
        buf.append(" :: ")
        .append( this.description );
        
        if ( this.type != null ) {
            buf.append(" :: ")
            .append( this.type );
        }

        buf.append(" ]");
        return buf.toString();
    }

    
    public boolean addValue( String value ) {

        switch( numberOfArgs ) {
            case UNINITIALIZED:
                return false;
            case UNLIMITED_VALUES:
                if( getValueSeparator() > 0 ) {
                    int index = 0;
                    while( (index = value.indexOf( getValueSeparator() ) ) != -1 ) {
                        this.values.add( value.substring( 0, index ) );
                        value = value.substring( index+1 );
                    }
                }
                this.values.add( value );
                return true;
            default:
                if( getValueSeparator() > 0 ) {
                    int index = 0;
                    while( (index = value.indexOf( getValueSeparator() ) ) != -1 ) {
                        if( values.size() > numberOfArgs-1 ) {
                            return false;
                        }
                        this.values.add( value.substring( 0, index ) );
                        value = value.substring( index+1 );
                    }
                }
                if( values.size() > numberOfArgs-1 ) {
                    return false;
                }
                this.values.add( value );
                return true;
        }
    }

    
    public String getValue() {
        return this.values.size()==0 ? null : (String)this.values.get( 0 );
    }

    
    public String getValue( int index ) 
    throws IndexOutOfBoundsException
    {
        return ( this.values.size()==0 ) ? null : (String)this.values.get( index );
    }

    
    public String getValue( String defaultValue ) {
        String value = getValue( );
        return ( value != null ) ? value : defaultValue;
    }

    
    public String[] getValues() {
        return this.values.size()==0 ? null : (String[])this.values.toArray(new String[]{});
    }

    
    public java.util.List getValuesList() {
        return this.values;
    }

    
    public Object clone() {
        Option option = new Option( getOpt(), getDescription() );
        option.setArgs( getArgs() );
        option.setOptionalArg( hasOptionalArg() );
        option.setRequired( isRequired() );
        option.setLongOpt( getLongOpt() );
        option.setType( getType() );
        option.setValueSeparator( getValueSeparator() );
        return option;
    }
}
