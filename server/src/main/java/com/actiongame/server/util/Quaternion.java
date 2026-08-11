package com.actiongame.server.util;

import java.util.Objects;

/**
 * 四元数旋转 (与Unity Quaternion一致, 左手坐标系)
 */
public final class Quaternion {
    public static final Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    public final float x;
    public final float y;
    public final float z;
    public final float w;

    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * 从欧拉角(度)创建四元数
     */
    public static Quaternion fromEuler(float xDeg, float yDeg, float zDeg) {
        float xr = (float) Math.toRadians(xDeg) * 0.5f;
        float yr = (float) Math.toRadians(yDeg) * 0.5f;
        float zr = (float) Math.toRadians(zDeg) * 0.5f;

        float cx = (float) Math.cos(xr), sx = (float) Math.sin(xr);
        float cy = (float) Math.cos(yr), sy = (float) Math.sin(yr);
        float cz = (float) Math.cos(zr), sz = (float) Math.sin(zr);

        return new Quaternion(
            sx * cy * cz - cx * sy * sz,
            cx * sy * cz + sx * cy * sz,
            cx * cy * sz - sx * sy * cz,
            cx * cy * cz + sx * sy * sz
        );
    }

    /**
     * 转换为欧拉角(度)
     */
    public float[] toEuler() {
        float sx = 2 * (w * x + y * z);
        float cx = 1 - 2 * (x * x + y * y);
        float xDeg = (float) Math.toDegrees(Math.atan2(sx, cx));

        float sy = 2 * (w * y - z * x);
        float yDeg;
        if (Math.abs(sy) >= 1f) {
            yDeg = (float) Math.toDegrees(Math.copySign(Math.PI / 2, sy));
        } else {
            yDeg = (float) Math.toDegrees(Math.asin(sy));
        }

        float sz = 2 * (w * z + x * y);
        float cz = 1 - 2 * (y * y + z * z);
        float zDeg = (float) Math.toDegrees(Math.atan2(sz, cz));

        return new float[]{xDeg, yDeg, zDeg};
    }

    /**
     * 四元数乘法 (组合旋转)
     */
    public Quaternion multiply(Quaternion q) {
        return new Quaternion(
            w * q.x + x * q.w + y * q.z - z * q.y,
            w * q.y - x * q.z + y * q.w + z * q.x,
            w * q.z + x * q.y - y * q.x + z * q.w,
            w * q.w - x * q.x - y * q.y - z * q.z
        );
    }

    /**
     * 旋转向量
     */
    public Vector3 rotate(Vector3 v) {
        float qx = x * 2f, qy = y * 2f, qz = z * 2f;
        float ww = w * w;
        float xx = x * qx, yy = y * qy, zz = z * qz;
        float xy = x * qy, xz = x * qz, yz = y * qz;
        float wx = w * qx, wy = w * qy, wz = w * qz;

        return new Vector3(
            v.x * (ww + xx - yy - zz) + v.y * (xy - wz) + v.z * (xz + wy),
            v.x * (xy + wz) + v.y * (ww - xx + yy - zz) + v.z * (yz - wx),
            v.x * (xz - wy) + v.y * (yz + wx) + v.z * (ww - xx - yy + zz)
        );
    }

    public static Quaternion slerp(Quaternion a, Quaternion b, float t) {
        float dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
        if (dot < 0) {
            b = new Quaternion(-b.x, -b.y, -b.z, -b.w);
            dot = -dot;
        }
        if (dot > 0.9995f) {
            return new Quaternion(
                a.x + t * (b.x - a.x),
                a.y + t * (b.y - a.y),
                a.z + t * (b.z - a.z),
                a.w + t * (b.w - a.w)
            ).normalized();
        }
        float theta = (float) Math.acos(dot);
        float sinTheta = (float) Math.sin(theta);
        float t0 = (float) Math.sin((1 - t) * theta) / sinTheta;
        float t1 = (float) Math.sin(t * theta) / sinTheta;
        return new Quaternion(
            t0 * a.x + t1 * b.x,
            t0 * a.y + t1 * b.y,
            t0 * a.z + t1 * b.z,
            t0 * a.w + t1 * b.w
        );
    }

    public Quaternion normalized() {
        float mag = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (mag < 1e-6f) return IDENTITY;
        return new Quaternion(x / mag, y / mag, z / mag, w / mag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quaternion)) return false;
        Quaternion q = (Quaternion) o;
        return Float.compare(q.x, x) == 0
            && Float.compare(q.y, y) == 0
            && Float.compare(q.z, z) == 0
            && Float.compare(q.w, w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f, %.3f, %.3f)", x, y, z, w);
    }
}
