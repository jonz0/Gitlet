package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeAList {
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
        timeAListConstruction();
    }

    public static void timeAListConstruction() {
        AList<Integer> elements = new AList<>();
        int[] ops = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000};
//        for (int i = 0; i < ops.length; i++) {
//
//        }
        AList<Double> times = new AList<>();
        for (int i = 0; i < ops.length; i++) {
            elements.addLast(ops[i]);

            AList<Integer> working_AList = new AList<>();
            Stopwatch s = new Stopwatch();
            for (int f = 0; f < ops[i]; f++) {
                working_AList.addLast(1);
            }
            times.addLast(s.elapsedTime());
        }

        printTimingTable(elements, times, elements);
    }
}
