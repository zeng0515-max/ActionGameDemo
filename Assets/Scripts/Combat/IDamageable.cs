namespace ActionGameDemo.Combat
{
    public interface IDamageable
    {
        void TakeDamage(DamageInfo damageInfo);

        /// <summary>获取角色的元素属性（用于元素克制计算）</summary>
        ElementType GetElementType();
    }
}
