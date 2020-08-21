package com.hyperion.motion.pathplanning;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;

import java.util.*;

/**
 * D* Lite Dynamic Path Planning Algorithm
 * References:
 * (1) http://idm-lab.org/bib/abstracts/papers/aaai02b.pdf
 * (2) https://www.youtube.com/watch?v=_4u9W1xOuts&t=2708s
 */

public class DStarLite {

    public static final double INFINITY = Double.MAX_VALUE;

    public Pose start;
    public Pose goal;

    public Set<Obstacle> fixedObstacles;
    public Set<Obstacle> dynamicObstacles;

    public Pose[][] grid;

    public DStarLite(ArrayList<Obstacle> fixedObstaclesList) {
        fixedObstacles = new HashSet<>();
        fixedObstacles.addAll(fixedObstaclesList);
        dynamicObstacles = new HashSet<>();
    }

    public void init(Pose start, Pose goal) {
        this.start = new Pose(start);
        this.goal = goal;
        this.grid = generatePathingGrid();
        
    }

    public ArrayList<Pose> getPath() {
        return new ArrayList<>();
    }

    public void recompute() {

    }

    public void robotMoved(Pose start) {
        this.start.setPose(start);
    }

    public boolean updateDynamicObstacles(ArrayList<Obstacle> dynamicObstaclesList) {
        boolean haveObstaclesChangedSignificantly = false;

        if (dynamicObstaclesList.size() != dynamicObstacles.size()) {
            haveObstaclesChangedSignificantly = true;
        } else if (dynamicObstacles.size() != 0) {
            boolean hasMatchingSucceeded = true;
            for (Obstacle obstacle : dynamicObstacles) {
                boolean isMatched = false;
                for (Obstacle compare : dynamicObstaclesList) {
                    if (obstacle.pose.distanceTo(compare.pose) <= Constants.getDouble("pathing.obstacleMoveThreshold")) {
                        isMatched = true;
                        break;
                    }
                }
                if (!isMatched) {
                    hasMatchingSucceeded = false;
                    break;
                }
            }

            haveObstaclesChangedSignificantly = !hasMatchingSucceeded;
        }

        if (haveObstaclesChangedSignificantly) {
            dynamicObstacles.clear();
            dynamicObstacles.addAll(dynamicObstaclesList);
        }

        return haveObstaclesChangedSignificantly;
    }

    public Set<Obstacle> obstacles() {
        Set<Obstacle> obstacles = new HashSet<>();
        obstacles.addAll(fixedObstacles);
        obstacles.addAll(dynamicObstacles);
        return obstacles;
    }

    public static Pose[][] generatePathingGrid() {
        double fsl = Constants.getDouble("localization.fieldSideLength");
        double mHat = Constants.getDouble("pathing.gridMhat");
        double buffer = ((fsl - 45.72) % mHat) / 2.0 + 22.86;

        int n = (int) MathUtils.round((fsl - 2 * buffer) / mHat, 0) + 1;
        Pose[][] pathingGrid = new Pose[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                double x = -(fsl / 2.0) + buffer + r * mHat;
                double y = -(fsl / 2.0) + buffer + c * mHat;
                pathingGrid[r][c] = new Pose(x, y, 0);
            }
        }
        return pathingGrid;
    }

}
