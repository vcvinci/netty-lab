package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.ResponseFuture;
import com.vinci.nettyclient.client.NettyClient;
import com.vinci.nettyclient.client.entity.RemotingCommand;
import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;

public class ClientHandlersInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandlersInitializer.class);

    private static final int FRAME_MAX_LENGTH =
            Integer.parseInt(System.getProperty("com.opay.remoting.frameMaxLength", "16777216"));

    private final ReconnectHandler reconnectHandler;

    private final HeartbeatHanlder heartbeatHanlder;

    private final ConcurrentHashMap<Integer, ResponseFuture> responseMap;

    public ClientHandlersInitializer(NettyClient tcpClient) {
        Assert.notNull(tcpClient, "TcpClient can not be null.");
        this.reconnectHandler = new ReconnectHandler(tcpClient);
        this.responseMap = tcpClient.getResponseMatcherMap();
        this.heartbeatHanlder = new HeartbeatHanlder(tcpClient);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(this.reconnectHandler);
        pipeline.addLast(new LengthFieldPrepender(2));
        pipeline.addLast(new Decoder());
        pipeline.addLast(new Eecoder());
        pipeline.addLast(this.heartbeatHanlder);
//        pipeline.addLast(new LengthFieldBasedFrameDecoder(FRAME_MAX_LENGTH, 0, 2, 0, 0));
        pipeline.addLast(new BizHandler());
    }

    class BizHandler extends SimpleChannelInboundHandler<RemotingCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            // todo vinci
            processResponseCommand(ctx, msg);
        }
        public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand responseIso) {
            final RemotingCommand cmd = responseIso;
            final int opaque = cmd.getOpaque();
            final ResponseFuture responseFuture = responseMap.get(opaque);
            if (responseFuture != null) {
                responseFuture.setResponseRemotingCommand(cmd);
                responseMap.remove(opaque);

                if (responseFuture.getInvokeCallback() != null) {
                    // todo vinci for async send request
                } else {
                    responseFuture.putResponse(cmd);
                    responseFuture.release();
                }
            } else {
                LOGGER.warn("ResponseFuture====为空");
                LOGGER.warn("receive response, but not matched any request, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                LOGGER.warn(cmd.toString());
            }
        }
    }
}