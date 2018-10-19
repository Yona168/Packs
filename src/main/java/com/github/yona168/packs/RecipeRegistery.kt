package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.onEnable
import com.gitlab.avelyn.architecture.base.Component
import monotheistic.mongoose.core.files.Configuration
import monotheistic.mongoose.core.utils.PluginImpl
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import java.nio.file.Path


class RecipeRegistery(path:Path) : Component() {
    init {
        onEnable {
            val result = createPackFor(PackLevel.COAL)
            val recipe = ShapedRecipe(NamespacedKey(PluginImpl("RecipeRegistry"), "RecipeRegistry"), result)
            recipe.shape("123", "456", "789")
            val config = Configuration(path, "config.yml", javaClass.classLoader)
            val section = config.configuration().getConfigurationSection("Pack Recipe")
            section.getKeys(false).forEach { recipe.setIngredient(it[0], Material.valueOf(section.getString(it))) }
            Bukkit.addRecipe(recipe)
        }
    }

}