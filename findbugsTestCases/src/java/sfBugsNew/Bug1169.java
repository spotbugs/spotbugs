package sfBugsNew;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Bug1169 {

    static abstract class Engine<R> {

        public abstract void reset();

        public abstract void update(byte[] buffer, int i, int get);

        public abstract R digest();
        
    }
    
    @NoWarning("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    public int foo() throws IOException {
        try (FileChannel c = open()) {
            final MappedByteBuffer mb 
            = c.map(MapMode.READ_ONLY, 0L, c.size());
            
            return mb.getInt();
          }
    }

     abstract FileChannel open();
     
     
     @Nonnull
     protected  <R> R executeEngine(@Nonnull final Engine<R> engine, @Nonnull final Path path) throws IOException {
       try (final FileChannel c = open(path, StandardOpenOption.READ)) {
         engine.reset();

         final MappedByteBuffer mb = c.map(MapMode.READ_ONLY, 0L, c.size());
         final int bufferLength = 8*1024;
         final byte[] buffer = new byte[bufferLength];
         while (mb.hasRemaining()) {
           final int get = Math.min(mb.remaining(), bufferLength);
           mb.get(buffer, 0, get);
           engine.update(buffer, 0, get);
         }

         return engine.digest();
       }

     }

  
     @Nonnull
     public <R> R execute2(@Nonnull final Engine<R> engine, @Nonnull final Path path, @Nonnull final byte[] buffer)
         throws UnsupportedOperationException, IOException {

       try (FileChannel c = open(path, StandardOpenOption.READ)) {
         engine.reset();
         final long size = c.size();

         long i = 0;
         while (i < size) {
           final MappedByteBuffer mb = c.map(MapMode.READ_ONLY, i, Math.min(size - i, Integer.MAX_VALUE));
           final int bl = buffer.length;
           while (mb.hasRemaining()) {
             final int get = Math.min(mb.remaining(), bl);
             mb.get(buffer, 0, get);
             engine.update(buffer, 0, get);
             i += get;
           }
         }
         return engine.digest();
       } 
     }
     
    abstract  FileChannel open(Path path, StandardOpenOption read);

}
