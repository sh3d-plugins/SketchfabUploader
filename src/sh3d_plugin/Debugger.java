package sh3d_plugin;

public class Debugger{
  public static boolean isEnabled(){
      return true;
  }

  public static void log(Object o){
      System.out.println(o.toString());
  }
}
