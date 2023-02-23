package org.realcpf.udsPong.node;


import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public final class NodeConf {
  private NodeConf() {
  }

  private final static Map<String, Channel> channelMap = new ConcurrentSkipListMap<>();

  public void putOrRemoveChannel(String name, Channel channel) {
    if (channelMap.containsKey(name)) {
      channelMap.remove(name);
      channel.writeAndFlush(Unpooled.copiedBuffer("REMOVE_DONE".getBytes(StandardCharsets.UTF_8)));
    } else {
      channelMap.put(name, channel);
      channel.writeAndFlush(Unpooled.copiedBuffer("REG_DONE".getBytes(StandardCharsets.UTF_8)));
    }
  }

  public Optional<Channel> getChannel(String name) {
    if (channelMap.containsKey(name)) {
      return Optional.of(channelMap.get(name));
    }
    return Optional.empty();
  }

  public static NodeConf getInstance() {
    return NodeConfInner.INSTANCE;
  }

  private static class NodeConfInner {
    static NodeConf INSTANCE = new NodeConf();
  }
}
