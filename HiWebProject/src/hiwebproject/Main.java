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

import hiwebproject.HiWebProject.User;
import hiwebproject.HiWebProject.UserUpdate;
import hiwebproject.task.ReblogTask;
import jahive.key.PiKey;
import jahive.tool.Tool;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jcodings.spi.Charsets;
import org.json.JSONObject;
import simpleserver.TestServer;
import static simpleserver.TestServer.conf;
import simpleserver.db.data.DB;
import simpleserver.db.data.DBUtils;
import simpleserver.db.data.TableModel;
import simpleserver.log.PrintFew;
import simpleserver.web.Conf;
import simpleserver.web.ConfFile;
import simpleserver.web.Utils;
import simpleserver.web.WebFiles;
import simpleserver.web.WebLog;
import simpleserver.web.impl.netty.FullHttpRequestHandler;

public class Main {
    public static String appName = "hivereblogger";
    public static String projectWebRoot = "/media/leo/Kaiba/kaiba/HReblog";
    public static String website = "https://localhost:8443";
    public static void main(String[] args) {
        try {
            TestServer.MAX_CONTENT_LENGTH = 4096*16;
            TestServer.httpHandler = createHandler();
            TestServer.webfiles = TestServer.httpHandler.webfiles;
            TestServer.conf = TestServer.webfiles.conf;
            
            var b = Tool.hexStringToByteArray(Files.readString(Paths.get(TestServer.conf.path.path.getAbsolutePath(), ".k"), StandardCharsets.UTF_8).trim());
            HiWebProject.initRubyAndClient(b);
            HiWebProject.startStreamThread();

            TestServer.actions = new ArrayList<>();
            TestServer.actions.add(new AbstractAction("ToggleDebug") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    HiWebProject.printIncomming = !HiWebProject.printIncomming;
                    System.out.println("Print incomming set to " + HiWebProject.printIncomming);
                }
            });
            TestServer.main(args);
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static File streamProperties;
    public static void close() {
        TestServer.db.close();
        WebLog.closeLogs();
        
        if(streamProperties != null) {
            try {
                if(HiWebProject.lastTrx != null) {
                    Properties p = new Properties();
                    p.put("lastTrx", HiWebProject.lastTrx);
                    p.put("lastBlockNum", ""+HiWebProject.lastBlockNum);
                    p.storeToXML(new FileOutputStream(streamProperties), "", StandardCharsets.UTF_8);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    public static FullHttpRequestHandler createHandler() throws Exception {
        return new FullHttpRequestHandler(createWebFiles());
    }
    public static WebFiles createWebFiles() throws Exception {
        Conf conf = new Conf();
        conf.path = new ConfFile(System.getProperty("user.home") + File.separatorChar + ".simpleserver");
        conf.webpath = Paths.get(projectWebRoot);
        conf.websitePath = website;
        streamProperties = new File(conf.path.dir(".stream"), "data.txt");
        WebLog.initLogs(conf);
        
        File f = new File(conf.path.dir(".webdb"), "reblog.db");
        DB db = new DB(f, DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath())) {
            @Override
            public void close() {
                if(db != null) HiWebProject.index.doSave(false);
                super.close(); 
            }
        };
        db.addTableModels(User.class);
       
        DBUtils utils = new DBUtils(db, conf.path.dir(".dbbackup"));
        utils.initBackupThread();
        
        var m = TableModel.getModelData(User.class);
        ResultSet r = db.s.executeQuery("select * from User");
        while(r.next()) {
            User d = m.fill(r);
            HiWebProject.users.put(d.user, d);
            HiWebProject.index.addAllTasks(d.tasks);
        }
        System.out.println("Loaded users " + HiWebProject.users.size());
        
        db.exec.scheduleAtFixedRate(()->{
            HiWebProject.index.doSave(true);
        }, Utils.timeMSTillNext(0), Utils.DAY, TimeUnit.MILLISECONDS);
        
        TestServer.db = db;
        
        WebFiles w = new WebFiles(conf) {
            @Override
            public void initActions() {
                post0("/form/task", (w,a,b)->{
                    Map<String, String> obj = parseBody(a,b);
                    try {
                        String reblogAccount = obj.get("reblogAccount");
                        String token = obj.get("token");
                        
                        if(reblogAccount == null || token == null) {
                            bridge.json(a, b, str(new JSONObject().put("success", "Error No Account!")));
                            return null;
                        }
                        HiWebProject.checkAccess(reblogAccount, token, (t2)->{
                            exe.execute(()->{
                                try {
                                    if(t2 == null) {
                                        bridge.json(a, b, str(new JSONObject().put("success", "logout")));
                                        return;
                                    }
                                    if(!t2.hasPostingAuth) {
                                        bridge.json(a, b, str(new JSONObject().put("success", "posting")));
                                        return;
                                    }

                                    boolean change = false;
                                    if(obj.containsKey("deleteTask")) {
                                        String de = obj.get("deleteTask");
                                        change = HiWebProject.index.remove(reblogAccount, de);
                                    }
                                    else if(obj.containsKey("refreshTask")) {
                                        String re = obj.get("refreshTask");
                                        var t = HiWebProject.index.get(reblogAccount, re);
                                        if(t != null) {
                                            t.rebloggedDay = 0;
                                            t.reblogged = 0;
                                            t.limited = 0;
                                            t.saveStats = true;
                                        }
                                    }
                                    else if(obj.containsKey("taskName")) {
                                        ReblogTask task = ReblogTask.from(reblogAccount, obj);
                                        HiWebProject.index.add(task);
                                        change = true;
                                    }
                                    var json = HiWebProject.index.getAllTasksJSON(reblogAccount);
                                    if(change) {
                                        TestServer.db.send(new UserUpdate(reblogAccount, json==null?"[]":json.toString()) {
                                            @Override
                                            public void run() {
                                                if(error) {
                                                    PrintFew.out("UserUpdateError");
                                                }
                                            }
                                        });
                                    }
                                    bridge.json(a, b, str(new JSONObject().put("success", JSONObject.NULL).put("tasks", json==null?JSONObject.NULL:json)));
                                }
                                catch(Exception e) {
                                    e.printStackTrace();
                                    bridge.json(a, b, str(new JSONObject().put("success", "Error!")));
                                }
                            });
                        });                        
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                        bridge.json(a, b, str(new JSONObject().put("success", "Error!")));
                    }
                    return null;
                });
            }
        };
        w.init();
        return w;
    }
}
