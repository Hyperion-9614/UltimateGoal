package com.hyperion.dashboard.simulator;

import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

public class Test extends Simulation {

    public Test() {
        super("auto", "test", new Pose(0, 0, Math.PI / 2));
    }

    @Override
    public void run() {
        Simotion.setDrive(new Vector2D(1, Math.PI / 4, false), 0);
        sleep(2000);
        Simotion.setDrive(0);
    }
}
