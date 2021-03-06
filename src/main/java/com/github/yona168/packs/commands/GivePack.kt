package com.github.yona168.packs.commands

import com.github.yona168.packs.PackCreator.createPackFor
import com.github.yona168.packs.PackLevel
import com.github.yona168.packs.conveniencies.addItemOrDrop
import com.github.yona168.packs.conveniencies.toPlayerOrNull
import com.github.yona168.packs.toPackLevelOrNull
import monotheistic.mongoose.core.components.commands.CommandPart
import monotheistic.mongoose.core.components.commands.CommandPartInfo
import monotheistic.mongoose.core.components.commands.PluginInfo
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.*
import java.util.Optional.of

class GivePack : CommandPart(CommandPartInfo("Gives a pack to target player", "givepack [target] [level]", "givepack", 1, true, true)) {
    override fun initExecute(sender: CommandSender, cmd: String?, args: Array<out String>?, info: PluginInfo?, p4: MutableList<Any>?): Optional<Boolean> {
        val pluginTag = info?.displayName ?: return of(false)
        val mainColor = info.mainColor ?: ChatColor.WHITE
        val target = args?.getOrNull(0)?.toPlayerOrNull()
        if (target == null) {
            sender.sendMessage(incorrectUsageMessage(info))
            return of(false)
        }
        val packLevel = args.getOrNull(1)?.toIntOrNull()?.toPackLevelOrNull() ?: PackLevel.COAL
        target.inventory.addItemOrDrop(createPackFor(packLevel, sender))
        target.sendMessage("$pluginTag $mainColor${ChatColor.BOLD}${sender.name
                ?: "Herobrine"}${ChatColor.RESET}$mainColor gave you a pack of level ${ChatColor.BOLD}${packLevel.level}.")
        return Optional.of(true)
    }

}