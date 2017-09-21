/*
 * $Id$
 */
package lia.util.net.common;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple class to keep the optins given in the command line
 *
 * @author ramiro
 */

//TODO - maybe in the future will use a more standard POSIX interface for the command line
public class FDTCommandLine {

    //immutable instances - no need for synchronization

    //any params should be in this map ( if the param is only a flag it's value will be non-null )
    private final HashMap<String, String> optionsMap;

    //all the arguments which do not had an associated flag
    private final ArrayList<String> leftArgs;

    /**
     * @param args
     */
    public FDTCommandLine(final String[] args) {
        HashMap<String, String> tmpCmdOptions = new HashMap<String, String>();
        ArrayList<String> tmpParamsLeft = new ArrayList<String>();

        //parse the options first
        int i = 0;
        for (i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (i == args.length - 1 || args[i + 1].startsWith("-")) {
                    tmpCmdOptions.put(args[i], "");
                } else {
                    tmpCmdOptions.put(args[i], args[i + 1]);
                    i++;
                }
            } else {
                break;
            }
        }//for()

        for (; i < args.length; i++) {
            tmpParamsLeft.add(args[i]);
        }

        optionsMap = tmpCmdOptions;
        leftArgs = tmpParamsLeft;
    }

    public HashMap<String, String> getOptionsMap() {
        return optionsMap;
    }

    public String getOption(String key) {
        return optionsMap.get(key);
    }

    public ArrayList<String> getLeftArguments() {
        return leftArgs;
    }
}
