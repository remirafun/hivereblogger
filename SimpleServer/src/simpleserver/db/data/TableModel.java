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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class TableModel extends DataModel {    
    public static HashMap<Class, DataModelData> tableModelData = new HashMap<>();
    public static class DataModelData {
        public Class cls;
        public Field[] fields;
        public String tableName;
        public SimpleQuery updateSQL;
        public SimpleQuery insertSQL, selectById, deleteById;
        public Field primaryKey;
        
        public void init() {
            updateSQL = createUpdate();
        }
        public SimpleQuery createUpdate() {
            String id = primaryKey.getName();

            StringBuilder a = new StringBuilder();
            a.append("update ").append(tableName).append(" set ");
            var list = new ArrayList<Field>();
            Field[] fs = cls.getDeclaredFields();
            boolean first = true;
            Field idField = null;
            for(Field f : fs) {
                String na = f.getName();
                if(na.equals(id)) { idField = f; continue; }
                if(!first) a.append(", "); first = false;
                a.append(na).append("=?");
                list.add(f);
            }
            a.append(" where ").append(id).append("=?");
            list.add(idField);
            return new SimpleQuery(a.toString(), list);
       }
        public <T extends DataModel> T fill(ResultSet r) throws Exception {
            DataModel t = (DataModel)cls.newInstance();
            for(var f : fields) {
                var ty = f.getType();
                var na = f.getName();
                Object o = null;
                if(ty.equals(String.class)) o=r.getString(na);
                else if(ty.equals(Integer.class) || ty.equals(int.class)) o=r.getInt(na);
                else if(ty.equals(Long.class) || ty.equals(long.class)) o=r.getLong(na);
                else if(ty.equals(byte[].class)) o=r.getBytes(na);
                else throw new UnsupportedOperationException("type not yet supported " + ty);
                t.set(na, o);
            }
            t.setExisting(true);
            return (T) t;
        }
    }
    
    public static class TableModelData extends DataModelData {
        public String tableSQL;

        public TableModelData() {}
        public TableModelData(Class cls) {
            this.cls = cls;
            this.fields = cls.getDeclaredFields();
            this.tableName = cls.getSimpleName();
            
            Field[] fs = cls.getDeclaredFields();
            boolean first = true;
            for(Field f : fs) {
                String na = f.getName();
//                if(!first) a.append(",\n"); first = false;
//                a.append(na).append(' ');
                var Type = f.getAnnotation(Type.class);
                if(Type != null) ;//a.append(Type.value());
                else {
                    var Null = f.getAnnotation(Null.class);
                    var PrimaryKey = f.getAnnotation(PrimaryKey.class);

                    var Char = f.getAnnotation(Char.class);
                    var Varchar = f.getAnnotation(Varchar.class);
                    var Blob = f.getAnnotation(Blob.class);

                    if(Char != null) ;//a.append("char(").append(Char.value()).append(") ");
                    else if(Varchar != null) ;//a.append("varchar(").append(Varchar.value()).append(") ");
                    else if(Blob != null) ;//a.append("blob ");
                    else {
                        Class type = f.getType();
                        if(type.equals(Integer.class) || type.equals(int.class)) ;//a.append("integer ");
                        else if(type.equals(Long.class) || type.equals(long.class)) ;//a.append("bigint ");
                        else throw new IllegalArgumentException(f.toString());
                    }
                    if(Null == null) ;//a.append("not null ");
                    if(PrimaryKey != null) {
                        if(primaryKey != null) throw new IllegalArgumentException("Multiple primary keys for table " + cls.getSimpleName());
                        primaryKey = f;
                    } //a.append("primary key");
                }
            }
            
            this.tableSQL = createTableSQL(cls);
            this.insertSQL = createInsertSQL(this);
            this.selectById = createQuery("select *", primaryKey.getName());
            this.deleteById = createQuery("delete", 0, primaryKey.getName());
            init();
        }
                
        public static String createTableSQL(Class cls) {
            String tableName = cls.getSimpleName();
            StringBuilder a = new StringBuilder();
            a.append("create table if not exists ").append(tableName).append(" (\n");
            Field[] fs = cls.getDeclaredFields();
            boolean first = true;
            for(Field f : fs) {
                String na = f.getName();
                if(!first) a.append(",\n"); first = false;
                a.append(na).append(' ');
                var Type = f.getAnnotation(Type.class);
                if(Type != null) a.append(Type.value());
                else {
                    var _Null = f.getAnnotation(Null.class);
                    var _Unique = f.getAnnotation(Unique.class);
                    var _PrimaryKey = f.getAnnotation(PrimaryKey.class);

                    var _Char = f.getAnnotation(Char.class);
                    var _Varchar = f.getAnnotation(Varchar.class);
                    var _Blob = f.getAnnotation(Blob.class);

                    if(_Char != null) a.append("char(").append(_Char.value()).append(") ");
                    else if(_Varchar != null) a.append("varchar(").append(_Varchar.value()).append(") ");
                    else if(_Blob != null) a.append("blob ");
                    else {
                        Class type = f.getType();
                        if(type.equals(Integer.class) || type.equals(int.class)) a.append("integer ");
                        else if(type.equals(Long.class) || type.equals(long.class)) a.append("bigint ");
                        else throw new IllegalArgumentException(f.toString());
                    }
                    if(_Null == null) a.append("not null ");
                    if(_Unique != null) {
                        a.append("unique ");
                        if(_Unique.value() != Unique.OR.none) 
                            a.append("on conflict ").append(_Unique.value().name()).append(' ');
                    }
                    if(_PrimaryKey != null) a.append("primary key");
                }
            }
            a.append(");\n");
            return a.toString();
        }
        public static SimpleQuery createInsertSQL(TableModelData t) {
            //("INSERT INTO users (username, email, heslo, sol, created, lastlogin, attempts, datastamp, storestamp, rating, unratedTime, ratedTime, pts, cup, gp1, gp2, bootcampticket, storeticket, chars, drills, gifts, store, settings) VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?, 500, 0, 0, 0, 0, 1000, 0, 0, 1, ?, ?, ?, ?, ?)");
            String tableName = t.tableName;
            StringBuilder a = new StringBuilder();
            a.append("insert into ").append(tableName).append(" (");
            boolean first = true;
            var list = new ArrayList<Field>();
            for(Field f : t.fields) {
                String na = f.getName();
                if(na.equals(t.primaryKey.getName())) continue;
                if(!first) a.append(","); first = false;
                a.append(na);
                list.add(f);
            }
            a.append(") values (");
            for(int i = 0; i < list.size(); i++) a.append(i==0?"?":",?");
            a.append(");");
            return new SimpleQuery(a.toString(), list);
        }
        
        public SimpleQuery createQuery(String query, String... names) {
            return createQuery(query, 1, names);
        }
        public SimpleQuery createQuery(String query, int limit, String... names) {
            String tableName = cls.getSimpleName();
            StringBuilder a = new StringBuilder();
            a.append(query).append(" from ").append(tableName).append(" where ");
            int fi = 0;
            Field[] fields = new Field[names.length];

            Field[] fs = cls.getDeclaredFields();
            boolean first = true;
            for(Field f : fs) {
                String na = f.getName();
                
                if(contains(names, na)) {
                    fields[fi++] = f;
                    if(!first) a.append(",\n"); first = false;
                    a.append(na).append(" = ? ");
                }
            }
            if(limit > 0) a.append("limit ").append(limit);
            if(fi == fields.length) {
                return new SimpleQuery(a.toString(), Arrays.asList(fields));
            }
            throw new IllegalArgumentException();
        }
        
        
    }
    public static TableModelData getTableData(Class cls) {
        return (TableModelData)getModelData(cls);
    }
    public static DataModelData getModelData(Class cls) {
        if(!DataModel.class.isAssignableFrom(cls)) throw new IllegalArgumentException();
        DataModelData obj = tableModelData.get(cls);
        if(obj == null) {
            obj = new TableModelData(cls);
            tableModelData.put(cls, obj);
        }
        return obj;
    } 
    
    
    public static boolean contains(String[] a, String val)  {
        for (int i = 0; i < a.length; ++i){
            if (a[i].equals(val)){
                return true;
            }
        }
        return false;
    }
    
    
}
