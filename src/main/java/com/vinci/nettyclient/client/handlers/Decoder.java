package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class Decoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Decoder.class);

    private static final int FRAME_MAX_LENGTH =
            Integer.parseInt(System.getProperty("com.opay.remoting.frameMaxLength", "16777216"));

    public Decoder() {
        super(FRAME_MAX_LENGTH, 0, 2, 0, 0);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }

            ByteBuffer byteBuffer = frame.nioBuffer();

            return RemotingHelper.decode(byteBuffer);
        } catch (Exception e) {
            String addrRemote = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            LOGGER.error("decode exception {}", addrRemote, e);
            ctx.channel().close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    LOGGER.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                            future.isSuccess());
                }
            });
        } finally {
            if (null != frame) {
                frame.release();
            }
        }

        return null;
    }
}
