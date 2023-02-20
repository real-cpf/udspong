package org.realcpf.udsPong.codec;

public class StringMessage implements Message{
  private final String value;
  public StringMessage(String value) {
    this.value = value;
  }
  @Override
  public long length() {
    return 0;
  }


  public String warp() {
    return value;
  }
}
