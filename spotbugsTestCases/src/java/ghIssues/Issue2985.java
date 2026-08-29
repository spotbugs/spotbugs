package ghIssues;

import java.io.Serializable;

public class Issue2985 {
	public static class NonStaticFactoryMethod implements Serializable {
		private static final NonStaticFactoryMethod DEFAULT_INSTANCE;
		static {
			DEFAULT_INSTANCE = new NonStaticFactoryMethod();
		}

		public static NonStaticFactoryMethod getDefaultInstance() {
			return DEFAULT_INSTANCE;
		}

		private NonStaticFactoryMethod() {
		}

		// This method should be recognized as (non private) factory method, this is therefore not a singleton
		protected Object newInstance() {
			return new NonStaticFactoryMethod();
		}
	}
	
	public static class PublicConstructor implements Serializable {
		private static final PublicConstructor DEFAULT_INSTANCE;
		static {
			DEFAULT_INSTANCE = new PublicConstructor();
		}

		public static PublicConstructor getDefaultInstance() {
			return DEFAULT_INSTANCE;
		}

		public PublicConstructor() {
		}
	}
}
