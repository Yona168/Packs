package com.github.yona168.packs

import com.github.yona168.packs.commands.GivePack
import com.github.yona168.packs.commands.Help
import com.github.yona168.packs.conveniencies.ktAddChild
import com.github.yona168.packs.conveniencies.onEnable
import com.gitlab.avelyn.core.components.ComponentPlugin
import monotheistic.mongoose.core.components.commands.CommandSelector
import monotheistic.mongoose.core.files.Configuration
import org.bukkit.ChatColor
import kotlin.streams.toList


class Packs : ComponentPlugin() {

    init {
        val config = Configuration.loadConfiguration(this.classLoader,this.dataFolder.toPath(), "config.yml")
        addChild(PackUsageHandler(config))
        addChild(PackCreator.ConfigOptionsProcessor(config))
        if (config.getBoolean("Crafting")) {
            addChild(RecipeRegistery(config))
        }
        val commandSelector = CommandSelector(pluginInfo) { sender, cmd, args, plInfo, objs ->
            sender.sendMessage("${plInfo.displayName}: ${ChatColor.RED}Invalid command! do /${plInfo.tag} help for help!")
            false
        }
        commandSelector ktAddChild GivePack()
        commandSelector ktAddChild Help(commandSelector.commandPartChildren.toList().toMutableList())
        this.onEnable {
            getCommand("pk").executor = commandSelector
        }
        addChild(commandSelector)
    }


}

