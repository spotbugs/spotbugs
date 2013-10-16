package sfBugsNew;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
    
    @NoWarning("RCN,NP")
    public int foo() throws IOException {
        try (FileChannel c = open()) {
            final MappedByteBuffer mb 
            = c.map(MapMode.READ_ONLY, 0L, c.size());
            
            return mb.getInt();
          }
    }

     abstract FileChannel open();
     
     
     @NoWarning("RCN,NP")
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

  
     @NoWarning("RCN,NP")
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

    @NoWarning("RCN,NP")
    public String reproduce() throws IOException {
        String str = "hi!";
        char[] buf = new char[str.length()];
        try (Reader reader = new StringReader(str)) {
            StringBuilder out = new StringBuilder();
            int bytes = reader.read(buf);
            while (bytes >= 0) {
                out.append(buf, 0, bytes);
                bytes = reader.read(buf);
            }
            return out.toString();
        }
        // workaround: move return outside of try-with-resources
        // return out.toString();
    }
}
