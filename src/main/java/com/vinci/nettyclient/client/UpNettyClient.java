package com.vinci.nettyclient.client;

import com.vinci.nettyclient.client.entity.RemotingCommand;
import com.vinci.nettyclient.client.exception.RemotingConnectException;
import com.vinci.nettyclient.client.exception.RemotingSendRequestException;
import com.vinci.nettyclient.client.exception.RemotingTimeoutException;
import com.vinci.nettyclient.client.handlers.ClientHandlersInitializer;
import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * @author wangandong
 */

@Component
public class UpNettyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpNettyClient.class);

    private static final String REDIS_KEY = "CHANNEL_UP_POS_SESSION_KEY";

    @Value("${up.connection.host:127.0.0.1}")
    private String host;
    @Value("${up.connection.port:9999}")
    private int port;

    private Bootstrap bootstrap;
    /** 重连策略 */
    private RetryPolicy retryPolicy;

    /** 将<code>Channel</code>保存起来, 可用于在其他非handler的地方发送数据 */
    private Channel channel;

    private final ConcurrentHashMap<Integer, ResponseFuture> responseMatcherMap = new ConcurrentHashMap<>(256);

    @Autowired
    public UpNettyClient() {
        this(new ExponentialBackOffRetry(1000, Integer.MAX_VALUE, 60 * 1000));
    }

    public UpNettyClient(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        config();
    }

    @PostConstruct
    public void init() {
        connect();
    }

    public RemotingCommand invokeSync0(final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {

        if (channel != null && channel.isActive()) {
            final SocketAddress addr = channel.remoteAddress();
            int opaque = request.getOpaque();
            try {
                final ResponseFuture responseFuture = new ResponseFuture(this.channel, opaque, timeoutMillis, null, null);
                this.responseMatcherMap.put(opaque, responseFuture);

                channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        responseFuture.setSendRequestOK(true);
                        return;
                    } else {
                        responseFuture.setSendRequestOK(false);
                    }

                    this.responseMatcherMap.remove(opaque);
                    responseFuture.setCause(f.cause());
                    responseFuture.putResponse(null);
                    LOGGER.warn("send a request command to channel <{}> failed.", addr);
                });

                RemotingCommand responseRemotingCommand = responseFuture.waitResponse(timeoutMillis);
                if (null == responseRemotingCommand) {
                    if (responseFuture.isSendRequestOK()) {
                        /*if (nettyClientConfig.isClientCloseSocketIfTimeout()) {
                            this.closeChannel(addr, channel);
                            log.warn("invokeSync: close socket because of timeout, {}ms, {}", timeoutMillis, addr);
                        }*/
                        // vinci todo whether close channel
                        LOGGER.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
                        throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr), timeoutMillis,
                                responseFuture.getCause());
                    } else {
                        LOGGER.warn("invokeSync: send request exception, so close the channel[{}]", addr);
                        RemotingHelper.closeChannel(channel);
                        throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
                    }
                }

                return responseRemotingCommand;
            } finally {
                this.responseMatcherMap.remove(opaque);
            }
        } else {
            RemotingHelper.closeChannel(channel);
            throw new RemotingConnectException(format("addr:%s:%s", host, port));
        }
    }

    /**
     * 向远程TCP服务器请求连接
     */
    public void connect() {
        synchronized (bootstrap) {
            LOGGER.info("Prepare to connect {}:{} .", host, port);
            ChannelFuture future = bootstrap.connect(host, port);
            LOGGER.info("Connected, {}:{} .", host, port);
            future.addListener(getConnectionListener());
            this.channel = future.channel();
        }
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    private void config() {
        EventLoopGroup group = new NioEventLoopGroup();
        // bootstrap 可重用, 只需在TcpClient实例化的时候初始化即可.
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ClientHandlersInitializer(UpNettyClient.this));
    }

    private ChannelFutureListener getConnectionListener() {
        return future -> {
            if (!future.isSuccess()) {
                future.channel().pipeline().fireChannelInactive();
            }
        };
    }


    public static void main(String[] args) {
        UpNettyClient tcpClient = new UpNettyClient();
        tcpClient.connect();
    }

    public ConcurrentHashMap<Integer, ResponseFuture> getResponseMatcherMap() {
        return responseMatcherMap;
    }
}
