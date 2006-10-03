package npe;

public class NonterminatingAnalysisBug implements Cloneable {

	static class Field {
		Field clone(NonterminatingAnalysisBug bug) {
			return this;
		}
	}

	Field[] fields = new Field[5];

	public NonterminatingAnalysisBug clone() throws CloneNotSupportedException {

			NonterminatingAnalysisBug newObject = (NonterminatingAnalysisBug) super
					.clone();

				newObject.fields = new Field[fields.length];
				for (int i = 0; i < fields.length; i++)
					newObject.fields[i] = (fields[i] == null ? null
							: fields[i].clone(newObject));

			return newObject;
	
	}

}
