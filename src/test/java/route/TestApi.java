package route;

import org.realcpf.udsPong.api.NodeMsgOperator;

import java.util.concurrent.TimeUnit;

public class TestApi {

  public static void main(String[] args) throws InterruptedException {
    NodeMsgOperator operator = NodeMsgOperator.getInstance();

    operator.init("Channel1", msg -> System.out.println("Channel1:" + msg));
    System.out.println(".");
    operator.sendToChannel("Channel2","hello world! from Channel2");
//    TimeUnit.SECONDS.sleep(10);
//    operator.unReg();
    System.out.println("unreg");
  }
}
