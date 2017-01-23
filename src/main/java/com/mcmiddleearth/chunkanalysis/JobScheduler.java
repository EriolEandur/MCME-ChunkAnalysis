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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Eriol_Eandur
 */
public class JobScheduler extends BukkitRunnable {
        
    private final int START_TASK_SIZE = 10;
    
    //private int serverTps = 15;
    
    //private final int serverTpsVar = 1;
    
    //private final int maxMissingTicks = 3;
    
    @Setter
    float i = 0;
    @Setter
    float d = 0.5f;
    @Setter
    float v = 1;
    
    @Setter
    private boolean suspended;
    
    @Setter
    private boolean cancel;
    
    private final List<Job> pendingJobs;
    
    private long tickCounter;
    
    private final RegulatoryValue tps = new RegulatoryValue(15,15,5);
    
    //Job currentJob;
    
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
        long logJobTaskStartTime = logTime;
        int logTaskSize = 0;
        int logProcessedBlocks = 0;
        int logReplacedBlocks = 0;
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
            while(!(pendingJobs.isEmpty())){// && isJobFinished())) {
                long currentTime = System.currentTimeMillis();
                long currentTickCounter = tickCounter;
                float tpsNew = (float)((currentTickCounter-lastTickCounter)*1000.0/(currentTime-lastTime));
                DevUtil.log(3,"current tps: "+tpsNew);
                tps.add(tpsNew);
                DevUtil.log(2,"averaged tps: "+tps.getAverage()+" set tps: "+tps.getDesired());
                if(suspended || recovery) {
                    pendingJobs.get(0).stopTask();
                } else {
                    if(!pendingJobs.get(0).isTaskPending()) {
                        if(pendingJobs.get(0).isFinished()) {
                            pendingJobs.remove(0);
                            DevUtil.log("job finished in time: "+(currentTime-logJobTaskStartTime)/1000+" sec");
                            if(pendingJobs.isEmpty()) {
                                break;
                            }
                        }
                        taskSize = START_TASK_SIZE;
                        pendingJobs.get(0).setTaskSize(taskSize);
                        pendingJobs.get(0).startTask();
                        logJobTaskStartTime = currentTime;
                        DevUtil.log("job started");
                        tps.reset(tps.getDesired());
                    } else {
                        taskSize = calculateTaskSize(taskSize);
                        pendingJobs.get(0).setTaskSize(taskSize);
                        DevUtil.log(2,"current task size: "+taskSize);
                    }
                }
                if(cancel) {
                    pendingJobs.get(0).stopTask();
                    pendingJobs.remove(0);
                    DevUtil.log(1,"canceled job, jobs left: "+pendingJobs.size());
                    if(pendingJobs.isEmpty()) {
                        break;
                    }
                    cancel = false;
                }
                DevUtil.log(3,"Dt: "+(currentTime-lastTime));
                if(logTime<currentTime-10000) {
                    DevUtil.log("***");
                    double serverTps = (currentTickCounter-logTicks)/((currentTime-logTime)/1000.0);
                    recovery = serverTps < tps.getAverage()-2 || serverTps < 10;
                    DevUtil.log("Server tps: "+serverTps+" server lag: "+recovery);
                    DevUtil.log("Workers: "+Bukkit.getScheduler().getActiveWorkers().size()+" Tasks: "+Bukkit.getScheduler().getPendingTasks().size());
                    DevUtil.log("Processed blocks: "+(pendingJobs.get(0).getAction().getProcessedBlocks()-logProcessedBlocks));
                    if(pendingJobs.get(0).getAction() instanceof JobActionReplace) {
                        DevUtil.log("Replaced blocks: "+(((JobActionReplace)pendingJobs.get(0).getAction()).getReplacedBlocks()-logReplacedBlocks));
                        logReplacedBlocks = ((JobActionReplace)pendingJobs.get(0).getAction()).getReplacedBlocks();
                    }
                    DevUtil.log("Working at coord: "+pendingJobs.get(0).getCurrent().getBlockX()*16+" "
                                                    +pendingJobs.get(0).getCurrent().getBlockZ()*16);
                    DevUtil.log("Done "+Math.min(100,(pendingJobs.get(0).getChunksDone()*100.0/pendingJobs.get(0).getJobSize()))+"%");
                    logProcessedBlocks = pendingJobs.get(0).getAction().getProcessedBlocks();
                    logTime=currentTime;
                    logTicks = currentTickCounter;
                    logTaskSize = 0;
                }
                lastTickCounter = currentTickCounter;
                lastTime = currentTime;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(JobScheduler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        finally {
            ticker.cancel();
        }
    }
    
    private float calculateTaskSize(float taskSize) {
        DevUtil.log(3,"i: "+i+" integral: "+tps.getIntegral());
        DevUtil.log(3,"d: "+d+" difference: "+tps.getDifference());
        DevUtil.log(3,"v: "+v+" velocity: "+tps.getVelocity());
        float diff= (taskSize)/tps.getDesired()*(i*tps.getIntegral() //+1 to get up again from task size 0;
                                                +d*tps.getDifference()
                                                +v*tps.getVelocity());
        DevUtil.log(3,"old task size: "+taskSize+" calculated diff: "+diff);
        taskSize = taskSize+diff;
        if(taskSize<0) {
            taskSize=0;
        }
        return taskSize;
    }
    
    public void setServerTps(int desiredTps) {
        int serverTps = desiredTps;
        if(serverTps>20) {
            serverTps = 20;
        } else if(serverTps<1) {
            serverTps = 1;
        }
        tps.setDesired(serverTps);
    }
    
}
