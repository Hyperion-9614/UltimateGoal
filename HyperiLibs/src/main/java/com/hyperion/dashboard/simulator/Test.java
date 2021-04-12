package com.hyperion.dashboard.simulator;

import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

public class Test extends Simulation {

    public Test() {
        super("auto", "test", new Pose(0, 0, 0));
    }

    @Override
    public void run() {
        Simotion.pidMove("xTest");
    }
}
