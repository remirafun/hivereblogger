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
package jahive.bx;

import java.util.Arrays;

/**
 * Base Converter
 * @author mirafun
 */
public class BaseX {
    public static byte[] EMPTY_BYTE = new byte[0];
    
    public static Ta createTable(String str) { return new Ta(str); }
    
    public static class Ta {
        private static final double LOG256 = Math.log(256);
        public byte[] da, ta;
        int ba;
        float fa1, fa2;
        public Ta(String str) {
            this(str, str.length());
        }
        public Ta(String str, int ba) {
            if(ba < 1 || ba > 255) throw ex("Supported for: 0 < base < 256: ", ba);
            if(str.length() != ba) throw ex("Alphabet expects number of characters equal to second argument: ", str.length());
            this.ba = ba;
            ta = new byte[ba];
            da = new byte[256];
            double l = Math.log(ba);
            fa1 = (float)(l/LOG256);
            fa2 = (float)(LOG256/l);
            Arrays.fill(da, (byte)-1);
            for(int i = 0; i < ba; i++) {
                char ch = str.charAt(i);
                if(ch >= 256) throw ex("Unicode not supported: ", ch);
                if(da[ch] != -1) throw ex("Duplicate character: ", ch);
                da[ch] = (byte)i;
                ta[i] = (byte)ch;
            }
        }
        public char ch(int i) { return (char)ta[i];}
        public byte[] en(byte[] data) {
            return en(data, null);
        }
        public byte[] en(byte[] data, byte[] re) {
            int dale = data.length;
            if(dale == 0) return EMPTY_BYTE;
            int zero = 0;
            while(zero < dale && data[zero] == 0) zero++;
            int capa = (int)((dale-zero)*fa2 + 1);
            if(re == null) re = new byte[capa];
            else {
                if(capa > re.length) throw new IllegalArgumentException("Too small array");
                Arrays.fill(re, (byte)0);
            }
            int si = 0; //capa-1;
            for(int i = zero; i < dale; i++) {
                int cary = data[i]&0xff;
                int j = 0;
                for(int k = capa-1; (j < si || cary != 0) && k != -1; j++, k--) {
                    cary += ((re[k]&0xff) << 8); 
                    re[k] = (byte)(cary % ba);
                    cary /= ba;
                }
                if(cary != 0) throw ex("carry is not zero");
                si = j;
            }
            int i = capa - si;
            while(i < capa && re[i] == 0) i++;

            byte b0 = ta[0];
            int s2 = zero + capa - i;
            if(capa == s2) {
                for(int j = 0; j < zero; j++) re[j] = b0;
                for(; i < capa; i++) re[i] = ta[re[i]];
            }
            else { 
                byte[] re2 = new byte[s2];
                for(int j = 0; j < zero; j++) re2[j] = 0;
                for(; i < capa; i++) re2[zero++] = ta[re[i]];
                return re2;
            }
           
            return re;
        }
        public byte[] de(byte[] data58) {
            return de(data58, null);
        }
        public byte[] de(byte[] data58, byte[] re) {
            int dale = data58.length;
            if(dale == 0) return EMPTY_BYTE;
            byte b0 = ta[0];
            int zero = 0;
            while(zero < dale && data58[zero] == b0) zero++;
            int capa = (int)((dale-zero)*fa1 + 1);
            if(re == null) re = new byte[capa];
            else {
                if(capa > re.length) throw ex("Too small array: ", capa);
                Arrays.fill(re, (byte)0);
            }
            int si = 0;
            for(int i = zero; i < dale; i++) {
                int cary = da[data58[i]&0xff]&0xff;
                if(cary == 255) throw ex("Invalid character ", (char)(data58[i]&0xff));
                int j = 0;
                for(int k = capa-1; (j < si || cary != 0) && k != -1; j++, k--) {
                    cary += ba*(re[k]&0xff); 
                    re[k] = (byte)(cary&0xff);
                    cary >>= 8;
                }
                if(cary != 0) throw ex("carry is not zero");
                si = j;
            }
            int i = capa - si;
            while(i < capa && re[i] == 0) i++;
            int s2 = zero + capa - i;
            if(s2 != capa) {
                byte[] re2 = new byte[s2];
                for(int j = 0; j < zero; j++) re2[j] = 0;
                for(; i < capa; i++) re2[zero++] = re[i];
                return re2;
            }
            return re;
        }
    }
    private static IllegalArgumentException ex(String msg) {
        return new IllegalArgumentException(msg);
    }
    private static IllegalArgumentException ex(String msg, char i) {
        return new IllegalArgumentException(msg + i);
    }
    private static IllegalArgumentException ex(String msg, int i) {
        return new IllegalArgumentException(msg + i);
    }
}
