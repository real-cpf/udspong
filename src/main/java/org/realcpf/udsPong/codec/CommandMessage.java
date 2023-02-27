package org.realcpf.udsPong.codec;

public class CommandMessage implements Message{
  public Command getCommand() {
    return command;
  }

  private Command command;
  public CommandMessage(int code){
    command = Command.values()[code - 49];
  }
  @Override
  public long length() {
    return 0;
  }
}
