package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.UpNettyClient;
import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatHanlder extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatHanlder.class);

    final AtomicInteger failedAttemptCount = new AtomicInteger(0);

    private UpNettyClient tcpClient;

    public HeartbeatHanlder(UpNettyClient client) {
        this.tcpClient = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ping(ctx.channel());
    }

    private void ping(Channel channel) {
        if (failedAttemptCount.intValue() >= 5) {
            RemotingHelper.closeChannel(channel);
        }
        ScheduledFuture<?> future = channel.eventLoop().schedule(() -> {
            if (channel.isActive()) {
                LOGGER.info("sending heart beat to the server...");
                try {
                    // vinci todo
                    /*if (!tcpClient.sendHeartbeat()) {
                        failedAttemptCount.incrementAndGet();
                    }*/
                } catch (Exception e) {
                    throw new RuntimeException("0 Heartbeat failed!");
                }
            } else {
                LOGGER.warn("The connection had broken, cancel the task that will send a heart beat.");
                RemotingHelper.closeChannel(channel);
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
        ctx.close();
    }
}
