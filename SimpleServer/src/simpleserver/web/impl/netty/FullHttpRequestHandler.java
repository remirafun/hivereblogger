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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import io.netty.handler.codec.http.HttpUtil;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.util.AsciiString;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import simpleserver.TestServer;
import simpleserver.log.Log;
import simpleserver.stats.IStats;
import simpleserver.stats.ResourceStats;
import simpleserver.web.WebFiles;
import simpleserver.web.WebLog;

public class FullHttpRequestHandler {
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    
    public WebFiles webfiles;

    public FullHttpRequestHandler(WebFiles webfiles) {
        this.webfiles = webfiles;
    }
    
    public boolean isHost(String host) {
        return host != null && webfiles.conf.websitePath.endsWith(host);
    }

    public void handle(ChannelHandlerContext ctx, FullHttpRequest req) {
        boolean keepAlive = HttpUtil.isKeepAlive(req);
        if(HttpUtil.is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
        }
        
        IStats istats = ((TestServer.XNioSocketChannel)ctx.channel()).istats;
        ResourceStats res = req.method() == HttpMethod.POST?istats.apiRequest:istats.httpRequest;
        if(!res.rede()) {
            if(res.li == 100) {
                //100 attempts... hmm...
                Log.log("IP " + istats.addr + " http/api request limit 100 times.");                        
            }
            //weblog hmm ignore log?

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.TOO_MANY_REQUESTS);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
            response.headers().set(HttpHeaderNames.RETRY_AFTER, "10000");
            writeKeepAlive(ctx, req, response, keepAlive);
            return;
        }

        handle0(ctx, req, keepAlive);
    }
    public void handle0(ChannelHandlerContext ctx, FullHttpRequest req, boolean keepAlive) {
        String uriQuery = req.uri();
        int question = uriQuery.indexOf('?');
        String uri/*, query*/;
        if(question == -1) {
            uri = uriQuery; /*query = null*/
        }
        else {
            uri = uriQuery.substring(0, question);
//              query = uriQuery.substring(question+1);
        }
        HttpHeaders h = req.headers();
        String host = h.get(HttpHeaderNames.HOST);
        String subdomain = null;
        boolean isWWW = false;
        if(host != null) {
            subdomain = getSubdomain(host);
            if(subdomain != null) {
                if(subdomain.equals("www")) isWWW = true;
                else uri = subdomain+uri;
            }
        }

        if(req.method() == HttpMethod.GET) {
            if(isWWW) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);
                response.headers().set(HttpHeaderNames.LOCATION, webfiles.conf.websitePath+uriQuery);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                writeKeepAlive(ctx, req, response, keepAlive);
                return;
            }
            //redirect www

            WebFiles.WebFile w = webfiles.webFiles.get(uri);
            if(w != null) {

                if(w.action != null) {
                    w = w.action.action(w, ctx, req);
                    if(w == null) return;
                }

                sendWebFile(ctx, w, req);
                return;
            }
        }
        else if(req.method() == HttpMethod.POST) {
            WebFiles.WebFile a = webfiles.webPost.get(uri);
            if(a != null && a.action != null) {
                WebFiles.WebFile w = a.action.action(a, ctx, req);
                if(w != null) {
                    sendWebFile(ctx, w, req);
                }
                return;
            }
        }

        if(req.method() == HttpMethod.GET || req.method() == HttpMethod.HEAD) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);
            response.headers().set(HttpHeaderNames.LOCATION, "/");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            writeKeepAlive(ctx, req, response, keepAlive);
        }
        else if(req.method() == HttpMethod.POST) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            writeKeepAlive(ctx, req, response, keepAlive);
        } 
        else {
            Log.log("METHOD " + req.method());
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            writeKeepAlive(ctx, req, response, keepAlive);
        }
   
    }
    public static void writeKeepAlive(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse response, boolean keepAlive) {
        char status = (char)response.status().code();
        if(status != 429) {
            IStats istats = ((TestServer.XNioSocketChannel)ctx.channel()).istats;
            HttpHeaders h = req.headers();
            String cookie = h.get(HttpHeaderNames.COOKIE);
            String username = null;
            if(cookie != null) {
                int i = cookie.indexOf("isession=");
                if(i != -1) {
                    int i2 = cookie.indexOf(';', i);
                    if(i2 == -1) i2 = cookie.length();
                    i += 8+24;
                    if(i < cookie.length()) username = cookie.substring(i, i2);
                }
            }
            AsciiString mm = req.method().asciiName();
            byte m[] = mm.array(); 
            int mo = mm.arrayOffset();
            mo = (m[mo] << 8) | m[mo+1];
            WebLog.access(System.currentTimeMillis(), istats.addrBytes, 
                response.content().readableBytes(), status, (char)mo, req.uri(), h.get(HttpHeaderNames.REFERER),
                h.get(HttpHeaderNames.ORIGIN), h.get(HttpHeaderNames.USER_AGENT), username);
        }
        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }
    public static DefaultFullHttpResponse prepareSendWebFile(ChannelHandlerContext ctx, WebFiles.WebFile w, HttpRequest req) {
            boolean keepAlive = HttpUtil.isKeepAlive(req);
        return prepareSendWebFile(ctx, w, req, keepAlive);
    }
    public static DefaultFullHttpResponse prepareSendWebFile(ChannelHandlerContext ctx, WebFiles.WebFile w, HttpRequest req, boolean keepAlive) {
        String ifModifiedSince = req.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        //System.out.println("modified " + ifModifiedSince);
        //System.out.println("w " + w.lastModified);
        //Mon, 15 Feb 2021 19:44:20 GMT

        DefaultFullHttpResponse response;

        if(w.lastModified != null && ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            try {
                Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
                long since = ifModifiedSinceDate.getTime();
//                    DateFormatter.parseHttpDate(ifModifiedSince);
                if(w.lastModifiedMs <= since) {
                    response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
                    response.headers().set(HttpHeaderNames.DATE, Date.from(Instant.now()));
                    return response;
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        if(useGZip(w, req)) {
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, w.contentZip.duplicate());
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, w.lengthZip);
            response.headers().set(HttpHeaderNames.CONTENT_ENCODING, HttpHeaderValues.GZIP);
        }                       
        else {
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, w.content.duplicate());
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, w.length);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, w.contentType);
//            response.headers().set(HttpHeaderNames.DATE, w.contentType);
        response.headers().set(HttpHeaderNames.DATE, Date.from(Instant.now()));
        if(w.lastModified != null) response.headers().set(HttpHeaderNames.LAST_MODIFIED, w.lastModified);

        return response;
    }
    public static void sendWebFile(ChannelHandlerContext ctx, WebFiles.WebFile w, FullHttpRequest req) {
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            DefaultFullHttpResponse response = prepareSendWebFile(ctx, w, req, keepAlive);
            writeKeepAlive(ctx, req, response, keepAlive);
    }
    public static boolean useGZip(WebFiles.WebFile w, HttpRequest req) {
        if(w.contentZip != null) {
            String contentEncoding = req.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
            if(contentEncoding != null && contentEncoding.contains("gzip")) {
                return true;
            }
        }
        return false;
    }
    public static String getSubdomain(String host) {
        int i = host.indexOf('/');
        i = host.lastIndexOf('.', i==-1?host.length():(i-1));
        if(i == -1) return null;
        i = host.lastIndexOf('.', i-1);
        if(i == -1) return null;
        return host.substring(0, i);
    }
}
