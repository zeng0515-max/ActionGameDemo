package com.actiongame.server.domain.combat;

/**
 * 命中结果
 */
public class HitResult {
    private final int targetEntityId;
    private final boolean hit;
    private final float damage;
    private final boolean critical;
    private final ElementType element;

    public HitResult(int targetEntityId, boolean hit, float damage, boolean critical, ElementType element) {
        this.targetEntityId = targetEntityId;
        this.hit = hit;
        this.damage = damage;
        this.critical = critical;
        this.element = element;
    }

    public static HitResult miss(int targetEntityId) {
        return new HitResult(targetEntityId, false, 0f, false, ElementType.NONE);
    }

    public static HitResult hit(int targetEntityId, float damage, boolean critical, ElementType element) {
        return new HitResult(targetEntityId, true, damage, critical, element);
    }

    public int getTargetEntityId() { return targetEntityId; }
    public boolean isHit() { return hit; }
    public float getDamage() { return damage; }
    public boolean isCritical() { return critical; }
    public ElementType getElement() { return element; }
}
