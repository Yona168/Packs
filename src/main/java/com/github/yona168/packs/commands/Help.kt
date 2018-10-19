package com.github.yona168.packs.commands

import monotheistic.mongoose.core.components.commands.CommandPart
import monotheistic.mongoose.core.components.commands.CommandPartInfo
import monotheistic.mongoose.core.components.commands.PluginInfo
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.*
import javax.activation.CommandInfo

class Help(private val commandRoots: MutableList<CommandPart>) : CommandPart(CommandPartInfo("Displays all commands", "help [page]", "help", 1, true, true)) {
    init {
        commandRoots.add(0, this)
    }

    override fun initExecute(sender: CommandSender?, p1: String?, args: Array<out String>?, info: PluginInfo?, p4: MutableList<Any>?): Optional<Boolean> {
        info ?: return Optional.of(false)
        val page = args?.getOrNull(0)?.toIntOrNull() ?: 1
        val first = info.mainColor
        val second = info.secondaryColor
        val pageAmt = commandRoots.size / 7 + if (commandRoots.size % 7 > 0) 1 else 0
        if (page > pageAmt) {
            sender?.sendMessage("${info.displayName}: ${ChatColor.RED}Page out of range!")
            return Optional.of(true)
        }
        sender?.sendMessage(StringBuilder().also { builder ->
            builder.append("-----${info.displayName}${ChatColor.WHITE} Commands $page/$pageAmt-----\n")
            commandRoots.asSequence().drop((page - 1) * 7).forEach { root ->
                builder.append("$first${root.partName}->$second${root.partDescription}\n")
            }
        }.toString())
        return Optional.of(true)
    }
}