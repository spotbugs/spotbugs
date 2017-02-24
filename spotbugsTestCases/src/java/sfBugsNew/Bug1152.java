package sfBugsNew;

import java.util.List;

public class Bug1152 {
    
    private Bug1152(List<?>[] generations) {
        super();
        this.generations = generations;
    }
    private final List<?>[] generations;
    public synchronized int[] getAllLevelSize()
    {
        int[] counts = new int[generations.length];
        for (int i = 0; i < counts.length; i++)
            counts[i] = generations[i].size();
        return counts;
    }
}
