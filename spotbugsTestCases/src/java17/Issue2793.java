package ghIssues;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.*;

public class Issue2793 implements Serializable {
	public static final long serialVersionUID = 1L;

	public record RecordWithoutSerialVersionUid(int x, int y, String message) implements Serializable {
		
	}
	
	public record ExternalizableRecord(int x, int y, String message) implements Externalizable {
		public void writeExternal(ObjectOutput out) throws IOException {
			
		}
		
		public void readExternal(ObjectInput in) throws IOException {
			
		}
	}

	public static class SerializableClass implements Serializable {
		public static final long serialVersionUID = 1L;

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
