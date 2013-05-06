package sfBugsNew;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Bug1169 {

    
    @NoWarning("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    public int foo() throws IOException {
        try (FileChannel c = open()) {
            final MappedByteBuffer mb 
            = c.map(MapMode.READ_ONLY, 0L, c.size());
            
            return mb.getInt();
          }
    }

     abstract FileChannel open();
}
