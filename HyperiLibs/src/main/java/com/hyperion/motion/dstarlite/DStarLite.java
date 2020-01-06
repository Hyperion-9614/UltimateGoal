package com.hyperion.motion.dstarlite;

import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Pose;

import java.util.*;

/**
 *
 * @author daniel beard
 * http://danielbeard.io
 * http://github.com/daniel-beard
 *
 * Copyright (C) 2012 Daniel Beard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

public class DStarLite {

    private ArrayList<Node> path = new ArrayList<>();
    public ArrayList<Node> obstacles = new ArrayList<>();
    private double k_m;
    private Node s_start = new Node();
    private Node s_goal = new Node();
    private Node s_last = new Node();
    private PriorityQueue<Node>	openList = new PriorityQueue<>();
    private HashMap<Node, CellInfo>	cellHash = new HashMap<>();
    private HashMap<Node, Float> openHash = new HashMap<>();

    private double C1 = 40;
    private double DIAG = Math.sqrt(2 * C1 * C1);

    public DStarLite() {

    }

    private void init(double sX, double sY, double gX, double gY) {
        cellHash.clear();
        path.clear();
        openHash.clear();
        while(!openList.isEmpty()) openList.poll();

        k_m = 0;

        s_start.x = sX;
        s_start.y = sY;
        s_goal.x  = gX;
        s_goal.y  = gY;

        CellInfo tmp = new CellInfo();
        tmp.g   = 0;
        tmp.rhs = 0;
        tmp.cost = C1;

        cellHash.put(s_goal, tmp);

        tmp = new CellInfo();
        tmp.g = tmp.rhs = heuristic(s_start,s_goal);
        tmp.cost = C1;
        cellHash.put(s_start, tmp);
        calculateKey(s_start);

        s_last = s_start;
    }

    private Node calculateKey(Node u) {
        double val = Math.min(getRHS(u), getG(u));
        u.k.setFirst (val + heuristic(u,s_start) + k_m);
        u.k.setSecond(val);
        return u;
    }

    private double getRHS(Node u) {
        if (u == s_goal) return 0;

        if (cellHash.get(u) == null) {
            return heuristic(u, s_goal);
        }
        return cellHash.get(u).rhs;
    }

    private double getG(Node u) {
        if (cellHash.get(u) == null)
            return heuristic(u,s_goal);
        return cellHash.get(u).g;
    }


    private double heuristic(Node a, Node b) {
        return eightCondist(a, b) * C1;
    }

    private double eightCondist(Node a, Node b) {
        double temp;
        double min = Math.abs(a.x - b.x);
        double max = Math.abs(a.y - b.y);
        if (min > max) {
            temp = min;
            min = max;
            max = temp;
        }
        return ((DIAG - C1) * min + max);
    }

    private void replan() {
        path.clear();

        int res = computeShortestPath();
        if (res < 0) {
            System.out.println("No Path to Goal");
            return;
        }

        LinkedList<Node> n;
        Node cur = s_start;

        if (getG(s_start) == Double.POSITIVE_INFINITY) {
            System.out.println("No Path to Goal");
            return;
        }

        while (cur.neq(s_goal)) {
            path.add(cur);
            n = getSucc(cur);

            if (n.isEmpty()) {
                System.out.println("No Path to Goal");
                return;
            }

            double cmin = Double.POSITIVE_INFINITY;
            double tmin = 0;
            Node smin = new Node();

            for (Node i : n) {
                double val  = cost(cur, i);
                double val2 = trueDist(i,s_goal) + trueDist(s_start, i);
                val += getG(i);
                if (close(val,cmin)) {
                    if (tmin > val2) {
                        tmin = val2;
                        cmin = val;
                        smin = i;
                    }
                } else if (val < cmin) {
                    tmin = val2;
                    cmin = val;
                    smin = i;
                }
            }
            n.clear();
            cur = new Node(smin);
        }
        path.add(s_goal);
    }

    private int computeShortestPath() {
        LinkedList<Node> s;
        if (openList.isEmpty()) {
            return 1;
        }

        int k = 0;
        while ((!openList.isEmpty()) &&
               (openList.peek().lt(s_start = calculateKey(s_start))) ||
               (getRHS(s_start) != getG(s_start))) {

            if (k++ > 80000) {
                System.out.println("At maxsteps");
                return -1;
            }

            Node u;
            boolean test = (getRHS(s_start) != getG(s_start));
            while(true) {
                if (openList.isEmpty()) return 1;
                u = openList.poll();
                if (!isValid(u)) continue;
                if (!(u.lt(s_start)) && (!test)) return 2;
                break;
            }

            openHash.remove(u);
            Node k_old = new Node(u);
            if (k_old.lt(calculateKey(u))) {
                insert(u);
            } else if (getG(u) > getRHS(u)) {
                setG(u,getRHS(u));
                s = getPred(u);
                for (Node i : s) {
                    updateVertex(i);
                }
            } else {
                setG(u, Double.POSITIVE_INFINITY);
                s = getPred(u);
                for (Node i : s) {
                    updateVertex(i);
                }
                updateVertex(u);
            }
        }
        return 0;
    }


    private LinkedList<Node> getSucc(Node u) {
        LinkedList<Node> s = new LinkedList<>();
        Pair<Double, Double> k = new Pair<>(-1.0, -1.0);
        if (occupied(u)) return s;

        s.addFirst(new Node(u.x + C1, u.y, k));
        s.addFirst(new Node(u.x + C1, u.y + C1, k));
        s.addFirst(new Node(u.x, u.y + C1, k));
        s.addFirst(new Node(u.x - 1, u.y + 1, k));
        s.addFirst(new Node(u.x - 1, u.y, new Pair<>(-1.0, -1.0)));
        s.addFirst(new Node(u.x - 1, u.y - 1, k));
        s.addFirst(new Node(u.x, u.y - 1, k));
        s.addFirst(new Node(u.x + 1, u.y - 1, k));

        return s;
    }

    private LinkedList<Node> getPred(Node u) {
        LinkedList<Node> s = new LinkedList<>();
        Node tempNode;

        tempNode = new Node(u.x + 1, u.y, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x + 1, u.y + 1, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x, u.y + 1, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x - 1, u.y + 1, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x - 1, u.y, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x - 1, u.y - 1, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x, u.y - 1, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);
        tempNode = new Node(u.x + 1, u.y - 1, new Pair<>(-1.0, -1.0));
        if (!occupied(tempNode)) s.addFirst(tempNode);

        return s;
    }

    private void updateGoal() {
        ArrayList<Pair<Pose, Double>> toAdd = new ArrayList<>();
        Pair<Pose, Double> tempPoint;
        for (Map.Entry<Node,CellInfo> entry : cellHash.entrySet()) {
            if (!close(entry.getValue().cost, C1)) {
                tempPoint = new Pair<>(new Pose(entry.getKey().x,entry.getKey().y), entry.getValue().cost);
                toAdd.add(tempPoint);
            }
        }
        cellHash.clear();
        openHash.clear();
        while(!openList.isEmpty())
            openList.poll();

        k_m = 0;
        s_goal.x = (double) 3;
        s_goal.y = (double) 2;

        CellInfo tmp = new CellInfo();
        tmp.g = tmp.rhs = 0;
        tmp.cost = C1;

        cellHash.put(s_goal, tmp);
        tmp = new CellInfo();
        tmp.g = tmp.rhs = heuristic(s_start, s_goal);
        tmp.cost = C1;
        cellHash.put(s_start, tmp);
        calculateKey(s_start);
        s_last = s_start;

        for (Pair<Pose, Double> poseDoublePair : toAdd) {
            tempPoint = poseDoublePair;
            updateCell(tempPoint.first().x, tempPoint.first().y, tempPoint.second());
        }
    }

    private void updateVertex(Node u) {
        LinkedList<Node> s;
        if (u.neq(s_goal)) {
            s = getSucc(u);
            double tmp = Double.POSITIVE_INFINITY;
            double tmp2;
            for (Node i : s) {
                tmp2 = getG(i) + cost(u,i);
                if (tmp2 < tmp) tmp = tmp2;
            }
            if (!close(getRHS(u),tmp)) setRHS(u,tmp);
        }
        if (!close(getG(u),getRHS(u))) insert(u);
    }

    private boolean isValid(Node u) {
        if (openHash.get(u) == null) return false;
        return close(keyHashCode(u), openHash.get(u));
    }

    private void setG(Node u, double g) {
        makeNewCell(u);
        cellHash.get(u).g = g;
    }

    private void setRHS(Node u, double rhs) {
        makeNewCell(u);
        cellHash.get(u).rhs = rhs;
    }

    private void makeNewCell(Node u) {
        if (cellHash.get(u) != null) return;
        CellInfo tmp = new CellInfo();
        tmp.g = tmp.rhs = heuristic(u,s_goal);
        tmp.cost = C1;
        cellHash.put(u, tmp);
    }

    private void updateCell(double x, double y, double val) {
        Node u = new Node();
        u.x = x;
        u.y = y;

        if ((u.eq(s_start)) || (u.eq(s_goal))) return;
        makeNewCell(u);
        cellHash.get(u).cost = val;
        updateVertex(u);
    }

    private void insert(Node u) {
        calculateKey(u);
        openHash.put(u, keyHashCode(u));
        openList.add(u);
    }

    private float keyHashCode(Node u) {
        return (float) (u.k.first() + 1193*u.k.second());
    }

    private boolean occupied(Node u) {
        if (cellHash.get(u) == null) {
            return false;
        }
        return (cellHash.get(u).cost < 0);
    }

    private double trueDist(Node a, Node b) {
        double x = a.x - b.x;
        double y = a.y - b.y;
        return Math.sqrt(x*x + y*y);
    }

    private double cost(Node a, Node b) {
        double dX = Math.abs(a.x - b.x);
        double dY = Math.abs(a.y - b.y);
        double scale = 1;
        if (dX + dY > C1) {
            scale = DIAG;
        }

        if (!cellHash.containsKey(a)) {
            return scale * C1;
        }
        return scale * cellHash.get(a).cost;
    }

    private boolean close(double x, double y) {
        if (x == Double.POSITIVE_INFINITY && y == Double.POSITIVE_INFINITY) {
            return true;
        }
        return (Math.abs(x - y) < Math.pow(10, -5));
    }

    public ArrayList<RigidBody> optimalPath(Pose start, Pose goal) {
        init(start.x, start.y, goal.x, goal.y);
        for (Node obstacle : obstacles) {
            updateCell(obstacle.x, obstacle.y, -1);
        }
        replan();
        updateGoal();

        ArrayList<RigidBody> toReturn = new ArrayList<>();
        for (Node node : path) {
            toReturn.add(new RigidBody(new Pose(node.x, node.y)));
        }
        return toReturn;
    }

    public class CellInfo {
        double g = 0;
        double rhs = 0;
        double cost = 0;
    }

}