package com.vinci.nettyclient.client.handlers;

import com.vinci.nettyclient.client.entity.RemotingCommand;
import com.vinci.nettyclient.client.utils.RemotingHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Eecoder extends MessageToByteEncoder<RemotingCommand> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Eecoder.class);

    @Override
    public void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out)
            throws Exception {
        try {
            byte[] msgBytes = RemotingHelper.encode(remotingCommand);
            out.writeBytes(msgBytes);
        } catch (Exception e) {
            LOGGER.error("encode exception, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
            if (remotingCommand != null) {
                LOGGER.error(remotingCommand.toString());
            }
            RemotingHelper.closeChannel(ctx.channel());
        }
    }
}
