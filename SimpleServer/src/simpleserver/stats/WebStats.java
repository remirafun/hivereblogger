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

import simpleserver.log.Log;

public class WebStats {
    public long ti;
    public long tire, tise;
    public long tore, tose;
    public int re, se;
    public int byre, byse, reca, seca;
    
    Thread t;

    public WebStats(int br, int bs, int rc, int sc) {
        this.byre = br; this.byse = bs; this.reca = rc; this.seca = sc;
        ti = System.currentTimeMillis();
        tire = ti; tise = ti;
    }
    public void init(int br, int bs, int rc, int sc) {
        this.byre = br; this.byse = bs; this.reca = rc; this.seca = sc;
    }
    public int re() {
        Thread t0 = Thread.currentThread();
        if(t == null) t = t0;
        else if(t != t0) {
            Log.log("Multithreaded " + t + ", " + t0);
            System.out.println();
        }
        long no = System.currentTimeMillis();
        long el = no-tire;
        re = (int)Math.max(0, re-el*byre);
        tire = no;
        return reca-re;
    }
    public void re(int _re) {
        tore += _re;
        re += _re;       
    }
    public int se() {
        long no = System.currentTimeMillis();
        long el = no-tise;
        se = (int)Math.max(0, se-el*byse);
        tise = no;
        return seca-se;
    }
    public void se(int _se) {
        //eg 1280 per packet max
        tose += _se;
        se += _se;
    }
    public int sery() {
        long no = System.currentTimeMillis();
        long el = no-tise;
        int _se = (int)Math.max(0, se-el*byse);
        return _se;
    }
}
