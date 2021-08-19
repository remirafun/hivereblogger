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
package jahive.common;

import jahive.key.PiKey;
import jahive.tool.Tool;
import java.awt.Point;
import java.net.URI;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author mirafun
 */
public class Client {
    public static byte[] defaultChainId = Tool.hexStringToByteArray("beeab0de00000000000000000000000000000000000000000000000000000000");
    public static byte[] testChainId = Tool.hexStringToByteArray("18dcf0a285365fc58b71f18b3d3fec954aa0c141c44e4e5cb4cf777b9eab274e");
    public byte[] chainId = defaultChainId;
    public String addressPrefix = "STM";
    public URI[] uri;
    public int currentUri = 0;
    public int attemptI = 0;
    public int maxAttempts = 5;
    public int attemptDelay = 500;
    public int chainDelay = 500;
    public int id;
    public String HIVESymbol = "HIVE";
    public String HBDSymbol = "HBD";
    public String jsonrpc = "2.0";
    
    public long lastCall = 0;
    
    public Condenser condenser;
    
    public Client(String... uri) {
        this.uri = Arrays.stream(uri).map(URI::create).toArray(URI[]::new);
        this.condenser = new Condenser(this);
    }
    public void setDefaultTest() {
        chainId = testChainId;
        addressPrefix = "TST";
        HIVESymbol = "TESTS";
        HBDSymbol = "TBD";
    }
    public void method(String method, Consumer<? super HttpResponse<String>> r) {
        json(jsonFrom(method, "[]"), r);
    } 
    public void method(String method, String params, Consumer<? super HttpResponse<String>> r) {
        json(jsonFrom(method, params), r);
    }
    public void json(String json, Consumer<? super HttpResponse<String>> r) {
        json(json, r, new Point());
    }
    public void json(String json, Consumer<? super HttpResponse<String>> r, Point p) {
        long ti = System.currentTimeMillis();
        if(lastCall+chainDelay<ti) {
            lastCall = ti;
            doJSON(json, r, p);
        }
        else {
            Tool.exec.schedule(()->{
                lastCall = System.currentTimeMillis();
                doJSON(json, r, p);
            }, chainDelay, TimeUnit.MILLISECONDS);
        }
    }
    public void doJSON(String json, Consumer<? super HttpResponse<String>> r, Point p) {   
        Tool.post(uri[p.x], json).thenAccept(r).exceptionally((ex)->{ 
            if(p.y < maxAttempts) {
                p.y++;
                Tool.exec.schedule(()->{
                    doJSON(json, r, p);
                }, attemptDelay, TimeUnit.MILLISECONDS);
            }
            else {
                if(p.x < uri.length) {
                    p.x++;
                    p.y = 0;
                    Tool.exec.schedule(()->{
                        doJSON(json, r, p);
                    }, attemptDelay, TimeUnit.MILLISECONDS);
                }
                else {
                    r.accept(null);
                }
            }
            return null; 
        });
    }
    public String jsonFrom(String method, String params) {
        var num = String.valueOf(id++);
        StringBuilder str = new StringBuilder(42+jsonrpc.length()+method.length()+params.length()+num.length());
        str.append("{\"jsonrpc\":\"").append(jsonrpc).append("\",\"method\":\"").append(method);
        str.append("\",\"params\":").append(params);
        str.append(",\"id\":").append(num).append("}");
        return str.toString();
    }
    
//    public static void main(String[] args) throws ParseException {
//        JSONObject vote = new JSONObject();
//        vote.put("voter", "mirafun");
//        vote.put("author", "schamangerbert");
//        vote.put("permlink", "meadow-macros-wiesenmakros-vanessa-cardui-distelfalter");
//        vote.put("weight", 10000);
//        
//        var op = op("vote", vote);
//        
//        System.out.println(op.toString());
//    }
    public static JSONArray voteOp(String voter, String author, String permlink, int weight) {
        JSONObject vote = new JSONObject();
        vote.put("voter", voter);
        vote.put("author", author);
        vote.put("permlink", permlink);
        vote.put("weight", weight);
        return op("vote", vote);
    }
    public static JSONArray customJSONOp(String id, String json, String[] required_auths, String[] required_posting_auths) {
        JSONObject vote = new JSONObject();
        vote.put("required_auths", required_auths==null?new JSONArray():new JSONArray(required_auths));
        vote.put("required_posting_auths", required_posting_auths==null?new JSONArray():new JSONArray(required_posting_auths));
        vote.put("id", id);
        vote.put("json", json);
        return op("custom_json", vote);
    } 
    public static JSONArray commentOp(String parent_author, String parent_permlink, String author, String permlink,
        String title, String body, String json_metadata) {
        if(title.length() > 256 || body.isEmpty()) throw new IllegalArgumentException();
        JSONObject vote = new JSONObject();
        vote.put("parent_author", parent_author);
        vote.put("parent_permlink", parent_permlink);
        vote.put("author", author);
        vote.put("permlink", permlink);
        vote.put("title", title);
        vote.put("body", body);
        vote.put("json_metadata", json_metadata);
        return op("comment", vote);
    } 
    public JSONArray commentOptionsOp(String author, String permlink, int percent_hbd) {
        return commentOptionsOp(author, permlink, "1,000,000.000 "+HBDSymbol, percent_hbd, null);
    }
    public JSONArray commentOptionsOp(String author, String permlink, TreeMap<String, Integer> beneficiaries) {
        return commentOptionsOp(author, permlink, "1,000,000.000 "+HBDSymbol, 5000, beneficiaries);
    }
    public JSONArray commentOptionsOp(String author, String permlink, int percent_hbd, TreeMap<String, Integer> beneficiaries) {
        return commentOptionsOp(author, permlink, "1,000,000.000 "+HBDSymbol, percent_hbd, beneficiaries);
    }
    public static JSONArray commentOptionsOp(String author, String permlink, String max_accepted_payout, int percent_hbd, TreeMap<String, Integer> beneficiaries) {
        return commentOptionsOp(author, permlink, max_accepted_payout, percent_hbd, true, true, beneficiaries);
    }
    public static JSONArray commentOptionsOp(String author, String permlink, String max_accepted_payout, int percent_hbd,
        boolean allow_votes, boolean allow_curation_rewards, TreeMap<String, Integer> beneficiaries) {
        JSONObject vote = new JSONObject();
        vote.put("author", author);
        vote.put("permlink", permlink);
        vote.put("max_accepted_payout", max_accepted_payout);
        vote.put("percent_hbd", percent_hbd);
        vote.put("allow_votes", allow_votes);
        vote.put("allow_curation_rewards", allow_curation_rewards);
        var arr = new JSONArray();
        if(beneficiaries != null && !beneficiaries.isEmpty()) {
            var arr2 = new JSONArray();
            arr2.put(0);
            var obj = new JSONObject();
            var barr = new JSONArray();
            for(var e : beneficiaries.entrySet()) {
                var v = new JSONObject();
                v.put("account", e.getKey());
                v.put("weight", e.getValue());
                barr.put(v);
            }
            obj.put("beneficiaries", barr);
            arr2.put(obj);
            arr.put(arr2);
        }
        vote.put("extensions", arr);
        return op("comment_options", vote);
    } 
    public JSONArray accountCreateOp(String fee, String creator, String new_account_name, String master, String json_metadata) throws Exception {
        var ownerKey = PiKey.fromLogin(new_account_name, master, "owner").createPublic(addressPrefix).toString();
        var activeKey = PiKey.fromLogin(new_account_name, master, "active").createPublic(addressPrefix).toString();
        var postingKey = PiKey.fromLogin(new_account_name, master, "posting").createPublic(addressPrefix).toString();
        var memoKey = PiKey.fromLogin(new_account_name, master, "memo").createPublic(addressPrefix).toString();
        return accountCreateOp(fee, creator, new_account_name, ownerKey, activeKey, postingKey, memoKey, json_metadata);
    }
    public JSONArray accountCreateOp(String fee, String creator, String new_account_name, String owner,
        String active, String posting, String memo, String json_metadata) {
        JSONObject vote = new JSONObject();
        vote.put("fee", fee+" "+HIVESymbol);
        vote.put("creator", creator);
        vote.put("new_account_name", new_account_name);
        vote.put("owner", keyAuth(owner));
        vote.put("active", keyAuth(active));
        vote.put("posting", keyAuth(posting));
        vote.put("memo", memo);
        vote.put("json_metadata", json_metadata==null?"":json_metadata);
        return op("account_create", vote);
    } 
    private static JSONObject keyAuth(String key) {
        JSONObject obj = new JSONObject();
        obj.put("weight_threshold", 1);
        obj.put("account_auths", new JSONArray());
        var arr = new JSONArray();
        var arr1 = new JSONArray();
        arr1.put(key);
        arr1.put(1);
        arr.put(arr1);
        obj.put("key_auths", arr);
        return obj;
    }
    public static JSONArray op(String opName, JSONObject opData) {
        var a = new JSONArray(2);
        a.put(0, opName);
        a.put(1, opData);
        return a;
    }
    
    public void broadcast(Consumer<JSONObject> callback, PiKey key, JSONArray... ops) {        
        broadcast(callback, key, Arrays.stream(ops).map(JSONArray::toString).toArray(String[]::new));
    }
    public void broadcast(Consumer<JSONObject> callback, PiKey key, String... ops) {        
        condenser.get_dynamic_global_properties((j)->{
            if(j == null || (j=j.getJSONObject("result")) == null) {
                System.out.println("error getting dyn props");
                callback.accept(null);
                return;
            }
            try {
                var exp = Tool.toDateString(j.getString("time"), 5*60000);
                var refBlockNum = j.getLong("head_block_number")&0xFFFF;
                var refBlockPrefix = Tool.blockIdToPrefix(j.getString("head_block_id"));
                StringBuilder tx = new StringBuilder();
                tx.append("[{\"expiration\": \"").append(exp).append("\", \"extensions\": [],");
                tx.append("\"operations\": ["); 
                for(int i = 0; i < ops.length; i++) {
                    tx.append(ops[i]);
                    if(i+1<ops.length)tx.append(',');
                }
                tx.append("],\"ref_block_num\":").append(refBlockNum);
                tx.append(",\"ref_block_prefix\": ").append(refBlockPrefix);
                tx.append(",\"signatures\": [");
                int signatureAppendPoint = tx.length();
                tx.append("]");
                tx.append("}]");
                
//                System.out.println(tx);
                condenser.get_transaction_hex((h)->{
                    if(h == null || !h.has("result")) {
                        System.out.println("error getting transaction hex props");
                        callback.accept(null);
                        return;
                    }
                    String hex = h.getString("result");
                    try {
                        hex = hex.substring(0, hex.length()-2);
                        var rr = Tool.hexStringToByteArray(hex);
                        byte[] sha = Tool.sha256(org.bouncycastle.util.Arrays.concatenate(chainId, rr));
                        
                        var s = key.sign(sha);
                        tx.insert(signatureAppendPoint, "\""+s.toHexString()+"\"");
                        condenser.broadcast_transaction(callback, tx.toString());
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                        callback.accept(null);
                        return;
                    }
                }, tx.toString());
            }
            catch(Exception e) {
                e.printStackTrace();
                callback.accept(null);
                return;
            }
        });
    }
}
