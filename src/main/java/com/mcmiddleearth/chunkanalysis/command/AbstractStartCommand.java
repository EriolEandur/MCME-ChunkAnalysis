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

import com.mcmiddleearth.architect.PluginData;
import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import com.mcmiddleearth.chunkanalysis.JobManager;
import com.mcmiddleearth.chunkanalysis.MessageManager;
import com.mcmiddleearth.chunkanalysis.job.PolyJob;
import com.mcmiddleearth.chunkanalysis.job.action.JobAction;
import com.mcmiddleearth.chunkanalysis.util.DBUtil;
import com.mcmiddleearth.pluginutil.NumericUtil;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.dags.resourceregions.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class AbstractStartCommand extends AbstractCommand {

    public AbstractStartCommand(int minArguments, boolean playerOnly, String... permissionNodes) {
        super(minArguments, playerOnly, permissionNodes);
    }
    
    @Override
    protected void execute(CommandSender cs, String... argz) {
//Logger.getGlobal().info("excecute AbstactStartCommand");
        List<String> args = Arrays.asList(argz);
        if(args.contains("-f")) {
            int index = args.indexOf("-f");
            String filename = "input.txt";
            if(index+1<args.size()) {
                filename = args.get(index+1);
            }
            String input = getInputFromFile(filename);
            args = new ArrayList(args);
            args.addAll(Arrays.asList(input.split(" ")));
//Logger.getGlobal().info("file input added AbstactStartCommand");
            
            /*for(String str: data) {
                args.add(str);
            }*/
        }
        if(!DBUtil.checkConnection() && !args.contains("-o")) {
            ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "No database connection. If the server crashes while the job is running it will not be continued. To start the job anyways at '-o' argument at own risk.");
            return;
        }
        /*if(args.contains("-m") && args.contains("-p")){
            ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Cannot have both map and pack");
            return;
        }*/
        if(!args.contains("-m") && !args.contains("-p") && !(cs instanceof Player)){
            ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Cannot use selection with console!");
            return;
        }
        if(args.contains("-p")) {
            if(ChunkAnalysis.getRegionManager()==null) {
                ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "ResourceRegions plugin or MCME Architect plugin not found. Argument '-p' not available");
                return;
            } else {
                int i = args.indexOf("-p");
                if(args.size()<i+2) {
                    sendMissingArgumentErrorMessage(cs);
                    return;
                }
                String packUrl = PluginData.getRpUrl(PluginData.matchRpName(args.get(i+1)));
                if(packUrl.equals("")) {
                    ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Unknown resource pack.");
                    return;
                }
                Map<World,List<Polygonal2DSelection>> areas = getPackAreas(packUrl);
                World map = null;
                if(args.contains("-m") ){
                    map=getMap(cs,args);
                    if(map==null) {
                        return;
                    }
                }
                for(World world: areas.keySet()) {
                    if(map==null || map.equals(world)) {
                        startPolyJob(areas.get(world), (Player) cs, args);
                    }
                }
                return;
            }
        }
        if(args.contains("-m")) {
            World map = getMap(cs, args);
            if(map==null) {
                return;
            }
            Vector minChunk = ((ChunkAnalysis)ChunkAnalysis.getInstance()).getMinBlock(map);
            Vector maxChunk = ((ChunkAnalysis)ChunkAnalysis.getInstance()).getMaxBlock(map);
            if(maxChunk==null || minChunk==null) {
                ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Configuration for this world is missing.");
                return;
            }
            minChunk.setY(0);
            maxChunk.setY(map.getMaxHeight()-1);
            startCuboidJob(new CuboidSelection(map, minChunk.toLocation(map), maxChunk.toLocation(map)),
                           ((Player)cs), args);
            return;
        }
        Selection sel = ChunkAnalysis.getWorldEdit().getSelection((Player) cs);
        if(sel instanceof CuboidSelection) {
//Logger.getGlobal().info("startCuboidJob AbstactStartCommand");
            startCuboidJob((CuboidSelection)sel, ((Player)cs), args);
        } else if(sel instanceof Polygonal2DSelection) {
            List<Polygonal2DSelection> area = new ArrayList<>();
            area.add((Polygonal2DSelection)sel);
            startPolyJob(area, ((Player)cs), args);
        }
    }

    private void startCuboidJob(CuboidSelection sel, Player player, List<String> args) {
        List<BlockVector2D> points= new ArrayList<>();
        points.add(new BlockVector2D(sel.getMinimumPoint().getBlockX(),
                                     sel.getMinimumPoint().getBlockZ()));
        points.add(new BlockVector2D(sel.getMinimumPoint().getBlockX(),
                                     sel.getMaximumPoint().getBlockZ()));
        points.add(new BlockVector2D(sel.getMaximumPoint().getBlockX(),
                                     sel.getMaximumPoint().getBlockZ()));
        points.add(new BlockVector2D(sel.getMaximumPoint().getBlockX(),
                                     sel.getMinimumPoint().getBlockZ()));
        Polygonal2DSelection selection = new Polygonal2DSelection(sel.getWorld(),
                                       points,sel.getMinimumPoint().getBlockY(),
                                              sel.getMaximumPoint().getBlockY());
        List<Polygonal2DSelection> regions = new ArrayList<>();
        regions.add(selection);
        startPolyJob(regions, player, args);
        /*
Logger.getGlobal().info("get action startCuboidJob");
        JobAction action = getAction(args);
Logger.getGlobal().info("action constructed startCuboidJob");
        if(action!=null) {
            JobManager.addJob(new CuboidJob(player.getUniqueId(),selection,action));
Logger.getGlobal().info("job added startCuboidJob");
            MessageManager.addListeningPlayer(player.getUniqueId());
            ChunkAnalysis.getMessageUtil().sendInfoMessage(player, "Cuboid job queued.");
        } else {
            sendInvalidActionArguments(player);
        }*/
    }
    
    private World getMap(CommandSender cs, List<String> args) {
        int i = args.indexOf("-m");
        World map = null;
        if(args.size()>=i+2) {
            map = Bukkit.getWorld(args.get(i+1));
            if(map==null) {
                ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "World not found.");
                return null;
            }
        }
        if(map==null) {
            if(cs instanceof Player) {
                map = ((Player) cs).getWorld();
            } else {
                ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "You need to specify the world name when using argument '-m' from console.");
                return null;
            }
        }
        return map;
    }
    
    private void startPolyJob(List<Polygonal2DSelection> selections, Player player, List<String> args) {
        JobAction action = getAction(args);
        if(action!=null) {
            JobManager.addJob(new PolyJob(player.getUniqueId(),selections,action));
            MessageManager.addListeningPlayer(player.getUniqueId());
            ChunkAnalysis.getMessageUtil().sendInfoMessage(player, "Polygonal job queued.");
        } else {
            sendInvalidActionArguments(player);
        }
    }
    
    abstract protected JobAction getAction(List<String> args);
    
    protected int getId(String data) {
        if(data.contains(":")) {
            return NumericUtil.getInt(data.split(":")[0]);
        } else {
            return NumericUtil.getInt(data);
        }
    }
    
    protected int getDv(String data) {
        if(data.contains(":")) {
            if(NumericUtil.isInt(data.split(":")[1])) {
                return NumericUtil.getInt(data.split(":")[1]);
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    protected String getInputFromFile(String filename) {
        try(Scanner fr = new Scanner(new File(ChunkAnalysis.getInstance().getDataFolder(),filename))) {
            return fr.nextLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractStartCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    private Map<World,List<Polygonal2DSelection>> getPackAreas(String packUrl) {
        Map<World,List<Polygonal2DSelection>> areas = new HashMap<>();
        Set<Region> regions = (Set<Region>) forceGet(ChunkAnalysis.getRegionManager(),"regions");
        for(Region region: regions) {
            if(region.getPackUrl().equalsIgnoreCase(packUrl)) {
                int[] xPoints = (int[]) forceGet(region,"xpoints");
                int[] zPoints = (int[]) forceGet(region,"zpoints");
                World world = Bukkit.getWorld((String) forceGet(region, "worldName"));
                if(world !=null) {
                    List<Polygonal2DSelection> mapAreas = areas.get(world);
                    if(mapAreas == null) {
                        mapAreas = new ArrayList<>();
                        areas.put(world, mapAreas);
                    }
                    List<BlockVector2D> points = new ArrayList<>();
                    for(int i=0; i<xPoints.length;i++) {
                        points.add(new BlockVector2D(xPoints[i], zPoints[i]));
                    }
                    Polygonal2DSelection polygon = new Polygonal2DSelection(world, points, 0, world.getMaxHeight()-1);
                    mapAreas.add(polygon);
                }
            }
        }
        return areas;
    }
    
    private Object forceGet(Object o, String name){
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(ChunkAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private void sendInvalidActionArguments(Player player) {
        ChunkAnalysis.getMessageUtil().sendErrorMessage(player, "Invalid Arguments");
    }
}
