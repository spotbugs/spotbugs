import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/*
 From Cliff Click:

 I ran findbugs on my NonBlockingHashMap,
 https://sourceforge.net/projects/high-scale-lib


 findbugs apparently does not recognize AtomicReferenceFieldUpdater's as modifying fields.  Or at least, I get "never written" errors for a field that is only ever updated via an AtomicReferenceFieldUpdater, then I get incorrect messages about redundant null-checks.  Here's the Updater code:

 volatile Object[] _newkvs;
 private final AtomicReferenceFieldUpdater<CHM,Object[]> _newkvsUpdater =
 AtomicReferenceFieldUpdater.newUpdater(CHM.class,Object[].class, "_newkvs");
 // Set the _next field if we can.
 boolean CAS_newkvs( Object[] newkvs ) {
 while( _newkvs == null )
 if( _newkvsUpdater.compareAndSet(this,null,newkvs) )
 return true;
 return false;
 }



 */
public class UnreadFieldFalsePositiveAtomicUpdater {
    volatile Object[] _newkvs;

    private final AtomicReferenceFieldUpdater<UnreadFieldFalsePositiveAtomicUpdater, Object[]> _newkvsUpdater = AtomicReferenceFieldUpdater
            .newUpdater(UnreadFieldFalsePositiveAtomicUpdater.class, Object[].class, "_newkvs");

    // Set the _next field if we can.
    boolean CAS_newkvs(Object[] newkvs) {
        while (_newkvs == null)
            if (_newkvsUpdater.compareAndSet(this, (Object[]) null, newkvs))
                return true;
        return false;
    }

}
