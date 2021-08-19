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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import io.netty.handler.codec.http.HttpUtil;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import java.nio.charset.StandardCharsets;
import simpleserver.TestServer;
import simpleserver.stats.IStats;
import simpleserver.web.WebFiles;

public class WebBridgeNetty implements WebBridge<ChannelHandlerContext, FullHttpRequest> {

    @Override
    public String getQuery(ChannelHandlerContext a, FullHttpRequest b) {
        String uriQuery = b.uri();
        int question = uriQuery.indexOf('?');
        return question == -1?null:uriQuery.substring(question+1);
    }

    
    
    @Override
    public String getHeader(ChannelHandlerContext a, FullHttpRequest b, CharSequence name) {
        return b.headers().get(name);
    }

    public static int getHexByte(int a) {
        if(a >= '0' && a <= '9') a -= '0';
        else if(a >= 'A' && a <= 'F') a -= '7';
        else if(a >= 'a' && a <= 'f') a -= 'W';
        else return -1;
        return a;
    }
    public static int getHexByte(int a, int b) {
        a = getHexByte(a);
        b = getHexByte(b);
        if(a != -1 && b != -1) return (a<<4)|b;
        return -1;
    } 
    @Override
    public String getContentString(ChannelHandlerContext a, FullHttpRequest b) {
        ByteBuf aa = b.content();
        if(aa == null) return null;
        
        int percent = 0;
        int r = aa.readerIndex();
        int len = aa.readableBytes();
        for(int i = 0; i < len; i++) {
            byte bb = aa.getByte(r+i);
            if(bb == 37) {
                percent++;
                if(i+2 >= len) return null;
                if(getHexByte(aa.getByte(r+i+1)&0xff, aa.getByte(r+i+2)&0xff) == -1) return null;
                i+=2;
            }
        }
        
        byte[] arr = new byte[len-(percent<<1)];
        int k = 0;
        for(int i = 0; i < len; i++) {
            byte bb = aa.getByte(r+i);
            if(bb == 37) {
                arr[k++] = (byte)getHexByte(aa.getByte(r+i+1)&0xff, aa.getByte(r+i+2));
                i+=2;
            }
            else arr[k++] = bb;
        }
        return new String(arr, StandardCharsets.UTF_8);
//        return aa.toString(StandardCharsets.UTF_8);
    }
    @Override
    public void addHeader(ChannelHandlerContext a, FullHttpRequest b, Object o, CharSequence name, Object val) {
        HttpResponse response = (HttpResponse)o;
        response.headers().add(name, val);
    }
    @Override
    public void setHeader(ChannelHandlerContext a, FullHttpRequest b, Object o, CharSequence name, Object val) {
        HttpResponse response = (HttpResponse)o;
        response.headers().set(name, val);
    }
    
    @Override
    public void json(ChannelHandlerContext a, FullHttpRequest b, String str) {
        jsonEnd(a, b, jsonBegin(a, b, str));
    }

    @Override
    public void binary(ChannelHandlerContext a, FullHttpRequest b, byte[] data) {        
        responseEnd(a, b, binaryBegin(a, b, data));
    }
    
    @Override
    public Object binaryBegin(ChannelHandlerContext a, FullHttpRequest b, byte[] data) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(data));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }


    @Override
    public Object jsonBegin(ChannelHandlerContext a, FullHttpRequest b, String str) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(str.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, WebFiles.APPLICATION_JSON_UTF8);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    @Override
    public void jsonEnd(ChannelHandlerContext a, FullHttpRequest b, Object o) {
        FullHttpResponse response = (FullHttpResponse)o;
        boolean keepAlive = HttpUtil.isKeepAlive(b);
        FullHttpRequestHandler.writeKeepAlive(a, b, response, keepAlive);
    }

    @Override
    public Object responseBegin(ChannelHandlerContext a, FullHttpRequest b, int type) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(type));
        
//        DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(type));
        return response;
    }

    @Override
    public void responseEnd(ChannelHandlerContext a, FullHttpRequest b, Object o) {
        FullHttpResponse response = (FullHttpResponse)o;
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        boolean keepAlive = HttpUtil.isKeepAlive(b);
        FullHttpRequestHandler.writeKeepAlive(a, b, response, keepAlive);
    }

    @Override
    public Object webFileBegin(ChannelHandlerContext a, FullHttpRequest b, WebFiles.WebFile w) {
        return FullHttpRequestHandler.prepareSendWebFile(a, w, b);
    }

    @Override
    public void webFileEnd(ChannelHandlerContext a, FullHttpRequest b, Object o) {
        FullHttpResponse response = (FullHttpResponse)o;
        boolean keepAlive = HttpUtil.isKeepAlive(b);
        FullHttpRequestHandler.writeKeepAlive(a, b, response, keepAlive);
    }
    
    @Override
    public IStats getIStats(ChannelHandlerContext a, FullHttpRequest b) {
        return ((TestServer.XNioSocketChannel)a.channel()).istats;
    }
    
    
}
