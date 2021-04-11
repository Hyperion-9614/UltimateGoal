package com.hyperion.dashboard.simulator;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.trajectory.PIDCtrl;

public abstract class Simulation {

    public ID id;
    public State state;
    public Pose start;

    public double fLPow;
    public double fRPow;
    public double bLPow;
    public double bRPow;

    public Simulation(String type, String name, Pose start) {
        this.id = new ID(type, "simulation", name);
        this.state = State.INACTIVE;
        this.start = new RigidBody(start);
    }

    public void init() {
        Simotion.init(this);
        this.state = State.ACTIVE;
        Simotion.setDrive(0, 0, 0, 0);
    }

    public abstract void run();

    protected void sleep(long ms) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < ms && state == Simulation.State.ACTIVE) {

        }
    }

    public enum State {
        ACTIVE, INACTIVE
    }

}
