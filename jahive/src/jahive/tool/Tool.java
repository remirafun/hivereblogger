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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import java.net.http.*;
import java.net.http.HttpClient.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.json.JSONObject;
/**
 *
 * @author mirafun
 */
public class Tool {
    public static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static byte NETWORK_ID = (byte)0x80;
    public static byte[] EMPTY_BYTE = new byte[0];
    public static byte[] ripemd160(byte[] a) throws Exception {
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(a, 0, a.length);
        var aa = new byte[digest.getDigestSize()];
        digest.doFinal(aa, 0);
        return aa;
    }
    public static byte[] sha256(byte[] a) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(a);
    }
    public static byte[] sha256sha256(byte[] a) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(digest.digest(a));
    }
    public static byte[] sha256sha256(byte[] a, int off, int len) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(a, off, len);
        return digest.digest(digest.digest());
    }
    public static void print(byte[] b) {
        for(byte a : b) System.out.print((a&0xff)+",");
    }
    public static String hexByte(int i) {
        String chars = "0123456789abcdef";
        StringBuilder result = new StringBuilder(2);
        int val = i & 0xff;
        result.append(chars.charAt(val / 16));
        result.append(chars.charAt(val % 16));
        return result.toString();
    }
    public static String hex(final byte[] bytes) {
        String chars = "0123456789abcdef";
        StringBuilder result = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            int val = b & 0xff;
            result.append(chars.charAt(val / 16));
            result.append(chars.charAt(val % 16));
        }
        return result.toString();
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    public static String jsonArray(Object... items) {
        StringBuilder s = new StringBuilder();
        s.append('[');
        for(int i = 0; i < items.length; i++) {
            var item = items[i];
            if(item instanceof CharSequence) {
                s.append('\"');
                s.append(item);
                s.append('\"');
            }
            else s.append(item);
            if(i+1 < items.length) s.append(',');
        }
        s.append(']');
        return s.toString();
    }
    public static boolean isError(JSONObject a) {
        return a == null || a.has("error");
    }
    public static boolean isErrorOrNoResult(JSONObject a) {
        return a == null || a.has("error") || !a.has("result");
    }
    public static <T> T getResult(JSONObject a) {
        return (T)a.get("result");
    }
    public static Date toDate(String s) throws ParseException { return dateFormat.parse(s); }
    public static Date toDate(String s, long add) throws ParseException { return new Date(dateFormat.parse(s).getTime()+add); }
    public static String toDateString(Date s) throws ParseException { return dateFormat.format(s); }
    public static String toDateString(String s, long add) throws ParseException { return dateFormat.format(toDate(s, add)); }
    public static long blockIdToPrefix(String blockId) {
        long r = Long.parseLong(blockId.substring(8, 16), 16);
        return ((r>>24)&0xffL) | ((r>>8)&0xff00L) | ((r<<8)&0xff0000L) | ((r<<24)&0xff000000L);
    }
    
    
    public static HttpClient cache = null;
    public static HttpClient getClient() {
        if(cache != null) return cache;
        HttpClient client = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NORMAL)
        .connectTimeout(Duration.ZERO.ofSeconds(20))
        //.proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
        //.authenticator(Authenticator.getDefault())
        .build();
        cache = client;
        return client;
    }
    
    public static CompletableFuture<HttpResponse<String>> post(URI uri, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofMinutes(1))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
       return getClient().sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8));  
    }
//    public static void main(String[] args) {
//        URI uri = URI.create("https://api.hive.blog");
////        post(uri, "{\"jsonrpc\":\"2.0\", \"method\":\"market_history_api.get_recent_trades\", \"params\":{\"limit\":10}, \"id\":1}")
////        post(uri, "{\"jsonrpc\":\"2.0\", \"method\":\"condenser_api.get_active_votes\", \"params\":[\"author\", \"permlink\"], \"id\":1}")
////        post(uri, "{\"jsonrpc\":\"2.0\", \"method\":\"condenser_api.get_block_header\", \"params\":[1], \"id\":1}")
//        post(uri, "{\"jsonrpc\":\"2.0\", \"method\":\"condenser_api.get_reblogged_by\", \"params\":[\"author\",\"permlink\"], \"id\":1}")
//            .thenAccept((r)->{
//                System.out.println(r);
//                System.out.println(r.statusCode());
//                System.out.println(r.body());
//                
//            }).exceptionally((ex)->{
//                System.out.println(ex);
//                return null;
//            }).join();
//        
//    }
}
