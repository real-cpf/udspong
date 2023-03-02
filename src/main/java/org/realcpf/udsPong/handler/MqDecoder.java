package org.realcpf.udsPong.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCountUtil;
import org.realcpf.udsPong.codec.*;
import org.realcpf.udsPong.node.NodeConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MqDecoder extends ByteToMessageDecoder {
  private final static Logger LOGGER = LoggerFactory.getLogger(MqDecoder.class);

  private void trimLine(ByteBuf in) {
    if (in.isReadable(2)) {
      in.readBytes(2);
    }
  }
  /*
  1 2types
  2 3 4 5 body len
  body


   */

  private final short GET_NUM = 1;
  private final short COMMAND_NUM = 2;
  private final short BYTE_VALUE_NUM = 3;
  private final short ROUTE_VALUE_NUM = 4;
  private final short REG_NUM = 5;
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    while (in.readableBytes() > 1) {
//      byte[] bytes = new byte[in.readableBytes()];
//      in.readBytes(bytes);
//      System.out.println(new String(bytes));

//      byte b1 = in.readByte();
//      byte b2 = in.readByte();
//      short start = 3;
      short start = in.readShort();
      if (0 == start){
        continue;
      }
      int bodyLen = in.readInt();
      ByteBuf dataBuf = in.readSlice(bodyLen);
      Message message = null;
      switch (start) {
        case GET_NUM ->{
          message = new StringMessage(dataBuf.toString(StandardCharsets.UTF_8));
        }
        case COMMAND_NUM ->{
          message = new CommandMessage(dataBuf.readByte());
        }
        case BYTE_VALUE_NUM ->{
          message = new ByteMessage(dataBuf);
        }
        case ROUTE_VALUE_NUM ->{
          tryRoutePlus(ctx, dataBuf);
        }
        case REG_NUM ->{
          String channelName = dataBuf.toString(StandardCharsets.UTF_8);
          NodeConf.getInstance().putOrRemoveChannel(channelName,ctx.channel());
          LOGGER.info("reg or remove channel name {}",channelName);
        }
      }
      trimLine(in);
      if (null != message) {
        out.add(message);
      }
    }

  }


  //  @Override
//  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
//    Types currType;
//    while (null != (currType = Types.from(in))) {
//      LOGGER.info("get type {}",currType);
//      Optional<Message> optionalMessage = translate(in, currType,ctx);
//      optionalMessage.ifPresent(out::add);
//      if (in.readableBytes() < 1) {
//        break;
//      }
//    }
//  }

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
      case COMMAND -> {
        return Optional.of(new CommandMessage(dataBuf.readByte()));
      }
      case BYTE_VALUE -> {
        return Optional.of(new ByteMessage(dataBuf));
      }
      case ROUTE_VALUE -> {
        final int index1 = dataBuf.forEachByte(ByteProcessor.FIND_ASCII_SPACE);
        if (0 > index1) {
          return Optional.empty();
        }
        tryRoute(ctx, dataBuf, index1);
        return Optional.empty();
      }
      case REG_C -> {
        String channelName = dataBuf.toString(StandardCharsets.UTF_8);
        NodeConf.getInstance().putOrRemoveChannel(channelName,ctx.channel());
        LOGGER.info("reg or remove channel name {}",channelName);
        return Optional.empty();
      }
      default -> {
        return Optional.empty();
      }
    }

  }

  private static void tryRoutePlus(ChannelHandlerContext ctx, ByteBuf dataBuf) {
    int index1 = dataBuf.forEachByte(ByteProcessor.FIND_ASCII_SPACE);
    ByteBuf routeByteBufKey = dataBuf.readSlice(index1);
    ByteBuf routeByteBufMsg = dataBuf.readSlice(dataBuf.readableBytes());

    String channelName = routeByteBufKey.toString(StandardCharsets.UTF_8);
    Optional<Channel> dstChannel = NodeConf.getInstance().getChannel(channelName);
    ByteBuf resBuf = ctx.alloc().buffer(4);
    if (dstChannel.isPresent()) {
      ReferenceCountUtil.retain(routeByteBufMsg);
      dstChannel.get().writeAndFlush(routeByteBufMsg);
      LOGGER.info("route to {} done",channelName);
      resBuf.writeCharSequence("R_D",StandardCharsets.UTF_8);
    } else {
      resBuf.writeCharSequence("N_F",StandardCharsets.UTF_8);
    }
    ctx.channel().writeAndFlush(resBuf);
  }

  private static void tryRoute(ChannelHandlerContext ctx, ByteBuf dataBuf, int index1) {
    ByteBuf routeByteBufKey = dataBuf.readSlice(index1);
    ByteBuf routeByteBufMsg = dataBuf.readSlice(dataBuf.readableBytes());

    String channelName = routeByteBufKey.toString(StandardCharsets.UTF_8);
    Optional<Channel> dstChannel = NodeConf.getInstance().getChannel(channelName);
    ByteBuf resBuf = ctx.alloc().buffer(4);
    if (dstChannel.isPresent()) {
      ReferenceCountUtil.retain(routeByteBufMsg);
      dstChannel.get().writeAndFlush(routeByteBufMsg);
      LOGGER.info("route to {} done",channelName);
      resBuf.writeCharSequence("R_D",StandardCharsets.UTF_8);
    } else {
      resBuf.writeCharSequence("N_F",StandardCharsets.UTF_8);
    }
    ctx.channel().writeAndFlush(resBuf);
  }


}
