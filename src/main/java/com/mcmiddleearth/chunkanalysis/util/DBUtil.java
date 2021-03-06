/*
 * Copyright (C) 2016 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.chunkanalysis.util;

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import com.mcmiddleearth.chunkanalysis.job.Job;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.mariadb.jdbc.MySQLDataSource;

/**
 *
 * @author Eriol_Eandur
 */
public class DBUtil {

    private static String dbUser;
    private static String dbPassword;
    
    private static MySQLDataSource dataBase;
    
    private static Connection dbConnection;
    
    private static PreparedStatement logJobCreate;
    private static PreparedStatement logJobCoord;
    private static PreparedStatement logBlockIds;
    private static PreparedStatement logJobStart;
    private static PreparedStatement logJobResultUpdate;
    private static PreparedStatement logBlock;
    private static PreparedStatement logChunk;
    private static PreparedStatement logMessage;
    private static PreparedStatement deleteJob;
    private static PreparedStatement deleteJobCoords;
    private static PreparedStatement deleteJobBlocks;
    private static PreparedStatement deleteMessages;
    
    private static PreparedStatement getJobs;
    private static PreparedStatement getJobCoords;
    private static PreparedStatement getBlockIds;
    private static PreparedStatement getJobIds;
    private static PreparedStatement getMessages;
    
    @Getter
    private static boolean connected = false;
    
    private final static boolean executeUpdatesAsync = false; //!!!true here crashes the server!!!
    
    public static void init(String ip, int port, String dbName, String newDbUser, String newDbPassword){
        dataBase = new MySQLDataSource(ip,port,dbName);
        dbUser = newDbUser;
        dbPassword = newDbPassword;
        connect();
    }
        
    public static synchronized void logJobCreation(final Job job) {
        if(isConnected()) {
            try {
                logJobCreate.setInt(1,job.getId());
                logJobCreate.setString(2,job.getOwner().toString());
                logJobCreate.setString(3,job.getClass().getName());
                logJobCreate.setString(4,job.getAction().getClass().getName());
                logJobCreate.setString(5,job.getWorld().getName());
                executeUpdate(logJobCreate);
                Vector[] coords = job.getCoords();
                String coordData = "";
                for(int i=0;i<coords.length;i++) {
                    Vector coord = coords[i];
                    coordData = coordData + coord.getBlockX()+" "
                                          + coord.getBlockY()+" "
                                          + coord.getBlockZ()+" ";
                }
                logJobCoord.setInt(1, job.getId());
                logJobCoord.setString(2, coordData);
                executeUpdate(logJobCoord);
                String blockIdData = "";
                for(int i=0;i<job.getAction().getBlockIds().length;i++) {
                    int[] blockId = job.getAction().getBlockIds()[i];
                    blockIdData = blockIdData + blockId[0] + " "
                                              + blockId[1] + " "
                                              + "0 ";
                }
                logBlockIds.setInt(1, job.getId());
                logBlockIds.setString(2, blockIdData);
//Logger.getGlobal().info(blockIdData);
                executeUpdate(logBlockIds);
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static synchronized void log_Block(Job job, int x, int y, int z, long processed, long found) {
        if(isConnected()) {
            try {
                logBlock.setInt(1,x);
                logBlock.setInt(2,y);
                logBlock.setInt(3,z);
                logBlock.setLong(4, processed);
                logBlock.setLong(5, found);
                logBlock.setInt(6,job.getId());
                executeUpdate(logBlock);
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static synchronized void logChunk(Job job, int chunkX, int chunkZ, int blockX, int blockY, int blockZ,
                                long processed, long found) {
        if(isConnected()) {
            try {
                logChunk.setInt(1,chunkX);
                logChunk.setInt(2,chunkZ);
                logChunk.setInt(3,blockX);
                logChunk.setInt(4,blockY);
                logChunk.setInt(5,blockZ);
                logChunk.setLong(6, processed);
                logChunk.setLong(7, found);
                logChunk.setInt(8,job.getId());
                executeUpdate(logChunk);
//Logger.getGlobal().info("logChunk: "+chunkX+" "+chunkZ+" "+blockX+" "+blockY+" "+blockZ);
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static synchronized void logStartTime(Job job) {
        if(isConnected()) {
            try {
                logJobStart.setTimestamp(1,new Timestamp(job.getStartTime()));
                logJobStart.setInt(2,job.getId());
                executeUpdate(logJobStart);
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static synchronized void logJobResult(int jobId, int index, int counter) {
        if(isConnected()) {
            try {
                logJobResultUpdate.setInt(1, counter);
                logJobResultUpdate.setInt(2, jobId);
                logJobResultUpdate.setInt(3, index);
                executeUpdate(logJobResultUpdate);
            } catch (SQLException ex) {
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static synchronized void deleteJob(Job job) {
        if(isConnected()) {
            try {
                deleteJob.setInt(1,job.getId());
                deleteJob.executeUpdate();
                deleteJobCoords.setInt(1,job.getId());
                executeUpdate(deleteJobCoords);
                deleteJobBlocks.setInt(1,job.getId());
                executeUpdate(deleteJobBlocks);
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static synchronized int[] getJobIds() {
        if(isConnected()) {
            try {
                ResultSet jobIdData =getJobIds.executeQuery();
                jobIdData.last();
                int[] jobIds = new int[jobIdData.getRow()];
                for(int i=0; i<jobIds.length;i++) {
                    jobIdData.absolute(i+1);
                    jobIds[i]=jobIdData.getInt("jobID");
                }
                return jobIds;
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new int[0];
    }
    
    public static synchronized List<Job> loadJobs() {
//Logger.getGlobal().info("Load jobs");
        if(isConnected()) {
//Logger.getGlobal().info("1");
            try {
//Logger.getGlobal().info("2");
                
                ResultSet jobData = getJobs.executeQuery();
                if(jobData.first()) {
//Logger.getGlobal().info("3");
                    List<Job> jobs = new ArrayList<>();
                    while(!jobData.isAfterLast()) {
//Logger.getGlobal().info("4");
                        String type = jobData.getString("jobType");
                        String actionType = jobData.getString("actionType");
                        String world = jobData.getString("world");
                        int id = jobData.getInt("jobID");
                        DevUtil.log("loading job Id "+id);
                        Vector currentChunk = new Vector(jobData.getInt("chunkX"),0,
                                                         jobData.getInt("chunkZ"));
                        Vector currentBlock = new Vector(jobData.getInt("x"),
                                                         jobData.getInt("y"),
                                                         jobData.getInt("z"));
//Logger.getGlobal().info("Starting: "+currentChunk.getBlockX()+" "+currentChunk.getBlockZ()+" "+
//                                     currentBlock.getBlockX()+" "+currentBlock.getBlockY()+" "+
//                                      currentBlock.getBlockZ());
                        getJobCoords.setInt(1, id);
                        ResultSet coordResult = getJobCoords.executeQuery();
                        coordResult.first();
                        String[] coordData = coordResult.getString("data").split(" ");
                        Vector[] jobCoords = new Vector[coordData.length/3];
//Logger.getGlobal().info(coordResult.getString("data"));
                        for(int i = 0; i<jobCoords.length;i++) {
                            jobCoords[i] = new Vector(Integer.parseInt(coordData[i*3]),
                                                      Integer.parseInt(coordData[i*3+1]),
                                                      Integer.parseInt(coordData[i*3+2]));
                        }
                        getBlockIds.setInt(1, id);
                        ResultSet blockResult = getBlockIds.executeQuery();
                        blockResult.first();
                        String[] blockData = blockResult.getString("data").split(" ");
                        int[][] blockIds = new int[blockData.length/3][];
//Logger.getGlobal().info(blockResult.getString("data"));
                        for(int i = 0; i<blockIds.length;i++) {
                            blockIds[i] = new int[]{Integer.parseInt(blockData[i*3]),
                                                    Integer.parseInt(blockData[i*3+1]),
                                                    Integer.parseInt(blockData[i*3+2])};
//Logger.getGlobal().info(""+blockIds[i]);                            
                        }
                        UUID uuid = UUID.fromString(jobData.getString("owner"));
                        Timestamp stamp = jobData.getTimestamp("startTime");
                        long startTime = 0;
                        if(stamp!=null) {
                            startTime = jobData.getTimestamp("startTime").getTime();
                        }
                        long processed = jobData.getLong("processed");
                        long found = jobData.getLong("found");
                        try {
                            Class jobClass = Class.forName(type);
                            Method jobCreator = jobClass.getMethod("createJob", new Class[]{int.class, 
                                                                                             world.getClass(),
                                                                                             uuid.getClass(),
                                                                                             jobCoords.getClass(),
                                                                                             actionType.getClass(),
                                                                                             blockIds.getClass(),
                                                                                             currentChunk.getClass(),
                                                                                             currentBlock.getClass(),
                                                                                             long.class,
                                                                                             long.class,
                                                                                             long.class});
                            Job job = (Job) jobCreator.invoke(null,id,world,uuid,jobCoords,actionType,blockIds,
                                                              currentChunk, currentBlock, startTime, processed, found);
                            jobs.add(job);
                        } catch (IllegalAccessException | InvocationTargetException | 
                                 ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
                            Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        jobData.next();
                    }
                    return jobs;
                }
            } catch (SQLException ex) {
                connected = false;
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new ArrayList<>();
    }
    
    public static synchronized void logMessage(String[] messages) {
        if(isConnected()) {
            try {
                logMessage.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
                String message = "";
                if(messages.length>0) {
                    message = messages[0];
                }
                for(int i=1;i<messages.length;i++) {
                    message = message + "\\n" + messages[i];
                }
                logMessage.setString(2, message);
                executeUpdate(logMessage);
            } catch (SQLException ex) {
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static synchronized List<String[]> getMessages() {
        List<String[]>messages = new ArrayList<>();
        if(isConnected()) {
            try {
                ResultSet data = getMessages.executeQuery();
                if(data.first()) {
                    data.last();
                    int numberOfMessages = data.getRow();
                    for(int i=1;i<=numberOfMessages;i++) {
                        data.absolute(i);
                        long time = data.getTimestamp("time").getTime();
                        String messageData = data.getString("message");
                        String[] temp = messageData.split("\\\\n");
                        String[] message = new String[temp.length+1];
                        message[0] = temp[0];
                        message[1] = "  ["+new Date(time)+"]  ";
                        System.arraycopy(temp, 1, message, 2, temp.length - 1);
                        messages.add(message);
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return messages;
    }
    
    public static synchronized void deleteMessages(int storagePeriod) {
        if(isConnected()) {
            try {
                deleteMessages.setTimestamp(1,new Timestamp(System.currentTimeMillis()
                                                            -(storagePeriod*24*3600*1000)));
                executeUpdate(deleteMessages);
            } catch (SQLException ex) {
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static synchronized boolean checkConnection() {
        try {
            if(connected && dbConnection.isValid(1)) {
                connected = true;
                return true;
            } else {
                throw new SQLException();
            }
        } catch (SQLException ex) {
            connected = false;
            connect();
            return isConnected();
        }
    }
    
    private static synchronized void connect() {
        try {
            dbConnection = dataBase.getConnection(dbUser, dbPassword);
            
            checkTables();
            
            logJobCreate = dbConnection.prepareStatement("INSERT INTO jobs VALUES (?,?,?,0,0,0,NULL,NULL,0,0,0,?,?)");
            //logJobCoord = dbConnection.prepareStatement("INSERT INTO jobcoords VALUES (?,?,?,?,?)");
            //logBlockIds = dbConnection.prepareStatement("INSERT INTO blockid VALUES (?,?,?,?,?)");
            logJobCoord = dbConnection.prepareStatement("INSERT INTO jobcoords VALUES (?,?)");
            logBlockIds = dbConnection.prepareStatement("INSERT INTO blockid VALUES (?,?)");
            logJobStart = dbConnection.prepareStatement("UPDATE jobs SET startTime = ? WHERE jobID = ?");
            logJobResultUpdate = dbConnection.prepareStatement("UPDATE blockid SET counter = ?"
                                                               +" WHERE jobID = ? AND `index` = ?");
            logBlock = dbConnection.prepareStatement("UPDATE jobs SET x = ?, y = ?, z = ?,"
                                                     + " processed = ?, found = ? WHERE jobID = ?");
            logChunk = dbConnection.prepareStatement("UPDATE jobs SET chunkX = ?, chunkZ = ?, x = ?, y = ?, z = ?,"
                                                     +" processed = ?, found = ? WHERE jobID = ?");
            logMessage = dbConnection.prepareStatement("INSERT INTO messages VALUES (?,?)");
            deleteJob = dbConnection.prepareStatement("DELETE FROM jobs WHERE jobID = ?");
            deleteJobCoords = dbConnection.prepareStatement("DELETE FROM jobcoords WHERE jobID = ?");
            deleteJobBlocks = dbConnection.prepareStatement("DELETE FROM blockid WHERE jobID = ?");
            deleteMessages = dbConnection.prepareStatement("DELETE FROM messages WHERE time < ?");
            getJobs = dbConnection.prepareStatement("SELECT * FROM jobs");
            //getJobCoords = dbConnection.prepareStatement("SELECT * FROM jobcoords WHERE jobID = ?");
            //getBlockIds = dbConnection.prepareStatement("SELECT * FROM blockid WHERE jobID = ?");
            getJobCoords = dbConnection.prepareStatement("SELECT data FROM jobcoords WHERE jobID = ?");
            getBlockIds = dbConnection.prepareStatement("SELECT data FROM blockid WHERE jobID = ?");
            getJobIds = dbConnection.prepareStatement("SELECT jobID FROM jobs");
            getMessages = dbConnection.prepareStatement("SELECT * FROM messages ORDER BY time DESC, message DESC");
            connected = true;
        } catch (SQLException ex) {
            connected = false;
            Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, "Connection to DB failed", ex);
        }
    }
    
    private static synchronized void checkTables() throws SQLException {
        //ResultSet result = dbConnection.createStatement().executeQuery("SHOW TABLE STATUS WHERE name = 'jobs'");
        //if(!result.first()) {
        dbConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS jobs "
                                              +"(jobID INT(11), owner VARCHAR(50), jobType VARCHAR(150),"
                                              +"startTime TIMESTAMP NULL, found BIGINT(20) DEFAULT 0, "
                                              +"processed BIGINT(20) DEFAULT 0, chunkX INT(11), chunkZ INT(11),"
                                              +"x SMALLINT(6), y SMALLINT(6), z SMALLINT(6), "
                                              +"actionType VARCHAR(150), world VARCHAR(50))");
        dbConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS blockid "
                                              +"(jobID SMALLINT(6), data LONGTEXT)");
        dbConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS jobcoords "
                                              +"(jobID INT(11), data LONGTEXT)");
        dbConnection.createStatement().execute("CREATE TABLE IF NOT EXISTS messages "
                                              +"(time TIMESTAMP NULL, message TEXT)");
    }
    private static void executeUpdate(final PreparedStatement statement) {
        if(executeUpdatesAsync) {
            new BukkitRunnable(){
                @Override
                public void run() {
                    try {
                        statement.executeUpdate();
                    } catch (SQLException ex) {
                        Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.runTaskAsynchronously(ChunkAnalysis.getInstance());
        } else {
            try {
                statement.executeUpdate();
            } catch (SQLException ex) {
                Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
