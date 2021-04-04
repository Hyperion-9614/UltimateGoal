package com.hyperion.dashboard.simulator;

import com.hyperion.motion.math.Pose;

public class Test extends Simulation {

    public Test() {
        super("auto", "test", new Pose(-100, 0, 0));
    }

    @Override
    public void run() {
        setDrive(1, 1, 1, 1);
        sleep(3000);
    }
}
