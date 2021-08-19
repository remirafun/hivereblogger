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
package simpleserver.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import simpleserver.log.Log;
import simpleserver.web.Utils;

public class ResourceMonitor {
    public static final int DAY_MS = 86400000;
   
    public int nu, si;
    public HashMap<String, Re> ma;
    public ResourceMonitor(int nu, int si) {
        this.nu = nu;
        this.si = si;
        ma = new HashMap<>();
    }
    
    public Re add(String na) {
        return add(na, si);
    }
    public Re add(String na, int si) {
        Re re = new Re(na, si, nu);
        ma.put(na, re);
        return re;
    }
    public Re ge(String na) { return ma.get(na); }
    
    public static long da(long ti) {
        return ti-(ti%DAY_MS);
    }
    
    public static class Re {
        public String na;
        public long ti;
        public int nu, si;
        public ArrayList<Dare> dare;
        
        public Thread t;

        public Re(String na, int si, int nu) {
            this.na = na;
            this.si = si;
            this.nu = nu;
            dare = new ArrayList<>(nu+1);
        }
        public void lo(float va) {
            lo(va, false);
        }
        public void lo(float va, boolean m) {
            if(t == null) t = Thread.currentThread();
            else if(t != Thread.currentThread()) {
                Log.err("Multiple threads, resource " + na);
                Thread.dumpStack();
            }
            long no = System.currentTimeMillis();
            long mo = (int)(no%DAY_MS);
            long da = no-mo;
            Dare a = dare.isEmpty()?null:Utils.peek(dare);
            if(a == null || da != a.ti) {
                if(dare.size() >= nu) { dare.remove(0); }
                a = new Dare(da, si);
                dare.add(a);
            }
            int po = (int)((mo*a.si)/DAY_MS);
            a.lo(po, va, m);
        }
    }
    
    public static class Dare {
        public static final float NONE = -10000;
        public long ti;
        public int si;
        public float[] data;

        //1440 = 24*60 minutes
        public Dare(long ti, int si) {
            this.ti = ti;
            this.si = si;
            data = new float[si<<1];
            Arrays.fill(data, NONE);
        }
        
        public float gemi(int po) {
            return data[po<<1];
        }
        public float gema(int po) {
            return data[(po<<1)+1];
        }
        public void lo(int po, float va, boolean m) {
            po = po<<1;
            float[] da = data;
            if(da[po] == NONE) {
                da[po] = va;
                da[po+1] = va;
            }
            else {
                if(m) {
                    da[po] = Math.min(da[po], va);
                    da[po+1] = Math.max(da[po+1], va);
                }
                else { 
                    da[po] += va;
                    da[po+1] += va;
                }
            }
        }
    }
}
