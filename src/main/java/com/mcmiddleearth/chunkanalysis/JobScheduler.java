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

import com.mcmiddleearth.chunkanalysis.util.DevUtil;
import com.mcmiddleearth.chunkanalysis.job.action.JobActionReplace;
import com.mcmiddleearth.chunkanalysis.job.Job;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Eriol_Eandur
 */
public class JobScheduler extends BukkitRunnable {
        
    private final int START_TASK_SIZE = 1;
    
    private final int MAX_TASK_SIZE = 10;
    
    @Setter
    float i = 0;
    @Setter
    float d = 0.5f;
    @Setter
    float v = 1;
    
    @Setter
    @Getter
    private boolean suspended;
    
    @Setter
    private boolean cancel; //cancel current job
    
    @Setter 
    private boolean disable; //stop JobScheduler Async task if plugin is disabled
    
    private final List<Job> pendingJobs;
    
    private long tickCounter;
    
    private final RegulatoryValue tps = new RegulatoryValue(15,15,5);
    
    public JobScheduler(List<Job> pendingJobs) {
        super();
        this.pendingJobs = pendingJobs;
    }
    
    @Override
    public void run() {
        tickCounter = 0;
        long logTicks = 0;
        long logTime = System.currentTimeMillis();
        long lastTime = logTime;
        long logProcessedBlocks = 0;
        long logReplacedBlocks = 0;
        DevUtil.log("start ticker");
        BukkitTask ticker = new BukkitRunnable() {
            @Override
            public void run() {
                tickCounter++;
            }
        }.runTaskTimer(ChunkAnalysis.getInstance(), 0, 1);
        try {
            float taskSize = START_TASK_SIZE;
            long lastTickCounter = 0;
            boolean recovery=false;
            while(!(pendingJobs.isEmpty()) && !disable){// && isJobFinished())) {
                long currentTime = System.currentTimeMillis();
                long currentTickCounter = tickCounter;
                float tpsNew = (float)((currentTickCounter-lastTickCounter)*1000.0/(currentTime-lastTime));
                DevUtil.log(3,"current tps: "+tpsNew);
                tps.add(tpsNew);
                DevUtil.log(2,"averaged tps: "+tps.getAverage()+" set tps: "+tps.getDesired());
                if(suspended || recovery) {
                    pendingJobs.get(0).stopTask(true);
                } else {
                    if(!pendingJobs.get(0).isTaskPending()) {
                        if(pendingJobs.get(0).isFinished()) {
                            DevUtil.log("job finished in time: "+(pendingJobs.get(0).getDuration())+" sec");
                            UUID owner = pendingJobs.get(0).getOwner();
                            MessageManager.sendJobFinished(pendingJobs.get(0));
                            pendingJobs.remove(0);
                            if(!JobManager.ownsJob(owner)) {
                                MessageManager.removeListeningPlayer(owner);
                            }
                            if(pendingJobs.isEmpty()) {
                                DevUtil.log("disable async job scheduler for no more jobs");
                                break;
                            }
                        }
                        if(pendingJobs.get(0).isPrepared()) {
                            taskSize = START_TASK_SIZE;
                            pendingJobs.get(0).setTaskSize(taskSize);
                            pendingJobs.get(0).startTask();
                            DevUtil.log("job started");
                            MessageManager.sendJobStarted(pendingJobs.get(0));
                            tps.reset(tps.getDesired());
                        }
                    } else {
                        if(tps.getAverage()<10) {
                            taskSize = taskSize / 2;
                            recovery= true;
                            pendingJobs.get(0).stopTask(true);
                        } else {
                            taskSize = calculateTaskSize(taskSize);
                        }
                        taskSize = Math.max(0.3f, taskSize);
                        pendingJobs.get(0).setTaskSize(taskSize);
                        pendingJobs.get(0).saveProgress();//0,0,0,false);
                        DevUtil.log(2,"current task size: "+taskSize);
                    }
                }
                if(cancel) {
                    pendingJobs.get(0).stopTask(false);
                    pendingJobs.get(0).clearJobData();
                    UUID owner = pendingJobs.get(0).getOwner();
                    MessageManager.sendJobCancelled(pendingJobs.get(0));
                    pendingJobs.remove(0);
                    if(!JobManager.ownsJob(owner)) {
                        MessageManager.removeListeningPlayer(owner);
                    }
                    DevUtil.log(1,"canceled job, jobs left: "+pendingJobs.size());
                    if(pendingJobs.isEmpty()) {
                        DevUtil.log("disable async job scheduler for no more jobs");
                        break;
                    }
                    cancel = false;
                }
                DevUtil.log(3,"Dt: "+(currentTime-lastTime));
                if(logTime<currentTime-10000) {
                    double serverTps = (currentTickCounter-logTicks)/((currentTime-logTime)/1000.0);
                    recovery = serverTps < tps.getAverage()-4 || serverTps < 10;
                    MessageManager.sendCurrentJobStatus(pendingJobs.get(0));

                    DevUtil.log("***");
                    DevUtil.log("Server tps: "+serverTps+" server lag: "+recovery);
                    DevUtil.log("Workers: "+Bukkit.getScheduler().getActiveWorkers().size()+" Tasks: "+Bukkit.getScheduler().getPendingTasks().size());
                    DevUtil.log("Processed blocks: "+(pendingJobs.get(0).getAction().getProcessedBlocks()-logProcessedBlocks));
                    if(pendingJobs.get(0).getAction() instanceof JobActionReplace) {
                        DevUtil.log("Replaced blocks: "+(((JobActionReplace)pendingJobs.get(0).getAction()).getFoundBlocks()-logReplacedBlocks));
                        logReplacedBlocks = ((JobActionReplace)pendingJobs.get(0).getAction()).getFoundBlocks();
                    }
                    DevUtil.log("Working at coord: "+pendingJobs.get(0).getCurrentChunk().getBlockX()*16+" "
                                                    +pendingJobs.get(0).getCurrentChunk().getBlockZ()*16);
                    DevUtil.log("Done "+Math.min(100,(pendingJobs.get(0).getChunksDone()*100.0/pendingJobs.get(0).getJobSize()))+"%");
                    /*if(recovery) {
                        DevUtil.log("Saving world...");
                        final World world = pendingJobs.get(0).getWorld();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                world.save();
                            }
                        }.runTask(ChunkAnalysis.getInstance());
                    }*/
                    logProcessedBlocks = pendingJobs.get(0).getAction().getProcessedBlocks();
                    logTime=currentTime;
                    logTicks = currentTickCounter;
                }
                lastTickCounter = currentTickCounter;
                lastTime = currentTime;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(JobScheduler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        finally {
            if(pendingJobs.size()>0) {
                pendingJobs.get(0).stopTask(true);
                //pendingJobs.get(0).saveProgress();
            }
            disable = false;
            ticker.cancel();
        }
        DevUtil.log("Async job scheduler disabled");
    }
    
    private float calculateTaskSize(float taskSize) {
        DevUtil.log(3,"i: "+i+" integral: "+tps.getIntegral());
        DevUtil.log(3,"d: "+d+" difference: "+tps.getDifference());
        DevUtil.log(3,"v: "+v+" velocity: "+tps.getVelocity());
        float diff= (taskSize)/tps.getDesired()*(i*tps.getIntegral()
                                                +d*tps.getDifference()
                                                +v*tps.getVelocity());
        DevUtil.log(3,"old task size: "+taskSize+" calculated diff: "+diff);
        taskSize = taskSize+diff;
        if(taskSize<0) {
            taskSize=0;
        }
        if(taskSize>MAX_TASK_SIZE) {
            taskSize = MAX_TASK_SIZE;
        }
        return taskSize;
    }
    
    public void setServerTps(int desiredTps) {
        int serverTps = desiredTps;
        if(serverTps>20) {
            serverTps = 20;
        } else if(serverTps<10) {
            serverTps = 10;
        }
        tps.setDesired(serverTps);
    }
    
}
