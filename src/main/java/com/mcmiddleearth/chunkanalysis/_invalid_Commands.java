/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.chunkanalysis;

//import com.mcmiddleearth.chunkanalysis.job.CuboidJob;
import com.mcmiddleearth.chunkanalysis.job.action.JobActionReplace;
import com.mcmiddleearth.chunkanalysis.util.DevUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.dags.resourceregions.region.Region;
import me.dags.resourceregions.region.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Eriol_Eandur
 */
public class _invalid_Commands extends JavaPlugin implements CommandExecutor{

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] argz) {
        RegionManager regionManager = ChunkAnalysis.getRegionManager();
        WorldEditPlugin worldEdit = ChunkAnalysis.getWorldEdit();
        final Point minChunk = new Point();
        final Point maxChunk = new Point();
        List<String> args = Arrays.asList(argz);
        if(args.size() > 0){
            if(args.get(0).equalsIgnoreCase("dev")) {
                if(argz.length>1 && argz[1].equalsIgnoreCase("true")) {
                    DevUtil.setConsoleOutput(true);
                    showDetails(sender);
                    return true;
                }
                else if(argz.length>1 && argz[1].equalsIgnoreCase("false")) {
                    DevUtil.setConsoleOutput(false);
                    showDetails(sender);
                    return true;
                }
                else if(argz.length>1) {
                    try {
                        int level = Integer.parseInt(argz[1]);
                        DevUtil.setLevel(level);
                        showDetails(sender);
                        return true;
                    }
                    catch(NumberFormatException e){};
                }
                if(sender instanceof Player) {
                    Player player = (Player) sender;
                    if(argz.length>1 && argz[1].equalsIgnoreCase("r")) {
                        DevUtil.remove(player);
                        showDetails(sender);
                        return true;
                    }
                    DevUtil.add(player);
                    showDetails(sender);
                }
                return true;
            }
            if(args.get(0).equalsIgnoreCase("suspend")) {
                JobManager.getJobScheduler().setSuspended(true);
                return true;
            }
            if(args.get(0).equalsIgnoreCase("resume")) {
                JobManager.getJobScheduler().setSuspended(false);
                return true;
            }
            if(args.get(0).equalsIgnoreCase("cancel")) {
                JobManager.getJobScheduler().setCancel(true);
                return true;
            }
            if(args.get(0).equalsIgnoreCase("tps") && args.size()>1) {
                JobManager.getJobScheduler().setServerTps(Integer.parseInt(args.get(1)));
                return true;
            }
            if(args.get(0).equalsIgnoreCase("i") && args.size()>1) {
                JobManager.getJobScheduler().setI(Float.parseFloat(args.get(1)));
                return true;
            }
            if(args.get(0).equalsIgnoreCase("d") && args.size()>1) {
                JobManager.getJobScheduler().setD(Float.parseFloat(args.get(1)));
                return true;
            }
            if(args.get(0).equalsIgnoreCase("v") && args.size()>1) {
                JobManager.getJobScheduler().setV(Float.parseFloat(args.get(1)));
                return true;
            }
            if(args.get(0).equalsIgnoreCase("analyse") && args.size() > 1){
                final Material   block;
                final byte data;
                String tags = null;
                boolean clip = false;
                String pack = null;
                if(args.get(1).contains(":")){
                    String[] bd = args.get(1).split(":");
                    block = Material.getMaterial(bd[0]);
                    data = Byte.parseByte(bd[1]);
                }else{
                    block = Material.getMaterial(args.get(1));
                    data = (byte) 0;
                }
                if(args.contains("-t")){
                    if(args.contains("-s") && args.contains("-p")){
                        int i = args.indexOf("-t");
                        tags = args.get(i+1);
                    }else{
                        sender.sendMessage("Cannot search whole map with block flags");
                        return true;
                    }
                }
                if(args.contains("-s") && args.contains("-p")){
                    sender.sendMessage("Cannot have both clip and pack");
                    return true;
                }
                if(args.contains("-s")){
                    if(sender instanceof Player){
                        clip = true;
                    }else{
                        sender.sendMessage("Cannot use selection with console!");
                        return true;
                    }
                }else if(args.contains("-p")){
                    if(sender instanceof Player && regionManager != null){
                        int i = args.indexOf("-p");
                        pack = args.get(i+1);
                    }else if(regionManager == null){
                        sender.sendMessage("Internal error: could not locate resource regions plugin data");
                        return true;
                    }else{
                        sender.sendMessage("Cannot use pack param with console!");
                        return true;
                    }
                }
                sender.sendMessage("Query Results:");
                sender.sendMessage("==============");
                final String t = tags;
                Runnable r;
                if(clip){
                    final Selection sel = worldEdit.getSelection((Player) sender);
                    r = new Runnable(){
                        @Override
                        public void run(){
                            int count = 0;
                            for(int x = sel.getMinimumPoint().getBlockX(); x <= sel.getMaximumPoint().getBlockX(); x++){
                                for(int y = sel.getMinimumPoint().getBlockY(); y <= sel.getMaximumPoint().getBlockY(); y++){
                                    for(int z = sel.getMinimumPoint().getBlockZ(); z <= sel.getMaximumPoint().getBlockZ(); z++){
                                        Block b = new Location(sel.getWorld(), x, y, z).getBlock();
                                        if(b.getType().equals(block) && b.getData() == data && (t != null ? checkFlags(t, b) : true)){
                                            sender.sendMessage(" - ("+x+","+y+","+z+")");
                                            count++;
                                            Thread.yield();
                                        }
                                    }
                                }
                            }
                            sender.sendMessage(count + " results found");
                        }
                    };

                }else if(pack != null){
                    Region reg = null;
                    for(Region re : (Set<Region>) forceGet(regionManager, "regions")){
                        if(re.getName().equalsIgnoreCase(pack)){
                            reg=re;
                            break;
                        }
                    }
                    if(reg==null){
                        sender.sendMessage("Pack not found!");
                        return true;
                    }
                    int n = (int) forceGet(regionManager, "n");
                    int[] xpoints = (int[]) forceGet(regionManager, "xpoints");
                    int[] zpoints = (int[]) forceGet(regionManager, "zpoints");
                    final Rectangle bounds = new Polygon(xpoints, zpoints, n).getBounds();
                    r = new Runnable(){
                        @Override
                        public void run(){
                            int count = 0;
                            for(int x = bounds.x; x < bounds.x + bounds.width; x++){
                                for(int z = bounds.y; z < bounds.y + bounds.height; z++){
                                    for(int y = 0; y < 255; y++){
                                        Block b = new Location(((Player) sender).getWorld(), x, y, z).getBlock();
                                        if(b.getType().equals(block) && b.getData() == data && (t != null ? checkFlags(t, b) : true)){
                                            sender.sendMessage(" - ("+x+","+y+","+z+")");
                                            count++;
                                            Thread.yield();
                                        }
                                    }
                                }
                            }
                            sender.sendMessage(count + "results found");
                        }
                    };
                }else{
                    final World w = ((Player) sender).getWorld();
                    r = new Runnable(){
                        @Override
                        public void run(){
                            int count = 0;
                            for(int cx = minChunk.x; cx <= maxChunk.x; cx++){
                                for(int cy = minChunk.y; cy <= maxChunk.y; cy++){
                                    ChunkSnapshot c = w.getChunkAt(cx, cy).getChunkSnapshot(true, false, false);
                                    for(int x = 0; x <= 15; x++){
                                        for(int y = 0; y <= 127; y++){
                                            for(int z = 0; z <= 15; z++){
                                                if(c.getBlockTypeId(x, y, z) == block.getId() && c.getBlockData(x, y, z) == data){
                                                    sender.sendMessage(" - ("+(cx*16+x)+","+y+","+(cy*16+z)+")");
                                                    count++;
                                                    Thread.yield();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            sender.sendMessage(count + " results found");
                        }
                    };
                }
                Bukkit.getScheduler().scheduleAsyncDelayedTask(this, r);
            }else if(args.get(0).equalsIgnoreCase("replace")){
                final Material blockFind;
                final byte dataFind;
                String tagsFind = null;
                final Material blockReplace;
                final byte dataReplace;
                String tagsReplace = null;
                boolean clip = false;
                String pack = null;
                boolean sw = false;
                /*int j = 2;
                for(;j<args.size();j+=2){
                    if(!args.get(j).contains("-")){
                        break;
                    }
                }
                List<String> findArgs = args.subList(1, j);
                List<String> replaceArgs = args.subList(j, args.size());
                if(findArgs.get(1).contains(":")){
                    String[] bd = findArgs.get(1).split(":");
                    blockFind = Material.getMaterial(bd[0]);
                    dataFind = Byte.parseByte(bd[1]);
                }else{*/
                    blockFind = Material.getMaterial(Integer.parseInt(args.get(1)));
                    dataFind = (byte) 0;
                /*}
                if(replaceArgs.get(1).contains(":")){
                    String[] bd = replaceArgs.get(1).split(":");
                    blockReplace = Material.getMaterial(bd[0]);
                    dataReplace = Byte.parseByte(bd[1]);
                }else{*/
                    blockReplace = Material.getMaterial(Integer.parseInt(args.get(2)));
                    dataReplace = (byte) 0;
                /*}
                if(findArgs.contains("-t")){
                    if(findArgs.contains("-s") && findArgs.contains("-p")){
                        int i = findArgs.indexOf("-t");
                        tagsFind = findArgs.get(i+1);
                    }else{
                        sender.sendMessage("Cannot search whole map with block flags");
                        return true;
                    }
                }
                if(replaceArgs.contains("-t")){
                    if(replaceArgs.contains("-s") && replaceArgs.contains("-p")){
                        int i = replaceArgs.indexOf("-t");
                        tagsReplace = replaceArgs.get(i+1);
                    }else{
                        sender.sendMessage("Cannot search whole map with block flags");
                        return true;
                    }
                }*/
                if(args.contains("-s")){
                    if(sender instanceof Player){
                        clip = true;
                    }else{
                        sender.sendMessage("Cannot use selection with console!");
                        return true;
                    }
                }
                if(args.contains("-p")){
                    if(sender instanceof Player){
                        int i = args.indexOf("-p");
                    pack = args.get(i+1);
                    }else{
                        sender.sendMessage("Cannot use pack param with console!");
                        return true;
                    }
                }
                if(args.contains("-d")){
                    sw = true;
                }
                final String tf = tagsFind;
                final String tr = tagsReplace;
                final boolean swap = sw;
                Runnable r;
                if(clip){
                    final Selection sel = worldEdit.getSelection((Player) sender);
                    r = new Runnable(){
                        @Override
                        public void run(){
                            int count = 0;
Logger.getGlobal().info("1");
                            for(int x = sel.getMinimumPoint().getBlockX(); x <= sel.getMaximumPoint().getBlockX(); x++){
                                for(int y = sel.getMinimumPoint().getBlockY(); y <= sel.getMaximumPoint().getBlockY(); y++){
                                    for(int z = sel.getMinimumPoint().getBlockZ(); z <= sel.getMaximumPoint().getBlockZ(); z++){
                                        Block b = new Location(sel.getWorld(), x, y, z).getBlock();
                                        if(b.getType().equals(blockFind) && b.getData() == dataFind && (tf != null ? checkFlags(tf, b) : true)){
                                            b.setType(blockReplace);
                                            b.setData(dataReplace, false);
                                            setFlags(tr, b);
                                            count++;
                                        }else if(b.getType().equals(blockReplace) && b.getData() == dataReplace && (tr != null ? checkFlags(tr, b) : true) && swap){
                                            b.setType(blockFind);
                                            b.setData(dataFind, false);
                                            setFlags(tf, b);
                                            count++;
                                        }
                                        Thread.yield();
                                    }
                                }
                            }
                            sender.sendMessage(count + " results found");
                        }
                    };
                }else if(pack != null){
                    Region reg = null;
                    for(Region re : (Set<Region>) forceGet(regionManager, "regions")){
                        if(re.getName().equalsIgnoreCase(pack)){
                            reg=re;
                            break;
                        }
                    }
                    if(reg==null){
                        sender.sendMessage("Pack not found!");
                        return true;
                    }
                    int n = (int) forceGet(regionManager, "n");
                    int[] xpoints = (int[]) forceGet(regionManager, "xpoints");
                    int[] zpoints = (int[]) forceGet(regionManager, "zpoints");
                    final Rectangle bounds = new Polygon(xpoints, zpoints, n).getBounds();
                    r = new Runnable(){
                        @Override
                        public void run(){
                            int count = 0;
Logger.getGlobal().info("2");
                            for(int x = bounds.x; x < bounds.x + bounds.width; x++){
                                for(int z = bounds.y; z < bounds.y + bounds.height; z++){
                                    for(int y = 0; y < 255; y++){
                                        Block b = new Location(((Player) sender).getWorld(), x, y, z).getBlock();
                                        if(b.getType().equals(blockFind) && b.getData() == dataFind && (tf != null ? checkFlags(tf, b) : true)){
                                            b.setType(blockReplace);
                                            b.setData(dataReplace, false);
                                            setFlags(tr, b);
                                            count++;
                                        }else if(b.getType().equals(blockReplace) && b.getData() == dataReplace && (tr != null ? checkFlags(tr, b) : true) && swap){
                                            b.setType(blockFind);
                                            b.setData(dataFind, false);
                                            setFlags(tf, b);
                                            count++;
                                        }
                                        Thread.yield();
                                    }
                                }
                            }
                            sender.sendMessage(count + " results found");
                        }
                    };

                }else{
                    final World w = ((Player) sender).getWorld();
Logger.getGlobal().info("3");
                    final Selection sel = worldEdit.getSelection((Player) sender);
                    int[][] data = new int[][]{{Integer.parseInt(args.get(1)),-1},{
                                                                   Integer.parseInt(args.get(2)),-1}};
                    JobActionReplace action = new JobActionReplace(data,0,0);
                    //JobManager.addJob(new CuboidJob(((Player)sender).getUniqueId(),(CuboidSelection)sel,action));
                    MessageManager.addListeningPlayer(((Player)sender).getUniqueId());
                    return true;
                    /*r = new Runnable(){
                        @Override
                        public void run(){
                            int count = 0;
                            for(int cx = minChunk.x; cx <= maxChunk.x; cx++){
                                for(int cy = minChunk.y; cy <= maxChunk.y; cy++){
                                    ChunkSnapshot c = w.getChunkAt(cx, cy).getChunkSnapshot(true, false, false);
                                    for(int x = 0; x <= 15; x++){
                                        for(int y = 0; y <= 127; y++){
                                            for(int z = 0; z <= 15; z++){
                                                if(c.getBlockTypeId(x, y, z) == blockFind.getId() && c.getBlockData(x, y, z) == dataFind){
                                                    Block b = w.getBlockAt(cx*16+x, y, cy*16+z);
                                                    b.setType(blockReplace);
                                                    b.setData(dataReplace, false);
                                                    count++;
                                                }else if(c.getBlockTypeId(x, y, z) == blockReplace.getId() && c.getBlockData(x, y, z) == dataReplace && swap){
                                                    Block b = w.getBlockAt(cx*16+x, y, cy*16+z);
                                                    b.setType(blockFind);
                                                    b.setData(dataFind, false);
                                                    count++;
                                                }
                                                Thread.yield();
                                            }
                                        }
                                    }
                                }
                            }
                            sender.sendMessage(count + " results found");
                        }
                    };*/
                }
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, r, 1);
            }
        }
        return false;
    }

    private boolean checkFlags(String flags, Block block){
        //TODO: add this
        return false;
    }

    private void setFlags(String flags, Block block){
        //TODO: add this
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
    
    private void showDetails(CommandSender cs) {
        cs.sendMessage("DevUtil: Level - "+DevUtil.getLevel()+"; Console - "+DevUtil.isConsoleOutput()+"; ");
        cs.sendMessage("         Developer:");
        for(OfflinePlayer player:DevUtil.getDeveloper()) {
        cs.sendMessage("                "+player.getName());
        }
    }

}
