package com.github.yona168.packs

import com.github.yona168.packs.PackCreator.createPackFor
import com.github.yona168.packs.conveniencies.onEnable
import com.gitlab.avelyn.architecture.base.Component
import monotheistic.mongoose.core.utils.PluginImpl
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ShapedRecipe


class RecipeRegistery(config: YamlConfiguration) : Component() {


    init {
        onEnable {
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

}

private fun CraftItemEvent.onCraftItem() {
    whoClicked ?: return
    PackCreator.addPlayerLore(whoClicked as Player, this.currentItem)
    if (!whoClicked.hasPermission("packs.craft") && inventory.result?.getPackLevel() != null) {
        isCancelled = true
    }
}




