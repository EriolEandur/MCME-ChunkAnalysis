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
package com.mcmiddleearth.chunkanalysis.job.action;

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import com.mcmiddleearth.resourceregions.DevUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 *
 * @author Eriol_Eandur
 */
public class JobActionQuickReplace extends JobAction {
    
    private final int[][] searchBlocks;
    
    private final int[][] replaceBlocks;
    
    private final Map<Mask,int[]> replacements = new HashMap<>();
    
    private Set<BlockState> pendingUpdates = new HashSet<>();
    //private Set<Chunk> pendingChunks = new HashSet<>();
    
    public JobActionQuickReplace(int[][] searchBlocks, int[][] replaceBlocks) {
        super(0, 0);
        this.searchBlocks=searchBlocks;
        this.replaceBlocks=replaceBlocks;
        createReplacements();
    }
    
    public JobActionQuickReplace(int[][] blockData, long processed, long found) {
        super(processed, found);
//Logger.getGlobal().info("replace");
//for(int i=0;i<blockData.length;i++) {
//Logger.getGlobal().info(""+blockData[0]+" "+blockData[1]);
//}
        searchBlocks = new int[blockData.length/2][];
        replaceBlocks = new int[blockData.length/2][];
        System.arraycopy(blockData, 0, searchBlocks, 0, blockData.length/2);
        System.arraycopy(blockData, blockData.length/2, replaceBlocks, 0, blockData.length/2);
        DevUtil.log("Search for "+ searchBlocks.length+" blocks ");
        createReplacements();
    }
    
    private void createReplacements() {
        replacements.clear();
        for(int i=0;i<searchBlocks.length;i++) {
            replacements.put(new Mask(searchBlocks[i][0],searchBlocks[i][1]),replaceBlocks[i]);
        }
    }
    
    @Override
    public void execute(Block block) {
        super.execute(block);
        //for(int i=0;i< searchBlocks.length;i++) {
        //    int[] blockData = searchBlocks[i];
        //    if(block.getTypeId()==blockData[0] 
        //            && (blockData[1] == -1 || block.getData()== blockData[1]) ) {
        final BlockState state = block.getState();
        int[] replaceData = replacements.get(new Mask(state.getTypeId(),state.getRawData()));
        if(replaceData!=null) {
            byte dv = state.getRawData();
            state.setTypeId(replaceData[0]);
            if(replaceData[1]>=0) {
                state.setRawData((byte) replaceData[1]);
            } else {
                state.setRawData(dv);
            }
            try{
                state.update(true, false);
                if(ChunkAnalysis.needsDoubleUpdate(state)) {
                    pendingUpdates.add(state);
                    //if(!pendingChunks.contains(state.getChunk())) {
                    //    pendingChunks.add(state.getChunk());    
                    //}
                }
            } catch(NullPointerException e) {
                Logger.getGlobal().info("NULL: "+state.getX()+" "+state.getY()+" "+state.getZ()+" "+state.getType()+" "+state.getRawData());
            }
            foundBlocks++;
        }
    }
    
    /**
     * Don't use. Too many tasks are created. Server crashes.
     */
    @Override
    protected synchronized void schedulePendingUpdates() {
        /*final Set<BlockState> states = pendingUpdates;
        pendingUpdates = new HashSet();
        final Set<Chunk> chunks = pendingChunks;
        pendingChunks = new HashSet();
        new BukkitRunnable() {
            @Override
            public void run() {
                for(BlockState state: states) {
                    state.update(true, false);
                }
                for(Chunk chunk: chunks) {
                    World world = chunk.getWorld();
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();
                    if(!world.unloadChunk(chunk)) {
                        DevUtil.log(4,"Unable to unload chunk directly after double update");
                        chunk = null;
                        if(!world.unloadChunkRequest(chunkX, chunkZ)) {
                            DevUtil.log(4,"Unable to queue unload chunk after double update");
                        }

                    }
                }
            }
        }.runTaskLater(ChunkAnalysis.getInstance(), 1);*/
    }
    
    @Override
    public boolean hasPendingUpdates() {
        return !pendingUpdates.isEmpty();
    }
    
    @Override
    public Set<BlockState> getPendingUpdates() {
        return pendingUpdates;
    }
    
    @Override
    public void clearPendingUpdates() {
        pendingUpdates = new HashSet<BlockState>();
    }
    /*@Override
    public String statMessage() {
        DevUtil.log("Search for "+ searchBlocks.length+" blocks "+searchBlocks[0][0]);
        DevUtil.log("Replace with for "+ replaceBlocks.length+" blocks "+replaceBlocks[0][0]);
        String result = "Replaced "+foundBlocks+" of ID ";
        for(int i=0; i<searchBlocks.length-1;i++) {
            result = result+searchBlocks[i][0]+":"+searchBlocks[i][1]+", ";
        }
        if(searchBlocks.length>0) {
            result = result+searchBlocks[searchBlocks.length-1][0]+":"+searchBlocks[searchBlocks.length-1][1];
        }
        return result;
    }*/
    
    @Override
    public int[][] getBlockIds() {
        int[][] result = new int[searchBlocks.length*2][];
        System.arraycopy(searchBlocks, 0, result, 0, searchBlocks.length);
        System.arraycopy(replaceBlocks, 0, result, searchBlocks.length, searchBlocks.length);
        return result;
    }

    @Override
    public void saveResults(int jobId) {
        // save nothing
    }

    @Override
    public String getName() {
        return "replacement";
    }

    @Override
    public String getDetails() {
        String result = "Replacing blocks:\n";
        for (int i = 0; i<searchBlocks.length;i++) {
            int[] searchData = searchBlocks[i];
            int[] replaceData = replaceBlocks[i];
            result = result + " - ["+searchData[0] + ":" + (searchData[1]==-1?"?":searchData[1]) + "] -> "
                                +"["+replaceData[0]+ ":" + (replaceData[1]==-1?"?":replaceData[1]) + "]\n";
        }
        return result;
    }

    class Mask {
        
        private final int searchID;
        private final int searchDV;
                
        
        public Mask(int searchID, int searchDV) {
            this.searchID = searchID;
            this.searchDV = searchDV;
        }
          
        @Override
        public boolean equals(Object compare) {
            if(!(compare instanceof Mask)) {
                return false;
            }
            Mask other = (Mask) compare;
            return (other.searchID==searchID 
                    && (other.searchDV == -1 || other.searchDV == searchDV) );
//            if(other.searchID == searchID) 
//Logger.getGlobal().info(""+searchID+" "+searchDV+" "+other.searchID+" "+other.searchDV+" "+result);
//            return result;
        }

        @Override
        public int hashCode() {
            //int hash = 7;this.ha
            //hash = 31 * hash + this.searchID;
            return this.searchID;//hash
        }


    }
}
