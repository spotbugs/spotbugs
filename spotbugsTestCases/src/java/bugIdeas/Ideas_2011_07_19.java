package bugIdeas;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Maps;

public class Ideas_2011_07_19 {
    static class Unhashable1 implements Comparable<Unhashable1> {
        int x, y;

        private Unhashable1(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Unhashable1 that) {
            int result = this.x - that.x;
            if (result != 0)
                return result;
            result = this.y - that.y;
            return result;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Unhashable1))
                return false;
            Unhashable1 that = (Unhashable1) o;
            return this.x == that.x && this.y == that.y;
        }

    }

    static class Unhashable2 implements Comparable<Unhashable2> {
        int x, y;

        private Unhashable2(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Unhashable2 that) {
            int result = this.x - that.x;
            if (result != 0)
                return result;
            result = this.y - that.y;
            return result;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Unhashable2))
                return false;
            Unhashable2 that = (Unhashable2) o;
            return this.x == that.x && this.y == that.y;
        }

    }

    static class Unhashable3 implements Comparable<Unhashable3> {
        int x, y;

        private Unhashable3(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Unhashable3 that) {
            int result = this.x - that.x;
            if (result != 0)
                return result;
            result = this.y - that.y;
            return result;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Unhashable3))
                return false;
            Unhashable3 that = (Unhashable3) o;
            return this.x == that.x && this.y == that.y;
        }

    }
    Map<Unhashable1, Integer> map1 = Maps.newTreeMap();

    Map<Unhashable2, Integer> map2 = new TreeMap<Unhashable2, Integer>();

    TreeMap<Unhashable3, Integer> map3 = new TreeMap<Unhashable3, Integer>();

    Map<Unhashable1, Integer> map4 = Maps.newHashMap();

    Map<Unhashable2, Integer> map5 = new HashMap<Unhashable2, Integer>();

    HashMap<Unhashable3, Integer> map6 = new HashMap<Unhashable3, Integer>();

//    public void test1(Unhashable1 h) {
//        map1.put(h, 1);
//    }
//
//    public void test2(Unhashable2 h) {
//        map2.put(h, 1);
//    }
//
//    public void test3(Unhashable3 h) {
//        map3.put(h, 1);
//    }
//
    public void test4(Unhashable1 h) {
        map4.put(h, 1);
    }

    public void test5(Unhashable2 h) {
        map5.put(h, 1);
    }

    public void test6(Unhashable3 h) {
        map6.put(h, 1);
    }

//    public Integer test1(Unhashable1 h1, Unhashable1 h2) {
//        Map<Unhashable1, Integer> map = Maps.newTreeMap();
//        map.put(h1, 1);
//        return map.get(h2);
//    }
//
//    public Integer test2(Unhashable2 h1, Unhashable2 h2) {
//        Map<Unhashable2, Integer> map = new TreeMap<Unhashable2, Integer>();
//        map.put(h1, 1);
//        return map.get(h2);
//    }
//
//    public Integer test3(Unhashable3 h1, Unhashable3 h2) {
//        TreeMap<Unhashable3, Integer> map = new TreeMap<Unhashable3, Integer>();
//        map.put(h1, 1);
//        return map.get(h2);
//    }
//
    public Integer test4(Unhashable1 h1, Unhashable1 h2) {
        Map<Unhashable1, Integer> map = Maps.newHashMap();
        map.put(h1, 1);
        return map.get(h2);
    }

    public Integer test5(Unhashable2 h1, Unhashable2 h2) {
        Map<Unhashable2, Integer> map = new HashMap<Unhashable2, Integer>();
        map.put(h1, 1);
        return map.get(h2);
    }

    public Integer test6(Unhashable3 h1, Unhashable3 h2) {
        HashMap<Unhashable3, Integer> map = new HashMap<Unhashable3, Integer>();
        map.put(h1, 1);
        return map.get(h2);
    }
}
