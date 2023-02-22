package org.realcpf.udsPong.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCountUtil;
import org.realcpf.udsPong.Main;
import org.realcpf.udsPong.codec.*;
import org.realcpf.udsPong.node.NodeConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MqDecoder extends ByteToMessageDecoder {
  private final static Logger LOGGER = LoggerFactory.getLogger(MqDecoder.class);

  private void trimLine(ByteBuf in) {
    if (in.isReadable(2)) {
      in.readBytes(2);
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    Types currType;
    while (null != (currType = Types.from(in))) {
      LOGGER.info("get type {}",currType);
      if (Types.SHUT_DOWN == currType) {
        LOGGER.info("shutdown {}", LocalDateTime.now());
        Main.exit();
      }
      Optional<Message> optionalMessage = translate(in, currType,ctx);
      optionalMessage.ifPresent(out::add);
      if (in.readableBytes() < 1) {
        break;
      }
    }
  }

  private Optional<Message> translate(ByteBuf in, Types types,ChannelHandlerContext ctx) {
    final int index = in.forEachByte(ByteProcessor.FIND_LF);
    if (0 > index) {
      return Optional.empty();
    }
    ByteBuf dataBuf = in.readSlice(index - in.readerIndex() - 1);
    trimLine(in);
    switch (types) {
      case GET_KEY -> {
        return Optional.of(new StringMessage(dataBuf.toString(StandardCharsets.UTF_8)));
      }
      case BYTE_VALUE -> {
        return Optional.of(new ByteMessage(dataBuf));
      }
      case ROUTE_VALUE -> {
        final int index1 = dataBuf.forEachByte(ByteProcessor.FIND_ASCII_SPACE);
        if (0 > index1) {
          return Optional.empty();
        }
        ByteBuf routeByteBufKey = dataBuf.readSlice(index1);
        ByteBuf routeByteBufMsg = dataBuf.readSlice(dataBuf.readableBytes());

        String channelName = routeByteBufKey.toString(StandardCharsets.UTF_8);
        Optional<Channel> dstChannel = NodeConf.getInstance().getChannel(channelName);
        dstChannel.ifPresent(c->{
          ReferenceCountUtil.retain(routeByteBufMsg);
          c.writeAndFlush(routeByteBufMsg);
        });
        LOGGER.info("route to {} done",channelName);
        ByteBuf resBuf = ctx.alloc().buffer(4);
        resBuf.writeCharSequence("R_D",StandardCharsets.UTF_8);
        ctx.channel().writeAndFlush(resBuf);
        return Optional.empty();
      }
      case REG_C -> {
        String channelName = dataBuf.toString(StandardCharsets.UTF_8);
        NodeConf.getInstance().putChannel(channelName,ctx.channel());
        LOGGER.info("reg channel name {}",channelName);
        return Optional.empty();
      }
      default -> {
        return Optional.empty();
      }
    }

  }


}
