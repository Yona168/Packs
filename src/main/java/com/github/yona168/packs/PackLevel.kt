package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.itemBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

enum class PackLevel(val level: Int) {
    COAL(1),
    IRON(2),
    REDSTONE(3),
    GOLD(4),
    DIAMOND(5),
    UNATTAINABLE(6);

    val slots: Int
        get() = level * 9

    fun next(): PackLevel? {
        val index = PackLevel.values().indexOf(this)
        if (index != PackLevel.values().size - 1)
            return PackLevel.values()[index + 1]
        return null
    }


}

fun ItemStack.getPackLevel(): PackLevel? {
    return BukkitSerializers.getSize(this).map { size ->
        PackLevel.values().find {
            it.slots == size
        }
    }.orElse(null)

}


fun Int.toPackLevelOrNull() = PackLevel.values().find { it.level == this }