package xyz.vopen.framework.chaos.core.test.rpc.api;

public class DemoApiImpl implements DemoApi {
  @Override
  public String test(String name) {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println(name);
    return "hello " + name;
  }

  @Override
  public int sum(int a, int b) {
    System.out.println(a + " " + b);
    return a + b;
  }
}
