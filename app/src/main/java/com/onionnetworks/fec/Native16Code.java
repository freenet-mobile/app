package com.onionnetworks.fec;

//import java.security.AccessController;
//import sun.security.action.*;
import com.onionnetworks.util.*;

/**
 * This class is the frontend for the JNI wrapper for the C implementation of
 * the FEC Codes.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class Native16Code extends FECCode {

    // One must be very very careful not to let code escape, it stores the
    // memory address of a fec_parms struct and if modified could give an
    // attacker the ability to point to anything in memory.
    final private long code;

    static {
        String path = NativeDeployer.getLibraryPath
            (Native8Code.class.getClassLoader(),"fec16");
        if (path != null) {
            System.load(path);
            initFEC();
        } else {
            System.out.println("Unable to find native library for fec16 for platform "+NativeDeployer.OS_ARCH);
            System.out.println(path);
        }
    }

    public Native16Code(int k, int n) {
        super(k,n);
        code = nativeNewFEC(k,n);
    }

    protected void encode(byte[][] src, int[] srcOff, byte[][] repair,
                          int[] repairOff, int[] index, int packetLength) {

        if (packetLength % 2 != 0) {
            throw new IllegalArgumentException("For 16 bit codes, buffers "+
                                               "must be 16 bit aligned.");
        }
        nativeEncode(src,srcOff,index,repair,repairOff,k,packetLength);
    }

    protected void decode(byte[][] pkts, int[] pktsOff,
                          int[] index, int packetLength, boolean inOrder) {
        if (packetLength % 2 != 0) {
            throw new IllegalArgumentException("For 16 bit codes, buffers "+
                                               "must be 16 bit aligned.");
        }
        if (!inOrder) {
            shuffle(pkts,pktsOff,index,k);
        }
        nativeDecode(pkts,pktsOff,index,k,packetLength);
    }

    protected native void nativeEncode
        (byte[][] src, int[] srcOff, int[] index, byte[][] repair,
         int[] repairOff, int k, int packetLength);

    protected native void nativeDecode(byte[][] pkts, int[] pktsOff,
                                       int[] index, int k, int packetLength);

    protected synchronized native long nativeNewFEC(int k, int n);

    protected synchronized native void nativeFreeFEC();

    protected static synchronized native void initFEC();

    protected void finalize() throws Throwable {
        nativeFreeFEC();
    }

    public String toString() {
        return new String("Native16Code[k="+k+",n="+n+"]");
    }
}
