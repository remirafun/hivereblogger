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
package test.jahive.tool;

import jahive.tool.B58;
import static jahive.tool.B58.en;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 * @author mirafun
 */
public class BXTest {
     public static void main(String[] args) {
        new BXTest().test();
        
        
        
//        en(new byte[]{1,2,3,4,5}, ta);
        
        //a -> 2g
    }
     
    public boolean test() {
        String[][] test = {{"a","2g"}, {"hello", "Cn8eVZg"}, {"abc", "ZiCa"}};
        
        for(String[] t : test) {
            byte[] da = B58.en(str(t[0]));
            equal(da, t[1]);
            byte[] a = B58.de(da);
            equal(a, t[0]);
        }
        
        
        return true;
    }
    
    private static void equal(byte[] arr, String str) {
        if(!eq(arr, str)) throw new IllegalArgumentException("Test Failed: '" + str(arr) + "' != " + str);
    }
    private static void equal(byte[] arr, byte[] arr2) {
        if(!Arrays.equals(arr, arr2)) throw new IllegalArgumentException("Test Failed: '" + str(arr) + "' != " + str(arr2));
    }
    public static byte[] b(int... a) {
        byte[] b = new byte[a.length];
        for(int i = 0; i < a.length; i++) b[i] = (byte)a[i];
        return b;
    }
    private static String str(byte[] b) { return new String(b, StandardCharsets.UTF_8); }
    private static byte[] str(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    private static boolean eq(byte[] arr, String str) {
        return Arrays.equals(arr, str.getBytes(StandardCharsets.UTF_8));
    }
}
