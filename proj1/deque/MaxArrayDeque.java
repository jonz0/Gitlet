package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {

    private Comparator<T> m;
    public MaxArrayDeque(Comparator<T> c) {
        super();
        m = c;
    }

//    public MaxArrayDeque() {
//        super();
//        m = new MaxComparator();
//    }

    private class MaxComparator implements Comparator<T> {

        @Override
        public int compare(Object o1, Object o2) {
            return (int) o1 - (int) o2;
        }
    }

    public T max(Comparator<T> c) {
        m = c;
        int maxIndex = 0;
        for (int i = 0; i < size(); i++) {
            if (m.compare(get(i), get(maxIndex)) > 0) {
                maxIndex = i;
            }
        }

        return get(maxIndex);
    }

    public T max() {
        int maxIndex = 0;
        for (int i = 0; i < size(); i++) {
            if (m.compare(get(i), get(maxIndex)) > 0) {
                maxIndex = i;
            }
        }

        return get(maxIndex);
    }
}
