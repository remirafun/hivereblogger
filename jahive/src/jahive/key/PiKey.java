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
package jahive.key;

import jahive.math.Secp256k1;
import jahive.tool.B58;
import jahive.tool.Tool;
import static jahive.tool.Tool.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * ECDSA (secp256k1) private key.
 * @author mirafun
 */
public class PiKey implements Signable {
    
    private Signable sign;
    public byte[] key;
    public PiKey(byte[] data) { this(data, false); }
    public PiKey(byte[] data, boolean hideKey) {
        if(hideKey) {
            //optionally hide key in memory
            //and reveal it only when signing
            Random r = new Random();
            byte[] h = new byte[data.length];
            r.nextBytes(h);
            final int ri = r.nextInt();
            for(int i = 0; i < data.length; i++) data[i] ^= h[i]^ri;
            
            sign = (byte[] msg) -> {
                byte[] tmp = null;
                try {
                    tmp = new byte[data.length];
                    for(int i = 0; i < data.length; i++) tmp[i] = (byte)(data[i]^h[i]^ri);
                    return PiKey.sign(msg, tmp);
                }
                catch(Exception e) {
                    return null;
                }
                finally {
                    Arrays.fill(tmp, (byte)0);
                }
            };
        }
        else this.key = data;
    }
   
    public static PiKey fromLogin(String username, String pass, String role) throws Exception {
        var seed = username+role+pass;
        return fromSeed(seed);
    }
    public static PiKey fromSeed(String seed) throws Exception {
        return new PiKey(sha256(seed.getBytes(StandardCharsets.UTF_8)));
    }
    public static PiKey fromString(String str) throws Exception {
        return fromString(str.getBytes(StandardCharsets.UTF_8));
    }
    public static PiKey fromString(byte[] str) throws Exception {
        var d = decodePrivate(str);
        if(d == null) return null;
        return new PiKey(d);
    }
    
    @Override
    public Signature sign(byte[] msg) {
        if(key != null) try {
            return sign(msg, key);
        } catch (Exception ex) {
            return null;
        }
        else return sign.sign(msg);
    }
    
    public static Signature sign(byte[] msg, byte[] key) throws Exception {
        int attempts = 0;
        byte[] tmp = Arrays.copyOf(msg, msg.length+1);
        byte[] result = new byte[64];
        for(int i = 0; i < 256; i++) {
            tmp[tmp.length-1] = (byte)(++attempts);
            var k = Tool.sha256(tmp);
            int recovery = Secp256k1.sig(msg, key, k, result);
            if(recovery == -1) continue;            
            if(isCanonicalSignature(result)) 
                return new Signature(result, recovery);
        }    
        return null;
    }
    public static boolean isCanonicalSignature(byte[] sig) {
           return !((128 & sig[0]) != 0 ||
               0 == sig[0] &&
               !((128 & sig[1]) != 0 ) 
               || (128 & sig[32]) != 0 
               || 0 == sig[32] 
               && !((128 & sig[33]) != 0));
    }
    public static byte[] encodePrivate(byte[] arr) throws Exception {
        if(arr[0] != NETWORK_ID) return null;
        byte[] data = B58.en(arr);
        byte[] sha = sha256sha256(data, 0, data.length-4);
        byte[] re = Arrays.copyOf(data, data.length+4);
        for(int i = 0; i < 4; i++) re[data.length+i] = sha[i];
        return re;
    }
    public static byte[] decodePrivate(byte[] arr) throws Exception {
        byte[] data = B58.de(arr);
        if(data[0] != NETWORK_ID) return null;
        byte[] sha = sha256sha256(data, 0, data.length-4);
        for(int i = 0; i < 4; i++) 
            if(data[data.length-4+i] != sha[i]) return null;
        byte[] key = new byte[data.length-5];
        for(int i = 1; i < data.length-4; i++) key[i-1] = data[i];
        return key;
    }
    public PuKey createPublic(String prefix) {
        if(key == null) return null;
        return new PuKey(Secp256k1.publicKeyCreate(key), prefix);
    }
}
