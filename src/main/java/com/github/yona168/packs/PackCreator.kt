package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.colored
import com.github.yona168.packs.conveniencies.itemBuilder
import com.gitlab.avelyn.architecture.base.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.*


object PackCreator : Component() {
    private lateinit var lore: MutableList<String>
    private lateinit var defaultName: String
    fun createPackFor(level: PackLevel): ItemStack {
        val uuid = UUID.randomUUID()
        return itemBuilder {
            type(Material.LEATHER)
            name(defaultName)
            lore(ArrayList(lore))
            addItemFlags(*ItemFlag.values())
        }.run {
            var item = BukkitSerializers.serializeInventoryToItem(this, Bukkit.createInventory(null, level.slots, this.itemMeta.displayName))
            item = BukkitSerializers.setString(item, "yona168_packs_id", uuid.toString())
            item
        }
    }

    fun createPackFor(level: PackLevel, player: CommandSender): ItemStack {
        val item = createPackFor(level)
        editItemWithPlaceholders(item, level, player)
        return item
    }

    fun editItemWithPlaceholders(item: ItemStack, level: PackLevel, player: CommandSender): ItemStack {
        item.mapLore { it.applyPlaceholders(player, level) }
        item.mapName { it.applyPlaceholders(player, level) }
        return item
    }

    private fun ItemStack.mapName(func: (String) -> String) {
        val meta = this.itemMeta
        val name = func(meta.displayName)
        meta.displayName = name
        this.itemMeta = meta
    }

    private fun ItemStack.mapLore(func: (String) -> String) {
        val meta = this.itemMeta
        val newLore = meta.lore.map(func)
        meta.lore = newLore
        this.itemMeta = meta
    }

    private fun ItemStack.setLore(index: Int, line: String) {
        val meta = itemMeta
        val lore = meta.lore
        lore.add(index, line)
        meta.lore = lore
        itemMeta = meta
    }

    private fun ItemStack.addLore(line: String) = setLore(itemMeta.lore.size, line)
    private fun String.applyPlaceholders(player: CommandSender, level: PackLevel): String {
        return this.replace("@p", player.name).replace("@l", level.level.toString())
    }

    class ConfigOptionsProcessor(config: YamlConfiguration) : Component() {
        init {
            onEnable {
                lore = config.getStringList("Default Lore").colored.toMutableList()
                defaultName = (config.getString("Default Name") ?: "&7pack").colored
            }
        }
    }

    private fun Component.onEnable(function: () -> Unit): Component = this.onEnable(Runnable { function() })
}
