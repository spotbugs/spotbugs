package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Set;

public class PublicIdentifiers {
    // contains all the public identifiers (classnames, method names, variables, constants) that are found in the Standard Library
    // for dev purpose I only use the most commons: code too large error
    // add the most common classes and interfaces identifier names in new lines
    // do not append the package hierarchy or class hierarchy
    // get a few random examples from the JSL and add them here
    static final Set<String> PUBLIC_IDENTIFIERS;

    static {
        PUBLIC_IDENTIFIERS = new HashSet<>();
        PUBLIC_IDENTIFIERS.add("AbstractCollection");
        PUBLIC_IDENTIFIERS.add("AbstractList");
        PUBLIC_IDENTIFIERS.add("AbstractMap");
        PUBLIC_IDENTIFIERS.add("AbstractSequentialList");
        PUBLIC_IDENTIFIERS.add("AbstractSet");
        PUBLIC_IDENTIFIERS.add("ArrayList");
        PUBLIC_IDENTIFIERS.add("Arrays");
        PUBLIC_IDENTIFIERS.add("Attribute");
        PUBLIC_IDENTIFIERS.add("Attributes");
        PUBLIC_IDENTIFIERS.add("BitSet");
        PUBLIC_IDENTIFIERS.add("Calendar");
        PUBLIC_IDENTIFIERS.add("Character");
        PUBLIC_IDENTIFIERS.add("CharacterData");
        PUBLIC_IDENTIFIERS.add("CharacterData00");
        PUBLIC_IDENTIFIERS.add("CharacterData01");
        PUBLIC_IDENTIFIERS.add("CharacterData02");
        PUBLIC_IDENTIFIERS.add("CharacterData0E");
        PUBLIC_IDENTIFIERS.add("CharacterDataLatin1");
        PUBLIC_IDENTIFIERS.add("CharacterDataPrivateUse");
        PUBLIC_IDENTIFIERS.add("CharacterDataUndefined");
        PUBLIC_IDENTIFIERS.add("CharacterName");
        PUBLIC_IDENTIFIERS.add("CharacterSubsets");
        PUBLIC_IDENTIFIERS.add("Charset");
        PUBLIC_IDENTIFIERS.add("CharsetDecoder");
        PUBLIC_IDENTIFIERS.add("CharsetEncoder");
        PUBLIC_IDENTIFIERS.add("Class");
        PUBLIC_IDENTIFIERS.add("ClassLoader");
        PUBLIC_IDENTIFIERS.add("Cloneable");
        PUBLIC_IDENTIFIERS.add("Collection");
        PUBLIC_IDENTIFIERS.add("Collections");
        PUBLIC_IDENTIFIERS.add("Comparator");
        PUBLIC_IDENTIFIERS.add("ConcurrentModificationException");
        PUBLIC_IDENTIFIERS.add("Console");
        PUBLIC_IDENTIFIERS.add("Currency");
        PUBLIC_IDENTIFIERS.add("Date");
        PUBLIC_IDENTIFIERS.add("Dictionary");
        PUBLIC_IDENTIFIERS.add("Double");
        PUBLIC_IDENTIFIERS.add("Enum");
        PUBLIC_IDENTIFIERS.add("EnumMap");
        PUBLIC_IDENTIFIERS.add("EnumSet");
        PUBLIC_IDENTIFIERS.add("EnumSetIterator");
        PUBLIC_IDENTIFIERS.add("Enumeration");
        PUBLIC_IDENTIFIERS.add("EventObject");
        PUBLIC_IDENTIFIERS.add("Exception");
        PUBLIC_IDENTIFIERS.add("Float");
        PUBLIC_IDENTIFIERS.add("Formatter");
        PUBLIC_IDENTIFIERS.add("GregorianCalendar");
        PUBLIC_IDENTIFIERS.add("HashMap");
        PUBLIC_IDENTIFIERS.add("Vector");
        PUBLIC_IDENTIFIERS.add("WeakHashMap");
        PUBLIC_IDENTIFIERS.add("Hashtable");
        PUBLIC_IDENTIFIERS.add("HashSet");
        PUBLIC_IDENTIFIERS.add("IdentityHashMap");
        PUBLIC_IDENTIFIERS.add("IllegalFormatCodePointException");
        PUBLIC_IDENTIFIERS.add("IllegalFormatException");
        PUBLIC_IDENTIFIERS.add("IllegalFormatConversionException");
        PUBLIC_IDENTIFIERS.add("IllegalFormatFlagsException");
        PUBLIC_IDENTIFIERS.add("IllegalFormatPrecisionException");
        PUBLIC_IDENTIFIERS.add("File");
        PUBLIC_IDENTIFIERS.add("FileDescriptor");
        PUBLIC_IDENTIFIERS.add("FileInputStream");
        PUBLIC_IDENTIFIERS.add("FileOutputStream");
        PUBLIC_IDENTIFIERS.add("FilePermission");
        PUBLIC_IDENTIFIERS.add("FileSystem");
        PUBLIC_IDENTIFIERS.add("FileSystemNotFoundException");
        PUBLIC_IDENTIFIERS.add("FileSystems");
        PUBLIC_IDENTIFIERS.add("FileTime");
        PUBLIC_IDENTIFIERS.add("System");
        PUBLIC_IDENTIFIERS.add("Runtime");
        PUBLIC_IDENTIFIERS.add("RuntimePermission");
        PUBLIC_IDENTIFIERS.add("RuntimeMXBean");
        PUBLIC_IDENTIFIERS.add("Runnable");
        PUBLIC_IDENTIFIERS.add("RunnableFuture");
        PUBLIC_IDENTIFIERS.add("RunnableScheduledFuture");
    }
}
