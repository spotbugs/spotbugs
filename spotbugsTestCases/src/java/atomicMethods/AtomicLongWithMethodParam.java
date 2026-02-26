package atomicMethods;

import com.google.common.base.MoreObjects;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongWithMethodParam {

    private final AtomicLong lastActive;

    AtomicLongWithMethodParam() {
        this.lastActive = new AtomicLong();
    }

    public void updateLastActive() {
        lastActive.set(System.currentTimeMillis());
    }

    public long getLastActive() {
        return lastActive.get();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lastActive", lastActive)
                .toString();
    }
}
