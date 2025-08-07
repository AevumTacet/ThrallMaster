package com.thrallmaster.IO;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import de.tr7zw.nbtapi.iface.NBTFileHandle;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.NBT;

public class NBTExporter {
    private Logger logger;
    private NBTFileHandle file;

    public NBTExporter(Plugin plugin) {
        this.logger = plugin.getLogger();
        File worldDir = Bukkit.getWorlds().get(0).getWorldFolder();

        try {
            file = NBT.getFileHandle(new File(worldDir, "thrall.dat"));
            this.getDataContainer();
        } catch (IOException e) {
            logger.warning("Thrall master NBT IO could not be initialized!");
            e.printStackTrace();
        }
    }

    public ReadWriteNBT getDataContainer() {
        var nbt = file.getOrCreateCompound("PlayerData");
        return nbt;
    }

    public void writePlayer(Serializable player) {
        var nbt = getDataContainer();
        player.export(nbt);
    }

    public void save() {
        try {
            file.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        file.clearNBT();
    }
}
