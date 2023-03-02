package kv;

import org.realcpf.udsPong.api.NodeMsgOperator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class TestKVMessage {
  public static void main(String[] args) throws IOException, InterruptedException {
    String channelName = "c1";

    NodeMsgOperator operator = NodeMsgOperator.getInstance();

    operator.init(channelName, System.out::println);

    operator.sendMessage("aaa","bbb");
//    IntStream.range(1,10).forEach(e->{
//      operator.sendMessage("k"+e,"v"+e);
//    });

    TimeUnit.SECONDS.sleep(10);

    operator.close();

  }
}
