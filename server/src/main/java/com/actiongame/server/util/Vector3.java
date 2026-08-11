package com.actiongame.server.util;

import java.util.Objects;

/**
 * 3D向量 (Y-Up, 与Unity坐标系一致)
 */
public final class Vector3 {
    public static final Vector3 ZERO = new Vector3(0, 0, 0);
    public static final Vector3 ONE = new Vector3(1, 1, 1);
    public static final Vector3 UP = new Vector3(0, 1, 0);
    public static final Vector3 FORWARD = new Vector3(0, 0, 1);
    public static final Vector3 RIGHT = new Vector3(1, 0, 0);

    public final float x;
    public final float y;
    public final float z;

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 multiply(float scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public float sqrMagnitude() {
        return x * x + y * y + z * z;
    }

    public float distance(Vector3 other) {
        return subtract(other).magnitude();
    }

    public Vector3 normalized() {
        float mag = magnitude();
        if (mag < 1e-6f) return ZERO;
        return new Vector3(x / mag, y / mag, z / mag);
    }

    public static float distance(Vector3 a, Vector3 b) {
        return a.distance(b);
    }

    public static Vector3 lerp(Vector3 a, Vector3 b, float t) {
        t = MathUtils.clamp01(t);
        return new Vector3(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector3)) return false;
        Vector3 v = (Vector3) o;
        return Float.compare(v.x, x) == 0
            && Float.compare(v.y, y) == 0
            && Float.compare(v.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}
