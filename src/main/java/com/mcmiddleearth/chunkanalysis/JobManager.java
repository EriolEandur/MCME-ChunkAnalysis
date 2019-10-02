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
import com.mcmiddleearth.chunkanalysis.job.Job;
import com.mcmiddleearth.chunkanalysis.util.DBUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Eriol_Eandur
 */
public class JobManager {
    
    private static BukkitTask schedulerTask;
    
    @Getter
    private static JobScheduler jobScheduler;
    
    @Getter
    private static List<Job> pendingJobs = new ArrayList<>();
    
    @Setter
    private static int tps;
    
    public static void init(int setTps) {
        tps = setTps;
//Logger.getGlobal().info("init Job Manager");
        pendingJobs = DBUtil.loadJobs();
        for(Job job: pendingJobs) {
            MessageManager.addListeningPlayer(job.getOwner());
            MessageManager.sendJobRestarted(job);
        }
        if(pendingJobs.size()>0) {
            startScheduler();
        }
    }
    public static void addJob(Job newJob) {
        pendingJobs.add(newJob);
        MessageManager.deleteOldMessages();
        startScheduler();
    }
    
    public static void removeJob(int jobId) {
        Job found = null;
        for(Job job:pendingJobs) {
            if(job.getId()==jobId) {
                found = job;
            }
        }
        if(found!=null) {
            pendingJobs.remove(found);
        }
    }
    
    private static void startScheduler() {
        if(!isSchedulerTaskRunning()) {
            DevUtil.log("start scheduler with tps "+tps);
            boolean dbConnected = DBUtil.checkConnection();
            DevUtil.log("Database connected: "+dbConnected);
            jobScheduler = new JobScheduler(pendingJobs);
            jobScheduler.setServerTps(tps);
            schedulerTask=jobScheduler.runTaskAsynchronously(ChunkAnalysis.getInstance());
        }
    }
   
    public static void cancelJob(Job cancelJob) {
        pendingJobs.remove(cancelJob);
    }
    
    public static boolean isSchedulerTaskRunning() {
        return schedulerTask!=null 
               && (Bukkit.getScheduler().isCurrentlyRunning(schedulerTask.getTaskId())
                   || Bukkit.getScheduler().isQueued(schedulerTask.getTaskId()));
    }
    
    public static boolean isCurrentJob(int jobId) {
        return !pendingJobs.isEmpty() && pendingJobs.get(0).getId()==jobId;
    }
    
    public static boolean hasJob(int jobId) {
        for(Job job: pendingJobs) {
            if(job.getId()==jobId) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean ownsJob(UUID player) {
        for(Job job: pendingJobs) {
            if(job.getOwner().equals(player)) {
                return true;
            }
        }
        return false;
    }
}
