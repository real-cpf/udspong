package route;

import org.realcpf.udsPong.api.NodeMsgOperator;

import java.util.concurrent.TimeUnit;

public class TestApi {

  public static void main(String[] args) throws InterruptedException {
    NodeMsgOperator operator = NodeMsgOperator.getInstance();

    operator.init("Channel2", msg -> System.out.println("Channel2:" + msg));
    System.out.println(".");
    operator.sendToChannel("Channel1","hello world! from Channel1");
    TimeUnit.SECONDS.sleep(10);
    operator.unReg();
    System.out.println("unreg");
  }
}
