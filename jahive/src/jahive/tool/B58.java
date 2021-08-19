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
package jahive.tool;

import jahive.bx.BaseX;
import jahive.bx.BaseX.Ta;
import java.nio.charset.StandardCharsets;

/**
 * A tool to convert between Base256 and Base58 encoding.
 * @author mirafun
 */
public class B58 {
    public static final Ta B58 = BaseX.createTable("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz");
    /**
     * Convert base58 byte array into base256 byte array;
     * @param data58 - base58 array
     * @return base256 array
     */
    public static byte[] de(byte[] data58) { return B58.de(data58); }
    /**
     * Convert base58 byte array into base256 byte array;
     * @param data58 - base58 array
     * @param result - null or large enough array to hold the result
     * @return base256 array
     */
    public static byte[] de(byte[] data58, byte[] result) { return B58.de(data58, result); }
    
    /**
     * onvert base256 byte array into base58 UTF8 String;
     * @param data - base256 array
     * @return base58 UTF8 String
     */
    public static String enUTF8(byte[] data) { return new String(en(data), StandardCharsets.UTF_8); }
    /**
     * Convert base256 byte array into base58 byte array;
     * @param data - base256 array
     * @return base58 array
     */
    public static byte[] en(byte[] data) { return B58.en(data); }
    /**
     * Convert base256 byte array into base58 byte array;
     * @param data - base256 array
     * @param result - null or large enough array to hold the result
     * @return base58 array
     */
    public static byte[] en(byte[] data, byte[] result) { return B58.en(data, result); }
    
}
