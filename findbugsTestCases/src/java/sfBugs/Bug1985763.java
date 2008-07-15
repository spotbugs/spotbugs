package sfBugs;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bug1985763 {
	
	int foo() {
		ReadWriteLock myLock = new ReentrantReadWriteLock(); 
		myLock.writeLock().lock();
		int a = 6;
		int b = 9;
		int wrongAnswer = a * b;
		int rightAnswer = 42;
		myLock.writeLock().unlock();
		return wrongAnswer + rightAnswer;
	}

}
