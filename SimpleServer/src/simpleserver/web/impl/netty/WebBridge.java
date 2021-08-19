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
package simpleserver.web.impl.netty;

import simpleserver.stats.IStats;
import simpleserver.web.WebFiles.WebFile;

public interface WebBridge<A, B> {
    public String getQuery(A a, B b);
    public String getHeader(A a, B b, CharSequence name);
    public String getContentString(A a, B b);
    public void json(A a, B b, String str);
    public Object jsonBegin(A a, B b, String str);
    public void addHeader(A a, B b, Object o, CharSequence name, Object val);
    public void setHeader(A a, B b, Object o, CharSequence name, Object val);
    public void jsonEnd(A a, B b, Object o);

    public void binary(A a, B b, byte[] data);       
    public Object binaryBegin(A a, B b, byte[] data);

    public Object responseBegin(A a, B b, int type);
    public void responseEnd(A a, B b, Object o);

    public Object webFileBegin(A a, B b, WebFile w);
    public void webFileEnd(A a, B b, Object o);

    public IStats getIStats(A a, B b);
}