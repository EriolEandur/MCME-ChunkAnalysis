/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.chunkanalysis.job.action;

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import org.bukkit.Material;
import org.bukkit.block.Bed;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Eriol_Eandur
 */
public class JobHalfBedReplace {
    
    public void execute(Block block) {
        if(block.getType().equals(Material.BED_BLOCK)) {
            if(!findSecondHalf(block)) {
                replace(block);
            }
        }
    }
    
    private boolean findSecondHalf(Block block) {
        switch(block.getData()) {
            case 0: return checkSecondHalf(block.getRelative(BlockFace.SOUTH),8);
            case 1: return checkSecondHalf(block.getRelative(BlockFace.WEST),9);
            case 2: return checkSecondHalf(block.getRelative(BlockFace.NORTH),10);
            case 3: return checkSecondHalf(block.getRelative(BlockFace.EAST),11);
            case 8: return checkSecondHalf(block.getRelative(BlockFace.NORTH),0);
            case 9: return checkSecondHalf(block.getRelative(BlockFace.EAST),1);
            case 10: return checkSecondHalf(block.getRelative(BlockFace.SOUTH),2);
            case 11: return checkSecondHalf(block.getRelative(BlockFace.WEST),3);
        }
        return false;
    }
    
    private boolean checkSecondHalf(Block block, int data) {
        return block.getType().equals(Material.BED_BLOCK)
            && block.getData() == (byte)data;
    }
    
    private void replace(Block block) {
        BlockState replacement = block.getState();
        byte data = block.getData();
        Bed bed = (Bed)block.getState();
        if(data<8) {
            switch(bed.getColor()) {
                case GREEN:
                    replacement.setType(Material.SPRUCE_FENCE_GATE);
                    replacement.setRawData((byte)(data+12));
                    replacement.update(true,false);
                    break;
                case RED:
                    replacement.setType(Material.FENCE_GATE);
                    replacement.setRawData((byte)(data+12));
                    replacement.update(true,false);
                    break;
                case BLUE:                
                    replacement.setType(Material.DARK_OAK_FENCE_GATE);
                    replacement.setRawData((byte)(data+4));
                    replacement.update(true,false);
                    break;
                default:
                    placeSign(block, bed, replacement, data);
            }
        } else {
            placeSign(block, bed, replacement, data);
        }
    }
    
    private void placeSign(Block block, Bed bed, BlockState replacement, byte data) {
        replacement.setType(Material.SIGN_POST);
        replacement.update(true,false);
        new BukkitRunnable() {
            @Override
            public void run() {
                BlockState state = block.getState();
                if(state instanceof Sign) {
                    Sign sign = (Sign) state;
                    sign.setLine(0, "BED");
                    sign.setLine(1, bed.getColor().toString());
                    sign.setLine(2, "Data: "+data);
                    sign.update(true,false);
                }
            }
        }.runTaskLater(ChunkAnalysis.getInstance(), 1);
    }
}
