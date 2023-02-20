package org.realcpf.udsPong.codec;

import io.netty.buffer.ByteBuf;

public class ByteMessage implements Message{

  private final ByteBuf byteBuf;
  public ByteMessage(ByteBuf byteBuf){
    this.byteBuf = byteBuf;
    this.byteBuf.retain(1);
  }

  @Override
  public long length() {
    return 0;
  }

  public ByteBuf warp(){
    return this.byteBuf;
  }
}
