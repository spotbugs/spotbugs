package puzzlers;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Puzzlers2010 {
    
    static enum Sex { MALE, FEMALE }

    
    public static void main(String[] args) {
        printSize(new HashMap<Sex, Sex>());
        printSize(new EnumMap<Sex, Sex>(Sex.class));
        printSize2(new EnumMap<Sex, Sex>(Sex.class));
    }
   
    private static void printSize(Map<Sex, Sex> map) {
        map.put(Sex.MALE, Sex.FEMALE);
        map.put(Sex.FEMALE,Sex.MALE);
        Set<Map.Entry<Sex, Sex>> set =
            new HashSet<Map.Entry<Sex, Sex>>(map.entrySet());
        System.out.print(set.size() + " ");
    }
    

    private static void printSize2(EnumMap<Sex, Sex> map) {
        map.put(Sex.MALE, Sex.FEMALE);
        map.put(Sex.FEMALE,Sex.MALE);
        Set<Map.Entry<Sex, Sex>> set =
            new HashSet<Map.Entry<Sex, Sex>>(map.entrySet());
        System.out.print(set.size() + " ");
        Set<Map.Entry<Sex, Sex>> set2 = new HashSet<Map.Entry<Sex, Sex>>();
        set2.addAll(map.entrySet());
        System.out.print(set2.size() + " ");
    }
  }

