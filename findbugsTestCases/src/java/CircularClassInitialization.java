
public class CircularClassInitialization {
	static class InnerClassSingleton extends CircularClassInitialization {
		static InnerClassSingleton singleton = new InnerClassSingleton();
	}
	
	static CircularClassInitialization foo = InnerClassSingleton.singleton;
	

}
