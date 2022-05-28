package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeGetLast();
    }

    public static void timeGetLast() {
        AList<Integer> elements = new AList<>();
        int[] ns = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000};
        AList<Double> times = new AList<>();
        AList<Integer> num_ops = new AList<>();
        int m = 10000;

        for (int i = 0; i < ns.length; i++) {
            elements.addLast(ns[i]);
            num_ops.addLast(m);

            SLList<Integer> working = new SLList<Integer>();
            for (int f = 0; f < ns[i] - 1; f++) {
                working.addFirst(1);
            }
            Stopwatch s = new Stopwatch();
            for (int f = 0; f < m; f++) {
                working.getLast();
            }
            times.addLast(s.elapsedTime());
        }

        printTimingTable(elements, times, num_ops);
    }

}
