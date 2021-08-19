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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieEncoder;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.AsciiString;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import simpleserver.db.DBRequest;
import simpleserver.log.Log;
import simpleserver.stats.*;
import simpleserver.web.PointText.BitSetLayer;
import simpleserver.web.impl.netty.WebBridge;
import simpleserver.web.impl.netty.WebBridgeNetty;

public class WebFiles {
    public static final AsciiString IMAGE_PNG = AsciiString.cached("image/png");
    public static final AsciiString IMAGE_JPG = AsciiString.cached("image/jpeg");
    public static final AsciiString IMAGE_GIF = AsciiString.cached("image/gif");
    public static final AsciiString IMAGE_ICO = AsciiString.cached("image/x-icon");
    public static final AsciiString FONT_WOFF = AsciiString.cached("font/woff");
    public static final AsciiString FONT_WOFF2 = AsciiString.cached("font/woff2");
    public static final AsciiString SVG_UTF8 = AsciiString.cached("image/svg+xml");
    public static final AsciiString TEXT_HTML_UTF8 = AsciiString.cached("text/html; charset=utf-8");
    public static final AsciiString TEXT_JS_UTF8 = AsciiString.cached("text/javascript; charset=utf-8");
    public static final AsciiString TEXT_CSS_UTF8 = AsciiString.cached("text/css; charset=utf-8");
    public static final AsciiString TEXT_PLAIN_UTF8 = AsciiString.cached("text/plain; charset=utf-8");
    public static final AsciiString APPLICATION_XML_UTF8 = AsciiString.cached("application/xml; charset=utf-8");
    
    public static final AsciiString APPLICATION_JSON_UTF8 = AsciiString.cached("application/json; charset=utf-8");
    
    public static final AsciiString LAYOUT_BODY = AsciiString.cached("layoutBody");
    public static final AsciiString LA = AsciiString.cached("la");
    public static final AsciiString LB = AsciiString.cached("lb");
    public static final AsciiString NAVBAR = AsciiString.cached("navbar");
    public static final AsciiString FOOTER = AsciiString.cached("footer");
    public static final AsciiString WW = AsciiString.cached("ww");
    public static final AsciiString TITLE = AsciiString.cached("title");
    public static final AsciiString CANONICAL = AsciiString.cached("canonical");
    public static final CharSequence COOKIE = HttpHeaderNames.COOKIE;
    public static final AsciiString CrossOriginOpenerPolicy = AsciiString.cached("Cross-Origin-Opener-Policy");
    public static final AsciiString CrossOriginEmbedderPolicy = AsciiString.cached("Cross-Origin-Embedder-Policy");
    public static final AsciiString sameorigin = AsciiString.cached("same-origin");
    public static final AsciiString requirecorp = AsciiString.cached("require-corp");

    public static final String ISESSSION = "isession";
    public static final String CACHE = "_cache";
    
    public static final byte[] ZERO_BYTE_ARRAY = new byte[0];    

   
   
    public static interface WebAction<A, B> {
        public WebFile action(WebFile f, A a, B b);
    }
    public static interface WebDBBridge {
        public void send(DBRequest a);
    }
    
    public static class WebFile {
        public String path;
        public AsciiString contentType;
        public int length, lengthZip;
        public ByteBuf content, contentZip;
        public long lastModifiedMs;
        public long lastModifiedExact;
        public AsciiString lastModified;
        
        public WebAction action;

        public void setLastModified(long l) {
            lastModifiedExact = l;
            lastModifiedMs = (l/1000)*1000;
            lastModified = new AsciiString(DateFormatter.format(Date.from(Instant.ofEpochMilli(lastModifiedMs))));
        }
        
        @Override
        public String toString() {
            return path;
        }
        
        public void clean() {
            releaseUnreleasable(content);
            releaseUnreleasable(contentZip);
        }
    }
    public static void releaseUnreleasable(ByteBuf b) {
        if(b != null) {
            for(int i = 0; i < 100; i++) {
                var bb = b.unwrap();
                if(bb == null) break;
                else b = bb;
            }
            b.release();
        }
    }
    public static class WebLayout {
        public byte[] data;
        public ArrayList<Object> points = new ArrayList<>();

        public WebLayout(byte[] data) {
            this.data = data;
            //<?=    ?>
            int f = 0;
            loop:
            for(int i = 0; i < data.length-2; i++) {
                
                if(data[i] == '<' && data[i+1] == '?' && data[i+2] == '=') {
                    //f i
                    if(f != i) points.add(new Point(f, i));
                    
                    for(int k = i+3; k < data.length-1; k++) {
                        if(data[k] == '?' && data[k+1] == '>') {
                            //i+3  k
                            points.add(new AsciiString(data, i+3, k-(i+3), false));                            
                            f = k+2;
                            i = k+1;
                            continue loop;
                        }
                    }
                }
            }
            if(f < data.length) {
                points.add(new Point(f, data.length));
            }
        }
        
        public int calcSize(HashMap<AsciiString, AsciiString> vars) {
            int l = 0;
            for(Object o : points) {
                if(o instanceof Point) {
                    Point p = (Point)o;
                    l += p.y-p.x;
                }
                else {
                    AsciiString str = (AsciiString)o;
                    AsciiString s = vars.get(str);
                    if(s == null) {
                        throw new NullPointerException(str.toString());
                    }
                    l += s.length();
                }
            }
            return l;
        }
        public byte[] create(HashMap<AsciiString, AsciiString> vars) {
            int l = calcSize(vars);
            //ByteBuf b = Unpooled.directBuffer(l, l);
            byte[] tmp = new byte[l];
            int pos = 0;
            for(Object o : points) {
                if(o instanceof Point) {
                    Point p = (Point)o;
//                    b.writeBytes(data, p.x, p.y-p.x);
                    System.arraycopy(data, p.x, tmp, pos, p.y-p.x);
                    pos += p.y-p.x;
                }
                else {
                    AsciiString str = (AsciiString)o;
                    AsciiString s = vars.get(str);
                    System.arraycopy(s.array(), s.arrayOffset(), tmp, pos, s.length());
                    pos += s.length();
                }
            }
            return tmp;
        }
        public void print() {
            for(Object o : points) {
                if(o instanceof Point) {
                    Point p = (Point)o;
                    System.out.println(new AsciiString(data, p.x, p.y-p.x, false));
                }
                else {
                    AsciiString str = (AsciiString)o;
                    System.out.println("'"+str+"'");
                }
            }
        }
        
    }

    public WebBridge bridge;
    public WebDBBridge db;
    public Executor exe;
    public HashMap<String, WebFile> webFiles = new HashMap<>();
    public HashMap<String, WebFile> webPost = new HashMap<>();
    
    public int[] serverId;
    public GStats stats;
    public Conf conf;
    
    public boolean init = false;
    
    public WebFiles(Conf conf) {
        this.conf = conf;
    }
    
    
    
    public void reload() {
        try {
            for(var v : webFiles.values()) v.clean();
            for(var v : webPost.values()) v.clean();
            webFiles.clear();
            webPost.clear();
            init = false;
            init();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void init() throws IOException {
        if(init) return;
        init = true;
        if(bridge == null) bridge = new WebBridgeNetty();
        //html files  except ~
        Path path = conf.webpath;
        
        Path filesGen = path.resolve("files-gen");
        
        HashMap<AsciiString, AsciiString> vars = new HashMap<>();
        initFilesGen(filesGen, vars);

//        initVars(Files.readAllBytes(Paths.get(filesGen.toString(), "vars.php")), vars);

        initHtml("", path, vars);
        initFiles("", path, vars);
        
        //1994-11-05T13:15:30Z
        //1994-11-05T13:15:30Z
        { 
            long file = 0;
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            for(WebFile w : webFiles.values()) {
                if(w.contentType == TEXT_HTML_UTF8) {
                    sb.append("<url>\n<loc>").append(conf.websitePath).append(w.path.equals("/")?"":w.path).append("</loc>\n")
                    .append("<lastmod>");
                    file = Math.max(file, w.lastModifiedMs);
                    sb.append(f.format(Date.from(Instant.ofEpochMilli(w.lastModifiedMs))));
                    sb.append("</lastmod>\n</url>\n");
                }
            }
            sb.append("</urlset>\n");
            byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
            
            WebFile w = new WebFile();
            w.path = "/sitemap.xml";
            w.contentType = APPLICATION_XML_UTF8;
            w.setLastModified(file);
            w.content = createByteBuf(data);
            w.length = data.length;

//            if(compress) {
                byte[] gzip = gzipCompress(data);
                if(gzip.length < data.length) {
                    w.contentZip = createByteBuf(gzip);
                    w.lengthZip = gzip.length;
                }
//            }
            webFiles.put(w.path, w);
        }
        
        initHost();
        
        long nonZip = 0, zip = 0;
        for(WebFile w : webFiles.values()) {
            System.out.println(w);
            nonZip += w.length;
            zip += w.lengthZip;
            //System.out.println(gzip.length/(float)data.length);
        } 
        System.out.println("file: " + nonZip + " zip " + zip + " total " + (nonZip+zip));
        
       
        initActions();
        initActions2();
        
        //File[] layouts = new File(html).listFiles();
    }
    private void initFilesGen(Path files, HashMap<AsciiString, AsciiString> vars) throws IOException {
         Files.walkFileTree(files, new FileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path uri = files.relativize(file);
                String fileName = uri.getName(uri.getNameCount()-1).toString();
                if(fileName.endsWith(".php")) {
                    vars.put(new AsciiString(fileName.substring(0, fileName.length()-4)),
                        new AsciiString(Files.readAllBytes(file), false));
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Log.err("WebFiles.init ", file, " ", exc);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            
        });
    }
    public void initFiles(String host, Path path, HashMap<AsciiString, AsciiString> vars) throws IOException {
        Path files = path.resolve("files");
        Files.walkFileTree(files, new FileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path uri = files.relativize(file);
                String fileName = uri.getName(uri.getNameCount()-1).toString();
                if(!fileName.endsWith("~")) {
                    String ext = ext(fileName);
                    WebFile w = new WebFile();
                    w.path = "/"+uri.toString();
                    boolean compress = true;
                    switch(ext) {
                        case "jpg":
                        case "jpeg": w.contentType = IMAGE_JPG; compress = false; break;
                        case "png":  w.contentType = IMAGE_PNG; compress = false; break;
                        case "gif":  w.contentType = IMAGE_GIF; compress = false; break;
                        case "ico":  w.contentType = IMAGE_ICO; break;
                        case "woff":w.contentType = FONT_WOFF; break;
                        case "woff2":w.contentType = FONT_WOFF2; break;
                        case "svg":w.contentType = SVG_UTF8; break;
                        case "js":   w.contentType = TEXT_JS_UTF8; break;
                        case "css":  w.contentType = TEXT_CSS_UTF8; break;
                        case "txt":  w.contentType = TEXT_PLAIN_UTF8; break;
                        case "glb":  w.contentType = HttpHeaderValues.APPLICATION_OCTET_STREAM; break;
                        default:
                            Log.err("WebFiles.unsupported extension ", file);
                            return FileVisitResult.CONTINUE;
                    }
                    byte[] data = Files.readAllBytes(file);
                    
                    if(ext.equals("js")) {
                        WebLayout l = new WebLayout(data);
                        data = l.create(vars);
//                      if(layoutInstant.isBefore(dataInstant)) {
//                          dataInstant = layoutInstant;
//                      }
                    }
                    
                    w.setLastModified(Files.getLastModifiedTime(file).toMillis());
                    w.content = createByteBuf(data);
                    w.length = data.length;
                    
                    if(compress) {
                        byte[] gzip = gzipCompress(data);
                        if(gzip.length < data.length) {
                            w.contentZip = createByteBuf(gzip);
                            w.lengthZip = gzip.length;
                        }
                    }
                    
                    webFiles.put(host+w.path, w);
                    
//                    byte[] d = gzipDecompress(gzip);
//                    System.out.println("d " + d.length);
//                    System.out.println(Arrays.equals(d, data));
                    
//                    ByteBuf buf = Unpooled.directBuffer(0, 0);
//                    buf.
//                    Unpooled.unreleasableBuffer(buf)
                    
                    
                    
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Log.err("WebFiles.init ", file, " ", exc);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            
        });
    }
    public void initHost() throws IOException {
        Path hostDir = conf.webpath.resolve("host");
        if(!Files.exists(hostDir)) return;
        File[] hosts = hostDir.toFile().listFiles();
        for(File host : hosts) {
            String hostName = host.getName();
            Path _path = host.toPath();
            
            HashMap<AsciiString, AsciiString> vars = new HashMap<>();
            initHtml(hostName, _path, vars);
            initFiles(hostName, _path, vars);
        }
    }
    public static int indexOf(byte[] arr, int from, int ch) {
        for(int i = from; i < arr.length; i++)
            if(arr[i] == ch) return i;
        return -1;
    }
    public static int indexOf(byte[] arr, int from, String ch) {
        loop:
        for(int i = from; i < arr.length; i++)
            if(arr[i] == ch.charAt(0)) {
                if(i+ch.length() >= arr.length) return -1;
                for(int k = 1; k < ch.length(); k++) {
                    if(arr[i+k] != ch.charAt(k)) continue loop;
                }
                return i;
            }
        return -1;
    }
    public AsciiString initVars(byte[] data, HashMap<AsciiString, AsciiString> vars) {
        return new AsciiString(data, 0, data.length, false);
    }
//    public static AsciiString initVars(byte[] data, HashMap<AsciiString, AsciiString> vars) {
//        if(data.length > 10 && data[0] == '~' && data[1] == '~' && data[2] == '~' && data[3] == '~' && data[4] == '~') {
//            int lineI = indexOf(data, 5, '\n');
//            if(lineI != -1) {
//                int f = lineI+1;
//                int e = indexOf(data, f, "~~~~~");
//                try {
//                    MiuList list = MiuModule.stage2List(new String(data, f, e, StandardCharsets.UTF_8), false);
//                    for(int i = 0; i < list.size(); i++) {
//                        if(list.isList(i)) {
//                            MiuList a = list.throwList(i);
//                            String name = a.throwSymbolString(0);
//                            String value = a.throwSymbolString(1);
//                            vars.put(new AsciiString(name), new AsciiString(value));
//                        }
//                    }
//                } catch (Throwable ex) {
//                    ex.printStackTrace();
//                }
//                int e2 = indexOf(data, e, '\n');
//                if(e2 == -1) e2 = e+5;
//                return new AsciiString(data, e2, data.length-e2, false);
//            }
//        }
//        return new AsciiString(data, 0, data.length, false);
//    }
    public void initHtml(String host, Path path, HashMap<AsciiString, AsciiString> def) throws IOException {
        Path html = path.resolve("html");
        File[] dirs = html.toFile().listFiles();
        for(File dir : dirs) {
            File layout = new File(dir, "layout.php");
            System.out.println("dir " + dir);
            WebLayout l;
            Instant layoutInstant;
            if(layout.exists()) {
                System.out.println("layout " + layout);
                l = new WebLayout(Files.readAllBytes(layout.toPath()));
                layoutInstant = Files.getLastModifiedTime(layout.toPath()).toInstant();
            }
            else { l = null; layoutInstant = null; }
            HashMap<AsciiString, AsciiString> vars = new HashMap<>();
            Path dirPath = dir.toPath();
            Files.walkFileTree(dirPath, new FileVisitor<Path>(){
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path uri = dirPath.relativize(file);
                    String fileName = uri.getName(uri.getNameCount()-1).toString();
                    if(!fileName.endsWith("~") && fileName.endsWith(".html")) {
                        byte[] data = Files.readAllBytes(file);
                        Instant dataInstant = Files.getLastModifiedTime(file).toInstant();

                        String uriStr = uri.toString();
                        if(uriStr.equals("index.html")) uriStr = "";
                        else uriStr = uriStr.substring(0, uriStr.length()-5);
                        
                        WebFile w = new WebFile();
                        w.path = "/"+uriStr;
                        
                        if(l != null) {                            
                            vars.clear();
                            vars.putAll(def);
                            vars.put(LAYOUT_BODY, initVars(data, vars));
                            if(!vars.containsKey(CANONICAL))
                                vars.put(CANONICAL, new AsciiString(conf.websitePath+(uriStr.isEmpty()?uriStr:w.path)));
                            else vars.put(CANONICAL, new AsciiString(conf.websitePath).concat((AsciiString)vars.get(CANONICAL)));
                            data = l.create(vars);
                            if(layoutInstant.isBefore(dataInstant)) {
                                dataInstant = layoutInstant;
                            }
                        }
                        
                        w.contentType = TEXT_HTML_UTF8;
                        w.setLastModified(dataInstant.toEpochMilli());
                        w.content = createByteBuf(data);
                        w.length = data.length;
                        
//                        if(compress) {
                            byte[] gzip = gzipCompress(data);
                            if(gzip.length < data.length) {
                                w.contentZip = createByteBuf(gzip);
                                w.lengthZip = gzip.length;
                            }
//                        }
                        webFiles.put(host+w.path, w);
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    public void initActions2() throws IOException {
        
    }
    public void initActions() {
       
      
       
       
    }
    
    
    public void get(String path, WebAction a) {
        WebFile w = webFiles.get(path);
        if(w == null) throw new NullPointerException(path);
        if(w.action != null) throw new IllegalArgumentException(path);
        w.action = a;
    }
    public void get0(String path, WebAction a) {
        WebFile w = webFiles.get(path);
        if(w != null) throw new IllegalArgumentException(path);
        w = new WebFile();
        w.path = path;
        w.action = a;
        webFiles.put(w.path, w);
    }
    public void post(String path, WebAction a) {
        WebFile w = webFiles.remove(path);
        if(w == null) throw new NullPointerException(path);
        if(w.action != null) throw new IllegalArgumentException(path);
        w.action = a;
        if(webPost.put(w.path, w) != null) throw new IllegalArgumentException(path);
    }
    public void post0(String path, WebAction a) {
        WebFile w = webFiles.get(path);
        if(w != null) throw new IllegalArgumentException(path);
        w = new WebFile();
        w.path = path;
        w.action = a;
        if(webPost.put(w.path, w) != null) throw new IllegalArgumentException(path);
    }
    private static ByteBuf createByteBuf(byte[] data) {
//        ByteBuf b = Unpooled.directBuffer(data.length, data.length);
//        b.writeBytes(data);
        ByteBuf b = Unpooled.wrappedBuffer(data);//directBuffer(data.length, data.length);
        return Unpooled.unreleasableBuffer(b);
    }
    private static byte[] gzipCompress(byte[] data) {
        byte[] r = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
            GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.close();
            r = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    private static byte[] gzipDecompress(byte[] gzip) {
        byte[] r = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(gzip);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPInputStream g = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = g.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            r = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }
    public Map<String,String> parseCookie(Object a, Object b) { return parseCookie(bridge.getHeader(a, b, COOKIE), ';');}
    public Map<String,String> parseQuery(Object a, Object b) { return parseCookie(bridge.getQuery(a, b), '&'); }
    public Map<String,String> parseBody(Object a, Object b) { return parseCookie(bridge.getContentString(a, b), '&'); }

    public Map<String,String> parseCookie(String cookie) { return parseCookie(cookie, ';');}
    public Map<String,String> parseQuery(String query) { return parseCookie(query, '&');}
    public static Map<String,String> parseCookie(String cookie, char s) {
        if(cookie != null) {
            HashMap<String, String> list = new HashMap<>();
            int f = 0;
            String key = null;
            for(var i = 0; i < cookie.length(); i++) {
                char ch = cookie.charAt(i);
                if(key == null) {
                    if(ch == '=') {
                        if(i-f > 0) {
                            key = cookie.substring(f, i).trim();
                            f = i+1;
                        }
                    }
                }
                else {
                    if(ch == s) {
                        var value = cookie.substring(f, i).trim();
                        list.put(key, value);
                        key = null;
                        f = i+1;
                    }
                }
            }

            if(key != null) {
                var value = cookie.substring(f, cookie.length()).trim();
                list.put(key, value);
                key = null;
            }
            return list;
        }
        return Collections.EMPTY_MAP;  
    }
    public static boolean isLoggedIn(Map<String,String> cookie) {
        return cookie.containsKey(ISESSSION) && "7".equals(cookie.get("_x"));
    }
    public static String cookieCache(Map<String,String> cookie) {
        return cookie.get(CACHE);
    }
    public static String ext(String filename) {
        int dot = filename.lastIndexOf('.');
        if(dot == -1) return "";
        return filename.substring(dot+1);
    }
    public static String str(JSONObject a) {
        return a.write(new StringWriter()).toString();
    }
}
