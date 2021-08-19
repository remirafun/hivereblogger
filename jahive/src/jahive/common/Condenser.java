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
package jahive.common;

import jahive.tool.Tool;
import java.util.function.Consumer;
import org.json.JSONObject;
import java.net.http.*;

/**
 *
 * @author leo
 */
public class Condenser implements API {
    public Client cl;
    public Condenser(Client client) {
        this.cl = client;
    }
    
    /**
     * condenser_api.get_dynamic_global_properties
     * @param a
     */
    public void get_dynamic_global_properties(Consumer<JSONObject> a) {
        cl.method("condenser_api.get_dynamic_global_properties", (r)->httpResponseToJson(r, a));
    }
            
    /**
     * condenser_api.get_active_votes
     * @param a
     * @param author
     * @param permlink
     */
    public void get_active_votes(Consumer<JSONObject> a, String author, String permlink) {
        cl.method("condenser_api.get_active_votes", Tool.jsonArray(author, permlink), (r)->httpResponseToJson(r, a));
    }
    
    /**
     * condenser_api.get_transaction_hex
     * @param a
     * @param txArr eg.: "[{expiration:...}]"
     */
    public void get_transaction_hex(Consumer<JSONObject> a, String txArr) {
        cl.method("condenser_api.get_transaction_hex", txArr, (r)->httpResponseToJson(r, a));
    }
    
    
    /**
     * condenser_api.get_block_header
     * @param a
     * @param block eg.: "[123]"
     */
    public void get_block_header(Consumer<JSONObject> a, int block) {
        cl.method("condenser_api.get_transaction_hex", Tool.jsonArray(block), (r)->httpResponseToJson(r, a));
    }    
    
    /**
     * condenser_api.broadcast_transaction
     * @param a
     * @param txArr eg.: "[{expiration:...}]"
     */
    public void broadcast_transaction(Consumer<JSONObject> a, String txArr) {
        cl.method("condenser_api.broadcast_transaction", txArr, (r)->httpResponseToJson(r, a));
    }
    
    public static void httpResponseToJson(HttpResponse<String> r, Consumer<JSONObject> a) {
        if(r != null && r.statusCode() == 200) {
            try {
                var j = new JSONObject(r.body());
                a.accept(j);
            }
            catch(Exception e) {
                a.accept(null);
            }
        }
        else a.accept(null);
    }
}
