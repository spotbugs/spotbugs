package ghIssues;

import  androidx.lifecycle.LiveData;
import  androidx.lifecycle.MutableLiveData;

public class Issue3762 {
    private final MutableLiveData<String> text = new MutableLiveData<>();

    public LiveData<String> getText() {
        return text; // <-- Rule 'EI_EXPOSE_REP' triggers here
    }
}
