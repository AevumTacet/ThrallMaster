package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.entity.Piglin;

import com.piglinenslaver.PiglinState;

import net.kyori.adventure.text.Component;

public abstract class Behavior {
    protected Piglin piglin;
    protected PiglinState state;

    public Behavior(Piglin piglin, PiglinState state)
    {
        this.piglin = piglin;
        this.state = state;

        var textComponent = Component.text("Slave [" + this.getBehaviorName() + "]");
        piglin.customName(textComponent);
    }
    public abstract String getBehaviorName();
    public abstract void onBehaviorStart();
    public abstract void onBehaviorTick();
    public abstract void onBehaviorInteract(Material material);
}
