package com.thrallmaster.IO;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.thrallmaster.Main;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTFile;

public class ThrallSaver
{   
    private static final String THRALLS = "ThrallStates";
    private static final String ALLIES = "Whitelists";
    private static ThrallSaver saver;

    private Logger logger;
    private NBTFile file;

    public ThrallSaver(Plugin plugin)
    {   
        this.logger = plugin.getLogger();
        File worldDir = Bukkit.getWorlds().get(0).getWorldFolder();

        try 
        {
            file = new NBTFile(new File(worldDir, "thrall.dat"));
            file.addCompound(THRALLS);
            file.addCompound(ALLIES);
        } 
        catch (IOException e) 
        {
            logger.warning("Thrall master NBT IO could not be initialized!");
            e.printStackTrace();
        }

        saver = this;
    }
    

    public static NBTCompound getThrall(UUID entityID)
    {
        return saver.addCompound(entityID, THRALLS);
    }
    public static NBTCompound getAlly(UUID playerID)
    {
        return saver.addCompound(playerID, ALLIES);
    }

    public static void removeThrall(UUID entityID)
    {
        saver.removeCompound(entityID, THRALLS);
    }

    public static Stream<NBTCompound> getThralls()
    {
        return saver.getStream(THRALLS);
    }
    public static Stream<NBTCompound> getAllies()
    {
        return saver.getStream(ALLIES);
    }

    public static int getThrallCount()
    {
        return saver.getSize(THRALLS);
    }


    private NBTCompound addCompound(UUID id, String container)
    {
        NBTCompound states = file.getCompound(container);
        return states.addCompound(id.toString());
    }
    
    private void removeCompound(UUID id, String container)
    {
        NBTCompound states = file.getCompound(container);
        states.removeKey(id.toString());
    }

    private Stream<NBTCompound> getStream(String container)
    {
        NBTCompound states = file.getCompound(container);
        return states.getKeys().stream().map(key -> states.getCompound(key));
    }

    private int getSize(String container)
    {
        NBTCompound states = file.getCompound(container);
        return states.getKeys().size();
    }

    public static int save()
    {
        try 
        {
            var count = saver.file.getCompound("ThrallStates").getKeys().size();
            saver.file.save();
            return count;
        } 
        catch (IOException e) 
        {
            saver.logger.warning("Could not save NBT settings!");
            e.printStackTrace();
            return 0;    
        }
    }
}
