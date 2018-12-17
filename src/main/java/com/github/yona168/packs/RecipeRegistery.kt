package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.*
import com.gitlab.avelyn.architecture.base.Component
import monotheistic.mongoose.core.components.commands.CommandSelector
import monotheistic.mongoose.core.files.Configuration
import monotheistic.mongoose.core.utils.ItemBuilder
import monotheistic.mongoose.core.utils.PluginImpl
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import java.nio.file.Path
import java.util.*


class RecipeRegistery(config: YamlConfiguration) : Component() {


    init {
        onEnable {
            lore=config.getStringList("Default Lore").colored.toMutableList()
            if(config.getBoolean("Show Crafter/Creator In Lore")){
                playerLore=config.getString("Crafter Lore String").colored
            }
            defaultName=(config.getString("Default Name")?:"&7pack").colored

            val result = createPackFor(PackLevel.COAL)
            val recipe = ShapedRecipe(NamespacedKey(PluginImpl("RecipeRegistry"), "RecipeRegistry"), result)
            recipe.shape("123", "456", "789")
            val section = config.getConfigurationSection("Pack Recipe")
            section.getKeys(false).forEach { recipe.setIngredient(it[0], Material.valueOf(section.getString(it))) }
            Bukkit.addRecipe(recipe)

            Bukkit.getPluginManager()?.registerEvent(CraftItemEvent::class.java, object : Listener {}, EventPriority.NORMAL,
                    { _, event ->
                        if (event is CraftItemEvent && isEnabled)
                            event.onCraftItem()
                    }, PluginImpl("CraftItemEvent"))
        }

    }

    private fun CraftItemEvent.onCraftItem() {
        whoClicked ?: return
        val meta=this.currentItem.itemMeta
        val lore=meta.lore
        lore.add((playerLore as String).replace("@p", whoClicked.name))
        meta.lore=lore
        currentItem.itemMeta=meta
        if (!whoClicked.hasPermission("packs.craft") && inventory.result?.getPackLevel() != null) {
            isCancelled = true
        }
    }

}


private lateinit var lore:MutableList<String>
private var playerLore:String? = null
private lateinit var defaultName:String
fun createPackFor(level: PackLevel): ItemStack {
    val uuid = UUID.randomUUID()
    return itemBuilder {
        type(Material.LEATHER)
        name(defaultName)
        val clone=ArrayList(lore)
        clone.add(0,"${ChatColor.GOLD}ID: $uuid")
        clone.add(1, "${pluginInfo.secondaryColor}Level: ${level.level}")
        lore(clone)
    }.run {
        var item=BukkitSerializers.serializeInventoryToItem(this, Bukkit.createInventory(null, level.slots, this.itemMeta.displayName))
        item=BukkitSerializers.setString(item, "id", uuid.toString())
        item
    }
}
fun createPackFor(level:PackLevel, player:CommandSender):ItemStack{
    val item= createPackFor(level)
    if(playerLore==null) {
        return item
    }
    else {
        val meta = item.itemMeta
        val lore = meta.lore
        lore.add((playerLore as String).replace("@p", player.name))
        meta.lore = lore
        item.itemMeta = meta
        return item
    }
}

fun editLevelInfo(item:ItemStack, level:PackLevel):ItemStack{
    val meta=item.itemMeta
    val lore=meta.lore
    lore[1]="${pluginInfo.secondaryColor}Level: ${level.level}"
    meta.lore=lore
    item.itemMeta=meta
    return item
}

