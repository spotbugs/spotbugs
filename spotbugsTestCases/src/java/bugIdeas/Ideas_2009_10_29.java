package bugIdeas;

import java.util.Random;

import junit.framework.TestCase;

public class Ideas_2009_10_29 extends TestCase {

    Random r = new Random();

    // Map<String, Integer> m1 = new HashMap<String,Integer>();
    //
    // Map<String, Integer> m2 = new TreeMap<String,Integer>();
    // Map<String, Long> m3 = new LinkedHashMap<String,Long>();
    //
    // boolean equalCounts() {
    // if (m1.size() != m2.size())
    // return false;
    // for (String s : m1.keySet())
    // if (m1.get(s) != m2.get(s))
    // return false;
    // return false;
    // }
    //
    // boolean equalCounts3() {
    // if (m1.size() != m2.size())
    // return false;
    // for (String s : m1.keySet())
    // if (!m1.get(s).equals( m3.get(s)))
    // return false;
    // return false;
    // }
    // boolean equalCounts(Map<String, Integer> map1, Map<String, Integer> map2)
    // {
    // if (map1.size() != map2.size())
    // return false;
    // for (String s : map1.keySet())
    // if (map1.get(s) != map2.get(s))
    // return false;
    // return false;
    // }
    //
    // boolean equalCounts2(Map<String, Integer> map1, Map<String, Integer>
    // map2) {
    // if (map1.size() != map2.size())
    // return false;
    // for (Map.Entry<String, Integer> s : map1.entrySet())
    // if (s.getValue() != map2.get(s.getKey()))
    // return false;
    // return false;
    // }
    //
    // boolean equalCounts3(Map<String, Integer> map1, Map<String, Long> map2) {
    // if (map1.size() != map2.size())
    // return false;
    // for (String s : map1.keySet())
    // if (!map1.get(s).equals(map2.get(s)))
    // return false;
    // return false;
    // }
    //
    // boolean equalCounts4(Map<String, Integer> map1, Map<String, Integer>
    // map2) {
    // if (map1.size() != map2.size())
    // return false;
    // for (Map.Entry<String, Integer> s : map1.entrySet())
    // if (s.getValue() != map2.get(s.getValue()))
    // return false;
    // return false;
    // }
    //
    // boolean checkKeySet(Map<String,Integer> m, Integer x) {
    // for(Iterator<String> i = m.keySet().iterator(); i.hasNext(); )
    // if (x.equals(i.next()))
    // return true;
    // return false;
    // }
    // boolean checkValues(Map<String,Integer> m, String x) {
    // for(Iterator<Integer> i = m.values().iterator(); i.hasNext(); )
    // if (x.equals(i.next()))
    // return true;
    // return false;
    // }
    // int nextRandom(int n) {
    // return r.nextInt() % n;
    // }

    int nextRandom2(int n) {
        int result = r.nextInt() % n;
        if (result < 0)
            return -result;
        return result;
    }

    // public void testFail() {
    // try {
    // Iterator<?> i = Collections.emptyList().iterator();
    // i.next();
    // fail("Should have thrown no such element exception");
    // } catch (Throwable e) {
    // assert true; // expect an exception to be thrown
    // }
    //
    // }

}
