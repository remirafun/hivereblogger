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

package simpleserver.web;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import simpleserver.log.Log;

public class WebLog {
    /** 
       #msg time
       time long

       #msg ip6
       ip6 i128

       #msg mere
       te mere

       #msg refe
       te refe

       #msg agen
       te agen

       #msg log entry
       reti  i15  
       ip    ptr ip     4 maps byte[]
       re    char
       byte  int
       mere  ptr mere
       refe  ptr refe
       agen  ptr agen
       name  ptr name

       6 messages total -> 3 bits... msg type 
       
       
       #msg log
       i64 i8[] i32 i16 i16 te te te te  
       
       in -> buf(16000) -> log
       
       [i64 i8[] i32 i16 i16 te te te te]  //arrays...
        @de fi [Fi [Fi ho] ".welo" "access" [+ "lo" [Sy no]]] 
        @dety alo [i64 i8[] i32 i16 i16 te te te te]
        @de ri [-> alo -> u8[8192] -> fi ]             
        
        @de msg [1234 [1 2 3 4] 10 200 200 "abc" "af" "sf" "sf"] 
        #OP -> ri  //optional
        msg -> ri
        
        #EO -> ri  //optional or program exit
        
        //read
        @de list []
        fi -> alo -> list   
        
       
        int tipo = a.ti(ti);
        int apo = a.by(ByteBuffer.wrap(addr));
        int merepo = a.te(mere);
        int oripo = a.te(ori);
        int agenpo = a.te(agen);
        int namepo = a.te(name);

        d.writeByte(a.LOG);
        d.writeInt(tipo);
        a.by(apo);
        d.writeInt(by);
        d.writeChar(re);
        d.writeChar(mo);

        a.te(merepo);
        a.te(oripo);
        a.te(agenpo);
        a.te(namepo);

     */
    public static interface LogEntry<T> {
        public T createObject();
        public void read(FileLog fi, DataInputStream dis, T store) throws Exception ;
    }
    public static class AccessLogEntry implements LogEntry<AccessMsg> {
        @Override
        public AccessMsg createObject() {
            return new AccessMsg();
        }
        @Override
        public void read(FileLog fi, DataInputStream d, AccessMsg store) throws Exception {
            store.ti = fi.reti(d);
            store.addr = fi.reby(d);
            store.by = d.readInt();
            store.re = d.readChar();
            store.mo = d.readChar();
            store.mere = fi.rete(d);
            store.ori = fi.rete(d);
            store.agen = fi.rete(d);
            store.name = fi.rete(d);
        }
    }
    public static class AccessMsg {
        public long ti;
        public byte[] addr;
        public int by;
        public char re;
        public char mo;
        public String mere;
        public String ori;
        public String agen;
        public String name;

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(Date.from(Instant.ofEpochMilli(ti)));
            s.append(" ");
            s.append(Arrays.toString(addr));
            s.append(" ");
            String str = Integer.toString(by);
            for(int i = str.length(); i < 10 ; i++)
                s.append(' ');
            s.append(str);
            s.append(" \t");
            s.append((int)re);
            s.append(" ");
            s.append((char)(mo>>8));
            s.append((char)(mo&0xff));
            s.append(" ");
            s.append(mere);
            s.append(" \t\t");
            s.append(ori);
            s.append(" ");
            s.append(agen);
            s.append(" ");
            s.append(name);
            return s.toString();
        }
    }
    public static class FileLog<T> {
        public DataOutputStream dos;
        public HashMap<Object, Integer> mapBy = new HashMap<>();
        public HashMap<Object, Integer> mapTe = new HashMap<>();
        
        public HashMap<Integer, Object> mapByRe;
        public HashMap<Integer, Object> mapTeRe;
        public ArrayList<T> log = new ArrayList<>();
        public static final int LOG=105, TI=71, IP32=32, IP128=128, TE=5;
        public long ti = 0;
        public String file;
        public LogEntry<T> logEntry;
        public int items;
        public FileLog(String file, LogEntry<T> log) throws Exception {
            this.file = file;
            this.logEntry = log;
        }
        public void checkSize() {
            
        }
        public void open(boolean readLog, boolean writeLog) throws Exception {
            if(readLog) {
                mapByRe = new HashMap<>();
                mapTeRe = new HashMap<>();
                
                mapTeRe.put(0, null);
            }
            mapTe.put(null, 0);
            File f = new File(file);
            if(f.isDirectory()) throw new IllegalArgumentException();
            if(f.exists()) {
                if(f.length() > 0) {
                    T store = logEntry.createObject();
                    byte[] b = Files.readAllBytes(f.toPath());
                    
//                    MultiBuffer.dumpHex(b, 157847, 22);
//                    MultiBuffer.dumpHex(b, 157866, 20);
//                    MultiBuffer.dumpHex(ByteBuffer.wrap(b, 157847, 21));
//                    MultiBuffer.dumpASCII(ByteBuffer.wrap(b, 157866, 16));
                    
//                    if(1 == 1) return;
                    
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
                    long size = dis.available();
                    long lastOk = 0;
                    int teCount = 0;
                    while(dis.available() > 0) {
                        int type = dis.read();
                        switch(type) {
                            case LOG:
                                lastOk = size - dis.available()-1;
                                items++;
                                logEntry.read(this, dis, store);
                                if(readLog) {
                                    log.add(store);
                                    store = logEntry.createObject();
                                }
                                break;
                            case TI:
                                lastOk = size - dis.available()-1;
                                ti = dis.readLong();
                                break;
                            case IP32:
                            case IP128:
                                lastOk = size - dis.available()-1;
                                byte[] ip32 = new byte[type==IP32?4:16];
                                dis.read(ip32);
                                Integer mapByI = mapBy.size();
                                ByteBuffer mapByB = ByteBuffer.wrap(ip32);
                                mapBy.put(mapByB, mapByI);
                                if(readLog) mapByRe.put(mapByI, mapByB);
                                break;
                            case TE:
                                teCount++;
                                lastOk = size - dis.available()-1;
                                int len = dis.read();
                                char[] arr = new char[len];
                                for(int i = 0; i < len; i++) arr[i] = dis.readChar();
                                String te = new String(arr); 
                                if(mapTe.containsKey(te)) {
                                    te += mapTe.size();
                                }
                                Integer teI = mapTe.size();
                                mapTe.put(te, teI);
                                if(readLog) mapTeRe.put(teI, te);
                                break;
                            default:
                                Log.log("warning log msg ", type + " " + (size-dis.available()) + " " + lastOk + " " + mapTeRe.size() + " " + teCount);
                        }
                    }
                }
            }
            else {
                File dirs = f.getParentFile();
                if(!dirs.exists()) dirs.mkdirs();
            }
            if(writeLog) dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true), 32768));
        }
        
        public int ti(long l) throws Exception {
            long diff = l-ti;
            if(diff <= Integer.MAX_VALUE) return (int)diff;
            dos.writeByte(TI);
            dos.writeLong(l);
            return 0;
        }
        public long reti(DataInputStream d) throws Exception {
            return ti+d.readInt();
        }
        public int by(ByteBuffer t) throws Exception {
            var map = mapBy;
            Integer i = map.get(t);
            if(i == null) {
                i = map.size();
                map.put(t, i);
                int len = Math.min(t.limit(), 255);
                if(len == 4) dos.writeByte(IP32);
                else if(len == 16) dos.writeByte(IP128);
                else throw new IllegalArgumentException();
                for(int j = 0; j < len; j++) dos.writeByte(t.get(j));
            }
            return i;
        }
        public void by(int i) throws Exception {
            var map = mapBy;
            if(map.size() < 256) {
                dos.writeByte(i);
            }
            else if(map.size() < 65536) {
                dos.writeChar(i);
            }
            else {
                dos.writeInt(i);
            }
        }
        public byte[] reby(DataInputStream d) throws Exception {
            var map = mapByRe;
            if(map.size() < 256) {
                return ((ByteBuffer)map.get(d.read())).array();
            }
            else if(map.size() < 65536) {
                return ((ByteBuffer)map.get((int)d.readChar())).array();
            }
            else {
                return ((ByteBuffer)map.get(d.readInt())).array();
            }
        }
        public int te(String t) throws Exception {
            var map = mapTe;
            Integer i = map.get(t);
            if(i == null) {
                i = map.size();
                map.put(t, i);
                int len = Math.min(t.length(), 255);
                dos.writeByte(TE);
                dos.writeByte(len);
                for(int j = 0; j < len; j++) dos.writeChar(t.charAt(j));
            }
            return i;
        }
        public void te(int i) throws Exception {
            var map = mapTe;
            if(map.size() < 256) {
                dos.writeByte(i);
            }
            else if(map.size() < 65536) {
                dos.writeChar(i);
            }
            else {
                dos.writeInt(i);
            }
        }
        public String rete(DataInputStream d) throws Exception {
            var map = mapTeRe;
            if(map.size() < 256) {
                return (String)map.get(d.read());
            }
            else if(map.size() < 65536) {
                return (String)map.get((int)d.readChar());
            }
            else {
                return (String)map.get(d.readInt());
            }
        }
        public void flush() {
            try {
                dos.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        public void close() {
            try {
                dos.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static class ThreadCheck {
        public boolean b = true;
        Thread t;
        public void check() {
            if(!b) return;
            Thread t2 = Thread.currentThread();
            if(t == null) t = t2;
            else if(t != t2) {
                System.err.println("Thread Check Multiple");
                Thread.dumpStack();
                b = false;
            }
        }
    }
    public static final long SAVE_BACKUP_TIME = 5L*86400000L;
    public static final int MAX_LOG_SIZE = 500000;
    public static File weblogDir;
    public static FileLog accessLog;
    public static File weblogAccessDir;
    public static int logEx = 0;
    public static final AccessLogEntry accessLogEntry = new AccessLogEntry();
    private static boolean logEnabled = true;
    public static void initLogs(Conf conf) {
        weblogDir = conf.path.dir(".welo");
        if(!weblogDir.exists()) weblogDir.mkdirs();
        weblogAccessDir = new File(weblogDir, "access");
        if(!weblogAccessDir.exists()) weblogAccessDir.mkdirs();
    }
    public static FileLog access() {
        if(!logEnabled || logEx > 100) return null;
        FileLog l = accessLog;
        
        if(l == null || l.items >= MAX_LOG_SIZE) {
            if(l != null) {
                l.close();
                accessLog = null;
            }
            try {
                l = new FileLog(weblogAccessDir.getAbsolutePath()+File.separatorChar+"lo"+System.currentTimeMillis(), accessLogEntry);
                l.open(false, true);
                accessLog = l;
            } catch (Exception ex) {
                logEx++;
                ex.printStackTrace();
            }
        }
        return l;
    }
    public static void flush() {
        if(accessLog != null) {
            accessLog.flush();
        }
    }
    public static void closeLogs() {
        if(accessLog != null) accessLog.close();
        accessLog = null;
        logEnabled = false;
    }
    //DateFormatter d;
    public static final char GE = (char)(('G'<<8)|'E');
    public static final char RE = (char)(('#'<<8)|'a');
    public static final char LO = (char)(('+'<<8)|'1');
    static ThreadCheck t = new ThreadCheck();
    public static void access(long ti, byte[] addr, int by, char re, char mo, String mere, String refe, String ori, String agen, String name) {
        t.check();
        if(ori == null) ori = refe;
        else if(refe != null) ori = refe;
        //         ti  addr      re  by  mo  mere orig agen name
        //access { i64 i8[](i32|i128) i16 i32 i16 te   te   te   te }
        try {
            FileLog a = access();
            if(a == null) return;
            var d = a.dos;
            
            a.items++;
            int tipo = a.ti(ti);
            int apo = a.by(ByteBuffer.wrap(addr));
            int merepo = a.te(mere);
            int oripo = a.te(ori);
            int agenpo = a.te(agen);
            int namepo = a.te(name);

            d.writeByte(a.LOG);
            d.writeInt(tipo);
            a.by(apo);
            d.writeInt(by);
            d.writeChar(re);
            d.writeChar(mo);
            
            a.te(merepo);
            a.te(oripo);
            a.te(agenpo);
            a.te(namepo);
            
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        /*String date = DateFormatter.format(Date.from(Instant.ofEpochMilli(ti)));
        //Date.from(Instant.ofEpochMilli(ti)).toGMTString()
        
        StringBuilder sb = new StringBuilder();
        sb.append(date).append(" ")
            .append(Arrays.toString(addr)).append(" ").append((int)re).append(" ").
            append(by).append(" \"").append((char)(mo>>8)).append((char)(mo&0xff)).append(mere).append("\" \"")
            .append(ori).append("\" \"").append(agen).append("\" \"")
            .append(name).append("\"\n");
        Log.log(sb.toString());*/
    }
    public static long getLogId(String n) {
        if(n.startsWith("lo")) {
            String num = n.substring(2);
            try {
                long t = Long.parseLong(num);
                return t;
            }
            catch(Exception e) { e.printStackTrace(); }
        }
        return -1;
    }
    public static File[] getWeblogFiles() {
        File backupFolder = weblogAccessDir;
        File[] backups = null;
        if(backupFolder.exists()) {
            backups = backupFolder.listFiles();
        }
        else {
            return null;
        }
        long ms = System.currentTimeMillis();
        ArrayList<File> files = new ArrayList<>();
        if(backups != null) {
            for(File f : backups) {
                String n = f.getName();
                if(n.startsWith("lo")) {
                    String num = n.substring(2);
                    try {
                        long t = Long.parseLong(num);
                        long d = ms-t;
                        if(d < SAVE_BACKUP_TIME) {
                            files.add(f);
                        }
                    }
                    catch(Exception e) { e.printStackTrace(); }
                }
            }
        }
        Collections.sort(files);
        return files.toArray(new File[files.size()]);
    }
}
