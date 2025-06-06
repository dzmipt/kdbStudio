package studio.kdb;

import java.util.Arrays;
import java.util.Comparator;

public class Sorter {

    public static int[] sort(KColumn column, int[] origIndex) {
        Integer[] index = new Integer[column.size()];
        for (int i=0; i<index.length; i++) {
            index[i] = i;
        }
        Comparator<Integer> indexComparator = new IndexComparator(column);
        Comparator<Integer> origIndexComparator = new OrigIndexComparator(origIndex);
        Arrays.sort(index, indexComparator.thenComparing(origIndexComparator));
        int[] res = new int[column.size()];
        for (int i=0; i<res.length; i++) {
            res[i] = index[i];
        }
        return res;
    }

    public static int[] reverse(KColumn column, int[] origIndex) {
        int count = origIndex.length;
        int[] res = new int[count];
        if (count == 0) return res;

        K.KBase current = column.get(origIndex[count-1]);
        int currentStart = 0;

        for (int i=1; ; i++) {
            K.KBase next = i == count ? null : column.get(origIndex[count-i-1]);

            if (next == null || next.compareTo(current) != 0) {
                System.arraycopy(origIndex, count - (i-1) - 1, res, currentStart, i - currentStart );
                if (next == null) break;
                current = next;
                currentStart = i;
            }
        }
        return res;
    }

    private static class IndexComparator implements Comparator<Integer> {
        private final KColumn column;

        IndexComparator(KColumn column) {
            this.column = column;
        }

        @Override
        public int compare(Integer i1, Integer i2) {
            return column.get(i1).compareTo(column.get(i2));
        }
    }

    private static int[] inverse(int[] index) {
        int[] res = new int[index.length];
        for (int i=0; i<index.length; i++) {
            res[index[i]] = i;
        }
        return res;
    }

    private static class OrigIndexComparator implements Comparator<Integer> {
        private final int[] index;
        OrigIndexComparator(int[] index) {
            this.index = inverse(index);
        }

        @Override
        public int compare(Integer i1, Integer i2) {
            return index[i1] - index[i2];
        }
    }

}
