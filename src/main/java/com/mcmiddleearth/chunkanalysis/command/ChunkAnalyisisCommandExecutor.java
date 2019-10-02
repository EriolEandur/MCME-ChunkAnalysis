/* 
 *  Copyright (C) 2017 Minecraft Middle Earth
 * 
 *  This file is part of ChunkAnalysis.
 * 
 *  CommonerVote is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CommonerVote is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ChunkAnalysis.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.chunkanalysis.command;

import com.mcmiddleearth.chunkanalysis.Permission;
import com.mcmiddleearth.chunkanalysis.ChunkAnalysis;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Ivanpl, Eriol_Eandur
 */
public class ChunkAnalyisisCommandExecutor implements CommandExecutor {

    @Getter
    private final Map<String,AbstractCommand> commands = new LinkedHashMap<>();
    
    public ChunkAnalyisisCommandExecutor() {
        addCommandHandler("dev", new DevCommand(Permission.DEV));
        addCommandHandler("list", new ListCommand(Permission.USE));
        addCommandHandler("suspend", new SuspendCommand(Permission.USE));
        addCommandHandler("resume", new ResumeCommand(Permission.USE));
        addCommandHandler("replace", new ReplaceCommand(Permission.USE));
        addCommandHandler("count", new CountCommand(Permission.USE));
        addCommandHandler("cancel", new CancelCommand(Permission.USE));
        addCommandHandler("help", new HelpCommand(Permission.USE));
        addCommandHandler("config", new ConfigCommand(Permission.USE));
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        if(!string.equalsIgnoreCase("block")) {
            return false;
        }
        if(strings == null || strings.length == 0) {
            sendNoSubcommandErrorMessage(cs);
            return true;
        }
        if(commands.containsKey(strings[0].toLowerCase())) {
            commands.get(strings[0].toLowerCase()).handle(cs, Arrays.copyOfRange(strings, 1, strings.length));
        } else {
            sendSubcommandNotFoundErrorMessage(cs);
        }
        return true;
    }
    
    private void sendNoSubcommandErrorMessage(CommandSender cs) {
        ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Not enough arguments for this command.");
    }
    
    private void sendSubcommandNotFoundErrorMessage(CommandSender cs) {
        ChunkAnalysis.getMessageUtil().sendErrorMessage(cs, "Subcommand not found.");
    }
    
    private void addCommandHandler(String name, AbstractCommand handler) {
        commands.put(name, handler);
    }    
}
