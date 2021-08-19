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
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import simpleserver.db.DBRequest;
import simpleserver.log.Log;
import simpleserver.log.PrintFew;

public class DB {
    public ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    public Executor msgExec;
    public ArrayList<Class> tableModels = new ArrayList<>();
    public HashMap<String, PreparedStatement> statements = new HashMap<>();
    public Connection db;
    public Statement s;
    public File file;

    public DB() {}
    public DB(File f, Connection db) throws SQLException {
        this.file = f;
        this.db = db;
        this.s = db.createStatement();
        s.execute("PRAGMA encoding = \"UTF-8\";");
    }
    
    public void send(DBRequest r) {
        exec.execute(()->{
            try { r.exec(this); }
            catch(Exception e) {
                r.error = true;
                PrintFew.out(e.toString());
            }
            finally { msgExec.execute(r::run); }
        });
    } 
    
    public void addTableModels(Class<? extends TableModel>... models) throws SQLException {
        for(var a : models) {
            s.execute(TableModel.getTableData(a).tableSQL);
            tableModels.add(a);
        }
    }
    public boolean add(DataModel t) throws Exception {
        if(t.isExisting()) throw new IllegalArgumentException();
        var d = TableModel.getModelData(t.getClass());
        var a = d.insertSQL;
        return a.fillInsert(statement(a.query), t, s);
    }
    public int update(DataModel t) throws Exception {
        var d = TableModel.getModelData(t.getClass());
        var a = d.updateSQL;
        return a.fillExecute(statement(a.query), t);
    }
    public int remove(DataModel t) throws Exception {
        var d = TableModel.getModelData(t.getClass());
        var a = d.deleteById;
        return a.fillDelete(statement(a.query), t);
    }
    public boolean selectById(DataModel t) throws Exception {
        var d = TableModel.getModelData(t.getClass());
        var a = d.selectById;
        return a.fillSelect(statement(a.query), t);
    } 
    
    public PreparedStatement statement(String str) throws SQLException {
        System.out.println(str);
        var p = statements.get(str);
        if(p == null) statements.put(str, p=db.prepareStatement(str));
        return p;
    }
    public void close() {
        if(db != null) {
            try {
                db.close();
                db = null;
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static DB createInMemoryDB() throws SQLException {
        return new DB(null, DriverManager.getConnection("jdbc:sqlite::memory:"));
    }
    public static DB createFileDB(File f) throws SQLException {
        return new DB(f, DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath()));
    }
}
