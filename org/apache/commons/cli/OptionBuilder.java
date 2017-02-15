

package org.apache.commons.cli;


public class OptionBuilder {

    
    private static String longopt;
    
    private static String description;
    
    private static String argName;
    
    private static boolean required;
    
    private static int numberOfArgs = Option.UNINITIALIZED;
    
    private static Object type;
    
    private static boolean optionalArg;
    
    private static char valuesep;

    
    private static OptionBuilder instance = new OptionBuilder();

    
    private OptionBuilder() {
    }

    
    private static void reset() {
        description = null;
        argName = null;
        longopt = null;
        type = null;
        required = false;
        numberOfArgs = Option.UNINITIALIZED;

        
        optionalArg = false;
        valuesep = (char) 0;
    }

    
    public static OptionBuilder withLongOpt( String longopt ) {
        instance.longopt = longopt;
        return instance;
    }

    
    public static OptionBuilder hasArg( ) {
        instance.numberOfArgs = 1;
        return instance;
    }

    
    public static OptionBuilder hasArg( boolean hasArg ) {
        instance.numberOfArgs = ( hasArg == true ) ? 1 : Option.UNINITIALIZED;
        return instance;
    }

    
    public static OptionBuilder withArgName( String name ) {
        instance.argName = name;
        return instance;
    }

    
    public static OptionBuilder isRequired( ) {
        instance.required = true;
        return instance;
    }

    
    public static OptionBuilder withValueSeparator( char sep ) {
        instance.valuesep = sep;
        return instance;
    }

    
    public static OptionBuilder withValueSeparator( ) {
        instance.valuesep = '=';
        return instance;
    }

    
    public static OptionBuilder isRequired( boolean required ) {
        instance.required = required;
        return instance;
    }

    
    public static OptionBuilder hasArgs( ) {
        instance.numberOfArgs = Option.UNLIMITED_VALUES;
        return instance;
    }

    
    public static OptionBuilder hasArgs( int num ) {
        instance.numberOfArgs = num;
        return instance;
    }

    
    public static OptionBuilder hasOptionalArg( ) {
        instance.numberOfArgs = 1;
        instance.optionalArg = true;
        return instance;
    }

    
    public static OptionBuilder hasOptionalArgs( ) {
        instance.numberOfArgs = Option.UNLIMITED_VALUES;
        instance.optionalArg = true;
        return instance;
    }

    
    public static OptionBuilder hasOptionalArgs( int numArgs ) {
        instance.numberOfArgs = numArgs;
        instance.optionalArg = true;
        return instance;
    }

    
    public static OptionBuilder withType( Object type ) {
        instance.type = type;
        return instance;
    }

    
    public static OptionBuilder withDescription( String description ) {
        instance.description = description;
        return instance;
    }

    
    public static Option create( char opt )
    throws IllegalArgumentException
    {
        return create( String.valueOf( opt ) );
    }

    
    public static Option create() 
    throws IllegalArgumentException
    {
        if( longopt == null ) {
            throw new IllegalArgumentException( "must specify longopt" );
        }

        return create( " " );
    }

    
    public static Option create( String opt ) 
    throws IllegalArgumentException
    {
        
        Option option = new Option( opt, description );

        
        option.setLongOpt( longopt );
        option.setRequired( required );
        option.setOptionalArg( optionalArg );
        option.setArgs( numberOfArgs );
        option.setType( type );
        option.setValueSeparator( valuesep );
        option.setArgName( argName );
        
        instance.reset();

        
        return option;
    }
}