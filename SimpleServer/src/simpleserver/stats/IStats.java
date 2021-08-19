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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import simpleserver.web.WebLog;

public class IStats {
    public static final int MAX_CONNS_PER_IP = 16;
    
    public InetAddress addr;
    public int addrHash;
    public byte[] addrBytes;
    public ArrayList<SStats> conns = new ArrayList<>(MAX_CONNS_PER_IP);
    public long ti;
    
    public ResourceStats acceptConnection = new ResourceStats(1, 1000, 16000);  //1000 per second... 16 connections   1 conn/second
    public ResourceStats httpRequest = new ResourceStats(1, 1000, 180000);   // 1000 per second / 48    
    public ResourceStats apiRequest = new ResourceStats(1, 1000, 60000);   // 1000 per second / 48    
    public ResourceStats loginRequest = new ResourceStats(1, 10000, 70000);   // 1000 per second / 7
    public ResourceStats registerRequest = new ResourceStats(1, 10000, 70000);   // 1000 per second / 7
    public ResourceStats lobbyRequest = new ResourceStats(1, 10000, 70000);   // 1000 per second / 7
    
    public ResourceStats loginSkipTextRequest = new ResourceStats(1, 3600000, 2*3600000);
    
    public byte[] te = new byte[6];
    
    public boolean checkTe(String str, boolean allowSkip) {
        if(te[0] == 0 || str.length() > te.length) return false; 
        if(te[0] < 3 && te[0] > 0) {
            te[0] = (byte)(te[0]-1);
            return true;
        }
        try {
            for(int i = 0; i < te.length; i++) {
                byte ch = te[i];
                if(ch == 0) return str.length() == i;
                if((te[i]&0xff) != str.charAt(i)) return false;
            }
            if(allowSkip && te[0] == '+') return loginSkipTextRequest.rede();
            return true;
        }
        finally { te[0] = 0; }
    }

    public IStats(InetAddress addr) {
        this.addr = addr;
        this.addrHash = addr.hashCode();
        addrBytes = addr.getAddress();
        ti = System.currentTimeMillis();
    }
    
    //accept thread
    public boolean canConnect() {
        return conns.size() < MAX_CONNS_PER_IP;
    }
    public SStats connect(Object o, Executor exe) {
        SStats s = new SStats(o);
        exe.execute(()->{
            ti = System.currentTimeMillis();
            conns.add(s);
            WebLog.access(ti, addrBytes, 0, (char)conns.size(), (char)11057, null, null, null, null, null);
        });
        return s;
    }
    public void disconnect(SStats s) {
        ti = System.currentTimeMillis();
        conns.remove(s);
    }

    @Override
    public int hashCode() {
        return addrHash;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof IStats) return addr.equals(((IStats) obj).addr);
        return false;
    }
    
    
    
}
