/* 
 * 
 * $Id: Test2MD5Sum.java 346 2007-08-16 13:48:25Z ramiro $
 *
 * Created on December 19, 2006, 2:37 AM
 *
 */

package lia.util.net.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ramiro
 */
public class Test2MD5Sum {
    
    /** Creates a new instance of Test2MD5Sum */
    public Test2MD5Sum() {
    }
    
    public static final void main(String[] args) throws Exception {
        
        if(args.length != 2) {
            System.err.println("Usage java -jar fdt.jar " + Test2MD5Sum.class.getName() + " md5sumFile1 md5sumFile2");
            System.exit(1);
        }
        
        TreeMap<String, String> firstMap = getMap(args[0]);
        TreeMap<String, String> secondMap = getMap(args[1]);
        
        System.out.println(" FirstMaps size: " + firstMap.size() + " SecondMap size: " + secondMap.size());
        
        if(firstMap.size() != secondMap.size()) {
            System.err.println(" Different size() ... will exit");
            System.exit(1);
        }

        ArrayList<String> nokFList = new ArrayList<String>();
        //check the first map
        for(Map.Entry<String, String> entry: firstMap.entrySet() ) {
            final String fName = entry.getKey();
            final String md5Sum = entry.getValue();
            
            final String md5Check = secondMap.get(fName);
            
            if(md5Check == null) {
                System.err.println(" The file " + fName + " form first map cannot be found in the second map");
                System.exit(1);
            }
            
            if(md5Check.equals(md5Sum)) {
                System.out.println(" File " + fName + " [ " + md5Sum + " ] is OK");
            } else {
                System.err.println(" File " + fName + " [ " + md5Sum + " ] is NOT OK");
                nokFList.add(fName);
            }
        }
        
        if(nokFList.size() == 0) {
            System.out.println(" Total md5sums compared: " + firstMap.size() + " ... All OK!");
        } else {
            System.out.println(" Total md5sums compared: " + firstMap.size() + " ...  NOT OK = " + nokFList.size());
            int i = 1;
            for(String fName: nokFList) {
                System.out.println(" NOKFile " + (i++) +" : " + fName);
            }
        }

    }
    
    private static final TreeMap<String, String> getMap(final String fName) throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(fName));
        
        String line;
        TreeMap<String, String> map = new TreeMap<String, String>();
        
        while((line = br.readLine()) != null)  {
            int firstSpace = line.indexOf(" ");
            if(firstSpace < 0) {
                System.out.println(" Ignoring line " + line + " from file " + fName);
            } else {
                map.put(line.substring(firstSpace + 1), line.substring(0, firstSpace));
            }
        }
        
        br.close();
        
        return map;
    }
}
