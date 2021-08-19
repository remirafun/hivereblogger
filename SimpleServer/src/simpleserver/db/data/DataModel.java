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
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DataModel<T extends TableModel> {
//    
//    public DataModel() {
//        this.updateSQL = createUpdate();
//    }
    
    private boolean _existing = false;

    public boolean isExisting() {
        return _existing;
    }

    public void setExisting(boolean _existing) {
        this._existing = _existing;
    }
    
    
    public String getString(String name) throws Exception {
        return (String)getClass().getDeclaredField(name).get(this);
    }
    public int getInt(String name) throws Exception {
        return getClass().getDeclaredField(name).getInt(this);
    }
    public long getLong(String name) throws Exception {
        return getClass().getDeclaredField(name).getLong(this);
    }
    public byte[] getBytes(String name) throws Exception {
        return (byte[])getClass().getDeclaredField(name).get(this);
    }
    public void set(String name, Object value) throws Exception {
        getClass().getDeclaredField(name).set(this, value);
    }
    public static class SimpleQuery {
        public String query;
        public List<Field> fields;
                
        public SimpleQuery(String query, List<Field> fields) {
            this.query = query;
            this.fields = fields;
        }

        @Override
        public String toString() {
            return query;
        }
        public boolean fillSelect(PreparedStatement p, DataModel t) throws Exception  {
            fill(p, t);
            var d = TableModel.getModelData(t.getClass());
            ResultSet r = p.executeQuery();
            if(r.next()) {
                for(var f : d.fields) {
                    var ty = f.getType();
                    var na = f.getName();
                    if(na.equals(d.primaryKey.getName())) continue;
                    Object o = null;
                    if(ty.equals(String.class)) o=r.getString(na);
                    else if(ty.equals(Integer.class) || ty.equals(int.class)) o=r.getInt(na);
                    else if(ty.equals(Long.class) || ty.equals(long.class)) o=r.getLong(na);
                    else if(ty.equals(byte[].class)) o=r.getBytes(na);
                    else throw new UnsupportedOperationException("type not yet supported " + ty);
                    t.set(na, o);
                }
                t._existing = true;
                return true;
            }
            return false;
        }
        public boolean fillInsert(PreparedStatement p, DataModel t, Statement s) throws Exception  {
            fill(p, t);
            int i = p.executeUpdate();
            if(i >= 1) {
                var d = TableModel.getModelData(t.getClass());
                ResultSet r = s.executeQuery("select last_insert_rowid();");
                if(r.next()) {
                    var f = d.primaryKey;
                    var ty = f.getType();
                    var na = f.getName();
                    Object o = null;
                    if(ty.equals(String.class)) o=r.getString(1);
                    else if(ty.equals(Integer.class) || ty.equals(int.class)) o=r.getInt(1);
                    else if(ty.equals(Long.class) || ty.equals(long.class)) o=r.getLong(1);
                    else if(ty.equals(byte[].class)) o=r.getBytes(1);
                    else throw new UnsupportedOperationException("type not yet supported " + ty);
                    t.set(na, o);
                    t._existing = true;
                    return true;
                }
            }
            return false;
        }
        public int fillExecute(PreparedStatement p, DataModel t) throws Exception  {
            fill(p, t);
            int i = p.executeUpdate();
            if(i > 0) { t._existing = true; }
            return i;
        }
        public int fillDelete(PreparedStatement p, DataModel t) throws Exception  {
            fill(p, t);
            int i = p.executeUpdate();
            if(i > 0) { t._existing = false; }
            return i;
        }
        public void fill(PreparedStatement p, DataModel t) throws Exception  {
            int i = 1;
            for(var f : fields) {
                var ty = f.getType();
                var na = f.getName();
                if(ty.equals(String.class)) p.setString(i, t.getString(na));
                else if(ty.equals(Integer.class) || ty.equals(int.class)) p.setInt(i, t.getInt(na));
                else if(ty.equals(Long.class) || ty.equals(long.class)) p.setLong(i, t.getLong(na));
                else if(ty.equals(byte[].class)) p.setBytes(i, t.getBytes(na));
                else throw new UnsupportedOperationException("type not yet supported " + ty);
                i++;
            }
        }
    }
}
