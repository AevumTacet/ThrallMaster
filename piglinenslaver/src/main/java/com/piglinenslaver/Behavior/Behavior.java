package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.entity.WitherSkeleton;

import com.piglinenslaver.ThrallState;

import net.kyori.adventure.text.Component;

public abstract class Behavior {
    protected WitherSkeleton entity;
    protected ThrallState state;

    public Behavior(WitherSkeleton entity, ThrallState state)
    {
        this.entity = entity;
        this.state = state;

        var textComponent = Component.text("Thrall [" + this.getBehaviorName() + "]");
        entity.customName(textComponent);
    }
    public abstract String getBehaviorName();
    public abstract void onBehaviorStart();
    public abstract void onBehaviorTick();
    public abstract void onBehaviorInteract(Material material);
}
