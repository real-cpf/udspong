package route;

import org.realcpf.udsPong.api.NodeMsgOperator;

import java.util.concurrent.TimeUnit;

public class TestApi {

  public static void main(String[] args) throws InterruptedException {
    NodeMsgOperator operator = NodeMsgOperator.getInstance();

    operator.init("Channel4", msg -> System.out.println("Channel4:" + msg));
    System.out.println(".");
    operator.sendToChannel("Channel1","hello world! from channel4");
    TimeUnit.SECONDS.sleep(10);
    operator.unReg();
    System.out.println("unreg");
  }
}
