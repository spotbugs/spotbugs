package ghIssues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class Issue608GenericObjectType {
	protected <C extends Collection<String>> C validateMBeansNamesList(C namesList, String name) {
		namesList.remove(name);
		
		Collection<String> availableNames = new ArrayList<>();
		availableNames.containsAll(namesList);
		
		return namesList;
	}


	protected <C extends Map<X, C>, X extends Number> C validateMBeansNamesList(C namesList, X number) {
		namesList.remove(number);

		Collection<X> availableNames = new ArrayList<>();
		if (availableNames.containsAll(namesList.values())) {
			throw new IllegalStateException();
		}

		return namesList;
	}
}
