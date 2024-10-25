package com.thrallmaster.IO;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTFile;

public class NBTExporter
{   
    private Logger logger;
    private NBTFile file;

    public NBTExporter(Plugin plugin)
    {   
        this.logger = plugin.getLogger();
        File worldDir = Bukkit.getWorlds().get(0).getWorldFolder();

        try 
        {
            file = new NBTFile(new File(worldDir, "thrall.dat"));
            this.getDataContainer();

            logger.warning(file.toString());
        } 
        catch (IOException e) 
        {
            logger.warning("Thrall master NBT IO could not be initialized!");
            e.printStackTrace();
        }
    }

    public NBTCompound getDataContainer()
    {
        var nbt = file.addCompound("PlayerData");
        return nbt;
    }

    public void savePlayer(Serializable player)
    {
        var nbt = getDataContainer();
        player.export(nbt);

        System.out.println("Player saved:");
        System.out.println(nbt.toString());

    }

    public void clear() {
        file.clearNBT();
    }
}
