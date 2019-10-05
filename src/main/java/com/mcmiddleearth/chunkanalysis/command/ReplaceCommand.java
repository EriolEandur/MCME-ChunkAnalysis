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
package com.mcmiddleearth.chunkanalysis.command;

import com.mcmiddleearth.chunkanalysis.job.action.JobAction;
import com.mcmiddleearth.chunkanalysis.job.action.JobActionQuickReplace;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Eriol_Eandur
 */
public class ReplaceCommand extends AbstractStartCommand {

    public ReplaceCommand(String... permissionNodes) {
        super(1, false, permissionNodes);
        setShortDescription(": Starts a block replace job.");
        setUsageDescription(": Starts a block replace job.");
    }
    
    @Override
    protected JobAction getAction(List<String> args) {
        List<int[]> searchDataList = new ArrayList<>();
        List<int[]> replaceDataList = new ArrayList<>();
        for(String arg: args) {
            if(arg.contains(">")) {
                String[] blockData;
                boolean swap = false;
                if(arg.contains("<>")) {
                    blockData = arg.split("<>");
                    swap = true;
                } else {
                    blockData = arg.split(">");
                }
                int[] searchData = new int[2];
                searchData[0] = getId(blockData[0]);
                searchData[1] = getDv(blockData[0]);
                int[] replaceData = new int[2];
                replaceData[0] = getId(blockData[1]);
                replaceData[1] = getDv(blockData[1]);
                searchDataList.add(searchData);
                replaceDataList.add(replaceData);
                if(swap) {
                    searchDataList.add(replaceData);
                    replaceDataList.add(searchData);
                }
            }
        }
//Logger.getGlobal().info("Arguments analysed getAction ReplaceCommand");
        return new JobActionQuickReplace(searchDataList.toArray(new int[0][0]),
                                         replaceDataList.toArray(new int[0][0]),
                                         args.contains("-chorus"),
                                         args.contains("-doors"),
                                         args.contains("-beds"));
    }

}
