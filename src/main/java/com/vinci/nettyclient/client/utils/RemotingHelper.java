package com.vinci.nettyclient.client.utils;

import com.alibaba.fastjson.JSON;
import com.vinci.nettyclient.client.entity.RemotingCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class RemotingHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotingHelper.class);
    private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }

    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();

            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }

    public static RemotingCommand decode(final ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        byteBuffer.getShort();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        RemotingCommand responseIso = decodeOf(bytes, RemotingCommand.class);
        return responseIso;
    }

    public static byte[] encode(RemotingCommand request) {
        return encodeOf(request);
    }

    /*public static RemotingCommand decode (final ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        byteBuffer.getShort();
        RemotingCommand responseIso = new RemotingCommand();
        responseIso.setId(byteBuffer.getInt());
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        responseIso.setRemark(new String(bytes));

        return responseIso;
    }

    public static byte[] encode(RemotingCommand request) {
        String remark = request.getRemark();
        byte[] bytes = remark.getBytes();
        // vinci todo encode
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + bytes.length);
        byteBuffer.putInt(request.getId());
        byteBuffer.put(bytes, 0, bytes.length);
        byte[] result = new byte[4 + bytes.length];
        byteBuffer.rewind();
        byteBuffer.get(result);
        return result;
    }*/

    public static <T> T decodeOf(final byte[] data, Class<T> classOfT) {
        final String json = new String(data, CHARSET_UTF8);
        return fromJson(json, classOfT);
    }

    public static byte[] encodeOf(final Object obj) {
        final String json = toJson(obj, false);
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return JSON.parseObject(json, classOfT);
    }

    public static String toJson(final Object obj, boolean prettyFormat) {
        return JSON.toJSONString(obj, prettyFormat);
    }

    public static void closeChannel(Channel channel) {
        final String addrRemote = parseChannelRemoteAddr(channel);
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOGGER.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                        future.isSuccess());
            }
        });
    }

    private static byte[] prependLenBytes(byte[] data) {
        short len = (short) data.length;
        byte[] newBytes = new byte[len + 2];
        newBytes[0] = (byte) (len / 256);
        newBytes[1] = (byte) (len & 255);
        System.arraycopy(data, 0, newBytes, 2, len);
        return newBytes;
    }

}
