package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.*
import com.gitlab.avelyn.architecture.base.Component
import com.google.common.collect.HashMultimap
import monotheistic.mongoose.core.files.Configuration
import monotheistic.mongoose.core.gui.MyGUI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.ChatColor.GOLD
import org.bukkit.ChatColor.GREEN
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.*
import java.nio.file.Path

class Listeners(config: Configuration) : Component() {
    private val stuffNeeded = HashMultimap.create<PackLevel, ItemStack>()

    init {
        onEnable {
            val upgradeSection = config.configuration().getConfigurationSection("Pack Upgrades")
            for (i in 2..5) {
                val section = upgradeSection.getConfigurationSection("${i - 1}->$i")
                section.getKeys(false).forEach {
                    stuffNeeded.put(i.toPackLevelOrNull(), ItemStack(Material.valueOf(it), section.getInt(it)))
                }
            }
        }
        this ktAddChild myListen<PlayerInteractEvent> { event ->
            val clickedItem = event.item
            clickedItem ?: return@myListen
            val packLevel = clickedItem.getPackLevel()
            packLevel ?: return@myListen
            when (event.player.isSneaking) {
                true -> {
                    val nextLevel = packLevel.next()
                    nextLevel ?: return@myListen
                    openGUIToPlayer(event.player, nextLevel, clickedItem)
                }
                false -> {
                    if (!event.player.canOpenPack()) return@myListen
                    BukkitSerializers.getInventoryFromItem(clickedItem).ifPresent {
                        event.player.openInventory(object : InventoryView() {
                            override fun getPlayer(): HumanEntity = event.player
                            override fun getType(): InventoryType = InventoryType.CHEST
                            override fun getTopInventory(): Inventory = it
                            override fun getBottomInventory(): Inventory = event.player.inventory
                        })
                    }
                }

            }

        }
        this ktAddChild myListen<InventoryCloseEvent> { event ->
            event.inventory.takeIf { BukkitSerializers.isASerializedInventory(it) }?.run {
                event.player.inventory.itemInMainHand = BukkitSerializers.serializeInventoryToItem(event.player.inventory.itemInMainHand, this)
            }

        }

    }


    private fun Player.canOpenPack() = this.hasPermission("packs.open")
    private fun Player.canUpgradeTo(level: PackLevel) = this.hasPermission("packs.upgradeto.${level.level}")
    private fun openGUIToPlayer(player: Player, level: PackLevel, pack: ItemStack) {
        if (!player.canUpgradeTo(level)){
            if(player.openInventory!=null){
                player.closeInventory()
            }
            return
        }
        val gui = createGUI(level, pack, player)
        gui.addItems(stuffNeeded.get(level))
        gui.open(player)
    }

    private fun createGUI(nextLevel: PackLevel, thePack: ItemStack, player: Player): MyGUI {
        return MyGUI("$GOLD Materials Needed to Upgrade", 54).set(49, itemBuilder {
            type(Material.EMERALD_BLOCK)
            name("$GREEN ${ChatColor.BOLD}>> Upgrade to level ${nextLevel.level} for ${nextLevel.slots} slots! <<")
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }) { invClickEvent ->
            val itemsRemoved=stuffNeeded.get(nextLevel).all { item ->
                invClickEvent.view.bottomInventory.myRemoveItem(item, item.amount)
            }
            if(!itemsRemoved){
                return@set
            }
            BukkitSerializers.getInventoryFromItem(thePack)
                    .map { inv ->
                        val newInv = Bukkit.createInventory(inv.holder, inv.size + 9, inv.name)
                        newInv.contents = inv.contents
                        newInv
                    }.ifPresent { newInv ->
                        val newItem = BukkitSerializers.serializeInventoryToItem(thePack, newInv)
                        player.inventory.itemInMainHand = newItem
                    }
            val afterClickNextLevel = nextLevel.next()
            if (afterClickNextLevel == null) {
                player.closeInventory()
            } else {
                openGUIToPlayer(player, afterClickNextLevel, player.inventory.itemInMainHand)
            }
        }
    }


}


