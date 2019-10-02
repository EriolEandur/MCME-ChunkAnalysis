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
package com.mcmiddleearth.chunkanalysis.job;

import com.mcmiddleearth.chunkanalysis.util.DevUtil;
import com.mcmiddleearth.chunkanalysis.job.action.JobAction;
import com.mcmiddleearth.chunkanalysis.util.DBUtil;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public final class PolyJob extends Job {
    
    private final Vector minCorner;
    private final Vector maxCorner;
    
    private final List<Polygonal2DSelection> regions;
    
    private Map<Chunk,Set<BlockState>> pendingUpdates = new HashMap<>();
  
    private boolean finished=false;
    
    public PolyJob(UUID owner, List<Polygonal2DSelection> regions, JobAction action) {
        this(owner, regions, action, getFreeJobId(), -1, 0);
        DBUtil.logJobCreation(this);
    }
    
    private PolyJob(UUID owner, List<Polygonal2DSelection> regions, JobAction action, int id, long startTime, int chunksDone) {
        super(owner, regions.get(0).getWorld(), action, id, startTime, chunksDone);
        world = regions.get(0).getWorld();
        this.regions = regions;
        DevUtil.log("create job in world "+world);
        List<BlockVector2D> tempPoints = new ArrayList<>();
        int minY=300;
        int maxY=0;
        for(Polygonal2DSelection region: regions) {
            tempPoints.addAll(region.getNativePoints());
            if(minY>region.getMinimumPoint().getBlockY()) {
                minY = region.getMinimumPoint().getBlockY();
            }
            if(maxY<region.getMaximumPoint().getBlockY()) {
                maxY = region.getMaximumPoint().getBlockY();
            }
        }
        Polygonal2DSelection temp = new Polygonal2DSelection(world, tempPoints, minY, maxY);
        //x and z are chunk coordinates; y is block coordinate
        minCorner = new Vector(world.getChunkAt(temp.getMinimumPoint()).getX(), 
                               minY,
                               world.getChunkAt(temp.getMinimumPoint()).getZ());
        DevUtil.log("create polygonal job with min corner "+minCorner);
        maxCorner = new Vector(world.getChunkAt(temp.getMaximumPoint()).getX(), 
                               maxY,
                               world.getChunkAt(temp.getMaximumPoint()).getZ());
        DevUtil.log("create polygonal job with max corner "+maxCorner);
        currentChunk = minCorner.clone();
        jobSize = (maxCorner.getBlockX()-minCorner.getBlockX())*(maxCorner.getBlockZ()-minCorner.getBlockZ());
        chunksDone = 0;
    }
    
    public static Job createJob(int id, String worldName, UUID owner, Vector[] coords, 
                                String actionType, int[][] blockData, Vector currentChunk, Vector currentBlock,
                                long startTime, long processed, long found) {
        World world = Bukkit.getWorld(worldName);
        List<Polygonal2DSelection> regions = new ArrayList<>();
        int index = 0;
        while(index<coords.length) {
            int size = coords[index].getBlockX();
            int yMin = coords[index].getBlockY();
            int yMax = coords[index].getBlockZ();
            index++;
            List<BlockVector2D> points = new ArrayList<>();
            for(int i=index; i < index+size; i++) {
                points.add(new BlockVector2D(coords[i].getBlockX(),coords[i].getBlockZ()));
            }
            index = index+size;
            regions.add(new Polygonal2DSelection(world, points, yMin, yMax));
        }
        JobAction newAction = createAction(actionType, blockData, processed, found);
        PolyJob newJob = new PolyJob(owner, regions, newAction, id, startTime, 0);
        newJob.chunksDone = (currentChunk.getBlockX()-newJob.minCorner.getBlockX())
                                *(newJob.maxCorner.getBlockZ()-newJob.minCorner.getBlockZ())
                          +currentChunk.getBlockZ()-newJob.minCorner.getBlockZ();
        newJob.currentChunk = currentChunk;
        newJob.currentBlock = currentBlock;
        return newJob;
    }
    
    @Override
    public Vector[] getCoords() {
        List<Vector> coords = new ArrayList<>();
        for(Polygonal2DSelection region: regions) {
            coords.add(new Vector(region.getNativePoints().size(),
                                  region.getMinimumPoint().getBlockY(),
                                  region.getMaximumPoint().getBlockY()));
            for(BlockVector2D point: region.getNativePoints()) {
                coords.add(new Vector(point.getBlockX(),0,point.getBlockZ()));
            }
        }
        return coords.toArray(new Vector[0]);
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    @Override
    protected void executeTask(final int taskSize) {
        DevUtil.log(6,"start poly job task current "+currentChunk.getBlockX()+" "+currentChunk.getBlockZ());
        int chunksDoneStart = chunksDone;
        int chunkCounter = pendingUpdates.size();
        handlePendingUpdates();
        while(chunkCounter<taskSize) {
            for(Polygonal2DSelection region: regions) {
                Location currentLocation = new Location(world,currentChunk.getBlockX()*16,
                                                      region.getMinimumPoint().getBlockY(),
                                                      currentChunk.getBlockZ()*16);
                if(region.contains(currentLocation)) {
                    int x = currentChunk.getBlockX();
                    int z = currentChunk.getBlockZ();
                    DevUtil.log(7,"handle chunk "+x+" "+z);
                    if(handleChunk(x,z,
                                   region.getMinimumPoint().getBlockY(),
                                   region.getMaximumPoint().getBlockY())) {
                        //DBUtil.logChunk(this,x,z,0,0,0, getAction().getProcessedBlocks(),
                        //                                getAction().getFoundBlocks());
                    } else {
                        return;
                    }
                    chunksDone++;
                    chunkCounter++;
                    break;
                }
            }
            if(currentChunk.getBlockX()==maxCorner.getBlockX() 
                    && currentChunk.getBlockZ()==maxCorner.getBlockZ()) {
                finished = true;
                DevUtil.log(4,"job finished");
                return;
            }
            currentChunk = nextCurrent(currentChunk);
        }
        //getAction().saveResults(getId());
        //currentChunk = nextCurrent(lastFinished);
        DevUtil.log(6,"end job task done chunks: "+(chunksDone-chunksDoneStart));
        DevUtil.log(6,"end job task next current "+currentChunk.getBlockX()+" "+currentChunk.getBlockZ());
    }
    
    private boolean handleChunk(int chunkX, int chunkZ, int minY, int maxY) {
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        int startY = (minY<currentBlock.getBlockY()?currentBlock.getBlockY():
                                                                   minY);
        int startZ = currentBlock.getBlockZ();
        for(int x = currentBlock.getBlockX(); x<16;x++) {
            currentBlock.setX(x);
            for(int z=startZ; z<16;z++) {
                currentBlock.setZ(z);
                for(int y=startY;y<=maxY;y++) {
                    currentBlock.setY(y);
                    if(taskCancelled) {
                        if(saveProgress) {
                            saveProgress(); 
                            saveProgress = false;
                        }
                        //DBUtil.logChunk(this,chunkX,chunkZ,x,y,z, getAction().getProcessedBlocks(),
                        //                                getAction().getFoundBlocks());
                        //getAction().saveResults(getId());
                        //DBUtil.logBlock(this,x,y,z, getAction().getProcessedBlocks(),
                        //                            getAction().getFoundBlocks());
                        //currentBlock = new Vector(x,y,z);
                        return false;
                    } 
                    action.execute(chunk.getBlock(x, y, z));
                }
                startY = minCorner.getBlockY();
            }
            startZ=0;
        }
        //DBUtil.logBlock(this, x, y, z);
        currentBlock = new Vector(0,0,0);
        if(action.hasPendingUpdates()) {
            pendingUpdates.put(chunk, action.getPendingUpdates());
            action.clearPendingUpdates();
        } else {
            unloadChunk(chunk);
        }
        return true;
    }
    
    @Override
    protected void handlePendingUpdates() {
        for(Chunk chunk:pendingUpdates.keySet()) {
            Set<BlockState> blockStates = pendingUpdates.get(chunk);
            for(BlockState state:blockStates) {
                state.update(true, false);
            }
            unloadChunk(chunk);
        }
        pendingUpdates = new HashMap<>();
    }
    
    private void unloadChunk(Chunk chunk) {
        if(!world.unloadChunk(chunk)) {
            DevUtil.log(4,"Unable to unload chunk directly");
            int x = chunk.getX();
            int z = chunk.getZ();
            chunk = null;
            if(!world.unloadChunkRequest(x, z)) {
                DevUtil.log(4,"Unable to queue unload chunk");
            }
        }
    }
    
    private Vector nextCurrent(Vector lastFinished) {
        if(lastFinished==null) {
            return currentChunk;
        }
        if(lastFinished.getBlockZ()==maxCorner.getBlockZ()) {
            return new Vector(lastFinished.getBlockX()+1,0,minCorner.getBlockZ());
        } else {
            return new Vector(lastFinished.getBlockX(),0, lastFinished.getBlockZ()+1);
        }
    }

    @Override
    public String detailMessage() {
        return "Polygonal job from " +ChatColor.GREEN
                                  +minCorner.getBlockX()*16+" "+minCorner.getBlockY()+" "+(minCorner.getBlockZ()*16+15)
                                  +ChatColor.AQUA+" to "+ChatColor.GREEN
                                  +maxCorner.getBlockX()*16+" "+maxCorner.getBlockY()+" "+(maxCorner.getBlockZ()*16+15)+".\n"
                                  +ChatColor.AQUA+"Total Size "+ChatColor.GREEN+jobSize+ChatColor.AQUA+" chunks.\n"
                                  +action.getDetails();
    }
    
    
}
