package com.onionnetworks.fec;

import com.onionnetworks.util.Util;
import com.onionnetworks.util.Buffer;
/**
 * This class, along with FECMath, provides the implementation of the pure
 * Java 16 bit FEC codes.  This is heavily dervied from Luigi Rizzos original
 * C implementation.  See the file "LICENSE" along with this distribution for
 * additional copyright information.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class Pure16Code extends PureCode {

    protected static final FECMath fecMath = new FECMath(16);


    /** Notes about large N support:
        you can just generate the top k*k vandermonde matrix, call it V,
        then invert it, then when you have k blocks generate a matrix M
        with the k rows you need <r_i> and E = M* V^{-1} is the encoding matrix
        for the systematic code which you then need to invert to perform
        the decoding. Probably there is a fast way to invert E given that M
        is also a vandermonde matrix so it is "easy" to compute M^{-1}
    */

    public Pure16Code(int k, int n) {
        super(k,n,fecMath.createEncodeMatrix(k,n));
    }

    /**
     * encode accepts as input pointers to n data packets of size sz,
     * and produces as output a packet pointed to by fec, computed
     * with index "index".
     */
    protected void encode(byte[][] src, int[] srcOff, byte[][] repair, 
                          int[] repairOff, int[] index, int packetLength) {
        if (packetLength % 2 != 0) {
            throw new IllegalArgumentException("For 16 bit codes, buffers "+
                                               "must be 16 bit aligned.");
        }
        char[][] srcChars = new char[src.length][];
        int[] srcCharsOff = new int[src.length];
        int numChars = packetLength/2;
        char[] repairChars = new char[numChars];
        for (int i=0;i<srcChars.length;i++) {
            srcChars[i] = new char[numChars];
            Util.arraycopy(src[i], srcOff[i], srcChars[i], 0, packetLength);
            srcCharsOff[i] = 0;
        }

        for (int i=0;i<repair.length;i++) {
            if (index[i] < k) { // < k, systematic so direct copy.
                System.arraycopy(src[index[i]],srcOff[index[i]],repair[i],
                                 repairOff[i], packetLength);
            } else {
                encode(srcChars,srcCharsOff,repairChars,0,index[i],numChars);
                Util.arraycopy(repairChars,0,repair[i],repairOff[i],
                               packetLength);
            }
        }
    }

    /**
     * ASSERT: index >= k && index < n
     */
    protected void encode(char[][] src, int[] srcOff, char[] repair, 
                          int repairOff, int index, int numChars) {
        int pos = index*k;
        Util.bzero(repair,repairOff,numChars);
        for (int i=0; i<k ; i++) {
            fecMath.addMul(repair,repairOff,src[i],srcOff[i],
                           encMatrix[pos+i],numChars);
        }
    }
    
    protected void decode(byte[][] pkts, int[] pktsOff, int[] index, 
                          int packetLength, boolean inOrder) {          
        if (packetLength % 2 != 0) {
            throw new IllegalArgumentException("For 16 bit codes, buffers "+
                                               "must be 16 bit aligned.");
        }

        if (!inOrder) {
            shuffle(pkts, pktsOff, index, k);
        }

        char[][] pktsChars = new char[pkts.length][];
        int[] pktsCharsOff = new int[pkts.length];
        int numChars = packetLength/2;
        for (int i=0;i<pktsChars.length;i++) {
            pktsChars[i] = new char[numChars];
            Util.arraycopy(pkts[i], pktsOff[i], pktsChars[i], 0, packetLength);
            pktsCharsOff[i] = 0;
        }

        char[][] result = decode(pktsChars, pktsCharsOff, index, numChars);

        for (int i=0;i<result.length;i++) {
            if (result[i] != null) {
                Util.arraycopy(result[i], 0, pkts[i], pktsOff[i], 
                               packetLength);
                index[i] = i;
            }
        }
    }

    protected char[][] decode(char[][] pkts, int[] pktsOff, int[] index, 
                          int numChars) {

        char[] decMatrix = fecMath.createDecodeMatrix(encMatrix,index,k,n);
        
        // do the actual decoding
        char[][] tmpPkts = new char[k][];
        for (int row=0; row<k; row++) {
            if (index[row] >= k) {
                tmpPkts[row] = new char[numChars];
                for (int col=0 ; col<k ; col++) {
                    fecMath.addMul(tmpPkts[row],0,pkts[col],pktsOff[col], 
                                   decMatrix[row*k + col], numChars);
                }
            }
        }

        return tmpPkts;
    }
    
    public String toString() {
        return new String("Pure16Code[k="+k+",n="+n+"]");
    }
}
