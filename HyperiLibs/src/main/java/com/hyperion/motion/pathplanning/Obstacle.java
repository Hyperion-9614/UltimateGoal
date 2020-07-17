package com.hyperion.motion.pathplanning;

import com.hyperion.common.Constants;
import com.hyperion.motion.math.Pose;

public class Obstacle {

    public Pose pose;
    public double rInner = Constants.getDouble("pathing.obstacles.rInner");
    public double rBuffer = Constants.getDouble("pathing.obstacles.rBuffer");

    public Obstacle(Pose pose) {
        this.pose = pose;
    }

    public Obstacle(Pose pose, double rInner) {
        this.pose = pose;
        this.rInner = rInner;
    }

    public Obstacle(Pose pose, double rInner, double rBuffer) {
        this.pose = pose;
        this.rInner = rInner;
        this.rBuffer = rBuffer;
    }

    public double radius() {
        return rInner + rBuffer;
    }

}
