package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.NettyClient;
import com.vinci.nettyclient.client.exception.RemotingConnectException;
import com.vinci.nettyclient.client.exception.RemotingSendRequestException;
import com.vinci.nettyclient.client.exception.RemotingTimeoutException;
import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ChannelHandler.Sharable
public class HeartbeatHanlder extends ChannelInboundHandlerAdapter {

    public static final int MAX_HEARTBEAT_TIMES = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatHanlder.class);

    final AtomicInteger failedAttemptCount = new AtomicInteger(0);

    private NettyClient tcpClient;


    public HeartbeatHanlder(NettyClient client) {
        this.tcpClient = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ping(ctx.channel());
    }

    private void ping(Channel channel) {
        if (failedAttemptCount.intValue() >= MAX_HEARTBEAT_TIMES) {
            RemotingHelper.closeChannel(channel);
        }
        ScheduledFuture<?> future = channel.eventLoop().schedule(() -> {
            if (channel.isActive()) {
                LOGGER.info("sending heart beat to the server...");
                CompletableFuture.runAsync(() -> {
                    try {
                        tcpClient.sendHeartBeat();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.warn("Send heart beat thread is interrupted.", e);
                    } catch (RemotingTimeoutException | RemotingSendRequestException | RemotingConnectException e) {
                        RemotingHelper.closeChannel(channel);
                        LOGGER.warn("Send heart beat request is failed", e);
                    }
                });
            } else {
                LOGGER.warn("The connection had broken, cancel the task that will send a heart beat.");
                channel.closeFuture();
                throw new RuntimeException("1 Heartbeat failed!");
            }
        }, 60, TimeUnit.SECONDS);

        future.addListener((GenericFutureListener) innerFuture -> {
            if (innerFuture.isSuccess()) {
                ping(channel);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当Channel已经断开的情况下, 仍然发送数据, 会抛异常, 该方法会被调用.
        cause.printStackTrace();
        LOGGER.warn("The channel already broken.");
        ctx.close();
    }
}
