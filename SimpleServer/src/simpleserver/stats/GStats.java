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

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import simpleserver.log.Log;
import simpleserver.stats.ResourceMonitor.Re;
import simpleserver.web.Utils;

public class GStats {
    public static final String BANDWIDTH_STAT = "BANDWIDTH_STAT";
        
    public static final String CONNECTION_STAT = "CONNECTION_STAT";
    public static final String HTTP_STAT = "HTTP_STAT";
    public static final String API_STAT = "API_STAT";
    public static final String LOGIN_STAT = "LOGIN_STAT";
    public static final String REGISTER_STAT = "REGISTER_STAT";
    public static final String LOBBY_STAT = "LOBBY_STAT";
    public static final String GAME_STAT = "GAME_STAT";
    
    public ConcurrentHashMap<InetAddress, IStats> stats = new ConcurrentHashMap<>();
    public ResourceMonitor remo;
    
    public Re co, we, api, logi, regi, loby, game;

    public GStats() {
        remo = new ResourceMonitor(3, 1440);
        
        co = remo.add(CONNECTION_STAT);
        we = remo.add(HTTP_STAT);
        api = remo.add(API_STAT);
        logi = remo.add(LOGIN_STAT);
        regi = remo.add(REGISTER_STAT);
        loby = remo.add(LOBBY_STAT);
        game = remo.add(GAME_STAT);
    }
        
    public IStats create(InetAddress ad) {
        IStats istats = new IStats(ad);
        istats.acceptConnection.reco = co; 
        istats.httpRequest.reco = we; 
        istats.apiRequest.reco = api; 
        istats.loginRequest.reco = logi; 
        istats.registerRequest.reco = regi; 
        istats.lobbyRequest.reco = loby; 
        return istats;
    }
    public IStats get(InetAddress addr) {
        return stats.computeIfAbsent(addr, this::create);
    }
    
    public void removeOld() {
        try {
            Log.log("Removing stats: ", stats.size());
            int i = 0;
            long ti = System.currentTimeMillis()-2*Utils.DAY;
            Iterator<Entry<InetAddress, IStats>> iter = stats.entrySet().iterator();
            while(iter.hasNext()) {
                var e = iter.next();
                IStats s = e.getValue();
                if(ti > s.ti) {
                    iter.remove();
                    i++;
                }
            }
            Log.log("Removed: ", i);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
