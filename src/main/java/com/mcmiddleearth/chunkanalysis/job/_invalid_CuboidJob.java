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
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public final class _invalid_CuboidJob extends Job {
    
    private final Vector minCorner;
    private final Vector maxCorner;
    
  
    private boolean finished=false;
    
    public _invalid_CuboidJob(UUID owner, CuboidSelection region, JobAction action) {
        this(owner, region,action, getFreeJobId(), -1, 0);
        DBUtil.logJobCreation(this);
    }
    
    private _invalid_CuboidJob(UUID owner, CuboidSelection region, JobAction action, int id, long startTime, int chunksDone) {
        super(owner, region.getWorld(), action, id, startTime, chunksDone);
        world = region.getWorld();
        DevUtil.log("create job in world "+world);
        //x and z are chunk coordinates; y is block coordinate
        minCorner = new Vector(world.getChunkAt(region.getMinimumPoint()).getX(), 
                               region.getMinimumPoint().getBlockY(),
                               world.getChunkAt(region.getMinimumPoint()).getZ());
        DevUtil.log("create cuboid job with min corner "+minCorner);
        maxCorner = new Vector(world.getChunkAt(region.getMaximumPoint()).getX(), 
                               region.getMaximumPoint().getBlockY(),
                               world.getChunkAt(region.getMaximumPoint()).getZ());
        DevUtil.log("create cuboid job with max corner "+maxCorner);
        currentChunk = minCorner.clone();
        jobSize = (maxCorner.getBlockX()-minCorner.getBlockX())*(maxCorner.getBlockZ()-minCorner.getBlockZ());
        chunksDone = 0;
    }
    
    public static Job createJob(int id, String worldName, UUID owner, Vector[] coords, 
                                String actionType, int[][] blockData, Vector currentChunk, Vector currentBlock,
                                long startTime, long processed, long found) {
        World world = Bukkit.getWorld(worldName);
        Vector minCorner = new Vector(coords[0].getBlockX()*16, coords[0].getBlockY(), coords[0].getBlockZ()*16);
        Vector maxCorner = new Vector(coords[1].getBlockX()*16, coords[1].getBlockY(), coords[1].getBlockZ()*16);
        CuboidSelection selection = new CuboidSelection(world, minCorner.toLocation(world), 
                                                               maxCorner.toLocation(world));
        JobAction newAction = createAction(actionType, blockData, processed, found);
        int chunksDone = (currentChunk.getBlockX()-coords[0].getBlockX())
                                *(coords[1].getBlockZ()-coords[0].getBlockZ())
                          +currentChunk.getBlockZ()-coords[0].getBlockZ();
        Job newJob = new _invalid_CuboidJob(owner, selection, newAction, id, startTime, chunksDone);
        newJob.currentChunk = currentChunk;
        newJob.currentBlock = currentBlock;
Logger.getGlobal().info("Creating job: "+currentChunk.getBlockX()+" "+currentChunk.getBlockZ()+" "+
                                     currentBlock.getBlockX()+" "+currentBlock.getBlockY()+" "+
                                      currentBlock.getBlockZ());
        return newJob;
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    @Override
    protected void executeTask(final int taskSize) {
        DevUtil.log(6,"start cuboid job task current "+currentChunk.getBlockX()+" "+currentChunk.getBlockZ());
        int chunksDoneStart = chunksDone;
        Vector lastFinished=null;
        int startX = currentChunk.getBlockX();
        int endX = currentChunk.getBlockX()+(currentChunk.getBlockZ()
                                       -minCorner.getBlockZ()
                                       +taskSize)/chunksPerRow();
        int stopEarly=0;
        if(endX>maxCorner.getBlockX()) {
            endX=maxCorner.getBlockX()+1;
            stopEarly=1;
        }
        int startZ = currentChunk.getBlockZ();
        for(int x = startX; x<=endX-stopEarly; x++) {
            int endZ = (x==endX?minCorner.getBlockZ()
                                +(currentChunk.getBlockZ()
                                  -minCorner.getBlockZ()
                                  +taskSize)%chunksPerRow()
                                :maxCorner.getBlockZ());
            for(int z = startZ; z<=endZ; z++) {
                DevUtil.log(7,"handle chunk "+x+" "+z);
                if(handleChunk(x,z)) {
                    lastFinished = new Vector(x,0,z);
                    currentChunk = nextCurrent(lastFinished);
                    //DBUtil.logChunk(this,x,z,0,0,0, getAction().getProcessedBlocks(),
                    //                                getAction().getFoundBlocks());
               } else {
                    return;
                }
                chunksDone++;
            }
            startZ = minCorner.getBlockZ();
        }
        //getAction().saveResults(getId());
        if(lastFinished==null || (lastFinished.getBlockX()==maxCorner.getBlockX() 
                && lastFinished.getBlockZ() == maxCorner.getBlockZ())) {
            finished = true;
            DevUtil.log(4,"job finished");
        }
        //currentChunk = nextCurrent(lastFinished);
        DevUtil.log(6,"end job task done chunks: "+(chunksDone-chunksDoneStart));
        DevUtil.log(6,"end job task next current "+currentChunk.getBlockX()+" "+currentChunk.getBlockZ());
    }
    
    private boolean handleChunk(int chunkX, int chunkZ) {
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        int startY = (minCorner.getBlockY()<currentBlock.getBlockY()?currentBlock.getBlockY():
                                                                   minCorner.getBlockY());
        int startZ = currentBlock.getBlockZ();
        for(int x = currentBlock.getBlockX(); x<16;x++) {
            for(int z=startZ; z<16;z++) {
                for(int y=startY;y<=maxCorner.getBlockY();y++) {
                    if(taskCancelled) {
                        //saveProgress(x,y,z,true);
                        //DBUtil.logChunk(this,chunkX,chunkZ,x,y,z, getAction().getProcessedBlocks(),
                        //                                getAction().getFoundBlocks());
                        //getAction().saveResults(getId());
                        //DBUtil.logBlock(this,x,y,z, getAction().getProcessedBlocks(),
                        //                            getAction().getFoundBlocks());
                        currentBlock = new Vector(x,y,z);
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
        if(!world.unloadChunk(chunk)) {
            DevUtil.log(4,"Unable to unload chunk directly");
            chunk = null;
            if(!world.unloadChunkRequest(chunkX, chunkZ)) {
                DevUtil.log(4,"Unable to queue unload chunk");
            }
        }
        return true;
    }
    
    private int chunksPerRow() {
        return maxCorner.getBlockZ()-minCorner.getBlockZ()+1;
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
        return "Cuboid job from " +ChatColor.GREEN
                                  +minCorner.getBlockX()*16+" "+minCorner.getBlockY()+" "+(minCorner.getBlockZ()*16+15)
                                  +ChatColor.AQUA+" to "+ChatColor.GREEN
                                  +maxCorner.getBlockX()*16+" "+maxCorner.getBlockY()+" "+(maxCorner.getBlockZ()*16+15)+".\n"
                                  +ChatColor.AQUA+"Total Size "+ChatColor.GREEN+jobSize+ChatColor.AQUA+" chunks.\n"
                                  +action.getDetails();
    }
    
    @Override
    public Vector[] getCoords() {
        return new Vector[]{minCorner,maxCorner};
    }
    
}
