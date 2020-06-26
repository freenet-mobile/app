package com.onionnetworks.fec;

import com.onionnetworks.util.Util;
import com.onionnetworks.util.Buffer;

/**
 *
 * This class provides the main API/SPI for the FEC library.  You cannot
 * construct an FECCode directly, rather you must use an FECCodeFactory.
 *
 * For example:
 * <code>
 *   int k = 32;
 *   int n = 256;
 *   FECCode code = FECCodeFactory.getDefault().createFECCode(k,n);
 * </code>
 *
 * All FEC
 * implementations will sublcass FECCode and implement the encode/decode
 * methods.  All codes implemented by this interface are assumed to be
 * systematic codes which means that the first k repair packets will be
 * the same as the original source packets.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public abstract class FECCode {

    protected int k,n;
    
    /**
     * Construct a new FECCode given <code>k</code> and <code>n</code>
     *
     * @param k The number of source packets to be encoded/decoded.
     * @param n The number of packets that the source packets will be encoded
     * to.
     */
    protected FECCode(int k, int n) {
        this.k = k;
        this.n = n;
    }

    /**
     * This method takes an array of source packets and generates a number
     * of repair packets from them.  This method could have taken in only
     * one repair packet to be generated, but in many cases it is more
     * efficient (and convenient) to encode multiple packets at once.  This
     * is especially true of the NativeCode implementation where data must
     * be copied and the Java->Native->Java transition is expensive.
     *
     * @param src An array of <code>k</code> byte[]'s that contain the source
     * packets to be encoded.  Often these byte[]'s are actually references
     * to a single byte[] that contains the entire source block, then you must
     * simply vary the srcOff's to pass it in in this fashion.  src[0] will
     * point to the 1st packet, src[1] to the second, etc.
     *
     * @param srcOff An array of integers which specifies the offset of each
     * each packet within its associated byte[].
     * 
     * @param repair Much like src, variable points to a number of buffers
     * to which the encoded repair packets will be written.  This array should
     * be the same length as repairOff and index.
     * 
     * @param repairOff This is the repair analog to srcOff.
     *
     * @param index This int[] specifies the indexes of the packets to be
     * encoded and written to <code>repair</code>.  These indexes must be
     * between 0..n (should probably be k..n, because encoding < k is a NOP)
     * 
     * @param packetLength the packetLength in bytes.  All of the buffers
     * in src and repair are assumed to be this long.
     */
    protected abstract void encode(byte[][] src, int[] srcOff, byte[][] repair,
                                   int[] repairOff, int[] index, 
                                   int packetLength);

    /**
     * This method takes an array of encoded packets and decodes them.  Before
     * the packets are decoded, they are shuffled so that packets that are
     * original source packets (index < k) are positioned so that their
     * index within the byte[][] is the same as their packet index.  If the
     * <code>shuffled</code> flag is set to true then it is assumed that
     * the packets are already in the proper order.  If not then they will 
     * have the references of the byte[]'s shuffled within the byte[][].  No
     * data will be copied, only references swapped.  This means that if the
     * byte[][] is wrapping an underlying byte[] then the shuffling proceedure
     * may bring the byte[][] out of sync with the underlying data structure.
     * From an SPI perspective this means that the implementation is expected
     * to follow the exact same behavior as the shuffle() method in this class
     * which means that you should simply call shuffle() if the flag is false.
     *
     * @param pkts An array of <code>k</code> byte[]'s that contain the repair
     * packets to be decoded.  The decoding proceedure will copy the decoded
     * data into the byte[]'s that are provided and will place them in order
     * within the byte[][].  If the byte[][] is already properly shuffled
     * then the byte[]'s will not be moved around in the byte[][].
     *
     * @param pktsOff An array of integers which specifies the offset of each
     * each packet within its associated byte[].
     * 
     * @param index This int[] specifies the indexes of the packets to be
     * decoded.  These indexes must be between 0..n
     * 
     * @param packetLength the packetLength in bytes.  All of the buffers
     * in pkts are assumed to be this long.
     */
    protected abstract void decode(byte[][] pkts, int[] pktsOff,
                                   int[] index, int packetLength, 
                                   boolean shuffled);

    /**
     * This method takes an array of source packets and generates a number
     * of repair packets from them.  This method could have taken in only
     * one repair packet to be generated, but in many cases it is more
     * efficient (and convenient) to encode multiple packets at once.  This
     * is especially true of the NativeCode implementation where data must
     * be copied and the Java->Native->Java transition is expensive.
     *
     * @param src An array of <code>k</code> Buffers that contain the source
     * packets to be encoded.  Often these Buffers are actually references
     * to a single byte[] that contains the entire source block.
     *
     * @param repair Much like src, variable points to a number of Buffers
     * to which the encoded repair packets will be written.  This array should
     * be the same length as index.
     * 
     * @param index This int[] specifies the indexes of the packets to be
     * encoded and written to <code>repair</code>.  These indexes must be
     * between 0..n (should probably be k..n, because encoding < k is a NOP)
     * 
     */
    public void encode(Buffer[] src, Buffer[] repair, int[] index) {
        byte[][] srcBufs = new byte[src.length][];
        int[] srcOffs = new int[src.length];
        byte[][] repairBufs = new byte[repair.length][];
        int[] repairOffs = new int[repair.length];
        for (int i=0;i<srcBufs.length;i++) {
            srcBufs[i] = src[i].b;
            srcOffs[i] = src[i].off;
        }
        for (int i=0;i<repairBufs.length;i++) {
            repairBufs[i] = repair[i].b;
            repairOffs[i] = repair[i].off;
        }

        encode(srcBufs,srcOffs,repairBufs,repairOffs,index,src[0].len);
    }

    /*
    protected void checkArguments() {
        if (index < 0 || index >= n) {
            throw new IllegalArgumentException("Invalid index "+index+
					       " (max "+(n-1)+")");
	}
        if (buf.len != repair.len) {
            throw new IllegalArgumentException
                ("Source buffer and output buffer not same length");
        }

        if (pkts.length != k || index.length != k) {
            throw new IllegalArgumentException("Must be exactly k packets "+
                                               "and index entries.");
        }
        }*/

    /**
     * This method takes an array of encoded packets and decodes them.  Before
     * the packets are decoded, they are shuffled so that packets that are
     * original source packets (index < k) are positioned so that their
     * index within the byte[][] is the same as their packet index. 
     *
     * We shuffle the packets using the copy mechanism to allow API users to 
     * be guarenteed that the Buffer[] references will not be shuffled around.
     * This allows the Buffer[] to wrap an underlying byte[], and once 
     * decoding is complete the entire block will be in the proper order 
     * in the underlying byte[].  If the packets are already in the proper
     * position then no copying will take place.
     *
     * @param pkts An array of <code>k</code> Buffers that contain the repair
     * packets to be decoded.  The decoding proceedure will copy the decoded
     * data into the Buffers that are provided and will place them in order
     * within the Buffer[].  If the Buffers are already properly shuffled
     * then no data will be copied around during the shuffle proceedure.
     *
     * @param index This int[] specifies the indexes of the packets to be
     * decoded.  These indexes must be between 0..n
     * 
     */
    public void decode(Buffer[] pkts, int[] index) {
        // Must pre-shuffle so that no future shuffles bring the byte[]'s
        // out of sync with the Buffer[]'s.  We use copyShuffle so that 
        // the Buffer[]'s don't have their references shuffled around and
        // therefore we can have the Buffer[]'s wrapping one large byte[]
        // that will be decoded with all of the data in order in that block.
        copyShuffle(pkts,index,k);

        byte[][] bufs = new byte[pkts.length][];
        int[] offs = new int[pkts.length];
        for (int i=0;i<bufs.length;i++) {
            bufs[i] = pkts[i].b;
            offs[i] = pkts[i].off;
        }
        decode(bufs,offs,index,pkts[0].len,true);
    }

    /**
     * Move packets with index < k into their position.  This method
     * copies the data using System.arraycopy rather than modifying the
     * Buffer[].
     */
    protected static final void copyShuffle(Buffer[] pkts, int index[], int k){
        byte[] b = null;
        for (int i = 0;i < k ;) {
            if (index[i] >= k || index[i] == i) {
                i++;
            } else {
                // put pkts in the right position (first check for conflicts).
                int c = index[i];
                
                if (index[c] == c) {
                    throw new IllegalArgumentException
                        ("Shuffle Error: Duplicate indexes at "+i);
                }
                // swap(index[c],index[i])
                int tmp = index[i];
                index[i] = index[c];
                index[c] = tmp;

                // swap(pkts[c],pkts[i])
                if (b == null) {
                    b = new byte[pkts[0].len];
                }
                System.arraycopy(pkts[i].b,pkts[i].off,b,0,b.length);
                System.arraycopy(pkts[c].b,pkts[c].off,pkts[i].b,pkts[i].off,
                                 b.length);
                System.arraycopy(b,0,pkts[c].b,pkts[c].off,b.length);
            }
        }
    }

    /**
     * shuffle move src packets in their position
     */
    protected static final void shuffle(byte[][] pkts, int[] pktsOff, 
                                        int[] index, int k) {
        for (int i=0; i<k;) {
            if (index[i] >= k || index[i] == i) {
                i++;
            } else {
                // put pkts in the right position (first check for conflicts).
                int c = index[i] ;
                
                if (index[c] == c) {
                    throw new IllegalArgumentException("Shuffle error at "+i);
                }
                // swap(pkts[c],pkts[i])
                byte[] tmp = pkts[i];
                pkts[i] = pkts[c];
                pkts[c] = tmp;

                // swap(pktsOff[c],pktsOff[i])
                int tmp2 = pktsOff[i];
                pktsOff[i] = pktsOff[c];
                pktsOff[c] = tmp2;

                // swap(index[c],index[i])
                tmp2 = index[i];
                index[i] = index[c];
                index[c] = tmp2;
            }
        }
    }
}
