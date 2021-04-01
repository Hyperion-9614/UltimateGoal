package com.hyperion.dashboard.simulator;

import com.hyperion.common.ID;
import com.hyperion.motion.math.RigidBody;

public abstract class Simulation {

    public ID id;
    public State state;
    public RigidBody robot;

    public double fLPow;
    public double fRPow;
    public double bLPow;
    public double bRPow;

    public Simulation(String name) {
        this.id = new ID("auto", "sim", name);
        this.state = State.INACTIVE;
    }

    public void init(RigidBody robot) {
        this.robot = robot;
        this.state = State.RUNNING;
    }

    public abstract void run();

    public void end() {
        this.state = State.INACTIVE;
    }

    public enum State {
        RUNNING, INACTIVE
    }

}
