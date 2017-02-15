

package org.apache.commons.cli;


public class PatternOptionBuilder {

    

    
    public static final Class STRING_VALUE        = java.lang.String.class;
    
    public static final Class OBJECT_VALUE        = java.lang.Object.class;
    
    public static final Class NUMBER_VALUE        = java.lang.Number.class;
    
    public static final Class DATE_VALUE          = java.util.Date.class;
    
    public static final Class CLASS_VALUE         = java.lang.Class.class;




    
    public static final Class EXISTING_FILE_VALUE = java.io.FileInputStream.class;
    
    public static final Class FILE_VALUE          = java.io.File.class;
    
    public static final Class FILES_VALUE         = java.io.File[].class;
    
    public static final Class URL_VALUE           = java.net.URL.class;

    
    public static Object getValueClass(char ch) {
        if (ch == '@') {
            return PatternOptionBuilder.OBJECT_VALUE;
        } else if (ch == ':') {
            return PatternOptionBuilder.STRING_VALUE;
        } else if (ch == '%') {
            return PatternOptionBuilder.NUMBER_VALUE;
        } else if (ch == '+') {
            return PatternOptionBuilder.CLASS_VALUE;
        } else if (ch == '#') {
            return PatternOptionBuilder.DATE_VALUE;
        } else if (ch == '<') {
            return PatternOptionBuilder.EXISTING_FILE_VALUE;
        } else if (ch == '>') {
            return PatternOptionBuilder.FILE_VALUE;
        } else if (ch == '*') {
            return PatternOptionBuilder.FILES_VALUE;
        } else if (ch == '/') {
            return PatternOptionBuilder.URL_VALUE;
        }
        return null;
    }
 
    
    public static boolean isValueCode(char ch) {
        if( (ch != '@') &&
            (ch != ':') &&
            (ch != '%') &&
            (ch != '+') &&
            (ch != '#') &&
            (ch != '<') &&
            (ch != '>') &&
            (ch != '*') &&
            (ch != '/')
          )
        {
            return false;
        }
        return true;
    }       
 
    
    public static Options parsePattern(String pattern) {
        int sz = pattern.length();

        char opt = ' ';
        char ch = ' ';
        boolean required = false;
        Object type = null;

        Options options = new Options();
        
        for(int i=0; i<sz; i++) {
            ch = pattern.charAt(i);

            
            
            if(!isValueCode(ch)) {
                if(opt != ' ') {
                    
                    options.addOption( OptionBuilder.hasArg( type != null )
                                                    .isRequired( required )
                                                    .withType( type )
                                                    .create( opt ) );
                    required = false;
                    type = null;
                    opt = ' ';
                }
                opt = ch;
            } else
            if(ch == '!') {
                required = true;
            } else {
                type = getValueClass(ch);
            }
        }

        if(opt != ' ') {
            
            options.addOption( OptionBuilder.hasArg( type != null )
                                            .isRequired( required )
                                            .withType( type )
                                            .create( opt ) );
        }

        return options;
    }

}
