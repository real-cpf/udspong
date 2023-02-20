package org.realcpf.udsPong.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.realcpf.udsPong.codec.ByteMessage;
import org.realcpf.udsPong.codec.KVMessage;
import org.realcpf.udsPong.codec.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ByteMessageAggregator extends MessageToMessageDecoder<Message> {
  private final Pop pop = new Pop();
  private final static Logger LOGGER = LoggerFactory.getLogger(ByteMessageAggregator.class);
  @Override
  protected void decode(ChannelHandlerContext ctx, Message msg, List<Object> out) {
    if (msg instanceof ByteMessage) {
      pop.store[pop.curr] = msg;
      pop.curr = pop.curr + 1;
      if (2 == pop.curr) {
        out.add(new KVMessage(pop.store[0], pop.store[1]));
        pop.curr = 0;
        pop.store = new Message[2];
      }
    } else {
      out.add(msg);
    }
  }

  private static class Pop {
    private int curr = 0;
    private Message[] store = new Message[2];
  }
}
