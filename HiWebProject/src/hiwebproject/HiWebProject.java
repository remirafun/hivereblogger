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
package hiwebproject;

import static hiwebproject.Main.streamProperties;
import hiwebproject.task.Post;
import hiwebproject.task.ReblogTask;
import hiwebproject.task.ReblogTaskIndex;
import jahive.common.Client;
import jahive.key.PiKey;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.json.JSONArray;
import org.json.JSONObject;
import simpleserver.db.DBRequest;
import simpleserver.db.data.DB;
import simpleserver.db.data.Null;
import simpleserver.db.data.PrimaryKey;
import simpleserver.db.data.TableModel;
import simpleserver.db.data.Unique;
import simpleserver.db.data.Unique.OR;
import simpleserver.db.data.Varchar;
import simpleserver.log.Log;
import simpleserver.log.PrintFew;
import simpleserver.web.Utils;

public class HiWebProject {
    public static long MAX_BACK_TRACK_BLOCKS = 128;
    public static ScriptEngine jruby;
    public static RubyInterface api;
    
    public static Client client;
    public static PiKey key;
    public static void initRubyAndClient(byte[] k) {
        try { 
            jruby = new ScriptEngineManager().getEngineByName("jruby");

            jruby.eval("require 'META-INF/init.rb'\n");
            jruby.eval("require 'hiwebproject/script1.rb' \n");
            
            api = (RubyInterface)jruby.eval("RubyAPI.new");
            
            client = new Client("https://api.hive.blog", "https://api.openhive.network", "https://hive-api.arcange.eu");
            key = new PiKey(k);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    public static void startStreamThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    long startFrom = -1;
                    try {
                        if(streamProperties != null && streamProperties.exists()) {
                            Properties p = new Properties();
                            p.loadFromXML(new FileInputStream(streamProperties));
                            String findT = (String)p.getOrDefault("lastTrx", null);
                            long findB = Long.parseLong((String)p.getOrDefault("lastBlockNum", "0"));
                            if(findB > 0 && findT != null) {
                                RubyHash h = api.get_dynamic_global_properties();
                                long l = (Long)h.get("last_irreversible_block_num");
                                if(findB+MAX_BACK_TRACK_BLOCKS > l) {
                                    startFrom = findB;
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    
                    
                    if(startFrom != -1) {
                        PrintFew.out("Starting stream from " + startFrom);
                        api.start_stream_from(startFrom);
                    }
                    else PrintFew.out("Starting stream...");
                    while(true) {
                        api.start_stream();
                        PrintFew.out("Stream closed... reopening in 5...");
                        try {
                            Utils.restInterruptibly(5000);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    
    public static class User extends TableModel {
        @PrimaryKey public int id;
        @Varchar(64) @Unique(OR.replace) public String user;
        @Null @Varchar(32000) public String tasks;
    }
    public static abstract class UserUpdate extends DBRequest {
        public String user;
        public String tasks;

        public UserUpdate(String user, String tasks) {
            this.user = user;
            this.tasks = tasks;
        }
        
        @Override
        public void exec(DB db) throws Exception {
            User u = HiWebProject.users.get(user);
            if(u == null) {
                u = new User();
                u.user = user;
                u.tasks = tasks;
                db.add(u);
            }
            else {
                u.tasks = tasks;
                db.update(u);
            }
        }
    }
    
    public static HashMap<String, User> users = new HashMap<>();
    public static ReblogTaskIndex index = new ReblogTaskIndex();
    
    public static class Token {
        public String username;
        public long validFor;
        public boolean hasPostingAuth;
        public Token(String username) {
            this.username = username;
            this.validFor = System.currentTimeMillis()+Utils.DAY;
        }
        
    }
    public static HashMap<String, Token> tokens = new HashMap<>();
            
    public static HttpClient cache = null;
    public static HttpClient getClient() {
        if(cache != null) return cache;
        HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ZERO.ofSeconds(20))
        //.proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
        //.authenticator(Authenticator.getDefault())
        .build();
        cache = client;
        return client;
    }
    public static final URI hivesignerUri = URI.create("https://hivesigner.com/api/me");
    public static CompletableFuture<HttpResponse<String>> hivesigner(String token) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(hivesignerUri)
            .timeout(Duration.ofMinutes(1))
            .header("Authorization", token)
            .header("Content-Type", "application/json")
            .GET()
            .build();
        return getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));  
    }
    public static void checkAccess(String account, String token, Consumer<Token> fn) {
        try {
            Token t = tokens.get(token);
            if(t != null) {
                fn.accept(t.username.equals(account)?t:null);
                return;
            }
            hivesigner(token).thenAccept((r)->{
                try {
                    if(r.statusCode() == 200) {
                        var obj = new JSONObject(r.body());
                        var a = obj.getJSONObject("account");
                        var name = a.getString("name");
                        var p = a.getJSONObject("posting").getJSONArray("account_auths");
                        var t2 = new Token(account);
                        for(var i = 0; i < p.length(); i++) {
                            var nn = p.getJSONArray(i).getString(0);
                            if(Main.appName.equals(nn)) {
                                t2.hasPostingAuth = true;
                                break;
                            }
                        }
                        if(t2.hasPostingAuth) tokens.put(token, t2);
                        fn.accept(account.equals(name)?t2:null);
                    }
                    else fn.accept(null);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    fn.accept(null);
                }
            }).exceptionally((ex)->{
                fn.accept(null);
//                PrintFew.out(Objects.toString(ex));
                ex.printStackTrace();
                return null;
            }).join();
        }
        catch(Exception e) {
            fn.accept(null);
            e.printStackTrace();
        }
    }
    private static void doReblog(ReblogTask task, String author, String permlink) {
//        System.out.println("reblogging " + task.reblogAccount + " " + task.taskName);
//        System.out.println("author " + author + "/"+permlink);
        task.saveStats = true;
        if(task.rebloggedDay < task.limitDay) {
            task.reblogged++;
            task.rebloggedDay++;
                      
            var op = Client.customJSONOp("reblog", 
            "[\"reblog\",{\"account\":\""+task.reblogAccount
                +"\",\"author\":\""+author
                +"\",\"permlink\":\""+permlink+"\"}]", null, new String[]{task.reblogAccount});
                       
            client.broadcast((p)->{
                if(p == null || p.has("error")) {
//                    PrintFew.out("Reblog Err1");
//                    System.out.println(p);
                    //try once again
                    client.broadcast((p2)->{}, key, op);
                }
                else {
                }
//                System.out.println(p);
            }, key, op);
        }
        else task.limited++;
    }
    private static void doReblog(ReblogTask task, Post post) {
        if(post.rebloggedBy.contains(task.reblogAccount)) return; 
        if(task.rebloggedDay >= task.limitDay) { task.limited++; return; }
        post.rebloggedBy.add(task.reblogAccount);
        doReblog(task, post.author, post.permlink);
    }
    public static void finishVoteTask(ReblogTask task, String author, String permlink, String voteAuthor, int voteWeigth) {
        index.exec.execute(()->{      
            try {
                RubyArray reblogged = api.get_reblogged_by(author, permlink);
                if(reblogged != null) {
                    int len = reblogged.getLength();
                    for(int i = 0; i < len; i++) {
                        Object o = reblogged.get(i);
                        if(task.reblogAccount.equals(o)) {
                            return;
                        }
                    }
                }
                if(task.requiresPost()) {
                    RubyHash hash = api.get_content(author, permlink);
                    Post p = createPost(hash);
                    if(!task.check(p, voteAuthor, voteWeigth)) return;
                }
                doReblog(task, author, permlink);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });
    }
    public static void finishTask(ReblogTask task, Post post) {
        if(task.rebloggedDay >= task.limitDay) { task.limited++; return; } 
        index.exec.execute(()->{
            try {
                doReblog(task, post);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });
    } 
    public static boolean printIncomming = false;
    public static String lastTrx = null;
    public static long lastBlockNum = 0;
    public static void incoming(String str, long l, RubyHash hash) {
        
//        System.out.println("--------------");
//        System.out.println(str);
//        System.out.println(l);
        lastTrx = str;
        lastBlockNum = l;
        
        try {
            String type = (String)hash.get("type");
            RubyHash value = (RubyHash)hash.get("value");

            if(type.equals("vote_operation")) {
                String voter = (String)value.get("voter");
                String author = (String)value.get("author");
                String permlink = (String)value.get("permlink");
                int weight = ((Long)value.get("weight")).intValue();
                
                if(printIncomming) {
                    System.out.println(voter + " " + author + " " + permlink + " " + weight);
                }
                index.postVote(voter, author, permlink, weight);
                                       
            }
            else {
                String parent_author = (String)value.get("parent_author");
                
                if(parent_author.isBlank()) {
                    Post p = createPost(value);
                    if(printIncomming) {
                        System.out.println(p.author + " " + p.permlink);
                    }
                    index.post(p);
                }
                else {
                    //comment
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private static Post createPost(RubyHash value) {
        String parent_permlink = (String)value.get("parent_permlink");
        String author = (String)value.get("author");
        String permlink = (String)value.get("permlink");
        String title = (String)value.get("title");
        String body = (String)value.get("body");
        String json_metadata = (String)value.get("json_metadata");

        String[] tags = parseTags(json_metadata);
        return Post.create(author, permlink, title, body, parent_permlink, tags);
    }
    private static String[] parseTags(String json_metadata) {
        try {
            if(json_metadata == null) return null;
            JSONObject j =new JSONObject(json_metadata);
            JSONArray a = j.optJSONArray("tags");
            if(a == null || a.length() == 0) return null;
            int maxTags = Math.min(32, a.length());
            String[] arr = new String[maxTags];
            for(int i = 0; i < arr.length; i++) {
                arr[i] = a.getString(i);
            }
            return arr;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
