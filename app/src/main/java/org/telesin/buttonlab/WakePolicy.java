package org.telesin.buttonlab;

/** Monotonic-time policy; the 800 ms window matches measured remote bounce. */
final class WakePolicy {
    enum Action { IGNORE, RESTORE_SLEEP, OPEN_CAMERA }
    private long lastOff = -1;
    void screenOff(long now) { lastOff = now; }
    void reset() { lastOff = -1; }
    Action screenOn(boolean enabled, long now) {
        if (!enabled) return Action.IGNORE;
        return lastOff >= 0 && now >= lastOff && now - lastOff < 800
            ? Action.RESTORE_SLEEP : Action.OPEN_CAMERA;
    }
}
