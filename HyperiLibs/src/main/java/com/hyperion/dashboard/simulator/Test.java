package com.hyperion.dashboard.simulator;

import com.hyperion.motion.math.Pose;

public class Test extends Simulation {

    public Test() {
        super("auto", "test", new Pose(-100, 0, 0));
    }

    @Override
    public void run() {
        pidMove(new Pose(100, 0, 0));
    }
}
