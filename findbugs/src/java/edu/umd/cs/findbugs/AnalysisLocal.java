package edu.umd.cs.findbugs;
import java.util.Map;

import edu.umd.cs.findbugs.ba.AnalysisContext;

public class AnalysisLocal<T> {
    protected T initialValue() {
        return null;
    }

    protected Map getMap() {
	AnalysisContext t = AnalysisContext.currentAnalysisContext();
	return  t.analysisLocals;
	}

   public T  get() {
		Map m = getMap();
		if (m.containsKey(this)) return (T) m.get(this);
		
		synchronized(m) {
		    if (m.containsKey(this)) return (T) m.get(this);
		    T result = initialValue();
		    m.put(this,result);
		    return (T) result;
		    }
		}
   public  void set(T value) {
		Map m = getMap();
		m.put(this, value);
		}
   public  void  remove() {
		Map m = getMap();
		m.remove(this);
		}
}
