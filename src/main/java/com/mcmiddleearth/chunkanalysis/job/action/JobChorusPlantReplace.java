/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.chunkanalysis.job.action;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class JobChorusPlantReplace {
    
    //private static Material newType;
    //private static byte newData;
    
    private Map<Vector,BlockState> replacements = new HashMap<>();
    
    public void execute(Block block) {
        if(block.getType().equals(Material.CHORUS_PLANT)) {
            //Logger.getGlobal().info("Found chorus structure!");
            replacements.clear();
            checkStructure(block,0);
            executeReplacements();
            replacements.clear();
            //Logger.getGlobal().info("FINISHED");
        }
    }
    
    private void checkStructure(Block block,int deep) {
        deep++;
        if(block.getType().equals(Material.CHORUS_PLANT)) {
            //Logger.getGlobal().info("Found chorus block! "+block.getLocation().getBlockX()
              //                       +" "+block.getLocation().getBlockY()
                //                     +" "+block.getLocation().getBlockZ()+" visited: "+isVisited(block)
                  //                   +" recursion: "+deep);
            if(!isVisited(block)) {
                checkBlock(block);
                //Logger.getGlobal().info("Scanning surrounding blocks...");
                checkStructure(block.getRelative(BlockFace.UP),deep);
                checkStructure(block.getRelative(BlockFace.DOWN),deep);
                checkStructure(block.getRelative(BlockFace.NORTH),deep);
                checkStructure(block.getRelative(BlockFace.WEST),deep);
                checkStructure(block.getRelative(BlockFace.EAST),deep);
                checkStructure(block.getRelative(BlockFace.SOUTH),deep);
            }
        }
    }
    
    private void executeReplacements() {
        replacements.forEach((key,value)-> {
            value.update(true, false);
        });
    }
    
    private boolean isVisited(Block block) {
        boolean result = replacements.containsKey(block.getLocation().toVector());
        //Logger.getGlobal().info("Visited: "+result);
        return result;
    }
    
    private void checkBlock(Block block) {
        if(        isConnected(block,"UD")
                || isConnected(block,"D")
                || isConnected(block,"U")) {
            addReplacement(block,Material.LIME_SHULKER_BOX,0);
        } else if (isConnected(block,"NS")
                || isConnected(block,"N")
                || isConnected(block,"S")) {
            addReplacement(block,Material.LIME_SHULKER_BOX,1);
        } else if (isConnected(block,"WE")
                || isConnected(block,"E")
                || isConnected(block,"W")) {
            addReplacement(block,Material.LIME_SHULKER_BOX,2);

        } else if (isConnected(block,"UDN")) {
            addReplacement(block,Material.LIME_SHULKER_BOX,3);
        } else if (isConnected(block,"UDE")) {
            addReplacement(block,Material.LIME_SHULKER_BOX,4);
        } else if (isConnected(block,"UDS")) {
            addReplacement(block,Material.LIME_SHULKER_BOX,5);
        } else if (isConnected(block,"UDW")) {
            addReplacement(block,Material.PINK_SHULKER_BOX,0);

        } else if (isConnected(block,"UDNE")) {
            addReplacement(block,Material.PINK_SHULKER_BOX,1);
        } else if (isConnected(block,"UDES")) {
            addReplacement(block,Material.PINK_SHULKER_BOX,2);
        } else if (isConnected(block,"UDSW")) {
            addReplacement(block,Material.PINK_SHULKER_BOX,3);
        } else if (isConnected(block,"UDNW")) {
            addReplacement(block,Material.PINK_SHULKER_BOX,4);

        } else if (isConnected(block,"UDNS")) {
            addReplacement(block,Material.PINK_SHULKER_BOX,5);
        } else if (isConnected(block,"UDWE")) {
            addReplacement(block,Material.GRAY_SHULKER_BOX,0);

        } else if (isConnected(block,"UDNES")) {
            addReplacement(block,Material.GRAY_SHULKER_BOX,1);
        } else if (isConnected(block,"UDESW")) {
            addReplacement(block,Material.GRAY_SHULKER_BOX,2);
        } else if (isConnected(block,"UDSWN")) {
            addReplacement(block,Material.GRAY_SHULKER_BOX,3);
        } else if (isConnected(block,"UDWNE")) {
            addReplacement(block,Material.GRAY_SHULKER_BOX,4);


        } else if (isConnected(block,"UDNESW")) {
            addReplacement(block,Material.GRAY_SHULKER_BOX,5);

        } else if (isConnected(block,"NESW")) {
            addReplacement(block,Material.SILVER_SHULKER_BOX,0);
        } else if (isConnected(block,"ESW")) {
            addReplacement(block,Material.SILVER_SHULKER_BOX,1);
        } else if (isConnected(block,"NSW")) {
            addReplacement(block,Material.SILVER_SHULKER_BOX,2);
        } else if (isConnected(block,"NEW")) {
            addReplacement(block,Material.SILVER_SHULKER_BOX,3);
        } else if (isConnected(block,"NES")) {
            addReplacement(block,Material.SILVER_SHULKER_BOX,4);

        } else if (isConnected(block,"NE")) {
            addReplacement(block,Material.SILVER_SHULKER_BOX,5);
        } else if (isConnected(block,"ES")) {
            addReplacement(block,Material.CYAN_SHULKER_BOX,0);
        } else if (isConnected(block,"SW")) {
            addReplacement(block,Material.CYAN_SHULKER_BOX,1);
        } else if (isConnected(block,"WN")) {
            addReplacement(block,Material.CYAN_SHULKER_BOX,2);
        } else {
            addReplacement(block,Material.CHORUS_PLANT,0);
        }
            /*if(isChorusPlant(block.getRelative(BlockFace.UP))
                    &&!isChorusPlant(block.getRelative(BlockFace.DOWN))
                    &&!isChorusPlant(block.getRelative(BlockFace.EAST))
                    && !isChorusPlant(block.getRelative(BlockFace.WEST))
                    && !isChorusPlant(block.getRelative(BlockFace.NORTH))
                    && !isChorusPlant(block.getRelative(BlockFace.SOUTH))) {
            }*/
    }
    
    private void addReplacement(Block block, Material type, int data) {
        BlockState state = block.getState();
        state.setType(type);
        state.setRawData((byte)data);
        replacements.put(block.getLocation().toVector(), state);
    }
    
    private boolean isConnected(Block block, String connect) {
        return isChorusPlant(block.getRelative(BlockFace.UP)) == connect.contains("U")
            && isChorusPlant(block.getRelative(BlockFace.NORTH)) == connect.contains("N")
            && isChorusPlant(block.getRelative(BlockFace.EAST)) == connect.contains("E")
            && isChorusPlant(block.getRelative(BlockFace.SOUTH)) == connect.contains("S")
            && isChorusPlant(block.getRelative(BlockFace.WEST)) == connect.contains("W")
            && isChorusPlant(block.getRelative(BlockFace.DOWN)) == connect.contains("D");
    }
    
    private boolean isChorusPlant(Block block) {
        return block.getType().equals(Material.CHORUS_PLANT);
    }
}
