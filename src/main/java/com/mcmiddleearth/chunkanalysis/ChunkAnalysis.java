/*
 * This file is part of ChunkAnalysis.
 * 
 * ChunkAnalysis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ChunkAnalysis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ChunkAnalysis.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */
package com.mcmiddleearth.chunkanalysis;

import com.mcmiddleearth.chunkanalysis.command.ChunkAnalyisisCommandExecutor;
import com.mcmiddleearth.chunkanalysis.util.DevUtil;
import com.mcmiddleearth.chunkanalysis.util.DBUtil;
import com.mcmiddleearth.pluginutil.NumericUtil;
import com.mcmiddleearth.pluginutil.message.MessageUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import me.dags.resourceregions.region.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 *
 * @author donoa_000, Eriol_Eandur
 */
public class ChunkAnalysis extends JavaPlugin {
    
        
    @Getter
    private static int messageStoragePeriod;
    
    @Getter
    private static WorldEditPlugin worldEdit=null;
    
    @Getter
    private static RegionManager regionManager = null;
    
    @Getter
    private static JavaPlugin instance;
    
    @Getter
    private final static MessageUtil messageUtil = new MessageUtil();
    
    private static List<Integer> doubleUpdateBlocks = new ArrayList<>();
    
    @Override
    public void onEnable(){
        instance = this;
        ConfigurationSection config = this.getConfig();
        ConfigurationSection dbConfig = config.getConfigurationSection("db");
        if(dbConfig==null) {
            dbConfig = new MemoryConfiguration();
        }
        messageStoragePeriod = config.getInt("messageStoragePeriod",1);
        //DevUtil.setLevel(2);
        messageUtil.setPluginName("ChunkAnalysis");
        DBUtil.init(dbConfig.getString("ip","localhost"),
                    dbConfig.getInt("port",3306),
                    dbConfig.getString("name","test"),
                    dbConfig.getString("user","test"),
                    dbConfig.getString("password","test"));
        //DBUtil.deleteMessages(messageStoragePeriod);
        MessageManager.deleteOldMessages();
        worldEdit = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
        this.getCommand("block").setExecutor(new ChunkAnalyisisCommandExecutor());
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new MessageManager(), this);
        try {
            Method gi = RegionManager.class.getDeclaredMethod("i");
            gi.setAccessible(true);
            regionManager = (RegionManager) gi.invoke(null);
            if(Bukkit.getPluginManager().getPlugin("MCME-Architect")==null) {
                regionManager = null;
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ChunkAnalysis.class.getName()).log(Level.WARNING, "Error",ex);
            Logger.getLogger(ChunkAnalysis.class.getName()).log(Level.WARNING, "ResourceRegions plugin or MCME Architect plugin not found, '-p' argument not available.");
        }
        this.saveDefaultConfig();
        JobManager.init(config.getInt("tps",15));
        loadConfig();
    }
    
    @Override
    public void onDisable(){
        if(JobManager.isSchedulerTaskRunning()) {
            JobManager.getJobScheduler().setDisable(true);
            DevUtil.log("Disabling async job scheduler.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ChunkAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(JobManager.isSchedulerTaskRunning()) {
                DevUtil.log("Failed to disable async job scheduler, please restart the server.");
            }
        }
    }
    
    public Vector getMinBlock(World map) {
        return getConfigVector(map,"minBlock");
    }
    
    public Vector getMaxBlock(World map) {
        return getConfigVector(map,"maxBlock");
    }
    
    private Vector getConfigVector(World map, String key) {
        ConfigurationSection section = this.getConfig().getConfigurationSection("worlds");
        if(section == null) {
            return new Vector(0,0,0);
        }
        section = section.getConfigurationSection(map.getName());
        if(section == null) {
          return new Vector(0,0,0);
        }
        String[] coords = section.getString(key).split(",");
        return new Vector(NumericUtil.getInt(coords[0]),
                          0,
                          NumericUtil.getInt(coords[1]));
    }
    
    public static boolean needsDoubleUpdate(BlockState state) {
        for(Integer type: doubleUpdateBlocks) {
            if(type == state.getTypeId()) {
                return true;
            }
        }
        return false;
    }
    
    public void loadConfig() {
        doubleUpdateBlocks = this.getConfig().getIntegerList("doubleUpdateBlocks");
        if(doubleUpdateBlocks == null) {
            doubleUpdateBlocks = new ArrayList<>();
        }
        JobManager.setTps(this.getConfig().getInt("tps",15));
        
    }
    
    public static void reloadConfiguration() {
        ((ChunkAnalysis)ChunkAnalysis.getInstance()).loadConfig();
    }
}
