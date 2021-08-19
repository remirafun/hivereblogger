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

import jahive.tool.B58;
import static jahive.tool.Tool.ripemd160;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.util.encoders.Hex;

/**
 *
 * @author mirafun
 */
public class PuKey {
    public byte[] data;
    public String prefix;
    public PuKey(byte[] data, String prefix) {
        this.data = data;
        this.prefix = prefix;
    }
    public PuKey(String b58) throws Exception {
        prefix = b58.substring(0,3);
        byte[] key = B58.de(b58.substring(3).getBytes(StandardCharsets.UTF_8));
        this.data = Arrays.copyOf(key, key.length-4);
        var checksum = ripemd160(data);
        if(!Arrays.equals(key, key.length-4, key.length, checksum, 0, 4))
            throw new IllegalArgumentException("Not a key");
    }
    public String toHexString() {
        return Hex.toHexString(data);
    }
    public String toB58UTF8() {
        try {
            var checksum = ripemd160(data);
            var add = org.bouncycastle.util.Arrays.concatenate(data, Arrays.copyOf(checksum, 4));
            return prefix+B58.enUTF8(add);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }        
    }

    @Override
    public String toString() {
        return toB58UTF8();
    }
    
}
