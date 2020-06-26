package com.onionnetworks.fec;

import java.util.*;
import java.io.IOException;
import java.lang.reflect.*;
import com.onionnetworks.util.Tuple;
import com.onionnetworks.util.TimedSoftHashMap;

/**
 * This is the default FECCodeFactory that wraps all of the FECCode 
 * implementations.  It provides a way to customize the codes through
 * a properties file specified by the property 
 * "com.onionnetworks.fec.defaultfeccodefactorypropertiesfile".  By default
 * it will use the "lib/fec.properties" file distributed with the JAR.
 * Please consult this file for an example of how this should be done.
 *
 * The properties in this file can also be passed manually to the 
 * System properties.  Again, consult the provided properties file for an
 * example.  This given, if you are adding new codes to this stuff please
 * let me know because I worked my ass of to provide this for you, so do me
 * a favor and at least let me know what you're using this for.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class DefaultFECCodeFactory extends FECCodeFactory {

    public static final int DEFAULT_CACHE_TIME = 2*60*1000;

    //protected TimedSoftHashMap codeCache = new HashMap();
    protected ArrayList eightBitCodes = new ArrayList();
    protected ArrayList sixteenBitCodes = new ArrayList();
    protected Properties fecProperties;

    public DefaultFECCodeFactory() {
        // Load in the properties file.
        try {
            fecProperties = new Properties();
            fecProperties.load
                (DefaultFECCodeFactory.class.getClassLoader().
                 getResourceAsStream
                 (System.getProperty
                  ("com.onionnetworks.fec.defaultfeccodefactorypropertiesfile",
                   "lib/fec.properties")));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException
                ("Unable to load /lib/fec.properties");
        }

        // Parse the keys
        StringTokenizer st = new StringTokenizer
            (getProperty("com.onionnetworks.fec.keys"),",");

        // Load the codes into the HashMaps.
        while (st.hasMoreTokens()) {
            String key = st.nextToken();
            try {
                Constructor con = Class.forName
                    (getProperty("com.onionnetworks.fec."+key+".class")).
                    getConstructor(new Class[] {int.class, int.class});
                String numBits = getProperty("com.onionnetworks.fec."+key+".bits");
                if ("8".equals(numBits)) {
                    eightBitCodes.add(con);
                } else if ("16".equals(numBits)) {
                    sixteenBitCodes.add(con);
                } else {
                    throw new IllegalArgumentException
                        ("Only 8 and 16 bit codes are currently supported");
                }
            } catch (Throwable t) {
                System.out.println(t.getMessage());
            }
        }
    }

    /**
     * Get a value, trying the System properties first and then checking
     * the fecProperties.
     */
    protected synchronized String getProperty(String key) {
        String result = System.getProperty(key);
        if (result == null) {
            result = fecProperties.getProperty(key);
        }
        return result;
    }

    /**
     * If you're only asking for an 8 bit code we will NOT give you a 16 bit
     * one.
     */
    public synchronized FECCode createFECCode(int k, int n) {
        Integer K = new Integer(k);
        Integer N = new Integer(n);
        Tuple t = new Tuple(K,N);

        // See if there is a cached code.
        FECCode result = null; //(FECCode) codeCache.get(t);
        if (result == null) {
            if (k < 1 || k > 65536 || n < k || n > 65536) {
                throw new IllegalArgumentException
                    ("k and n must be between 1 and 65536 and n must not be "+
                     "smaller than k: k="+k+",n="+n);
            }

            Iterator it;
            if (n <= 256 && !eightBitCodes.isEmpty()) {
                it = eightBitCodes.iterator();
            } else {
                it = sixteenBitCodes.iterator();
            }
            while (it.hasNext()) {
                try {
                    result = (FECCode) ((Constructor) it.next()).newInstance
                        (new Object[] {K, N});
                    break;
                } catch (Throwable doh) {
                    doh.printStackTrace();
                }
            }
                        
            //if (result != null) {
            //    codeCache.put(t,result,DEFAULT_CACHE_TIME);
            // }
        } 
        return result;
    }
}
