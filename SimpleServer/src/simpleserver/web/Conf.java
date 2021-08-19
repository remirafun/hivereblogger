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
package simpleserver.web;

import java.nio.file.Path;
import java.util.ArrayList;

public class Conf {
    public boolean debug;
    public SSLCertificateLetsEncrypt sslLetsEncrypt;
    /** web path without www*/
    public String websitePath; 
    /** temporary dir for storage: database, properties, etc */
    public ConfFile path;
    /** website files to be served*/
    public Path webpath;
    
    /**
     * Creates a free ssl certificate with lets encrypt
     * Add domains and subdomains to create the same certificate for multiple subdomains.
     * Eg. ("example.com", "www.example.com", "mail.example.com")
     */
    public static class SSLCertificateLetsEncrypt {
        public ArrayList<String> domains;

        public SSLCertificateLetsEncrypt(String... _domains) {
            domains = new ArrayList<>();
            for(String s : _domains) {
                int i = s.indexOf("://");
                if(i != -1) s = s.substring(i+3);
                domains.add(s);
            }
            
        }
        
    }
}
