package edu.umd.cs.findbugs.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MutableCloudTask implements Cloud.CloudTask {
    private String name;

    private CopyOnWriteArrayList<Cloud.CloudTaskListener> listeners = new CopyOnWriteArrayList<Cloud.CloudTaskListener>();

    private String substatus = "";

    private double percentDone = 0;

    /** A listener used only if no other listeners are present. */
    private Cloud.CloudTaskListener defaultListener;

    private boolean finished = false;

    private boolean useDefaultListener = true;

    public MutableCloudTask(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getStatusLine() {
        return substatus;
    }

    public double getPercentCompleted() {
        return percentDone;
    }

    public void addListener(Cloud.CloudTaskListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(Cloud.CloudTaskListener listener) {
        listeners.remove(listener);
    }

    public boolean isFinished() {
        return finished;
    }

    public void setUseDefaultListener(boolean enabled) {
        this.useDefaultListener = enabled;
    }

    public void update(String substatus, double percentDone) {
        this.substatus = substatus;
        this.percentDone = percentDone;
        for (Cloud.CloudTaskListener listener : getListeners()) {
            listener.taskStatusUpdated(substatus, percentDone);
        }
    }

    public void finished() {
        finished = true;
        for (Cloud.CloudTaskListener listener : getListeners()) {
            listener.taskFinished();
        }
        clearListeners();
    }

    public void failed(String message) {
        finished = true;
        for (Cloud.CloudTaskListener listener : getListeners()) {
            listener.taskFailed(message);
        }
        clearListeners();
    }

    /** A listener used only if no other listeners are present. */
    public void setDefaultListener(Cloud.CloudTaskListener defaultListener) {
        this.defaultListener = defaultListener;
    }

    private List<Cloud.CloudTaskListener> getListeners() {
        List<Cloud.CloudTaskListener> myListeners = new ArrayList<Cloud.CloudTaskListener>(listeners);
        if (useDefaultListener && myListeners.isEmpty() && defaultListener != null) {
            myListeners.add(defaultListener);
        }
        return myListeners;
    }

    /** I think this is a good idea for garbage collection purposes -Keith */
    private void clearListeners() {
        listeners.clear();
        defaultListener = null;
    }

    public boolean isUsingDefaultListener() {
        return listeners.isEmpty();
    }
}
