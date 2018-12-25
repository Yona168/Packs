package com.github.yona168.packs

import org.bukkit.inventory.ItemStack

enum class PackLevel(val level: Int) {
    COAL(1),
    IRON(2),
    REDSTONE(3),
    GOLD(4),
    DIAMOND(5);

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