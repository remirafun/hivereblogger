/*
 * Copyright (c) 2021, mirafun
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jahive.math;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.DSAKCalculator;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

/**
 *
 * @author mirafun
 */
public class Secp256k1 {
    private static final X9ECParameters secp256k1 = CustomNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters CURVE = new ECDomainParameters(
            secp256k1.getCurve(), secp256k1.getG(), secp256k1.getN(), secp256k1.getH());
    
    public static int sig(byte[] msg, byte[] key, byte[] k0, byte[] result) throws Exception {
        if(msg.length != 32) throw new IllegalArgumentException();
        
        BigInteger priv = new BigInteger(1, key);
        HMacDSAKCalculator a = new HMacDSAKCalculator(new SHA256Digest());

        var seed = org.bouncycastle.util.Arrays.concatenate(msg, k0);
        var n = secp256k1.getN();
        var nh = n.shiftRight(1);
        var n1 = n.subtract(BigInteger.ONE);
        var msgN = new BigInteger(1, msg);
        a.init2(n, priv, seed);
        for(int i = 0; i < 100000; i++) {
            var k = a.nextK();
            if(!(0 <= k.compareTo(n1))) {
                var m = mul_G(k);
                BigInteger x = m[0], y = m[1];
                BigInteger r = x.mod(n);
                if(r.compareTo(BigInteger.ZERO) != 0) {
                    var s = k.modInverse(n).multiply(r.multiply(priv).add(msgN));
                    if ((s = s.mod(n)).compareTo(BigInteger.ZERO) != 0) {
                        var recovery = (y.testBit(0) ? 1 : 0) | (x.compareTo(r) != 0 ? 2 : 0);
                        if(0 < s.compareTo(nh)) {
                            s = n.subtract(s);
                            recovery ^= 1;
                            BigIntegers.asUnsignedByteArray(r, result, 0, 32);
                            BigIntegers.asUnsignedByteArray(s, result, 32, 32);
                            return recovery;
                        }
                    }
                }
                return -1;
            }
        }
        return -1;
    }
    public static byte[] to_fixLength(byte[] data, int len) {
        if(data.length < len) {
            byte[] out = new byte[len];
            System.arraycopy(data, 0, out, len-data.length, data.length);
            return out;
        }	
        if(data.length > len) return Arrays.copyOf(data, len);
        return data;
    }
    public static boolean verify(byte[] hash, BigInteger[] sig, String pub) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPublicKeyParameters pubKey = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(Hex.decode(pub)), CURVE);
        signer.init(false, pubKey);
        if (signer.verifySignature(hash, sig[0], sig[1])) {
            return true;
        } else {
            return false;
        }
    }
//    public static BigIntegers[] mul_G(BigInteger factor) {
//        var kp = secp256k1.getG().multiply(factor);
//    }
    public static byte[] publicKeyCreate(byte[] privateKey) {
        var d = new BigInteger(1, privateKey);
        if (0 <= d.compareTo(secp256k1.getN()) || d.compareTo(BigInteger.ZERO) == 0) throw new IllegalArgumentException();
        var arr = secp256k1.getG().multiply(d).getEncoded(true);
        return arr;
    } 
    
    public static BigInteger[] mul_G(BigInteger factor) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint Q = spec.getG().multiply(factor).normalize();
        BigInteger[] erg = new BigInteger[2];
        erg[0] = Q.getAffineXCoord().toBigInteger();
        erg[1] = Q.getAffineYCoord().toBigInteger();
        return erg;
    }
    
    
    public static class HMacDSAKCalculator implements DSAKCalculator {
        private static final BigInteger ZERO = BigInteger.valueOf(0);

        private final HMac hMac;
        private final byte[] K;
        private final byte[] V;

        private BigInteger n;
        
        private long seedCounter = 1;

        /**
         * Base constructor.
         *
         * @param digest digest to build the HMAC on.
         */
        public HMacDSAKCalculator(Digest digest) {
            this.hMac = new HMac(digest);
            this.V = new byte[hMac.getMacSize()];
            this.K = new byte[hMac.getMacSize()];
        }

        public boolean isDeterministic() {
            return true;
        }

        public void init(BigInteger n, SecureRandom random) {
            throw new IllegalStateException("Operation not supported");
        }
//        public void init(byte[] seed) {
//            Arrays.fill(K, (byte)0);
//            Arrays.fill(V, (byte)0x01);
//
//            update(seed);
//            seedCounter = 1;
//        }
//        public void update(byte[] seed) {
//            hMac.init(new KeyParameter(K));
//
//            hMac.update(V, 0, V.length);
//            hMac.update((byte)0x00);
//            hMac.update(x, 0, x.length);
//            hMac.update(m, 0, m.length);
//
//            hMac.doFinal(K, 0);
//        }
        public void init2(BigInteger n, BigInteger d, byte[] message) {
            this.n = n;

            org.bouncycastle.util.Arrays.fill(V, (byte)0x01);
            org.bouncycastle.util.Arrays.fill(K, (byte)0);

            int size = BigIntegers.getUnsignedByteLength(n);
            byte[] x = new byte[size];
            byte[] dVal = BigIntegers.asUnsignedByteArray(d);

            System.arraycopy(dVal, 0, x, x.length - dVal.length, dVal.length);

            //byte[] m = new byte[size];

            /*BigInteger mInt = bitsToInt(message);

            if (mInt.compareTo(n) >= 0)
            {
                mInt = mInt.subtract(n);
            }

            byte[] mVal = BigIntegers.asUnsignedByteArray(mInt);*

            System.arraycopy(mVal, 0, m, m.length - mVal.length, mVal.length);*/
            
            //System.out.println(mVal.length);
            //System.out.println(Arrays.equals(message, mVal));
            byte[] m = message;

            hMac.init(new KeyParameter(K));

            hMac.update(V, 0, V.length);
            hMac.update((byte)0x00);
            hMac.update(x, 0, x.length);
            hMac.update(m, 0, m.length);

            hMac.doFinal(K, 0);

            hMac.init(new KeyParameter(K));

            hMac.update(V, 0, V.length);

            hMac.doFinal(V, 0);

            hMac.update(V, 0, V.length);
            hMac.update((byte)0x01);
            hMac.update(x, 0, x.length);
            hMac.update(m, 0, m.length);

            hMac.doFinal(K, 0);

            hMac.init(new KeyParameter(K));

            hMac.update(V, 0, V.length);

            hMac.doFinal(V, 0);
        }
        public void init(BigInteger n, BigInteger d, byte[] message) {
            this.n = n;

            org.bouncycastle.util.Arrays.fill(V, (byte)0x01);
            org.bouncycastle.util.Arrays.fill(K, (byte)0);

            int size = BigIntegers.getUnsignedByteLength(n);
            byte[] x = new byte[size];
            byte[] dVal = BigIntegers.asUnsignedByteArray(d);

            System.arraycopy(dVal, 0, x, x.length - dVal.length, dVal.length);

            byte[] m = new byte[size];

            BigInteger mInt = bitsToInt(message);

            if (mInt.compareTo(n) >= 0)
            {
                mInt = mInt.subtract(n);
            }

            byte[] mVal = BigIntegers.asUnsignedByteArray(mInt);

            System.arraycopy(mVal, 0, m, m.length - mVal.length, mVal.length);

            hMac.init(new KeyParameter(K));

            hMac.update(V, 0, V.length);
            hMac.update((byte)0x00);
            hMac.update(x, 0, x.length);
            hMac.update(m, 0, m.length);

            hMac.doFinal(K, 0);

            hMac.init(new KeyParameter(K));

            hMac.update(V, 0, V.length);

            hMac.doFinal(V, 0);

            hMac.update(V, 0, V.length);
            hMac.update((byte)0x01);
            hMac.update(x, 0, x.length);
            hMac.update(m, 0, m.length);

            hMac.doFinal(K, 0);

            hMac.init(new KeyParameter(K));

            hMac.update(V, 0, V.length);

            hMac.doFinal(V, 0);
        }

        public BigInteger nextK() {
            byte[] t = new byte[BigIntegers.getUnsignedByteLength(n)];

            for (;;) {
                int tOff = 0;

                while (tOff < t.length) {
                    hMac.update(V, 0, V.length);

                    hMac.doFinal(V, 0);

                    int len = Math.min(t.length - tOff, V.length);
                    System.arraycopy(V, 0, t, tOff, len);
                    tOff += len;
                }

                BigInteger k = bitsToInt(t);

                if (k.compareTo(ZERO) > 0 && k.compareTo(n) < 0) {
                    return k;
                }

                hMac.update(V, 0, V.length);
                hMac.update((byte)0x00);

                hMac.doFinal(K, 0);

                hMac.init(new KeyParameter(K));

                hMac.update(V, 0, V.length);

                hMac.doFinal(V, 0);
            }
        }

        private BigInteger bitsToInt(byte[] t) {
            BigInteger v = new BigInteger(1, t);

            if (t.length * 8 > n.bitLength()) {
                v = v.shiftRight(t.length * 8 - n.bitLength());
            }

            return v;
        }
    }
}
