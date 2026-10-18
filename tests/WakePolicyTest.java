package org.telesin.buttonlab;

public final class WakePolicyTest {
    private static void expect(WakePolicy.Action actual, WakePolicy.Action expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        WakePolicy policy = new WakePolicy();
        expect(policy.screenOn(false, 1000), WakePolicy.Action.IGNORE);
        expect(policy.screenOn(true, 1000), WakePolicy.Action.OPEN_CAMERA);
        policy.screenOff(2000);
        for (int offset : new int[]{0,54,67,124,417,594,605,799})
            expect(policy.screenOn(true, 2000 + offset), WakePolicy.Action.RESTORE_SLEEP);
        expect(policy.screenOn(true, 2800), WakePolicy.Action.OPEN_CAMERA);
        expect(policy.screenOn(true, 4000), WakePolicy.Action.OPEN_CAMERA);
        expect(policy.screenOn(false, 2001), WakePolicy.Action.IGNORE);
        policy.reset();
        expect(policy.screenOn(true, 2001), WakePolicy.Action.OPEN_CAMERA);
        policy.screenOff(0);
        expect(policy.screenOn(true, 124), WakePolicy.Action.RESTORE_SLEEP);
        expect(policy.screenOn(true, -1), WakePolicy.Action.OPEN_CAMERA);
        System.out.println("Wake policy checks passed: measured bounces, threshold, pause/reset, fresh startup and monotonic boundary");
    }
}
