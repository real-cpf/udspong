package org.realcpf.udsPong.codec;

public final class KVMessage implements Message{

  public Message getKey() {
    return key;
  }

  public Message getValue() {
    return value;
  }

  private final Message key;
  private final Message value;
  public KVMessage(Message key,Message value) {
    this.key = key;
    this.value = value;
  }


  @Override
  public long length() {
    return 0;
  }
}
