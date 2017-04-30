import java.util.ArrayList;
import java.util.List;

class PerformanceTest {
  private static final long MEGABYTE = 1024L * 1024L;

  static long bytesToMegabytes(long bytes) {
    return bytes / MEGABYTE;
  }

  public static void main(String[] args) {
    // I assume you will know how to create a object Person yourself...
    List<String> list = new ArrayList<String>();
    for (int i = 0; i <= 10000000; i++) {
      list.add(new String("Jim"));
    }
    // Get the Java runtime
    Runtime runtime = Runtime.getRuntime();
    // Run the garbage collector
    runtime.gc();
    // Calculate the used memory
    long memory = runtime.totalMemory() - runtime.freeMemory();
    System.out.println("Used memory is bytes: " + memory);
    System.out.println("Used memory is megabytes: "
        + bytesToMegabytes(memory));
  }
}
