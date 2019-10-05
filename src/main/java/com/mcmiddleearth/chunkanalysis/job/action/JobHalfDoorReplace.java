/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.chunkanalysis.job.action;

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Eriol_Eandur
 */
public class JobHalfDoorReplace {
 
    Set<Material> doors = new HashSet<>();
    
    public JobHalfDoorReplace() {
        doors.add(Material.ACACIA_DOOR);
        doors.add(Material.IRON_DOOR_BLOCK);
        doors.add(Material.JUNGLE_DOOR);
        doors.add(Material.BIRCH_DOOR);
        doors.add(Material.WOODEN_DOOR);
        doors.add(Material.SPRUCE_DOOR);
        doors.add(Material.DARK_OAK_DOOR);
    }

    public void execute(Block block) {
        if(isLowerDoor(block)) {
            //Logger.getGlobal().info("Found lower door!");
            Block upper = block.getRelative(BlockFace.UP);
            if(!(upper.getType().equals(block.getType()) && upper.getData()>=8)) {
                //Logger.getGlobal().info("Replace lower door!");
                BlockState state = block.getState();
                setReplacement(state);
                state.update(true, false);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        state.update(true,false);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                state.update(true,false);
                            }

                        }.runTaskLater(ChunkAnalysis.getInstance(), 1);
                    }
                    
                }.runTaskLater(ChunkAnalysis.getInstance(), 1);
            }
        }
    }
    
    private boolean isLowerDoor(Block block) {
        return isDoor(block) && block.getData()<8;
    }
    
    private boolean isDoor(Block block) {
        Material mat = block.getType();
        return doors.contains(mat);
    }
    
    private void setReplacement(BlockState state) {
        Material mat = state.getType();
        byte data = state.getRawData();
        if(mat.equals(Material.WOODEN_DOOR)) {
            state.setType(Material.END_ROD);
            state.setRawData(data);
        } else if(mat.equals(Material.ACACIA_DOOR)) {
            switch(data) {
                case 0: case 1:
                    state.setType(Material.END_ROD);
                    state.setRawData((byte)(data+4));
                    break;
                default:
                    state.setType(Material.OBSERVER);
                    state.setRawData((byte)(data-2));
            }
        } else if(mat.equals(Material.JUNGLE_DOOR)) {
            state.setType(Material.OBSERVER);
            state.setRawData((byte)(data+2));
        } else if(mat.equals(Material.IRON_DOOR_BLOCK)) {
            state.setType(Material.DISPENSER);
            state.setRawData((byte)(data+8));
        } else if(mat.equals(Material.SPRUCE_DOOR)) {
            if(data<2) {
                state.setType(Material.DISPENSER);
                state.setRawData((byte)(data+12));
            } else {
                state.setType(Material.HOPPER);
                state.setRawData((byte)data);
            }
        } else if(mat.equals(Material.DARK_OAK_DOOR)) {
            if(data<3) {
                state.setType(Material.HOPPER);
                if(data<2) {
                    state.setRawData((byte)(data+4));
                } else {
                    state.setRawData((byte)8);
                }
            } else {
                state.setType(Material.ENDER_PORTAL_FRAME);
                state.setRawData((byte)1);
            }
        } else if(mat.equals(Material.BIRCH_DOOR)) {
            switch(data) {
                case 0: 
                    state.setType(Material.SEA_LANTERN);
                    state.setRawData((byte)0);
                    break;
                case 1: 
                    state.setType(Material.STAINED_GLASS);
                    state.setRawData((byte)15);
                    break;
                default:
                    state.setType(Material.GREEN_SHULKER_BOX);
                    state.setRawData((byte)(data+2));
            }
        }
    }
    
}

