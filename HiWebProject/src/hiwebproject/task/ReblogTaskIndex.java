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
package hiwebproject.task;

import hiwebproject.HiWebProject;
import hiwebproject.HiWebProject.UserUpdate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import simpleserver.TestServer;
import simpleserver.log.Log;
import simpleserver.log.PrintFew;
import simpleserver.web.Utils;

public class ReblogTaskIndex {
    public ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    public MultiMap<String, ReblogTask> voteTask = new MultiMap<>();
    public ConcurrentSkipListSet<ReblogTask> postTask = new ConcurrentSkipListSet<>();
    
    public MultiMap2<String, ReblogTask> users = new MultiMap2<>();

    public ReblogTaskIndex() {
        
    }
    
    public void doSave(boolean daily) {
        try {
            var u = new UserUpdate(null, null) {
                @Override
                public void run() {
                    if(error) {
                        PrintFew.out("UserUpdateError");
                    }
                }
            };
            int tasks = 0;
            for(var li : users.map.entrySet()) {
                var reblogAccount = li.getKey();
                boolean change = false;
                for(var ta : li.getValue().values()) {
                    tasks++;
                    if(daily && ta.rebloggedDay > 0) {
                        ta.rebloggedDay = Math.max(0, ta.rebloggedDay-ta.limitDay);
                        change = true;
                    }
                    else change |= ta.saveStats;
                    ta.saveStats = false;
                }
                if(change) {
                    var json = HiWebProject.index.getAllTasksJSON(reblogAccount);
                    u.error = false;
                    u.user = reblogAccount;
                    u.tasks = json.toString();
                    u.exec(TestServer.db);
                    u.run();
                }
            }
            Log.log("Atm got this many tasks: " + tasks);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    public void postVote(String voter, String author, String permlink, int weight) {
        ConcurrentSkipListSet<ReblogTask> set = voteTask.map.get(voter);
        if(set == null) return; 
        for(ReblogTask t : set) {
            if(t.checkVoteWeight(weight)) {
                HiWebProject.finishVoteTask(t, author, permlink, voter, weight);
            }
        }
    }
    public void post(Post post) {
        for(ReblogTask t : postTask) 
            if(t.check(post, null, 0)) {
                HiWebProject.finishTask(t, post);
            }
    }
    public void add(ReblogTask task) {
        if(task.reblogAccount == null || task.taskName == null) throw new IllegalArgumentException();
        ReblogTask taskObject = users.get(task.reblogAccount, task.taskName);
        if(taskObject != null) remove(taskObject);
        users.put(task.reblogAccount, task.taskName, task);
        if(task.isVoteTask()) {
            for(String s : task.fromVote) voteTask.put(s, task);
        }
        else {
            postTask.add(task);
        }
    }
    public ReblogTask get(String reblogAccount, String taskName) {
        return users.get(reblogAccount, taskName);
    }
    public boolean remove(String reblogAccount, String taskName) {
        ReblogTask taskObject = users.get(reblogAccount, taskName);
        if(taskObject != null) { remove(taskObject);  return true; }
        return false;
    }
    public void remove(ReblogTask task) {
        users.remove(task.reblogAccount, task.taskName, task);
        if(task.isVoteTask()) {
            for(String s : task.fromVote) 
                if(!voteTask.remove(s, task)) {
                    Log.log("Error task not removed");
                }
        }
        else {
            postTask.remove(task);
        }
    }
    
    public void addAllTasks(String tasks) {
        if(tasks == null) return;
        JSONArray o = new JSONArray(tasks);
        for(int i = 0; i < o.length(); i++) add(ReblogTask.fromJSON(o.getJSONObject(i)));
    }
    public JSONArray getAllTasksJSON(String reblogAccount) {
        var m = users.get(reblogAccount);
        if(m == null || m.isEmpty()) return null;
        JSONArray o = new JSONArray();
        for(var r : m.values()) o.put(r.toJSON());
        return o;
    }
    
    public static class MultiMap<K, V> {
        public ConcurrentHashMap<K, ConcurrentSkipListSet<V>> map = new ConcurrentHashMap<>();
        public void put(K k, V v) {
            ConcurrentSkipListSet<V> l = map.get(k);
            if(l == null) {
                l = new ConcurrentSkipListSet<>();
                l.add(v);
                map.put(k, l);
            }
            else l.add(v);
        }
        public ConcurrentSkipListSet<V> get(K k) {
            return map.get(k);
        }
        public boolean remove(K k, V v) {
            ConcurrentSkipListSet l = map.get(k);
            if(l == null) return false;
            return l.remove(v);
        }
    }
    public static class MultiMap2<K, V> {
        public ConcurrentHashMap<K, ConcurrentHashMap<K, V>> map = new ConcurrentHashMap<>();
        public void put(K k, K k2, V v) {
            ConcurrentHashMap<K, V> l = map.get(k);
            if(l == null) {
                l = new ConcurrentHashMap<>();
                l.put(k2, v);
                map.put(k, l);
            }
            else l.put(k2, v);
        }
        public ConcurrentHashMap<K, V> get(K k) {
            return map.get(k);
        }
        public V get(K k, K k2) {
            ConcurrentHashMap<K, V> l = map.get(k);
            if(l == null) return null;
            return l.get(k2);
        }
        public boolean remove(K k, K k2, V v) {
            ConcurrentHashMap<K, V> l = map.get(k);
            if(l == null) return false;
            return l.remove(k2, v);
        }
    }
}
