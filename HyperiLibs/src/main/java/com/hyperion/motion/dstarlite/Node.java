package com.hyperion.motion.dstarlite;

public class Node {

    public double x = 0;
    public double y = 0;
    public Pair<Double, Double> k = new Pair<>(0.0,0.0);

    public Node() {

    }

    public Node(double x, double y, Pair<Double,Double> k) {
        this.x = x;
        this.y = y;
        this.k = k;
    }

    public Node(Node other) {
        this.x = other.x;
        this.y = other.y;
        this.k = other.k;
    }

    public boolean eq(final Node s2) {
        return ((this.x == s2.x) && (this.y == s2.y));
    }

    public boolean neq(final Node s2) {
        return ((this.x != s2.x) || (this.y != s2.y));
    }

    public boolean gt(final Node s2) {
        if (k.first()-0.00001 > s2.k.first()) return true;
        else if (k.first() < s2.k.first()-0.00001) return false;
        return k.second() > s2.k.second();
    }

    public boolean lte(final Node s2) {
        if (k.first() < s2.k.first()) return true;
        else if (k.first() > s2.k.first()) return false;
        return k.second() < s2.k.second() + 0.00001;
    }

    public boolean lt(final Node s2) {
        if (k.first() + 0.000001 < s2.k.first()) return true;
        else if (k.first() - 0.000001 > s2.k.first()) return false;
        return k.second() < s2.k.second();
    }

    public int compareTo(Object that) {
        Node other = (Node)that;
        if (k.first()-0.00001 > other.k.first()) return 1;
        else if (k.first() < other.k.first()-0.00001) return -1;
        if (k.second() > other.k.second()) return 1;
        else if (k.second() < other.k.second()) return -1;
        return 0;
    }

    @Override
    public int hashCode() {
        return (int) Math.round(this.x + 34245*this.y);
    }

    @Override
    public boolean equals(Object aThat) {
        if (this == aThat) return true;
        if (!(aThat instanceof Node)) return false;
        Node that = (Node)aThat;
        return this.x == that.x && this.y == that.y;
    }

}
