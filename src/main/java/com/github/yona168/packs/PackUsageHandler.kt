package com.github.yona168.packs

import com.github.yona168.packs.conveniencies.*
import com.gitlab.avelyn.architecture.base.Component
import com.google.common.collect.HashMultimap
import monotheistic.mongoose.core.gui.GUI
import monotheistic.mongoose.core.gui.MyGUI
import monotheistic.mongoose.core.utils.ItemBuilder
import monotheistic.mongoose.core.utils.PluginImpl
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.conversations.*
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack


private val ID_TAG = "yona168_packs_id"

class PackUsageHandler(config: YamlConfiguration) : Component() {
    private val stuffNeeded = HashMultimap.create<PackLevel, ItemStack>()
    private val conversationFactory: ConversationFactory = ConversationFactory(PluginImpl("Packs Conversation"))
            .thatExcludesNonPlayersWithMessage("${pluginInfo.displayName}${ChatColor.RED} You must be a player!").withFirstPrompt(prompt)
            .withTimeout(15)
    private val CANNOT_UPGRADE = itemBuilder {
        type(Material.REDSTONE_BLOCK)
        name("$RED${BOLD}You cannot upgrade this pack any farther!")
        addItemFlags(*ItemFlag.values())
    }
    private val UPGRADE = itemBuilder {
        type(Material.EMERALD_BLOCK)
        name("${GREEN}${BOLD}Upgrade pack size!")
        addItemFlags(*ItemFlag.values())
    }
    private val RENAME_PACK = itemBuilder {
        type(Material.PAPER)
        addItemFlags(*ItemFlag.values())
        name("$WHITE${BOLD}Rename your pack!")
    }
    private val BACK_TO_MAIN= itemBuilder {
        type(Material.REDSTONE_BLOCK)
        addItemFlags(*ItemFlag.values())
        name("$RED$BOLD<--- Back")
    }

    fun mainMenuGui(nextPackLevel: PackLevel?, player: Player, thePack: ItemStack): GUI {
        val gui = MyGUI(pluginInfo.displayName, 9).apply {
            if (nextPackLevel == null) {
                set(CANNOT_UPGRADE, 4)
            } else {
                set(4, UPGRADE) {
                    openGUIToPlayer(player, nextPackLevel, thePack)
                }
            }
            set(3, RENAME_PACK)
            { invClickEvent ->
                val playerThatClicked = invClickEvent.whoClicked as Player
                if (playerThatClicked.canEditPackName()) {
                    playerThatClicked.closeInventory()
                    val item = player.inventory.itemInMainHand
                    val conversation = conversationFactory.buildConversation(playerThatClicked)
                    val id = BukkitSerializers.getString(item, ID_TAG)
                    conversation.context.setSessionData(ChangePackNameData.ID_OF_ITEM, id.get())
                    conversation.begin()
                }
            }
        }
        return gui
    }

    private fun upgradeGui(nextLevel: PackLevel, thePack: ItemStack, player: Player): GUI {
        val gui = MyGUI("$GOLD Materials Needed to Upgrade", 54)
        gui.set(49, itemBuilder {
            type(Material.EMERALD_BLOCK)
            name("$GREEN ${ChatColor.BOLD}>> Upgrade to level ${nextLevel.level} for ${nextLevel.slots} slots! <<")
            addItemFlags(*ItemFlag.values())
        }) { invClickEvent ->
            val itemsRemoved = stuffNeeded.get(nextLevel).all { item ->
                invClickEvent.view.bottomInventory.myRemoveItem(item, item.amount)
            }
            if (!itemsRemoved) {
                return@set
            }
            BukkitSerializers.getInventoryFromItem(thePack)
                    .map { inv ->
                        val newInv = Bukkit.createInventory(inv.holder, inv.size + 9, inv.name)
                        newInv.contents = inv.contents
                        newInv
                    }.ifPresent { newInv ->
                        val newItem = BukkitSerializers.serializeInventoryToItem(thePack, newInv)
                        player.inventory.itemInMainHand = PackCreator.editItemWithPlaceholders(newItem, nextLevel, player)
                    }
            val afterClickNextLevel = nextLevel.next()
            if (afterClickNextLevel == null) {
                player.closeInventory()
            } else {
                openGUIToPlayer(player, afterClickNextLevel, player.inventory.itemInMainHand)
            }
        }
        gui.set(45, BACK_TO_MAIN){
            player.closeInventory()
            mainMenuGui(nextLevel, player, player.inventory.itemInMainHand).open(player)
        }
        return gui
    }

    init {
        onEnable {
            val upgradeSection = config.getConfigurationSection("Pack Upgrades")
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
                    mainMenuGui(nextLevel, event.player, clickedItem).open(event.player)
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
        this ktAddChild myListen<InventoryClickEvent> {
            val view = it.view
            val bottomInv = view.bottomInventory
            if (it.inventory.holder === bottomInv.holder)
                return@myListen
            if (it.currentItem != null && BukkitSerializers.isASerializedInventory(view.topInventory)
                    && BukkitSerializers.getSize(it.currentItem).filter { it != 0 }.isPresent) {
                it.isCancelled = true
            }
        }

    }


    private fun Player.canOpenPack() = this.hasPermission("packs.open")
    private fun Player.canUpgradeTo(level: PackLevel) = this.hasPermission("packs.upgradeto.${level.level}")
    private fun Player.canEditPackName() = this.hasPermission("packs.edit")
    private fun openGUIToPlayer(player: Player, level: PackLevel, pack: ItemStack) {
        if (!player.canUpgradeTo(level)) {
            if (player.openInventory != null) {
                player.closeInventory()
            }
            return
        }
        val gui = upgradeGui(level, pack, player)
        stuffNeeded[level].forEach { gui.addItems(it) }
        gui.open(player)
    }


}

private val prompt = object : StringPrompt() {
    override fun getPromptText(p0: ConversationContext?) = "${pluginInfo.displayName}${pluginInfo.mainColor} Enter the desired new name: "

    override fun acceptInput(context: ConversationContext, input: String): Prompt {
        val player = context.forWhom as Player
        val item = player.inventory.itemInMainHand
        val id = BukkitSerializers.getString(item, ID_TAG)
        val contextId = context.getSessionData(ChangePackNameData.ID_OF_ITEM)
        if (!id.isPresent || id.get() != contextId) {
            return tryAgainPrompt
        } else {
            val name = ChatColor.translateAlternateColorCodes('&', input)
            context.setSessionData(ChangePackNameData.NAME, name)
            player.inventory.itemInMainHand = ItemBuilder(item).name(name).build()
            return confirmedPrompt
        }
    }

    private val confirmedPrompt = object : MessagePrompt() {
        override fun getNextPrompt(p0: ConversationContext?) = Prompt.END_OF_CONVERSATION
        override fun getPromptText(p0: ConversationContext?) = "${pluginInfo.displayName}${pluginInfo.mainColor} Name Changed to ${ChatColor.RESET}${p0?.getSessionData(ChangePackNameData.NAME)}"
    }
    private val tryAgainPrompt = object : MessagePrompt() {
        override fun getNextPrompt(p0: ConversationContext?) = Prompt.END_OF_CONVERSATION
        override fun getPromptText(p0: ConversationContext?) = "${pluginInfo.displayName}${ChatColor.RED} You are not holding the pack!"
    }


}

private enum class ChangePackNameData {
    NAME,
    ID_OF_ITEM
}


