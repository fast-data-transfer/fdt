package lia.util.net.common;

import java.util.ArrayList;
import java.util.HashMap;




public class FDTCommandLine {

    
    
    
    private final HashMap<String, String> optionsMap;
    
    
    private final ArrayList<String> leftArgs;
    
    
    public FDTCommandLine(final String[] args) {
        HashMap<String, String> tmpCmdOptions = new HashMap<String, String>();
        ArrayList<String> tmpParamsLeft = new ArrayList<String>();
        
        
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
        }
        
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
