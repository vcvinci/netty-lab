package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.RetryPolicy;
import com.vinci.nettyclient.client.NettyClient;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ReconnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectHandler.class);

    private volatile int retries = 0;
    private RetryPolicy retryPolicy;

    private NettyClient tcpClient;

    public ReconnectHandler(NettyClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Successfully established a connection to the server.");
        retries = 0;
        CompletableFuture.runAsync(() -> tcpClient.afterConnectInvoke());
        ctx.fireChannelActive();
    }



    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (retries == 0) {
            LOGGER.warn("Lost the TCP connection with the server.");
            ctx.close();
        }

        boolean allowRetry = getRetryPolicy().allowRetry(retries);
        if (allowRetry) {

            long sleepTimeMs = getRetryPolicy().getSleepTimeMs(retries);

            LOGGER.info("Try to reconnect to the server after {}ms. Retry count: {}.", sleepTimeMs, ++retries);
            final EventLoop eventLoop = ctx.channel().eventLoop();
            eventLoop.schedule(() -> {
                LOGGER.info("Reconnecting ...");
                tcpClient.connect();
            }, sleepTimeMs, TimeUnit.MILLISECONDS);
        }
        ctx.fireChannelInactive();
    }


    private RetryPolicy getRetryPolicy() {
        if (this.retryPolicy == null) {
            this.retryPolicy = tcpClient.getRetryPolicy();
        }
        return this.retryPolicy;
    }

}