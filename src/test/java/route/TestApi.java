package route;

import org.realcpf.udsPong.api.NodeMsgOperator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestApi {

  public static void main(String[] args) throws InterruptedException, IOException {
    NodeMsgOperator operator = NodeMsgOperator.getInstance();

    operator.init("c2", msg -> System.out.println("Channel1:" + msg));
    System.out.println(".");
//    System.in.read();
    operator.sendToChannel("c1","yoyoyo world!");
//    TimeUnit.SECONDS.sleep(10);
//    operator.unReg();
    System.out.println("unreg");
  }
}
