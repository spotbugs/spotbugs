package npe;

public class NonterminatingAnalysisBug implements Cloneable {

	static class Field {
		Object clone(NonterminatingAnalysisBug bug) {
			return this;
		}
	}

	Field[] fields = new Field[5];

	public NonterminatingAnalysisBug clone() throws CloneNotSupportedException {

			NonterminatingAnalysisBug newObject = (NonterminatingAnalysisBug) super
					.clone();
			if (fields.length > 0) {
				newObject.fields = new Field[fields.length];
				for (int i = 0; i < fields.length; i++)
					newObject.fields[i] = (Field) (fields[i] == null ? null
							: fields[i].clone(newObject));
			}
			return newObject;
	
	}

}
