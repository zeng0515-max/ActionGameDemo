package com.actiongame.server.config;

import com.actiongame.server.domain.combat.ElementType;

/**
 * 元素配置 (对应Unity ElementData ScriptableObject)
 * 从JSON加载, 服务端权威
 */
public class ElementConfig {
    private ElementType elementType = ElementType.NONE;
    private ElementType counterElement = ElementType.NONE;
    private float counterMultiplier = 1.5f;
    private String elementName = "";

    public ElementType getElementType() { return elementType; }
    public void setElementType(ElementType elementType) { this.elementType = elementType; }
    public ElementType getCounterElement() { return counterElement; }
    public void setCounterElement(ElementType counterElement) { this.counterElement = counterElement; }
    public float getCounterMultiplier() { return counterMultiplier; }
    public void setCounterMultiplier(float counterMultiplier) { this.counterMultiplier = counterMultiplier; }
    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }

    @Override
    public String toString() {
        return "ElementConfig{type=" + elementType + ", counters=" + counterElement + ", mult=" + counterMultiplier + "}";
    }
}
