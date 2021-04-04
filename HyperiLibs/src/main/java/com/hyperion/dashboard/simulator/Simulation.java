package com.hyperion.dashboard.simulator;

import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;

import javafx.application.Platform;
import javafx.scene.input.MouseEvent;

public abstract class Simulation {

    public ID id;
    public State state;
    public RigidBody robot;
    public RigidBody start;

    public double fLPow;
    public double fRPow;
    public double bLPow;
    public double bRPow;

    public Simulation(String type, String name, Pose start) {
        this.id = new ID(type, "simulation", name);
        this.state = State.INACTIVE;
        this.start = new RigidBody(start);
    }

    public void init(RigidBody robot) {
        this.robot = robot;
        this.state = State.RUNNING;
    }

    public abstract void run();

    protected void setDrive(double fLPow, double fRPow, double bLPow, double bRPow) {
        this.fLPow = fLPow;
        this.fRPow = fRPow;
        this.bLPow = bLPow;
        this.bRPow = bRPow;
    }

    protected void sleep(long ms) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < ms) {

        }
    }

    public void end() {
        this.state = State.INACTIVE;
        Dashboard.simulator.activeSim = null;
        Platform.runLater(() -> Dashboard.leftPane.runStopSim.setText("Run OpMode\nSimulation"));
    }

    public enum State {
        RUNNING, INACTIVE
    }

}
