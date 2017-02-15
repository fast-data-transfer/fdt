

package org.apache.commons.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


public class HelpFormatter
{
   

   public static final int DEFAULT_WIDTH              = 74;
   public static final int DEFAULT_LEFT_PAD           = 1;
   public static final int DEFAULT_DESC_PAD           = 3;
   public static final String DEFAULT_SYNTAX_PREFIX   = "usage: ";
   public static final String DEFAULT_OPT_PREFIX      = "-";
   public static final String DEFAULT_LONG_OPT_PREFIX = "--";
   public static final String DEFAULT_ARG_NAME        = "arg";

   

   

   public int defaultWidth;
   public int defaultLeftPad;
   public int defaultDescPad;
   public String defaultSyntaxPrefix;
   public String defaultNewLine;
   public String defaultOptPrefix;
   public String defaultLongOptPrefix;
   public String defaultArgName;

   
   public HelpFormatter()
   {
      defaultWidth = DEFAULT_WIDTH;
      defaultLeftPad = DEFAULT_LEFT_PAD;
      defaultDescPad = DEFAULT_DESC_PAD;
      defaultSyntaxPrefix = DEFAULT_SYNTAX_PREFIX;
      defaultNewLine = System.getProperty("line.separator");
      defaultOptPrefix = DEFAULT_OPT_PREFIX;
      defaultLongOptPrefix = DEFAULT_LONG_OPT_PREFIX;
      defaultArgName = DEFAULT_ARG_NAME;
   }

   

   public void printHelp( String cmdLineSyntax,
                          Options options )
   {
       printHelp( defaultWidth, cmdLineSyntax, null, options, null, false );
   }

   public void printHelp( String cmdLineSyntax,
                          Options options,
                          boolean autoUsage )
   {
       printHelp( defaultWidth, cmdLineSyntax, null, options, null, autoUsage );
   }

   public void printHelp( String cmdLineSyntax,
                          String header,
                          Options options,
                          String footer )
   {
       printHelp( cmdLineSyntax, header, options, footer, false );
   }

   public void printHelp( String cmdLineSyntax,
                          String header,
                          Options options,
                          String footer,
                          boolean autoUsage )
   {
      printHelp(defaultWidth, cmdLineSyntax, header, options, footer, autoUsage );
   }
   
   public void printHelp( int width,
                          String cmdLineSyntax,
                          String header,
                          Options options,
                          String footer )
   {
       printHelp( width, cmdLineSyntax, header, options, footer, false );
   }

   public void printHelp( int width,
                          String cmdLineSyntax,
                          String header,
                          Options options,
                          String footer,
                          boolean autoUsage )
   {
      PrintWriter pw = new PrintWriter(System.out);
      printHelp( pw, width, cmdLineSyntax, header,
                 options, defaultLeftPad, defaultDescPad, footer, autoUsage );
      pw.flush();
   }
   public void printHelp( PrintWriter pw,
                          int width,
                          String cmdLineSyntax,
                          String header,
                          Options options,
                          int leftPad,
                          int descPad,
                          String footer )
   throws IllegalArgumentException
   {
       printHelp( pw, width, cmdLineSyntax, header, options, leftPad, descPad, footer, false );
   }

   public void printHelp( PrintWriter pw,
                          int width,
                          String cmdLineSyntax,
                          String header,
                          Options options,
                          int leftPad,
                          int descPad,
                          String footer,
                          boolean autoUsage )
      throws IllegalArgumentException
   {
      if ( cmdLineSyntax == null || cmdLineSyntax.length() == 0 )
      {
         throw new IllegalArgumentException("cmdLineSyntax not provided");
      }

      if ( autoUsage ) {
          printUsage( pw, width, cmdLineSyntax, options );
      }
      else {
          printUsage( pw, width, cmdLineSyntax );
      }

      if ( header != null && header.trim().length() > 0 )
      {
         printWrapped( pw, width, header );
      }
      printOptions( pw, width, options, leftPad, descPad );
      if ( footer != null && footer.trim().length() > 0 )
      {
         printWrapped( pw, width, footer );
      }
   }

   
   public void printUsage( PrintWriter pw, int width, String app, Options options ) 
   {
       
       StringBuffer buff = new StringBuffer( defaultSyntaxPrefix ).append( app ).append( " " );
       
       
       ArrayList list = new ArrayList();

       
       Option option;

       
       for ( Iterator i = options.getOptions().iterator(); i.hasNext(); )
       {
           
           option = (Option) i.next();

           
           OptionGroup group = options.getOptionGroup( option );

           
           
           if( group != null && !list.contains(group)) {

               
               list.add( group );

               
               Collection names = group.getNames();

               buff.append( "[" ); 

               
               for( Iterator iter = names.iterator(); iter.hasNext(); ) {
                   buff.append( iter.next() );
                   if( iter.hasNext() ) {
                       buff.append( " | " );
                   }
               }
               buff.append( "]" );
           }
           
           else {
               
               if( !option.isRequired() ) {
                   buff.append( "[" );
               }
               
               if( !" ".equals( option.getOpt() ) ) {
                   buff.append( "-" ).append( option.getOpt() );
               }
               else {
                   buff.append( "--" ).append( option.getLongOpt() );
               }

               if( option.hasArg() ){
                   buff.append( " " );
               }

               
               if( option.hasArg() ) {
                   buff.append( option.getArgName() );
               }

               
               if( !option.isRequired() ) {
                   buff.append( "]" );
               }
               buff.append( " " );
           }
       }

       
       printWrapped( pw, width, buff.toString().indexOf(' ')+1,
                     buff.toString() );
   }

   public void printUsage( PrintWriter pw, int width, String cmdLineSyntax )
   {
      int argPos = cmdLineSyntax.indexOf(' ') + 1;
      printWrapped(pw, width, defaultSyntaxPrefix.length() + argPos,
                   defaultSyntaxPrefix + cmdLineSyntax);
   }

   public void printOptions( PrintWriter pw, int width, Options options, int leftPad, int descPad )
   {
      StringBuffer sb = new StringBuffer();
      renderOptions(sb, width, options, leftPad, descPad);
      pw.println(sb.toString());
   }

   public void printWrapped( PrintWriter pw, int width, String text )
   {
      printWrapped(pw, width, 0, text);
   }

   public void printWrapped( PrintWriter pw, int width, int nextLineTabStop, String text )
   {
      StringBuffer sb = new StringBuffer(text.length());
      renderWrappedText(sb, width, nextLineTabStop, text);
      pw.println(sb.toString());
   }

   

   protected StringBuffer renderOptions( StringBuffer sb,
                                         int width,
                                         Options options,
                                         int leftPad,
                                         int descPad )
   {
      final String lpad = createPadding(leftPad);
      final String dpad = createPadding(descPad);

      
      
      
      int max = 0;
      StringBuffer optBuf;
      List prefixList = new ArrayList();
      Option option;
      List optList = options.helpOptions();
      Collections.sort( optList, new StringBufferComparator() );
      for ( Iterator i = optList.iterator(); i.hasNext(); )
      {
         option = (Option) i.next();
         optBuf = new StringBuffer(8);

         if (option.getOpt().equals(" "))
         {
             optBuf.append(lpad).append("   " + defaultLongOptPrefix).append(option.getLongOpt());
         }
         else
         {
             optBuf.append(lpad).append(defaultOptPrefix).append(option.getOpt());
             if ( option.hasLongOpt() )
             {
                optBuf.append(',').append(defaultLongOptPrefix).append(option.getLongOpt());
             }

         }

         if( option.hasArg() ) {
             if( option.hasArgName() ) {
                 optBuf.append(" <").append( option.getArgName() ).append( '>' );
             }
             else {
                 optBuf.append(' ');
             }
         }

         prefixList.add(optBuf);
         max = optBuf.length() > max ? optBuf.length() : max;
      }
      int x = 0;
      for ( Iterator i = optList.iterator(); i.hasNext(); )
      {
         option = (Option) i.next();
         optBuf = new StringBuffer( prefixList.get( x++ ).toString() );

         if ( optBuf.length() < max )
         {
             optBuf.append(createPadding(max - optBuf.length()));
         }
         optBuf.append( dpad );
         
         int nextLineTabStop = max + descPad;
         renderWrappedText(sb, width, nextLineTabStop,
                           optBuf.append(option.getDescription()).toString());
         if ( i.hasNext() )
         {
             sb.append(defaultNewLine);
         }
      }

      return sb;
   }

   protected StringBuffer renderWrappedText( StringBuffer sb,
                                             int width,
                                             int nextLineTabStop,
                                             String text )
   {
      int pos = findWrapPos( text, width, 0);
      if ( pos == -1 )
      {
         sb.append(rtrim(text));
         return sb;
      }
      else
      {
         sb.append(rtrim(text.substring(0, pos))).append(defaultNewLine);
      }

      
      final String padding = createPadding(nextLineTabStop);

      while ( true )
      {
         text = padding + text.substring(pos).trim();
         pos = findWrapPos( text, width, nextLineTabStop );
         if ( pos == -1 )
         {
            sb.append(text);
            return sb;
         }

         sb.append(rtrim(text.substring(0, pos))).append(defaultNewLine);
      }

   }

   
   protected int findWrapPos( String text, int width, int startPos )
   {
      int pos = -1;
      
      if ( ((pos = text.indexOf('\n', startPos)) != -1 && pos <= width)  ||
           ((pos = text.indexOf('\t', startPos)) != -1 && pos <= width) )
      {
         return pos;
      }
      else if ( (startPos + width) >= text.length() )
      {
         return -1;
      }

      
      pos = startPos + width;
      char c;
      while ( pos >= startPos && (c = text.charAt(pos)) != ' ' && c != '\n' && c != '\r' )
      {
         --pos;
      }
      
      if ( pos > startPos )
      {
         return pos;
      }
      else
      {
         
         pos = startPos + width;
         while ( pos <= text.length() && (c = text.charAt(pos)) != ' ' && c != '\n' && c != '\r' )
         {
            ++pos;
         }
         return pos == text.length() ? -1 : pos;
      }
   }

   protected String createPadding(int len)
   {
      StringBuffer sb = new StringBuffer(len);
      for ( int i = 0; i < len; ++i )
      {
         sb.append(' ');
      }
      return sb.toString();
   }

   protected String rtrim( String s )
   {
      if ( s == null || s.length() == 0 )
      {
         return s;
      }

      int pos = s.length();
      while ( pos >= 0 && Character.isWhitespace(s.charAt(pos-1)) )
      {
         --pos;
      }
      return s.substring(0, pos);
   }

   
   
   
   
   

    private static class StringBufferComparator
    implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            String str1 = stripPrefix(o1.toString());
            String str2 = stripPrefix(o2.toString());
            return (str1.compareTo(str2));
        }

        private String stripPrefix(String strOption)
        {
            
            int iStartIndex = strOption.lastIndexOf('-');
            if (iStartIndex == -1)
            {
              iStartIndex = 0;
            }
            return strOption.substring(iStartIndex);

        }
    }
}
