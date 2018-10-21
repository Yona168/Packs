package com.github.yona168.packs

import com.github.yona168.packs.commands.GivePack
import com.github.yona168.packs.commands.Help
import com.github.yona168.packs.conveniencies.ktAddChild
import com.github.yona168.packs.conveniencies.onEnable
import com.gitlab.avelyn.core.components.ComponentPlugin
import monotheistic.mongoose.core.components.commands.*
import org.bukkit.ChatColor


class Packs : ComponentPlugin() {

    init {
        addChild(Listeners(this.dataFolder.toPath()))
        addChild(RecipeRegistery(this.dataFolder.toPath()))
        val commandSelector = CommandSelector(PluginInfo("Packs", "pk", ChatColor.RED, ChatColor.GOLD)) { sender, cmd, args, plInfo, objs ->
            sender.sendMessage("${plInfo.displayName}: ${ChatColor.RED}Invalid command! do /${plInfo.tag} for help!")
            false
        }


        commandSelector ktAddChild GivePack()
        commandSelector ktAddChild Help(commandSelector.commandPartChildren)
        this.onEnable { getCommand("pk").executor = commandSelector }
        addChild(commandSelector)

    }


}

