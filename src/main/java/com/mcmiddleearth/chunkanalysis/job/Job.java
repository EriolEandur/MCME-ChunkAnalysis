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

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import com.mcmiddleearth.chunkanalysis.util.DevUtil;
import com.mcmiddleearth.chunkanalysis.job.action.JobAction;
import com.mcmiddleearth.chunkanalysis.util.DBUtil;
import de.schlichtherle.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class Job {
    
    @Getter
    @Setter
    private boolean prepared = true;
    
    @Getter
    private final int id;
    
    @Getter
    private final UUID owner;
    
    private File statsFile;
    
    @Getter
    protected int jobSize; //number of chunks
    
    @Getter
    protected int chunksDone;
    
    @Getter
    protected Vector currentChunk;
    
    @Getter
    protected Vector currentBlock = new Vector(0,0,0);
    
    private BukkitTask jobTask;
    
    @Setter
    private float taskSize;
    
    @Getter
    private long startTime=-1;
    
    private long finishTime = -1;
    
    
    private boolean taskWorking = false;    //flag if task is currently working
    protected boolean taskCancelled;        //flag to cancel task during working in a chunk
    protected boolean saveProgress = false; //flag if the progress should be saved on next task cancel

    
    @Getter
    protected JobAction action;
    
    @Getter
    protected World world;
    
    private final Job self;
    
    public Job(UUID owner, World world, JobAction action, int id, long startTime, int chunksDone) {
        this.chunksDone = chunksDone;
        this.startTime = startTime;
        this.owner = owner;
        this.world = world;
        this.action = action;
        this.id = id;
        //if(Bukkit.getOfflinePlayer(owner).)
        //MessageManager.addListeningPlayer();
        self = this;
    }
    
    public boolean isTaskPending() {
        return jobTask != null && (Bukkit.getScheduler().isCurrentlyRunning(jobTask.getTaskId())
                   || Bukkit.getScheduler().isQueued(jobTask.getTaskId()));
    }
    
    public abstract boolean isFinished();
    
    public void startTask() {
        if(!isTaskPending()) {
            taskCancelled=false;
            if(startTime==-1) {
                startTime=System.currentTimeMillis()/1000;
                DBUtil.logStartTime(this);
            }
            //jobTask = createJobTask(taskSize).runTask(ChunkAnalysis.getInstance());
            jobTask = new BukkitRunnable() {
                @Override
                public void run() {
                    taskWorking = true;
                    DevUtil.log(6,"Starting job with size: "+taskSize);
                    int taskSizeInt = Math.round(taskSize);
                    if(taskSizeInt>0) {
                        executeTask(taskSizeInt);
                    }
                    if(isFinished()) {
                        finishTime = System.currentTimeMillis()/1000;
                        cancel();
                        clearJobData();
                    }
                    taskWorking = false;
                }
            }.runTaskTimer(ChunkAnalysis.getInstance(), 0, 1);
        }
    }
    
    //protected abstract BukkitRunnable createJobTask(int askSize);
    
    public void stopTask(boolean save) {
        if(isTaskPending() && !taskCancelled) {//jobTask!=null && !taskCancelled) {
            taskCancelled = true;
            jobTask.cancel();
            if(save) {
                if(taskWorking) {
                    DevUtil.log("save during working in chunk scheduled");
                    saveProgress = true;
                } else {
                    saveProgress();
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    handlePendingUpdates();
                }
            }.runTaskLater(ChunkAnalysis.getInstance(), 1);
        }
    }
    
    public void saveProgress(){//int blockX, int blockY, int blockZ, boolean ignoreCancel) {
        //if(!taskCancelled || ignoreCancel) {
            DevUtil.log("save progress");
            DBUtil.logChunk(this, currentChunk.getBlockX(),
                                  currentChunk.getBlockZ(), 
                                  currentBlock.getBlockX(),
                                  currentBlock.getBlockY(),
                                  currentBlock.getBlockZ(), 
                                  getAction().getProcessedBlocks(),
                                  getAction().getFoundBlocks());
            this.action.saveResults(getId());
        //}
    }
    
    protected void handlePendingUpdates() {};
    
    public void clearJobData() {
        DBUtil.deleteJob(this);
    }

    public long getDuration() {
        return System.currentTimeMillis()/1000-startTime;
    }
    
    protected abstract void executeTask(int size);
    
    public abstract Vector[] getCoords();

    public String statMessage() {
        /*if(isFinished()) {
            return "Finished in "+(finishTime-startTime)+"seconds. "+action.statMessage();
        } else if(startTime==-1){
            return "Not yet started. "+action.statMessage();
        } else {*/
            String percentage = ""+Math.min(100,(getChunksDone()*100.0/getJobSize()));
            if(percentage.contains(".") && percentage.length()>percentage.indexOf(".")+3) {
                percentage = percentage.substring(0,percentage.indexOf(".")+3);
            }
            if(isPrepared()) {
                return ChatColor.GREEN+percentage+"%"
                        +ChatColor.AQUA+" done. Working at "+ChatColor.GREEN
                        + (currentChunk.getBlockX()*16)+" "+(currentChunk.getBlockZ()*16)
                        +ChatColor.AQUA+". ";
            } else {
                return ChatColor.AQUA+"preparing...";
                
            }
        //}
    }
    
    protected static int getFreeJobId() {
        int[] usedIds = DBUtil.getJobIds();
        int free = 0;
        boolean found = false;
        while(!found) {
            found = true;
            for(int i=0; i<usedIds.length;i++) {
                if(usedIds[i]==free) {
                    found=false;
                    free++;
                    break;
                }
            }
        }
        return free;
    }
    
    protected static JobAction createAction(String type, int[][] blockData, long processed, long found) {
        Class actionClass;
        try {
            actionClass = Class.forName(type);
            Constructor constructor = actionClass.getConstructor(new Class[]{blockData.getClass(),
                                                                 long.class, long.class});
            return (JobAction) constructor.newInstance((Object) blockData, processed, found);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String getOwnerName() {
        return Bukkit.getOfflinePlayer(getOwner()).getName();
    }
    
    public String getActionName() {
        return action.getName();
    }
    
    public abstract String detailMessage();
}
