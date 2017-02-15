

package org.apache.commons.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.commons.lang.math.NumberUtils;

    
public class TypeHandler {

    
    public static Object createValue(String str, Object obj) {
        return createValue(str, (Class)obj);
    }

    
    public static Object createValue(String str, Class clazz) {
        if( PatternOptionBuilder.STRING_VALUE == clazz) {
            return str;
        } else
        if( PatternOptionBuilder.OBJECT_VALUE == clazz) {
            return createObject(str);
        } else
        if( PatternOptionBuilder.NUMBER_VALUE == clazz) {
            return createNumber(str);
        } else
        if( PatternOptionBuilder.DATE_VALUE   == clazz) {
            return createDate(str);
        } else
        if( PatternOptionBuilder.CLASS_VALUE  == clazz) {
            return createClass(str);
        } else
        if( PatternOptionBuilder.FILE_VALUE   == clazz) {
            return createFile(str);
        } else
        if( PatternOptionBuilder.EXISTING_FILE_VALUE   == clazz) {
            return createFile(str);
        } else
        if( PatternOptionBuilder.FILES_VALUE  == clazz) {
            return createFiles(str);
        } else
        if( PatternOptionBuilder.URL_VALUE    == clazz) {
            return createURL(str);
        } else {
            return null;
        }
    }

    
    public static Object createObject(String str) {
        Class cl = null;
        try {
            cl = Class.forName(str);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Unable to find: "+str);
            return null;
        }

        Object instance = null;

        try {
            instance = cl.newInstance();
        } catch (InstantiationException cnfe) {
            System.err.println("InstantiationException; Unable to create: "+str);
            return null;
        }
        catch (IllegalAccessException cnfe) {
            System.err.println("IllegalAccessException; Unable to create: "+str);
            return null;
        }

        return instance;
    }

    
    public static Number createNumber(String str) {
        
        try {
            
            return NumberUtils.createNumber(str);
        } catch (NumberFormatException nfe) {
            System.err.println(nfe.getMessage());
            return null;
        }
    }

    
    public static Class createClass(String str) {
        try {
            return Class.forName(str);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("Unable to find: "+str);
            return null;
        }
    }

    
    public static Date createDate(String str) {
        Date date = null;
        if(date == null) {
            System.err.println("Unable to parse: "+str);
        }
        return date;
    }

    
    public static URL createURL(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException mue) {
            System.err.println("Unable to parse: "+str);
            return null;
        }
    }

    
    public static File createFile(String str) {
        return new File(str);
    }

    
    public static File[] createFiles(String str) {


        return null;
    }

}
