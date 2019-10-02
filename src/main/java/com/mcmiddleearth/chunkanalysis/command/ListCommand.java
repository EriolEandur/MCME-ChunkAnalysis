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

import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import com.mcmiddleearth.chunkanalysis.JobManager;
import com.mcmiddleearth.chunkanalysis.MessageManager;
import com.mcmiddleearth.chunkanalysis.job.Job;
import com.mcmiddleearth.pluginutil.NumericUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public class ListCommand extends AbstractCommand {

    public ListCommand(String... permissionNodes) {
        super(0, false, permissionNodes);
        setShortDescription(": Shows queued ChunkAnalysis jobs or messages.");
        setUsageDescription(" [jobs | messages] [page]: Shows queued ChunkAnalysis jobs or messages. Without optional argument jobs are shown.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        if(args.length>0 && args[0].equalsIgnoreCase("messages")) {
            int page = 1;
            if(args.length>1 && NumericUtil.isInt(args[1])) {
                page = NumericUtil.getInt(args[1]);
            }
            FancyMessage header = new FancyMessage(MessageType.INFO,
                                                    ChunkAnalysis.getMessageUtil())
                                            .addSimple("Stored ChunkAnalysis messages");
            List<FancyMessage> messages = new ArrayList<>();
            List<String[]> messageData = MessageManager.getMessages();
            for(String[] data:messageData) {
                String details = "";
                for(int i = 2; i<data.length;i++) {
                    details=details+data[0]+data[i]+"\\n";
                }
                FancyMessage message = new FancyMessage(MessageType.INFO_NO_PREFIX,
                                                        ChunkAnalysis.getMessageUtil())
                        .addTooltipped(ChatColor.DARK_GREEN+data[1],details);
                messages.add(message);
            }
            ChunkAnalysis.getMessageUtil().sendFancyListMessage((Player)cs, header, messages,
                                                             "/block list message", page);
        } else {
            int pageIndex = 0;
            if(args.length>0) {
                if(args[0].equalsIgnoreCase("jobs")) {
                    pageIndex = 1;
                } else if (!NumericUtil.isInt(args[0])) {
                    ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Invalid subcommand.");
                    return;
                }
            }
            int page = 1;
            if(args.length>pageIndex && NumericUtil.isInt(args[pageIndex])) {
                page = NumericUtil.getInt(args[pageIndex]);
            }
            FancyMessage header = new FancyMessage(MessageType.INFO,
                                                    ChunkAnalysis.getMessageUtil())
                                            .addSimple("Queued ChunkAnalysis jobs");
            List<FancyMessage> messages = new ArrayList<>();
            for(Job job:JobManager.getPendingJobs()) {
                FancyMessage message = new FancyMessage(MessageType.INFO_NO_PREFIX,
                                                        ChunkAnalysis.getMessageUtil())
                        .addFancy(ChatColor.GREEN+"[ID"+job.getId()+"] "+ChatColor.WHITE
                                    + job.getActionName()+" job by "+ChatColor.GREEN+job.getOwnerName(),
                                  "/block cancel "+job.getId(),
                                  (job.detailMessage()+"\n"+ChatColor.BLUE+"Click to cancel."));
                messages.add(message);
            }
            ChunkAnalysis.getMessageUtil().sendFancyListMessage((Player)cs, header, messages,
                                                             "/block list ", page);
        }
    }
    
    
}
