package org.realcpf.udsPong.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import org.realcpf.udsPong.Main;
import org.realcpf.udsPong.codec.ByteMessage;
import org.realcpf.udsPong.codec.Message;
import org.realcpf.udsPong.codec.StringMessage;
import org.realcpf.udsPong.codec.Types;
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
      Optional<Message> optionalMessage = translate(in, currType);
      optionalMessage.ifPresent(out::add);
      if (in.readableBytes() < 1) {
        break;
      }
    }
  }

  private Optional<Message> translate(ByteBuf in, Types types) {
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
      default -> {
        return Optional.empty();
      }
    }

  }


}