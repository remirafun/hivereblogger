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

import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReblogTask implements Comparable<ReblogTask> {
    public String reblogAccount;
    public String taskName;
    public String[] fromVote, fromAuthor, fromTag;
    public int fromVoteMin=0, fromVoteMax=10000;
    public String[] withAllTitle, withOneTitle, withAllBody,
        withOneBody, withAllTag, withOneTag;
    public String[] withoutTitle, withoutBody, withoutTag;
    public int limitDay=10;
    public int limited=0;
    public int reblogged=0;
    public int rebloggedDay=0;
    
    public boolean saveStats = false;

    public ReblogTask(String reblogAccount) {
        this.reblogAccount = reblogAccount;
    }
    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        p(j, "reblogAccount", reblogAccount);
        p(j, "taskName", taskName);
        p(j, "fromVote", fromVote);
        p(j, "fromAuthor", fromAuthor);
        p(j, "fromTag", fromTag);
        p(j, "fromVoteMin", fromVoteMin);
        p(j, "fromVoteMax", fromVoteMax);
        p(j, "withAllTitle", withAllTitle);
        p(j, "withOneTitle", withOneTitle);
        p(j, "withAllBody", withAllBody);
        p(j, "withOneBody", withOneBody);
        p(j, "withAllTag", withAllTag);
        p(j, "withOneTag", withOneTag);
        p(j, "withoutTitle", withoutTitle);
        p(j, "withoutBody", withoutBody);
        p(j, "withoutTag", withoutTag);
        p(j, "limitDay", limitDay);
        p(j, "limited", limited);
        p(j, "reblogged", reblogged);
        p(j, "rebloggedDay", rebloggedDay);
        return j;
    }
    public static ReblogTask fromJSON(String str) {
        JSONObject j = new JSONObject(str); 
        return fromJSON(j);
    }
    public static ReblogTask fromJSON(JSONObject j) {
        ReblogTask r = new ReblogTask(j.optString("reblogAccount"));
        r.taskName = j.optString("taskName");
        r.fromVote = a(j, "fromVote");
        r.fromAuthor = a(j, "fromAuthor");
        r.fromTag = a(j, "fromTag");
        r.fromVoteMin = j.optInt("fromVoteMin");
        r.fromVoteMax = j.optInt("fromVoteMax");
        r.withAllTitle = a(j, "withAllTitle");
        r.withOneTitle = a(j, "withOneTitle");
        r.withAllBody = a(j, "withAllBody");
        r.withOneBody = a(j, "withOneBody");
        r.withAllTag = a(j, "withAllTag");
        r.withOneTag = a(j, "withOneTag");
        r.withoutTitle = a(j, "withoutTitle");
        r.withoutBody = a(j, "withoutBody");
        r.withoutTag = a(j, "withoutTag");
        r.reblogged = j.optInt("reblogged");
        r.rebloggedDay = j.optInt("rebloggedDay");
        r.limited = j.optInt("limited");
        r.limitDay = j.optInt("limitDay");
        return r;
    }
    private static String[] a(JSONObject j, String s) { 
        var a = j.optJSONArray(s);
        if(a == null) return null;
        String[] arr = new String[a.length()];
        for(int i = 0; i < arr.length; i++) arr[i] = a.optString(i);
        return arr;
    }
    private static void p(JSONObject j, String s, Object o) { j.put(s, o==null?JSONObject.NULL:o);}
    private static void p(JSONObject j, String s, String[] o) { j.put(s, o==null?JSONObject.NULL:new JSONArray(o));}
    
    public boolean isVoteTask() {
        return fromVote != null && fromVote.length != 0;
    }
    public boolean quickCheckVote(String voteAuthor, int voteWeigth) {
        if(voteAuthor == null) {
            //new post
            if(isVoteTask()) return false;
        }
        else {
            //vote
            if(!isVoteTask()) return false;
            if(Arrays.binarySearch(fromVote, voteAuthor) < 0) return false;
            if(voteWeigth < fromVoteMin || voteWeigth > fromVoteMax) return false;
        }
        return true;
    }
    public boolean checkVoteWeight(int voteWeigth) {
        if(voteWeigth < fromVoteMin || voteWeigth > fromVoteMax) return false;
        return true;
    }
    public boolean requiresPost() {
        return fromAuthor != null || fromTag != null || withAllTitle != null
            || withAllBody != null || withAllTag != null || withOneTitle != null
            || withOneBody != null || withOneTag != null || withoutTitle != null
            || withoutBody != null || withoutTag != null;
    }
    public boolean check(Post p, String voteAuthor, int voteWeigth) {
        if(!quickCheckVote(voteAuthor, voteWeigth)) return false;
        
        if(fromAuthor != null && Arrays.binarySearch(fromAuthor, p.author) < 0) return false;
        if(fromTag != null && (p.parent_permlink == null || Arrays.binarySearch(fromTag, p.parent_permlink) < 0)) return false;
        
        if(withAllTitle != null && !containsAll(p.title, withAllTitle)) return false;
        if(withAllBody != null && !(containsAll(p.title, p.body, withAllBody))) return false;
        if(withAllTag != null && !containsAll(p.tags, withAllTag)) return false;
        
        if(withOneTitle != null && !containsOne(p.title, withOneTitle)) return false;
        if(withOneBody != null && !(containsOne(p.title, p.body, withOneBody))) return false;
        if(withOneTag != null && !containsOne(p.tags, withOneTag)) return false;
        
        if(withoutTitle != null && containsOne(p.title, withoutTitle)) return false;
        if(withoutBody != null && containsOne(p.title, p.body, withoutBody)) return false;
        if(withoutTag != null && containsOne(p.tags, withoutTag)) return false;
        
        return true;
    }
    public static boolean containsOne(Set<String> a, String[] ar) {
        for(String aa : ar) if(a.contains(aa)) return true;
        return false;
    }
    public static boolean containsOne(Set<String> a, Set<String> b, String[] ar) {
        for(String aa : ar) if(a.contains(aa) || b.contains(aa)) return true;
        return false;
    }
    public static boolean containsAll(Set<String> a, String[] ar) {
        for(String aa : ar) if(!a.contains(aa)) return false;
        return true;
    }
    public static boolean containsAll(Set<String> a, Set<String> b, String[] ar) {
        for(String aa : ar) if(!(a.contains(aa) || b.contains(aa))) return false;
        return true;
    }
    
    public static ReblogTask from(String reblogAccount, Map<String, String> map) {
        ReblogTask task = new ReblogTask(reblogAccount);
        task.taskName = map.get("taskName");
        task.fromVote = toWords(map.get("fromVote"), 100, 1024);
        task.fromAuthor = toWords(map.get("fromAuthor"), 100, 1024);
        task.fromTag = toWords(map.get("fromTag"), 100, 1024);
        if(task.fromVote != null) Arrays.sort(task.fromVote);
        if(task.fromAuthor != null) Arrays.sort(task.fromAuthor);
        if(task.fromTag != null) Arrays.sort(task.fromTag);
        
        task.withAllTitle = toWords(map.get("withAllTitle"), 100, 1024);
        task.withOneTitle = toWords(map.get("withOneTitle"), 100, 1024);
        task.withAllBody = toWords(map.get("withAllBody"), 100, 1024);
        task.withOneBody = toWords(map.get("withOneBody"), 100, 1024);
        task.withAllTag = toWords(map.get("withAllTag"), 100, 1024);
        task.withOneTag = toWords(map.get("withOneTag"), 100, 1024);
        task.withoutTitle = toWords(map.get("withoutTitle"), 100, 1024);
        task.withoutBody = toWords(map.get("withoutBody"), 100, 1024);
        task.withoutTag = toWords(map.get("withoutTag"), 100, 1024);
        task.fromVoteMin = toInt(map.get("fromVoteMin"), -100, 100, 0)*100;
        task.fromVoteMax = toInt(map.get("fromVoteMax"), -100, 100, 100)*100;
        task.limitDay = toInt(map.get("limitDay"), 0, 10000, 10);
        return task;
    }
    public static int toInt(String s, int min, int max, int def) {
        if(s == null || s.isEmpty() || (s=s.trim()).isEmpty()) return def;
        int i = Integer.parseInt(s);
        if(i <= min) return min;
        if(i >= max) return max;
        return i;
    }
    public static String[] toWords(String s) {
        return toWords(s, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    public static String[] toWords(String s, int wordLimit, int lengthLimit) {
        if(s == null || s.isEmpty() || (s=s.trim()).isEmpty()) return null;
        if(s.length() > lengthLimit) throw new IllegalArgumentException();
        String[] arr = s.split("[\\W]+");
        if(arr.length > lengthLimit) throw new IllegalArgumentException();
        if(arr.length == 0) return null;
        for(int i = 0; i < arr.length; i++) arr[i] = arr[i].toLowerCase();
        return arr;
    }

    @Override
    public int compareTo(ReblogTask o) {
        int i = reblogAccount.compareTo(o.reblogAccount);
        if(i == 0) return taskName.compareTo(o.taskName);
        return i;
    }

    @Override
    public int hashCode() {
        return reblogAccount.hashCode()^taskName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ReblogTask) return compareTo((ReblogTask)obj) == 0;
        return false;
    }
    
    
    
}
