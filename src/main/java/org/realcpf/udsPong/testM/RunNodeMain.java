package org.realcpf.udsPong.testM;

import org.realcpf.udsPong.api.NodeMsgOperator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunNodeMain {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("need channel name");
      return;
    }
    String channelName = args[0].trim();

    NodeMsgOperator operator = NodeMsgOperator.getInstance();

    operator.init(channelName,msg -> {
      System.out.printf("[%s]:%s%n",channelName,msg);
    });

    System.err.println("start node " + channelName);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    System.err.println("please input [channel msg]");
    for (;;){
      String line = in.readLine();
      if (null == line || "quit".equalsIgnoreCase(line)) {
        if ("quit".equalsIgnoreCase(line)){
          operator.closeConnection().addListener(future -> {
            if (future.isDone()){
              operator.close();
            }
          });
        }
        break;
      }
      String[] ss = line.split(" ");
      if (ss.length != 2){
        System.err.println("need channelName and Msg");
        continue;
      }
      operator.sendToChannel(ss[0].trim(),ss[1].trim());
    }
  }
}
