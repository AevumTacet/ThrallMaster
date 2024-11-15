package com.thrallmaster.Behavior;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.ThrallUtils;
import com.thrallmaster.States.ThrallState;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class HealBehavior extends Behavior {

    private long startTime;
    private Behavior prevBehavior;

    public HealBehavior(UUID entityID, ThrallState state, Behavior prevBehavior) {
        super(entityID, state);
        this.prevBehavior = prevBehavior;
    }

    @Override
    public void onBehaviorStart() 
    {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onBehaviorTick() {
        Skeleton entity = this.getEntity();
        if (entity == null)
        {
            return;
        }

        ItemStack item = entity.getEquipment().getItemInMainHand();
        
        if (state.target != null && state.target.isValid() && ThrallUtils.isThrall(state.target))
        {
            double distance = entity.getLocation().distance(state.target.getLocation());
            
            if (state.target.getHealth() >= state.target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
            {
                state.target = null;
                return;
            }

            entity.lookAt(state.target);

            if (distance <= 1)
            {
                item.setAmount(item.getAmount() - 1);
                entity.getEquipment().setItemInMainHand(item);

                entity.getWorld().spawnParticle(Particle.HEART, state.target.getEyeLocation(), 1);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                state.target.heal(1);
                ((Skeleton) state.target).getPathfinder().moveTo(entity, 0);
                
                entity.swingHand(EquipmentSlot.HAND);
                entity.getPathfinder().moveTo(state.target.getLocation(), 0);
            }
            else
            {
                entity.getPathfinder().moveTo(state.target.getLocation());
            }

        }
        
        long currentTime = System.currentTimeMillis();

        if (!MaterialUtils.isBone(item.getType()) || item.getAmount() == 0)
        {
            state.target = null;
            state.aggressionState = AggressionState.DEFENSIVE;
        }

        if ((state.target == null || !state.target.isValid()) || (currentTime - startTime > 30 * 1000) )
        {
            returnToPreviousState();
            this.startTime = currentTime;
        }
    }

    @Override
    public String getBehaviorName() {
        return "Healing";
    }

    @Override
    public void onBehaviorInteract(Material material) {
        Skeleton entity = this.getEntity();

        if (material == Material.AIR)
        {
            state.aggressionState = AggressionState.DEFENSIVE;
            
            ItemStack currentItem = entity.getEquipment().getItemInMainHand();
            entity.getWorld().dropItemNaturally(entity.getLocation(), currentItem);
            entity.getEquipment().setItemInMainHand(ItemStack.of(Material.AIR, 1));

            returnToPreviousState();
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 0.6f);
        }
    }

    @Override
    public void onBehaviorStuck() 
    {
        prevBehavior.onBehaviorStuck();
        returnToPreviousState();
    }

    public void returnToPreviousState()
    {
        state.setBehavior(prevBehavior);
        state.target = null;
        this.getEntity().setTarget(null);
    }

    @Override
    protected void onSetPersistentData(ReadWriteNBT nbt) {
        if (prevBehavior != null)
        {
            prevBehavior.onSetPersistentData(nbt);
        }
    }
    
    @Override
    protected void onRemovePersistentData(ReadWriteNBT nbt) {
        if (prevBehavior != null)
        {
            prevBehavior.onRemovePersistentData(nbt);
        }
    }
}
