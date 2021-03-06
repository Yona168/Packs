package com.github.yona168.packs.conveniencies

import monotheistic.mongoose.core.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack


fun itemBuilder(init: ItemBuilder.() -> Unit): ItemStack {
    val builder = ItemBuilder(Material.APPLE)
    init(builder)
    return builder.build()
}

fun Inventory.addItemOrDrop(vararg item: ItemStack) {
    val result = this.addItem(*item)
    if (!result.isEmpty())
        result.forEach { _, item -> this.location?.world?.dropItem(this.location, item) }
}

fun Inventory.myRemoveItem(item: ItemStack, amt: Int): Boolean {
    var amt = amt
    val removeActions = emptySet<(Inventory) -> (Unit)>().toMutableSet()
    var currentItem: ItemStack?
    for (i in 0..35) {
        currentItem = this.getItem(i)
        if ((currentItem) != null && currentItem.isSimilar(item)) {
            if (currentItem.amount >= amt) {
                val theItem = currentItem
                removeActions.add { theItem.amount = theItem.amount - amt }
                removeActions.forEach { it.invoke(this) }
                return true
            } else {
                amt -= currentItem.amount
                removeActions.add { setItem(i, null) }
            }
        }
    }
    return false
}

fun String.toPlayerOrNull(): Player? = Bukkit.getPlayer(this)

val String.colored: String
    get() = ChatColor.translateAlternateColorCodes('&', this)
val List<String>.colored: List<String>
    get() = map(String::colored)
