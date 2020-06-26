package com.onionnetworks.fec;

import com.onionnetworks.util.Util;

/**
 * This class provides the majority of the logic for the pure Java 
 * implementation of the vandermonde FEC codes.  This code is heavily derived
 * from Luigi Rizzo's original C implementation.  Copyright information can
 * be found in the 'LICENSE' file that comes with this distribution.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class FECMath {
    
    /**
     * The following parameter defines how many bits are used for
     * field elements.  This probably only supports 8 and 16 bit codes
     * at this time because Java lacks a typedef construct.  This code
     * should perhaps be redone with some sort of template language/
     * precompiler for Java.
     */
    
    // code over GF(2**gfBits) - change to suit
    public int gfBits;  
    
    /*
     * You should not need to change anything beyond this point.
     * The first part of the file implements linear algebra in GF.
     *
     * gf is the type used to store an element of the Galois Field.
     * Must constain at least gfBits bits.
     */
    // 2^n-1 = the number of elements in this extension field
    public int gfSize; // powers of alpha
    
    /**
     * Primitive polynomials - see Lin & Costello, Appendix A,
     * and  Lee & Messerschmitt, p. 453.
     */
    public static final String[] prim_polys = {    
                                // gfBits	polynomial
        null,  	                //  0            no code
        null,                   //  1            no code
        "111", 	                //  2            1+x+x^2
        "1101",	                //  3            1+x+x^3
        "11001",       	        //  4            1+x+x^4
        "101001",      	        //  5            1+x^2+x^5
        "1100001",     	        //  6            1+x+x^6
        "10010001",    	        //  7            1 + x^3 + x^7
        "101110001",            //  8            1+x^2+x^3+x^4+x^8
        "1000100001",           //  9            1+x^4+x^9
        "10010000001",          // 10            1+x^3+x^10
        "101000000001",         // 11            1+x^2+x^11
        "1100101000001",        // 12            1+x+x^4+x^6+x^12
        "11011000000001",       // 13            1+x+x^3+x^4+x^13
        "110000100010001",      // 14            1+x+x^6+x^10+x^14
        "1100000000000001",     // 15            1+x+x^15
        "11010000000010001"     // 16            1+x+x^3+x^12+x^16
    };

    
    /**
     * To speed up computations, we have tables for logarithm, exponent
     * and inverse of a number. If gfBits <= 8, we use a table for
     * multiplication as well (it takes 64K, no big deal even on a PDA,
     * especially because it can be pre-initialized an put into a ROM!),
     * otherwhise we use a table of logarithms.
     */
    
    // index->poly form conversion table
    public char[] gf_exp;
    // Poly->index form conversion table
    public int[] gf_log;
    // inverse of field elem.
    public char[] inverse;

    // inv[\alpha**i]=\alpha**(gfSize-i-1)

    /**
     * gf_mul(x,y) multiplies two numbers. If gfBits<=8, it is much
     * faster to use a multiplication table.
     *
     * USE_GF_MULC, GF_MULC0(c) and GF_ADDMULC(x) can be used when multiplying
     * many numbers by the same constant. In this case the first
     * call sets the constant, and others perform the multiplications.
     * A value related to the multiplication is held in a local variable
     * declared with USE_GF_MULC . See usage in addMul1().
     */
    public char[][] gf_mul_table;

    public FECMath() {
        this(8);
    }

    public FECMath(int gfBits) {
        this.gfBits = gfBits;
        this.gfSize = ((1 << gfBits) - 1);
        
        gf_exp = new char[2*gfSize];
        gf_log = new int[gfSize+1];
        inverse = new char[gfSize+1];

        if (gfBits < 2 || gfBits > 16) {
            throw new IllegalArgumentException("gfBits must be 2 .. 16");
        }
        generateGF();
        if (gfBits <= 8) {
            initMulTable();
        }
    }

    public final void generateGF() {
        int i;

        String primPoly = prim_polys[gfBits];
        
        char mask = 1;	// x ** 0 = 1
        gf_exp[gfBits] = 0; // will be updated at the end of the 1st loop
        /*
         * first, generate the (polynomial representation of) powers of \alpha,
         * which are stored in gf_exp[i] = \alpha ** i .
         * At the same time build gf_log[gf_exp[i]] = i .
         * The first gfBits powers are simply bits shifted to the left.
         */
        for (i = 0; i < gfBits; i++, mask <<= 1 ) {
            gf_exp[i] = mask;
            gf_log[gf_exp[i]] = i;
            /*
             * If primPoly[i] == 1 then \alpha ** i occurs in poly-repr
             * gf_exp[gfBits] = \alpha ** gfBits
             */
            if (primPoly.charAt(i) == '1') {
                gf_exp[gfBits] ^= mask;
            }
        }
        /*
         * now gf_exp[gfBits] = \alpha ** gfBits is complete, so can als
         * compute its inverse.
         */
        gf_log[gf_exp[gfBits]] = gfBits;
        /*
         * Poly-repr of \alpha ** (i+1) is given by poly-repr of
         * \alpha ** i shifted left one-bit and accounting for any
         * \alpha ** gfBits term that may occur when poly-repr of
         * \alpha ** i is shifted.
         */
        mask = (char) (1 << (gfBits - 1)) ;
        for (i = gfBits + 1; i < gfSize; i++) {
            if (gf_exp[i-1] >= mask) {
                gf_exp[i] = (char) (gf_exp[gfBits] ^ 
                                    ((gf_exp[i-1] ^ mask) << 1));
            } else {
                gf_exp[i] = (char) (gf_exp[i-1] << 1);
            }
            gf_log[gf_exp[i]] = i;
        }
        /*
         * log(0) is not defined, so use a special value
         */
        gf_log[0] = gfSize;
        // set the extended gf_exp values for fast multiply
        for (i = 0 ; i < gfSize ; i++) {
            gf_exp[i + gfSize] = gf_exp[i];
        }
        
        /*
         * again special cases. 0 has no inverse. This used to
         * be initialized to gfSize, but it should make no difference
         * since noone is supposed to read from here.
         */
        inverse[0] = 0 ;
        inverse[1] = 1;
        for (i=2; i<=gfSize; i++) {
            inverse[i] = gf_exp[gfSize-gf_log[i]];
        }
    }
    
    public final void initMulTable() {
        if (gfBits <= 8) {
            gf_mul_table = new char[gfSize + 1][gfSize + 1];

            int i, j;
            for (i=0; i< gfSize+1; i++) {
                for (j=0; j< gfSize+1; j++) {
                    gf_mul_table[i][j] = gf_exp[modnn(gf_log[i] + gf_log[j])];
                }
            }
            for (j=0; j< gfSize+1; j++) {
                gf_mul_table[0][j] = gf_mul_table[j][0] = 0;
            }
        }
    }

    /**
     * modnn(x) computes x % gfSize, where gfSize is 2**gfBits - 1,
     * without a slow divide.
     */
    public final char modnn(int x) {
        while (x >= gfSize) {
            x -= gfSize;
            x = (x >> gfBits) + (x & gfSize);
        }
        return (char) x;
    }

    public final char mul(char x, char y) {
        if (gfBits <= 8) {
            return gf_mul_table[x][y];
        } else {
            if (x == 0 || y == 0) { 
                return 0;
            }
     
            return gf_exp[gf_log[x] + gf_log[y]] ;
        }
    }

    /**
     * Generate GF(2**m) from the irreducible polynomial p(X) in p[0]..p[m]
     * Lookup tables:
     *     index->polynomial form		gf_exp[] contains j= \alpha^i;
     *     polynomial form -> index form	gf_log[ j = \alpha^i ] = i
     * \alpha=x is the primitive element of GF(2^m)
     *
     * For efficiency, gf_exp[] has size 2*gfSize, so that a simple
     * multiplication of two numbers can be resolved without calling modnn
     */
    public static final char[] createGFMatrix(int rows, int cols) {
        return new char[rows * cols];
    }
    
    /*
     * addMul() computes dst[] = dst[] + c * src[]
     * This is used often, so better optimize it! Currently the loop is
     * unrolled 16 times, a good value for 486 and pentium-class machines.
     * The case c=0 is also optimized, whereas c=1 is not. These
     * calls are unfrequent in my typical apps so I did not bother.
     * 
     */
    public final void addMul(char[] dst, int dstPos, char[] src, 
                             int srcPos, char c, int len) {
        // nop, optimize
        if (c == 0) {
            return;
        }

        int unroll = 16; // unroll the loop 16 times.
        int i = dstPos;
        int j = srcPos;
        int lim = dstPos + len;

        if (gfBits <= 8) { // use our multiplication table.
            // Instead of doing gf_mul_table[c,x] for multiply, we'll save
            // the gf_mul_table[c] to a local variable since it is going to
            // be used many times.
            char[] gf_mulc = gf_mul_table[c];
            
            // Not sure if loop unrolling has any real benefit in Java, but 
            // what the hey.
            for (;i < lim && (lim-i) > unroll; i += unroll, j += unroll) {
                // dst ^= gf_mulc[x] is equal to mult then add (xor == add)
                
                dst[i] ^= gf_mulc[src[j]];
                dst[i+1] ^= gf_mulc[src[j+1]];
                dst[i+2] ^= gf_mulc[src[j+2]];
                dst[i+3] ^= gf_mulc[src[j+3]];
                dst[i+4] ^= gf_mulc[src[j+4]];
                dst[i+5] ^= gf_mulc[src[j+5]];
                dst[i+6] ^= gf_mulc[src[j+6]];
                dst[i+7] ^= gf_mulc[src[j+7]];
                dst[i+8] ^= gf_mulc[src[j+8]];
                dst[i+9] ^= gf_mulc[src[j+9]];
                dst[i+10] ^= gf_mulc[src[j+10]];
                dst[i+11] ^= gf_mulc[src[j+11]];
                dst[i+12] ^= gf_mulc[src[j+12]];
                dst[i+13] ^= gf_mulc[src[j+13]];
                dst[i+14] ^= gf_mulc[src[j+14]];
                dst[i+15] ^= gf_mulc[src[j+15]];
            }
            
            // final components
            for (;i < lim; i++, j++) {
                dst[i] ^= gf_mulc[src[j]];
            }

        } else { // gfBits > 8, no multiplication table
            int mulcPos = gf_log[c];

            // unroll your own damn loop.
            int y;
            for (;i < lim;i++,j++) {
                if ((y=src[j]) != 0) {
                    dst[i] ^= gf_exp[mulcPos+gf_log[y]];
                }
            }
        }
    }

    /*
     * addMul() computes dst[] = dst[] + c * src[]
     * This is used often, so better optimize it! Currently the loop is
     * unrolled 16 times, a good value for 486 and pentium-class machines.
     * The case c=0 is also optimized, whereas c=1 is not. These
     * calls are unfrequent in my typical apps so I did not bother.
     * 
     */
    public final void addMul(byte[] dst, int dstPos, byte[] src, 
                             int srcPos, byte c, int len) {
        // nop, optimize
        if (c == 0) {
            return;
        }

        int unroll = 16; // unroll the loop 16 times.
        int i = dstPos;
        int j = srcPos;
        int lim = dstPos + len;

        // use our multiplication table.
        // Instead of doing gf_mul_table[c,x] for multiply, we'll save
        // the gf_mul_table[c] to a local variable since it is going to
        // be used many times.
        char[] gf_mulc = gf_mul_table[c & 0xff];
        
        // Not sure if loop unrolling has any real benefit in Java, but 
        // what the hey.
        for (;i < lim && (lim-i) > unroll; i += unroll, j += unroll) {
            // dst ^= gf_mulc[x] is equal to mult then add (xor == add)
            
            dst[i] ^= gf_mulc[src[j] & 0xff];
            dst[i+1] ^= gf_mulc[src[j+1] & 0xff];
            dst[i+2] ^= gf_mulc[src[j+2] & 0xff];
            dst[i+3] ^= gf_mulc[src[j+3] & 0xff];
            dst[i+4] ^= gf_mulc[src[j+4] & 0xff];
            dst[i+5] ^= gf_mulc[src[j+5] & 0xff];
            dst[i+6] ^= gf_mulc[src[j+6] & 0xff];
            dst[i+7] ^= gf_mulc[src[j+7] & 0xff];
            dst[i+8] ^= gf_mulc[src[j+8] & 0xff];
            dst[i+9] ^= gf_mulc[src[j+9] & 0xff];
            dst[i+10] ^= gf_mulc[src[j+10] & 0xff];
            dst[i+11] ^= gf_mulc[src[j+11] & 0xff];
            dst[i+12] ^= gf_mulc[src[j+12] & 0xff];
            dst[i+13] ^= gf_mulc[src[j+13] & 0xff];
            dst[i+14] ^= gf_mulc[src[j+14] & 0xff];
            dst[i+15] ^= gf_mulc[src[j+15] & 0xff];
        }
        
        // final components
        for (;i < lim; i++, j++) {
            dst[i] ^= gf_mulc[src[j] & 0xff];
        }
    }

    /*
     * computes C = AB where A is n*k, B is k*m, C is n*m
     */
    public final void matMul(char[] a, char[] b, char[] c, 
                             int n, int k, int m) {
	matMul(a,0,b,0,c,0,n,k,m);
    }

    /*
     * computes C = AB where A is n*k, B is k*m, C is n*m
     */
    public final void matMul(char[] a, int aStart, char[] b, int bStart,
 				    char[] c, int cStart, int n, int k, int m){

        for (int row = 0; row < n ; row++) {
            for (int col = 0; col < m ; col++) {
                int posA = row * k;
                int posB = col;
                char acc = 0 ;
                for (int i = 0; i < k ; i++, posA++, posB += m) {
                    acc ^= mul(a[aStart+posA],b[bStart+posB]);
                }
                c[cStart+(row * m + col)] = acc ;
            }
        }
    }
    
    /*
     * Checks to see if the square matrix is identiy
     * @return whether it is an identity matrix or not
     */
    public static final boolean isIdentity(char[] m, int k) {
        int pos = 0;
        for (int row=0; row<k; row++) {
            for (int col=0; col<k; col++) {
                if ((row==col && m[pos] != 1) || 
                    (row!=col && m[pos] != 0)) {
                    return false;
                } else {
                    pos++ ;
                }
            }
        }
        return true;
    }
    
    
    /*
     * invertMatrix() takes a matrix and produces its inverse
     * k is the size of the matrix.
     * (Gauss-Jordan, adapted from Numerical Recipes in C)
     * Return non-zero if singular.
     */
    public final void invertMatrix(char[] src, int k) 
        throws IllegalArgumentException {
        
        int[] indxc = new int[k];
        int[] indxr = new int[k];

        // ipiv marks elements already used as pivots.
        int[] ipiv = new int[k];

        char[] id_row = createGFMatrix(1, k);
        char[] temp_row = createGFMatrix(1, k);
        
        for (int col = 0; col < k ; col++) {
            /*
             * Zeroing column 'col', look for a non-zero element.
             * First try on the diagonal, if it fails, look elsewhere.
             */
            int irow = -1;
            int icol = -1;
            boolean foundPiv = false;

            if (ipiv[col] != 1 && src[col*k + col] != 0) {
                irow = col ;
                icol = col ;
                foundPiv = true;
            }
            if (!foundPiv) {
            loop1: for (int row = 0 ; row < k ; row++) {
                if (ipiv[row] != 1) {
                    for (int ix = 0 ; ix < k ; ix++) {
                        if (ipiv[ix] == 0) {
                            if (src[row*k + ix] != 0) {
                                irow = row ;
                                icol = ix ;
                                foundPiv = true;
                                break loop1;
                            }
                        } else if (ipiv[ix] > 1) {
                            throw new IllegalArgumentException
                                ("singular matrix");
                        }
                    }
                }
            }
            }

            // redundant??? I'm too lazy to figure it out -Justin
            if (!foundPiv && icol == -1) { 
                throw new IllegalArgumentException("XXX pivot not found!");
            }

            // Ok, we've found a pivot by this point, so we can set the 
            // foundPiv variable back to false.  The reason that this is
            // so shittily laid out is that the original code had goto's :(
            foundPiv = false;

            ipiv[icol] = ipiv[icol] + 1;
            /*
             * swap rows irow and icol, so afterwards the diagonal
             * element will be correct. Rarely done, not worth
             * optimizing.
             */
            if (irow != icol) {
                for (int ix = 0 ; ix < k ; ix++ ) {
                    // swap 'em
                    char tmp = src[irow*k + ix];
                    src[irow*k + ix] = src[icol*k + ix];
                    src[icol*k + ix] = tmp;
                }
            }
            indxr[col] = irow;
            indxc[col] = icol;

            int pivotRowPos = icol*k;
            char c = src[pivotRowPos + icol];
            if (c == 0) {
                throw new IllegalArgumentException("singular matrix 2");
            }
            if (c != 1) { /* otherwhise this is a NOP */
                /*
                 * this is done often , but optimizing is not so
                 * fruitful, at least in the obvious ways (unrolling)
                 */
                c = inverse[c];
                src[pivotRowPos+icol] = 1;
                for (int ix = 0 ; ix < k ; ix++ ) {
                    src[pivotRowPos+ix] = mul(c, src[pivotRowPos+ix]);
                }
            }
            /*
             * from all rows, remove multiples of the selected row
             * to zero the relevant entry (in fact, the entry is not zero
             * because we know it must be zero).
             * (Here, if we know that the pivotRowPos is the identity,
             * we can optimize the addMul).
             */
            id_row[icol] = 1;
            if (!Util.arraysEqual(src,pivotRowPos,id_row,0,k)) {
                for (int p = 0, ix = 0 ; ix < k ; ix++, p += k) {
                    if (ix != icol) {
                        c = src[p+icol];
                        src[p+icol] = 0;
                        addMul(src,p,src,pivotRowPos, c, k);
                    }
                }
            }
            id_row[icol] = 0;
        } // done all columns
        
        for (int col = k-1 ; col >= 0 ; col--) {
            if (indxr[col] <0 || indxr[col] >= k) {
                System.err.println("AARGH, indxr[col] "+indxr[col]);
            } else if (indxc[col] <0 || indxc[col] >= k) {
                System.err.println("AARGH, indxc[col] "+indxc[col]);
            } else {
                if (indxr[col] != indxc[col] ) {
                    for (int row = 0 ; row < k ; row++ ) {
                        // swap 'em
                        char tmp = src[row*k + indxc[col]];
                        src[row*k + indxc[col]] = src[row*k + indxr[col]];
                        src[row*k + indxr[col]] = tmp;
                    }
                }
            }
        }
    }
    
    /*
     * fast code for inverting a vandermonde matrix.
     * XXX NOTE: It assumes that the matrix
     * is not singular and _IS_ a vandermonde matrix. Only uses
     * the second column of the matrix, containing the p_i's.
     *
     * Algorithm borrowed from "Numerical recipes in C" -- sec.2.8, but
     * largely revised for my purposes.
     * p = coefficients of the matrix (p_i)
     * q = values of the polynomial (known)
     */
    
    public final void invertVandermonde(char[] src, int k) {

        if (k == 1) {	// degenerate case, matrix must be p^0 = 1
            return;
        }
        
        /*
         * c holds the coefficient of P(x) = Prod (x - p_i), i=0..k-1
         * b holds the coefficient for the matrix inversion
         */
        char[] c = createGFMatrix(1, k);
        char[] b = createGFMatrix(1, k);
        char[] p = createGFMatrix(1, k);
        
        for (int j=1,i=0; i < k ; i++, j+=k) {
            c[i] = 0;
            p[i] = src[j];    /* p[i] */
        }
        /*
         * construct coeffs. recursively. We know c[k] = 1 (implicit)
         * and start P_0 = x - p_0, then at each stage multiply by
         * x - p_i generating P_i = x P_{i-1} - p_i P_{i-1}
         * After k steps we are done.
         */
        c[k-1] = p[0];	/* really -p(0), but x = -x in GF(2^m) */
        for (int i = 1 ; i < k ; i++) {
            char p_i = p[i]; /* see above comment */
            for (int j = k-1  - ( i - 1 ) ; j < k-1 ; j++ ) {
                c[j] ^= mul( p_i, c[j+1] );
            }
            c[k-1] ^= p_i;
        }
        
        for (int row = 0 ; row < k ; row++ ) {
            /*
             * synthetic division etc.
             */
            char xx = p[row] ;
            char t = 1 ;
            b[k-1] = 1 ; /* this is in fact c[k] */
            for (int i = k-2 ; i >= 0 ; i-- ) {
                b[i] = (char) (c[i+1] ^ mul(xx, b[i+1])) ;
                t = (char) (mul(xx, t) ^ b[i]) ;
            }
            for (int col = 0 ; col < k ; col++ ) {
                src[col*k + row] = mul(inverse[t], b[col]);
            }
        }
    }

    public final char[] createEncodeMatrix(int k, int n) {
        if (k > gfSize + 1 || n > gfSize + 1 || 
	    k > n ) {
            throw new IllegalArgumentException
		("Invalid parameters n="+n+",k="+k+",gfSize="+
		 gfSize);
        }


        char[] encMatrix = createGFMatrix(n,k);
	
	/*
	 * The encoding matrix is computed starting with a Vandermonde matrix,
	 * and then transforming it into a systematic matrix.
	 *
         * fill the matrix with powers of field elements, starting from 0.
         * The first row is special, cannot be computed with exp. table.
         */
        char[] tmpMatrix = createGFMatrix(n, k);

        tmpMatrix[0] = 1;
	// first row should be 0's, fill in the rest.
	for (int pos = k, row = 0; row < n-1 ; row++, pos += k) {
            for (int col = 0 ; col < k ; col ++ ) {
                tmpMatrix[pos+col] = gf_exp[modnn
                                           (row*col)];
	    }
        }
        
        /*
         * quick code to build systematic matrix: invert the top
         * k*k vandermonde matrix, multiply right the bottom n-k rows
         * by the inverse, and construct the identity matrix at the top.
         */
        // much faster than invertMatrix 
        invertVandermonde(tmpMatrix, k); 
        matMul(tmpMatrix,k*k, tmpMatrix,0,encMatrix,k*k, n - k, 
                           k, k);

        /*
         * the upper matrix is I so do not bother with a slow multiply
         */
        Util.bzero(encMatrix, 0, k*k);
        for (int i = 0, col = 0; col < k ; col++, i += k+1 ) {
            encMatrix[i] = 1;
	}
        
        return encMatrix;
    }

    /**
     * createDecodeMatrix constructs the encoding matrix given the
     * indexes.
     */
    protected final char[] createDecodeMatrix(char[] encMatrix, int[] index,
                                              int k, int n) {
        
        char[] matrix = createGFMatrix(k, k);
        for (int i = 0, pos = 0; i < k ; i++, pos += k) {
            System.arraycopy(encMatrix,index[i]*k,matrix,pos,k);
        }
        
        invertMatrix(matrix, k);
        
        return matrix;
    }
}
