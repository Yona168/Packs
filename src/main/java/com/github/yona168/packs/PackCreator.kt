package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.colored
import com.github.yona168.packs.conveniencies.itemBuilder
import com.gitlab.avelyn.architecture.base.Component
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.util.*


object PackCreator : Component() {
    private lateinit var lore: MutableList<String>
    private var playerLore: String? = null
    private lateinit var defaultName: String
    fun createPackFor(level: PackLevel): ItemStack {
        val uuid = UUID.randomUUID()
        return itemBuilder {
            type(Material.LEATHER)
            name(defaultName)
            val clone = ArrayList(lore)
            clone.add(0, "${ChatColor.GOLD}ID: $uuid")
            clone.add(1, "${pluginInfo.secondaryColor}Level: ${level.level}")
            lore(clone)
        }.run {
            var item = BukkitSerializers.serializeInventoryToItem(this, Bukkit.createInventory(null, level.slots, this.itemMeta.displayName))
            item = BukkitSerializers.setString(item, "yona168_packs_id", uuid.toString())
            item
        }
    }

    fun createPackFor(level: PackLevel, player: CommandSender): ItemStack {
        val item = createPackFor(level)
        if (playerLore != null) {
            addPlayerLore(player, item)
        }
        return item
    }

    fun editLevelInfo(item: ItemStack, level: PackLevel): ItemStack {
        item.setLore(1, "${pluginInfo.secondaryColor}Level: ${level.level}")
        return item
    }

    private fun ItemStack.setLore(index: Int, line: String) {
        val meta = itemMeta
        val lore = meta.lore
        lore.add(index, line)
        meta.lore = lore
        itemMeta = meta
    }

    fun addPlayerLore(whoMadeIt: CommandSender, item: ItemStack) {
        item.addLore((playerLore as String).replace("@p", whoMadeIt.name))
    }

    private fun ItemStack.addLore(line: String) = setLore(itemMeta.lore.size, line)

    class ConfigOptionsProcessor(config: YamlConfiguration) : Component() {
        init {
            onEnable {
                lore = config.getStringList("Default Lore").colored.toMutableList()
                if (config.getBoolean("Show Crafter/Creator In Lore")) {
                    playerLore = config.getString("Crafter Lore String").colored
                }
                defaultName = (config.getString("Default Name") ?: "&7pack").colored
            }
        }
    }

    private fun Component.onEnable(function: () -> Unit): Component = this.onEnable(Runnable { function() })
}
