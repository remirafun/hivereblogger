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
package simpleserver.db.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import simpleserver.log.Log;
import simpleserver.web.Utils;

public class DBUtils {    
    public static ScheduledExecutorService backupExec = Executors.newSingleThreadScheduledExecutor();

    public int MIN_BACKUPS = 7;
    public long MIN_BACKUP_TIME = 7L*86400000L;
    public long SAVE_BACKUP_TIME = 5L*86400000L;
    public long lastBackupTime;
     
    public DB db;
    public File backupFolder;

    public DBUtils(DB db, File backupPath) {
        this.db = db;
        this.backupFolder = backupPath;
    }
    
    public void initBackupThread() {
        long ti = System.currentTimeMillis();

        long da = Utils.days(ti);
        int h = Utils.hours(ti);
        int m = Utils.minutes(ti);

        Log.log("da " + da + " h " + h + ", " + m);
        Log.log("ti " + ti);
        
        lastBackupTime = (da)*Utils.DAY;
        long initDelay = (60-m)*Utils.MINUTE;
        Log.log("lastB " + lastBackupTime);
        Log.log("init " + initDelay);
        
        backupExec.scheduleAtFixedRate(()->{
            long ms = System.currentTimeMillis();
            long d = ms-lastBackupTime;
            if(d > Utils.DAY) {
                Log.log("backup thread ");
                backup();
//                try { TestServer.webssl.updateIfNeeded(); }
//                catch(Exception e) { e.printStackTrace(); }
//                TestServer.stats.removeOld();
            }
        }, initDelay, Utils.HOUR, TimeUnit.MILLISECONDS);
    }
    
    
    
    public File[] getBackupFiles() {
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
                if(n.startsWith("backup") && n.endsWith(".db")) {
                    String num = n.substring(6, n.length()-3);
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
    public static long getBackupId(String n) {
        if(n.startsWith("backup") && n.endsWith(".db")) {
            String num = n.substring(6, n.length()-3);
            try {
                long t = Long.parseLong(num);
                return t;
            }
            catch(Exception e) { e.printStackTrace(); }
        }
        return -1;
    }
    public void backup()  {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Log.log("Backup start ");
                    File[] backups = null;
                    if(backupFolder.exists()) {
                        backups = backupFolder.listFiles();
                    }
                    else {
                        Log.log("Creating backup dir: " + backupFolder);
                        if(backupFolder.mkdir()) {
                            Log.log("\tSuccess.");
                        }
                        else {
                            Log.log("\tFail.");
                            return;
                        }
                    }
                    long ms = System.currentTimeMillis();
                    
                    String filename = backupFolder.getAbsolutePath() + File.separatorChar + "backup"+ms+".db";
                    Statement s = db.db.createStatement();
                    int res = s.executeUpdate("backup to " + filename);
                    long el = System.currentTimeMillis()-ms;
                    Log.log("Backup " + res + ", " + el);
                    lastBackupTime = ms;
                    
                    if(backups != null) {
                        Log.log("Currently present ", backups.length);
                        if(backups.length > MIN_BACKUPS) {
                            //MIN_BACKUP_TIME
                            for(File f : backups) {
                                String n = f.getName();
                                if(n.startsWith("backup") && n.endsWith(".db")) {
                                    String num = n.substring(6, n.length()-3);
                                    try {
                                        long t = Long.parseLong(num);
                                        long d = ms-t;
                                        if(d > MIN_BACKUP_TIME) {
                                            Log.log("Deleting: ", f.getPath());
                                            f.delete();
                                        }
                                    }
                                    catch(Exception e) { e.printStackTrace(); }
                                }
                            }
                        }
                    }
                   
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        t.start();
    }
    public void restore(long file)  {
        try {
            String filename = backupFolder.getAbsolutePath() + File.separatorChar + "backup"+file+".db";

            Log.log("Restore start " + filename);
            long ms = System.currentTimeMillis();
            Statement s = db.db.createStatement();
            int res = s.executeUpdate("restore from " + filename);
            ms = System.currentTimeMillis()-ms;
            Log.log("Restore " + res + ", " + ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
