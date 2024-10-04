package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.entity.Skeleton;

import com.piglinenslaver.ThrallState;
import net.kyori.adventure.text.Component;


public abstract class Behavior {
    protected Skeleton entity;
    protected ThrallState state;

    public Behavior(Skeleton entity, ThrallState state)
    {
        this.entity = entity;
        this.state = state;
    }
    public abstract String getBehaviorName();
    public abstract void onBehaviorStart();
    public abstract void onBehaviorTick();
    public abstract void onBehaviorInteract(Material material);

    public void setEntityName()
    {
        var textComponent = Component.text("Thrall [" + this.getBehaviorName() + "]");
        entity.customName(textComponent);
    }
}
