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
package com.mcmiddleearth.chunkanalysis.command;

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import com.mcmiddleearth.pluginutil.NumericUtil;
import java.util.Comparator;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author Eriol_Eandur
 */
public class ConfigCommand extends AbstractCommand {

    public ConfigCommand(String... permissionNodes) {
        super(0, false, permissionNodes);
        setShortDescription(": Configure ChunkAnalysis.");
        setUsageDescription("<world | tps | doubleUpdate>: Configure ChunkAnalysis.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        FileConfiguration config = ChunkAnalysis.getInstance().getConfig();
        if(args.length<1) {
            ChunkAnalysis.getMessageUtil().sendNotEnoughArgumentsError(cs);
        } else if(args[0].equalsIgnoreCase("tps")) {
            if(args.length<2) {
                ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "Current tps setting: "+config.getString("tps"));
            } else if(NumericUtil.isInt(args[1])){
                config.set("tps", Math.max(10, NumericUtil.getInt(args[1])));
                ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "tps set to: "+config.getString("tps"));
                ChunkAnalysis.getInstance().saveConfig();
                ChunkAnalysis.reloadConfiguration();
            } else {
                ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "Invalid argument.");
            }
        } else if(args[0].equalsIgnoreCase("world")) {
            ConfigurationSection section = config.getConfigurationSection("worlds");
            if(args.length<7) {
                ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "Map Boundaries:");
                for(String key: section.getKeys(false)) {
                    ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, key+": "
                                                                        +section.getString(key+".minBlock") + " to "
                                                                        +section.getString(key+".maxBlock"));
                }
            } else {
                ConfigurationSection worldSection = section.getConfigurationSection(args[1]);
                if(worldSection==null) {
                    worldSection = section.createSection(args[1]);
                }
                worldSection.set("minBlock", args[2]+","+args[3]);
                worldSection.set("maxBlock", args[4]+","+args[5]);
                ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "New boudaries for "+args[1]+": "
                                                                    +section.getString(args[1]+".minBlock") + " to "
                                                                    +section.getString(args[1]+".maxBlock"));
                ChunkAnalysis.getInstance().saveConfig();
                ChunkAnalysis.reloadConfiguration();
            }
        } else  if(args[0].equalsIgnoreCase("doubleUpdate")) {
            List<Integer> data = config.getIntegerList("doubleUpdateBlocks");
            if(args.length<3) {
                String message = "Double update blocks:";
                for(Integer block: data) {
                    message = message + " "+block;
                }
                ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, message);
            } else if(NumericUtil.isInt(args[2])) {
                if(args[1].equalsIgnoreCase("add")) {
                    if(!data.contains(NumericUtil.getInt(args[2]))) {
                        data.add(NumericUtil.getInt(args[2]));
                        data.sort(null);
                        config.set("doubleUpdateBlocks", data);
                        ChunkAnalysis.getInstance().saveConfig();
                        ChunkAnalysis.reloadConfiguration();
                    }
                    ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "Added to double update blocks: "+args[2]);
                } else if(args[1].equalsIgnoreCase("remove")){
                    data.remove(new Integer(NumericUtil.getInt(args[2])));
                    config.set("doubleUpdateBlocks", data);
                    ChunkAnalysis.getInstance().saveConfig();
                    ChunkAnalysis.reloadConfiguration();
                    ChunkAnalysis.getMessageUtil().sendInfoMessage(cs, "Removed from double update blocks: "+args[2]);
                } else {
                    ChunkAnalysis.getMessageUtil().sendInvalidSubcommandError(cs);
                }                
            } else {
                ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Invalid Argument");
            }
        } else {
            ChunkAnalysis.getMessageUtil().sendInvalidSubcommandError(cs);
        }
    }   
    
}
