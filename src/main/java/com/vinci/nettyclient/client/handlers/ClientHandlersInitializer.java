package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.ResponseFuture;
import com.vinci.nettyclient.client.UpNettyClient;
import com.vinci.nettyclient.client.entity.RemotingCommand;
import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;

public class ClientHandlersInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandlersInitializer.class);

    private static final int FRAME_MAX_LENGTH =
            Integer.parseInt(System.getProperty("com.opay.remoting.frameMaxLength", "16777216"));

    private ReconnectHandler reconnectHandler;


    private final ConcurrentHashMap<Integer, ResponseFuture> responseMap;

    private final UpNettyClient tcpClient;


    public ClientHandlersInitializer(UpNettyClient tcpClient) {
        Assert.notNull(tcpClient, "TcpClient can not be null.");
        this.tcpClient = tcpClient;
        this.reconnectHandler = new ReconnectHandler(tcpClient);
        this.responseMap = tcpClient.getResponseMatcherMap();
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(FRAME_MAX_LENGTH, 0, 2, 0, 0));
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new UpDecoder());
        pipeline.addLast(new UpEecoder());
        pipeline.addLast(new ReconnectHandler(this.tcpClient));
        pipeline.addLast(new HeartbeatHanlder(this.tcpClient));
        pipeline.addLast(new BizHandler());
    }

    class BizHandler extends SimpleChannelInboundHandler<RemotingCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            // todo vinci
            processResponseCommand(ctx, msg);
        }
        public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand responseIso) {

            final int opaque = responseIso.getOpaque();
            final ResponseFuture responseFuture = responseMap.get(opaque);
            if (responseFuture != null) {
                responseFuture.setResponseRemotingCommand(responseIso);
                responseMap.remove(opaque);

                if (responseFuture.getInvokeCallback() != null) {
                    // todo vinci for async send request
                } else {
                    responseFuture.putResponse(responseIso);
                    responseFuture.release();
                }
            } else {
                LOGGER.warn("ResponseFuture====为空");
                LOGGER.warn("receive response, but not matched any request, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                LOGGER.warn(responseIso.toString());
            }
        }
    }
}