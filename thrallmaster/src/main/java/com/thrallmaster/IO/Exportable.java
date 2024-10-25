package com.thrallmaster.IO;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public interface Exportable 
{
    public void export(NBTCompound nbt);    
}
