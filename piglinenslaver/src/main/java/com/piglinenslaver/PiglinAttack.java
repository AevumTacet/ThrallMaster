package com.piglinenslaver;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.HashMap;
import java.util.UUID;

public class PiglinAttack implements Listener {
    
    public enum AggressionState {
        DEFENSIVE,
        HOSTILE
    }
    
    private HashMap<UUID, AggressionState> aggressionMap = new HashMap<>(); // Estado de agresión de cada Piglin
    private HashMap<UUID, Player> owners = new HashMap<>(); // Mapa de dueños de los Piglins

    // Método para establecer el dueño de un Piglin
    public void setOwner(Piglin piglin, Player owner) {
        owners.put(piglin.getUniqueId(), owner);
        aggressionMap.put(piglin.getUniqueId(), AggressionState.DEFENSIVE); // Por defecto, en estado DEFENSIVE
    }
    
    // Evento para manejar ataques a piglins o a su dueño
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity attacker = event.getDamager();

        // Si el dañado es un Piglin domesticado
        if (damaged instanceof Piglin) {
            Piglin piglin = (Piglin) damaged;
            Player owner = owners.get(piglin.getUniqueId());

            // Si el Piglin está domesticado y el atacante no es el dueño ni otro Piglin del mismo dueño
            if (owner != null && attacker != owner && !(attacker instanceof Piglin && isSameOwner((Piglin) attacker, owner))) {
                // El Piglin atacará al atacante en estado DEFENSIVE
                if (aggressionMap.get(piglin.getUniqueId()) == AggressionState.DEFENSIVE) {
                    piglin.setTarget((LivingEntity) attacker);
                }
            }
        }
        
        // Si el dañado es el dueño del Piglin
        if (damaged instanceof Player) {
            Player player = (Player) damaged;

            for (UUID piglinId : owners.keySet()) {
                if (owners.get(piglinId).equals(player)) {
                    Piglin piglin = (Piglin) getEntityByUniqueId(piglinId);
                    if (piglin != null) {
                        // Si el atacante no es otro Piglin domesticado por el mismo dueño
                        if (!(attacker instanceof Piglin && isSameOwner((Piglin) attacker, player))) {
                            // El Piglin defendiendo al dueño
                            piglin.setTarget((LivingEntity) attacker);
                        }
                    }
                }
            }
        }
    }
    
    // Método para verificar si dos Piglins tienen el mismo dueño
    private boolean isSameOwner(Piglin piglin, Player owner) {
        return owners.get(piglin.getUniqueId()) != null && owners.get(piglin.getUniqueId()).equals(owner);
    }

    // Método para obtener una entidad por su UUID
    private Entity getEntityByUniqueId(UUID uniqueId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uniqueId)) {
                    return entity;
                }
            }
        }
        return null;
    }
}
