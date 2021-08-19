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
package simpleserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.AsciiString;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import simpleserver.stats.GStats;
import simpleserver.log.Log;
import simpleserver.web.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import simpleserver.db.data.DB;
import simpleserver.log.PrintFew;
import simpleserver.stats.*;
import simpleserver.web.WebFiles.WebDBBridge;
import simpleserver.web.WebFiles.WebFile;
import simpleserver.web.impl.netty.FullHttpRequestHandler;
import simpleserver.web.impl.netty.WebBridgeNetty;

public class TestServer {

    static boolean SSL = false;//System.getProperty("ssl") != null;
    static int PORT_HTTP = 8080;
    static int PORT = SSL?8443:8080;//Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));
    public static int MAX_CONTENT_LENGTH = 4096;

    static GlobalTrafficShapingHandler glBandwidth;
    static DefaultChannelGroup channels0 = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    static DefaultChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    public static Conf conf;
    public static WebFiles webfiles;
    public static FullHttpRequestHandler httpHandler;
    public static EventLoopGroup workerGroup;
    public static DB db;
    
    public static GStats stats;
    
    public static void reloadFiles() {
        if(webfiles != null) {
            workerGroup.execute(webfiles::reload);
        } 
    }
    
    public static ArrayList<AbstractAction> actions = null;
    
    static WebSocketServerInitializer initializer;
    public static WebSSL webssl;
    public static void main(String[] args) throws Exception {

        if(conf == null) {
            conf = new Conf();
            conf.path = new ConfFile(System.getProperty("user.home") + File.separatorChar + ".simpleserver");
        }
        WebLog.initLogs(conf);
        
        boolean tools = true;
        boolean createAccount = false;
        String webPath;
        {
            if(args != null) {
                tools = false;
                
                HashMap<String, String> map = new HashMap<>();
                for(int i = 0; i < args.length; i+=2) 
                    map.put(args[i], args[i+1]);
                
                SSL = true;
                PORT_HTTP = Integer.parseInt(map.getOrDefault("po", "8080"));
                PORT = Integer.parseInt(map.getOrDefault("ss", "8443"));
                webPath = map.getOrDefault("we", null);
                tools = map.getOrDefault("to", "false").equals("true");
                conf.websitePath = map.getOrDefault("do", "https://localhost:"+PORT);
                conf.debug = map.getOrDefault("debug", "true").equals("true");
                createAccount = map.getOrDefault("aa", "false").equals("true");           
                
            }
            else return;
        }
        
        //if(webPath == null) return;
        
        boolean isLocalhost = conf.websitePath.contains("localhost");
        
        if(isLocalhost) {
//            if(test) {
//                MAX_CONTENT_LENGTH = 4096*128;
//            }
            if(db == null) db = DB.createInMemoryDB();
            Log.log("Reference check");
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        }
        else {
//            if(test) return;
            if(db == null) db = DB.createFileDB(new File(conf.path.dir(".webdb"), "data.db"));
            Log.log("Reference check none");
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
        
        
//        db.addTableModels(models);
        
//        db.stats = stats;
        
//        AUtils.b = new WebLobbyBridgeNetty();
//        WebDB.init();
        WebDBBridge dbBridge = (a)->db.send(a);
                
        if(webfiles == null) webfiles = new WebFiles(conf);
        if(webPath != null) webfiles.conf.webpath = Paths.get(webPath);
        httpHandler = new FullHttpRequestHandler(webfiles);
        //Conf.websitePath = "http"+(SSL?"s":"")+"://"+  "localhost:"+PORT;
        
        Log.log("Path: " + conf.websitePath + ", debug " + conf.debug + " localhost " + isLocalhost);
       
        webfiles.bridge = new WebBridgeNetty();
        webfiles.db = dbBridge;
        webfiles.init();
        
        if(stats == null) stats = new GStats();
        webfiles.stats = stats;
        
//        System.out.println(SslProvider.isTlsv13Supported(SslProvider.JDK));
//        System.out.println(SslProvider.isTlsv13Supported(SslProvider.OPENSSL));
        

        //start http server
        Security.addProvider(new BouncyCastleProvider());
        webssl = new WebSSL(conf);
       
        
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);//new SingleThreadEventLoop//
//        EventLoopGroup workerGroup = new DefaultEventLoop();//new SingleThreadEventLoop//new NioEventLoopGroup();
//        EventLoopGroup workerGroup = new DefaultEventLoopGroup();//new SingleThreadEventLoop//new NioEventLoopGroup();
            
         db.msgExec = workerGroup;
         webfiles.exe = workerGroup;
//        glBandwidth = new GlobalTrafficShapingHandler(workerGroup, 1024>>2, 1024, 15000);
//        EpollServerSocketChannel d;

        try {
            if(createAccount) {
                if(SSL && !isLocalhost) {
                    webssl.init();
                    webssl.createAccount();
                    webssl.saveProps();
                }
                else Log.log("Not ssl or localhost ");
                return;
            }
            
            ServerBootstrap b0 = new ServerBootstrap();
            b0.group(bossGroup, workerGroup)
                .channel(XNioServerSocketChannel.class)
//                .channel(NioServerSocketChannel.class)
//                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new HttpInitializer());
            
            Channel ch0 = b0.bind(PORT_HTTP).sync().channel();
            
            // Configure SSL.
            final SslContext sslCtx;
            if (SSL) {
                if(isLocalhost) {
                    SelfSignedCertificate ssc = new SelfSignedCertificate();
                    sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).sslProvider(SslProvider.JDK).build();
                }
                else {
                    webssl.init();
                    webssl.updateIfNeeded();
                    sslCtx = SslContextBuilder.forServer(webssl.DOMAIN_CHAIN_FILE, webssl.DOMAIN_KEY_FILE).sslProvider(SslProvider.JDK).build();
                }
            } else {
                sslCtx = null;
            }
        
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(XNioServerSocketChannel.class)
//                .channel(NioServerSocketChannel.class)
//                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(initializer=new WebSocketServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();

            Thread shutdownThread = new Thread() {
                @Override
                public void run(){
                    db.close();
                    WebLog.closeLogs();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownThread);

            if(tools) {
                JFrame f = new JFrame("Tools");
                JToolBar bar = new JToolBar();
                bar.add(new AbstractAction("Reload") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        reloadFiles();
                    }
                });
                if(actions != null) 
                    for(AbstractAction a : actions)
                        bar.add(a);
                
//                bar.add(new AbstractAction("Backup") {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        WebDB.exec.execute(() -> {
//                            try {
//                                WebDB.backup();
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                            }
//                        });
//                    }
//                });
                
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setLayout(new BorderLayout());
                f.add(bar, BorderLayout.NORTH);

                f.pack();
                f.setVisible(true);
            }
            
            System.out.println("Open your web browser and navigate to " + conf.websitePath);

            ch0.closeFuture().sync();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        
        
    }
    
    public static class XNioServerSocketChannel extends NioServerSocketChannel {
        private static final InternalLogger logger = InternalLoggerFactory.getInstance(XNioServerSocketChannel.class);

        
        
        @Override
        protected int doReadMessages(List<Object> buf) throws Exception {
            java.nio.channels.SocketChannel ch = SocketUtils.accept(javaChannel());

            try {
                if (ch != null) {
                    SocketAddress a = ch.getRemoteAddress();
                    if(a instanceof InetSocketAddress) {
                        InetSocketAddress aa = (InetSocketAddress)a;
                        
                        InetAddress addr = aa.getAddress();
                        
                        if(addr != null) {
                            IStats s = stats.get(addr);
                            if(s.canConnect() && s.acceptConnection.rede()) {
//                                Log.log("Connect ", Thread.currentThread());
                                //Log.log("Connect", addr);
                                XNioSocketChannel chan = new XNioSocketChannel(this, ch);
                                chan.istats = s;
                                chan.stats = s.connect(chan, workerGroup);
                                chan.closeFuture().addListener((e)->{
                                    workerGroup.execute(()->{
//                                        Log.log("Disconnect ", Thread.currentThread());
                                        s.disconnect(chan.stats);
                                    });
                                });
                                buf.add(chan);
                                return 1;
                            }
                            else {
                                if(s.acceptConnection.li == 100) {
                                    //100 attempts... hmm...
                                    Log.log("IP " + addr + " reached connection limit 100 times.");
                                }
                            }
                        }
                    }
                    
                    ch.close();
                    return 0;
                }
            } catch (Throwable t) {
                logger.warn("Failed to create a new channel from an accepted socket.", t);

                try {
                    ch.close();
                } catch (Throwable t2) {
                    logger.warn("Failed to close a socket.", t2);
                }
            }
            return 0;
        }
        
    }
    public static class XNioSocketChannel extends NioSocketChannel {
        private static final AtomicInteger idGen = new AtomicInteger();
        public int chanId = 0;
        public IStats istats;
        public SStats stats;
        private XNioSocketChannel(XNioServerSocketChannel aThis, java.nio.channels.SocketChannel ch) {
            super(aThis, ch);
            chanId = idGen.getAndIncrement();
            //System.out.println("xNIO ");
        }

        @Override
        protected int doReadBytes(ByteBuf byteBuf) throws Exception {
            WebStats ss = stats.stats;
            int maxRe = ss.re();
            
            //System.out.println("read " + System.currentTimeMillis() + "          " + maxRe);//negative?

            if(maxRe < 1280) { return 0; }

            int wI = byteBuf.writerIndex();
            
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
            allocHandle.attemptedBytesRead(byteBuf.writableBytes());
            int r = byteBuf.writeBytes(javaChannel(), allocHandle.attemptedBytesRead()); 
            
//            System.out.println("");
//            System.err.println("~~~~~~~~~~~~~READ " + r);
//            int wI2 = byteBuf.writerIndex();
//            for(int i = wI; i < wI2; i++) {
//                byte b = byteBuf.getByte(i);
//                System.err.print((char)b);
//            }
//            System.err.println("\n~~~~~~~~~~~~~");
            
            ss.re(r);
            return r;
            
//            return super.doReadBytes(byteBuf); //To change body of generated methods, choose Tools | Templates.
            
        }
        
        

        @Override
        protected int doWriteBytes(ByteBuf buf) throws Exception {
            //System.out.println("do write byte buf");
            final int expectedWrittenBytes = buf.readableBytes();
            int wrote = buf.readBytes(javaChannel(), expectedWrittenBytes);
            //System.out.println("write " + wrote + "/" + expectedWrittenBytes);
            return wrote;
        }

        @Override
        protected long doWriteFileRegion(FileRegion region) throws Exception {
            //System.out.println("do write file region");
            return super.doWriteFileRegion(region); //To change body of generated methods, choose Tools | Templates.
        }
                
//        @Override
//        protected void doWrite(ChannelOutboundBuffer in) throws Exception {
//            System.out.println("do write " + System.currentTimeMillis());
//            super.doWrite(in);
//        }
        @Override
        protected void doWrite(ChannelOutboundBuffer in) throws Exception {
//            super.doWrite(in);
            WebStats ss = stats.stats;
            int maxSend = ss.se();
            if(maxSend < 1280) { incompleteWrite(true); return; }
            
            java.nio.channels.SocketChannel ch = javaChannel();
            int writeSpinCount = config().getWriteSpinCount();
            do {
                //Log.log("Write spin ", writeSpinCount);
                if (in.isEmpty()) {
                    // All written so clear OP_WRITE
                    clearOpWrite();
                    // Directly return here so incompleteWrite(...) is not called.
                    return;
                }

                // Ensure the pending writes are made of ByteBufs only.
//                int maxBytesPerGatheringWrite = ((NioSocketChannel.NioSocketChannelConfig) config).getMaxBytesPerGatheringWrite();
                int maxBytesPerGatheringWrite = maxSend;
                ByteBuffer[] nioBuffers = in.nioBuffers(1024, maxBytesPerGatheringWrite);
                int nioBufferCount = in.nioBufferCount();
                
                //System.out.println("count " + nioBufferCount + ", " + maxSend);
                
                if(nioBufferCount == 0) {
                    //Log.err("count 0 ");
                    return;
                }
                int wroteTotal = 0;
                try {
                    for(int i = 0; i < nioBufferCount; i++) {
                        //maxSend
                        ByteBuffer b = nioBuffers[i];
                        int r = b.remaining();
                        //System.out.println(chanId + ": rem " + r);
                        if(r <= 0) continue;
                        int w;
                        if(r > maxSend) {
                            int saveLimit = b.limit();
                            try {
                                b.limit(b.position()+maxSend);
                                w = ch.write(b);
                                //System.out.println("w " + w);
                                if(w > 0) {
                                    ss.se(w);
                                    maxSend -= w;
                                    wroteTotal += w;
                                }
                                incompleteWrite(true);
                                return;
                            }
                            finally {
                                b.limit(saveLimit);
                            }
                        }
                        else {
//                            System.out.println("");
//                            System.err.println("~~~~~~~~~~~~~WRITE " + r);
//                            int wI2 = b.limit();
//                            for(int j = b.position(); j < wI2; j++) {
//                                byte bb = b.get(j);
//                                System.err.print((char)bb);
//                            }
//                            System.err.println("\n~~~~~~~~~~~~~");
                            
                            w = ch.write(b);
                            //System.out.println("W " + w + " " + b.remaining());
                            if (w <= 0) {
                                incompleteWrite(true);
                                return;
                            }
                            ss.se(w);
                            maxSend -= w;
                            wroteTotal += w;
                            if(b.remaining() > 0) {
                                incompleteWrite(true);
                                return;
                            }
                        }
                    }
                    incompleteWrite(false);
                    return;
                } 
                finally {
                    if(wroteTotal > 0) in.removeBytes(wroteTotal);
                }

            } while (writeSpinCount > 0);

            //incompleteWrite(writeSpinCount < 0);
        }

        @Override
        public ChannelFuture write(Object msg) {
            //System.out.println("write");
            return super.write(msg);
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            //System.out.println("write2");
            return super.write(msg, promise); 
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            //System.out.println("writeFlush");
            return super.writeAndFlush(msg); 
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            //System.out.println("writeFlush2");
            return super.writeAndFlush(msg, promise);
        }
        
        
    }
    
    
    @ChannelHandler.Sharable
    public static class MyFilterHandler extends AbstractRemoteAddressFilter {
        public MyFilterHandler() {
            //System.out.println("NEW MyFilterHandler");
        }
        
        @Override
        protected boolean accept(ChannelHandlerContext ctx, SocketAddress remoteAddress) throws Exception {
            if(remoteAddress instanceof InetSocketAddress) {
                InetSocketAddress addrPort = (InetSocketAddress)remoteAddress;
                //addr.getAddress().getAddress();
                InetAddress addr = addrPort.getAddress();
                if(addr instanceof Inet4Address) {
                    Inet4Address addr4 = (Inet4Address)addr;
                    //addr4.
                }
                //if(addr instanceof )
            }
            //System.out.println("CHECK " + remoteAddress + " " + remoteAddress.getClass());
            return true;
        }   
    }

    public static class HttpInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            channels0.add(ch);

            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(4096));
            pipeline.addLast(new HttpRedirectHandler());
           
        }
    }
    public static class HttpRedirectHandler extends ChannelInboundHandlerAdapter {
        public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
        public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//            System.out.println("THREAD " + Thread.currentThread());
//            System.out.println("channel " + ctx + " " + ctx.getClass());
//            System.out.println("read " + msg);
//            ((SocketChannel)ctx.channel()).config().setTcpNoDelay(true);
            //ctx.channel().config().setOption(ChannelOption.TCP_NODELAY, true);
            //System.out.println("read ");
            if(msg instanceof FullHttpRequest) {
                IStats istats = ((XNioSocketChannel)ctx.channel()).istats;
//                SStats stats = ((XNioSocketChannel)ctx.channel()).stats;
                
                FullHttpRequest req = (FullHttpRequest) msg;
                boolean keepAlive = HttpUtil.isKeepAlive(req);
                
                //System.out.println(req);
                //System.out.println();
//                System.out.println("headers " + ((HttpRequest) msg).headers());
                
                if(HttpUtil.is100ContinueExpected(req)) {
                    ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
                }
                
                ResourceStats res = req.method() == HttpMethod.POST?istats.apiRequest:istats.httpRequest;
                if(!res.rede()) {
                    if(res.li == 100) {
                        //100 attempts... hmm...
                        Log.log("IP " + istats.addr + " http/api request limit 100 times.");                        
                    }
                    
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.TOO_MANY_REQUESTS);
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
                    response.headers().set(HttpHeaderNames.RETRY_AFTER, "10000");
                    writeKeepAlive(ctx, response, keepAlive);
                    return;
                }
                
                String uriQuery = req.uri();
                int question = uriQuery.indexOf('?');
                String uri/*, query*/;
                if(question == -1) {
                    uri = uriQuery; /*query = null*/;
                }
                else {
                    uri = uriQuery.substring(0, question);
//                    query = uriQuery.substring(question+1);
                }
                if(req.method() == HttpMethod.GET) {
                    String s = "/.well-known/acme-challenge/";
                    if(uri.startsWith(s)) {
                        String f = webssl.getHttpChallengeFile();
                        String d = webssl.getHttpChallengeContent();
                        if(f != null && d != null && uri.substring(s.length()).equals(f)) {
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(d, StandardCharsets.UTF_8));
                            response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                            writeKeepAlive(ctx, response, keepAlive);
                            return;
                        }
                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND);
                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                        writeKeepAlive(ctx, response, keepAlive);
                        return;
                    }
                    
//                    WebFile w = webfiles.webFiles.get(uri);
//                    if(w != null) {
//                        
//                        if(w.action != null) {
//                            w = w.action.action(w, ctx, req);
//                            if(w == null) return;
//                        }
//                        
//                        sendWebFile(ctx, w, req);
//                        return;
//                    }
                }
                else if(req.method() == HttpMethod.POST) {
//                    WebFile a = webfiles.webPost.get(uri);
//                    if(a != null && a.action != null) {
//                        WebFile w = a.action.action(a, ctx, req);
//                        if(w != null) {
//                            sendWebFile(ctx, w, req);
//                        }
//                        return;
//                    }
                }
                else {                    
                    //System.out.println("------------------------------");
                    Log.log("METHOD " + req.method());
                    //System.out.println("------------------------------");
                }
                
                
                
                
                if(req.method() == HttpMethod.GET || req.method() == HttpMethod.HEAD) {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);
                    response.headers().set(HttpHeaderNames.LOCATION, conf.websitePath+uriQuery);
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                    writeKeepAlive(ctx, response, keepAlive);
                }
                else if(req.method() == HttpMethod.POST) {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                    writeKeepAlive(ctx, response, keepAlive);
                } 
                else {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                    writeKeepAlive(ctx, response, keepAlive);
                }
                
                
//                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(content));
//                response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
//                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
//                
//
//                
//                //just simple requests...
//                //static files
//                
//                //some post requests 
//                
                
                
            }
//            else if(msg instanceof BinaryWebSocketFrame) {
//                ws.channelRead0(ctx, (WebSocketFrame)msg);
//            }
            else {
                Log.log("MSG IS " + msg);
                
            }
        }
        public static void writeKeepAlive(ChannelHandlerContext ctx, DefaultFullHttpResponse response, boolean keepAlive) {
            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.write(response);
            }
        }
        public static DefaultFullHttpResponse prepareSendWebFile(ChannelHandlerContext ctx, WebFile w, HttpRequest req) {
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            return prepareSendWebFile(ctx, w, req, keepAlive);
        }
        public static DefaultFullHttpResponse prepareSendWebFile(ChannelHandlerContext ctx, WebFile w, HttpRequest req, boolean keepAlive) {
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
        public static void sendWebFile(ChannelHandlerContext ctx, WebFile w, HttpRequest req) {
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            DefaultFullHttpResponse response = prepareSendWebFile(ctx, w, req, keepAlive);
            writeKeepAlive(ctx, response, keepAlive);
        }
        public static boolean useGZip(WebFile w, HttpRequest req) {
            if(w.contentZip != null) {
                String contentEncoding = req.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
                if(contentEncoding != null && contentEncoding.contains("gzip")) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(cause instanceof DecoderException) {
                PrintFew.out(PrintFew.DECODER_EXCEPTION);
            }
            else {
                cause.printStackTrace();
            }
            ctx.close();
            //super.exceptionCaught(ctx, cause);
            //cause.printStackTrace();
            //ctx.close();
        }
    }
    public static class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
  
      private static final String WEBSOCKET_PATH = "/websocket";
  
      private final SslContext sslCtx;
  
      public WebSocketServerInitializer(SslContext sslCtx) {
          this.sslCtx = sslCtx;
      }
  
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
            channels.add(ch);
            ChannelPipeline pipeline = ch.pipeline();
          
            if(sslCtx != null) { pipeline.addLast(sslCtx.newHandler(ch.alloc())); }
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
            pipeline.addLast(new HttpSimpleServerHandler(httpHandler));
     }
  }
    
    public static class HttpSimpleServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
//        public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
//        public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        
        public FullHttpRequestHandler handler;
        public HttpSimpleServerHandler(FullHttpRequestHandler handler) {
            this.handler = handler;
        }
        

//        @Override
//        public void channelReadComplete(ChannelHandlerContext ctx) {
//            super.channelReadComplete(ctx);
//            ctx.flush();
//        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
//            System.out.println("THREAD " + Thread.currentThread());
//            System.out.println("channel " + ctx + " " + ctx.getClass());
//            System.out.println("read " + msg);
//            ((SocketChannel)ctx.channel()).config().setTcpNoDelay(true);
            //ctx.channel().config().setOption(ChannelOption.TCP_NODELAY, true);
            //System.out.println("read ");
            if(msg instanceof FullHttpRequest) {
                FullHttpRequest req = msg;
                HttpHeaders h = req.headers();
                String host = h.get(HttpHeaderNames.HOST);
                
                handler.handle(ctx, req);
            }
//            else if(msg instanceof BinaryWebSocketFrame) {
////                ws.channelRead0(ctx, (WebSocketFrame)msg);
//                Log.log("B Frame");
//            }
            else {
                Log.log("MSG IS " + msg);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if(cause instanceof DecoderException) {
                PrintFew.out(PrintFew.DECODER_EXCEPTION);
            }
            else {
                cause.printStackTrace();
            }
            ctx.close();
        }
    }
    
    
   
}
