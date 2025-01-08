package com.thrallmaster.Utils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.States.ThrallState;

public final class InteractionUtils {
	private static ThrallManager manager = Main.manager;

	private InteractionUtils() {
	}

	public static boolean equipThrall(LivingEntity entity, ItemStack item) {
		ThrallState state = manager.getThrall(entity.getUniqueId());
		World world = entity.getWorld();
		Material material = item.getType();
		EntityEquipment equipment = entity.getEquipment();

		if (MaterialUtils.isWeapon(material)) {
			world.dropItemNaturally(entity.getLocation(), equipment.getItemInMainHand());
			equipment.setItemInMainHand(item);

			if (state.aggressionState == AggressionState.HEALER) {
				state.aggressionState = AggressionState.DEFENSIVE;
			}

			if (MaterialUtils.isRanged(material)) {
				if (MaterialUtils.isShield(equipment.getItemInOffHand().getType())) {
					world.dropItemNaturally(entity.getLocation(), equipment.getItemInOffHand());
					equipment.setItemInOffHand(null);
				}
			}
			return true;
		} else if (MaterialUtils.isArmor(material)) {
			switch (MaterialUtils.getArmorType(material)) {
				case HELMET:
					world.dropItemNaturally(entity.getLocation(), equipment.getHelmet());
					equipment.setHelmet(item);
					return true;

				case CHESTPLATE:
					world.dropItemNaturally(entity.getLocation(), equipment.getChestplate());
					equipment.setChestplate(item);
					return true;

				case LEGGINGS:
					world.dropItemNaturally(entity.getLocation(), equipment.getLeggings());
					equipment.setLeggings(item);
					return true;

				case BOOTS:
					world.dropItemNaturally(entity.getLocation(), equipment.getBoots());
					equipment.setBoots(item);
					return true;

				default:
					return false;

			}
		} else if (MaterialUtils.isBone(material)) {
			ItemStack currentItem = equipment.getItemInMainHand();

			if (!MaterialUtils.isBone(currentItem.getType())) {
				world.dropItemNaturally(entity.getLocation(), currentItem);
				equipment.setItemInMainHand(item);
			} else {
				equipment.setItemInMainHand(currentItem.add(item.getAmount()));
			}

			state.aggressionState = AggressionState.HEALER;
			return true;
		} else if (MaterialUtils.isShield(material)) {
			// Check if its not a ranger
			if (MaterialUtils.isRanged(equipment.getItemInMainHand().getType())) {
				return false;
			}

			ItemStack currentItem = equipment.getItemInOffHand();
			world.dropItemNaturally(entity.getLocation(), currentItem);

			equipment.setItemInOffHand(item);
			return true;
		}
		return false;
	}
}
