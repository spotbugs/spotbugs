package ghIssues;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.*;

// For issue #2935 we need a serializable class analyzed right before the YangLibModule record
// Due to the ordering of class analysis, we're making this class serializable to reproduce the issue
public class Issue2793 implements Serializable { // SE_NO_SERIALVERSIONID
	public record RecordWithoutSerialVersionUid(int x, int y, String message) implements Serializable {
		
	}
	
	public record ExternalizableRecord(int x, int y, String message) implements Externalizable {
		public void writeExternal(ObjectOutput out) throws IOException {
			
		}
		
		public void readExternal(ObjectInput in) throws IOException {
			
		}
	}

	public static class SerializableClass implements Serializable { // SE_NO_SERIALVERSIONID
		public Integer x;
		public Integer y;
	}

	public record YangLibModule(SerializableClass identifier,
								SerializableClass namespace,
								Map<SerializableClass, YangLibModule> submodules,
								Set<SerializableClass> features,
								Set<SerializableClass> deviationModuleNames,
								SerializableClass source) {
	}
}
