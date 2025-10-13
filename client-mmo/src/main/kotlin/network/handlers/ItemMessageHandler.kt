/*
 * This file is part of [GreenVale]
 *
 * [GreenVale] is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * [GreenVale] is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *7 agi i 12 jak dodam itemy
 * You should have received a copy of the GNU General Public License
 * along with [GreenVale].  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.decodesoft.network.handlers

import com.badlogic.gdx.graphics.Color
import pl.decodesoft.MMOGame
import pl.decodesoft.network.MessageHandler
import pl.decodesoft.items.character.ClientItem
import pl.decodesoft.items.inventory.InventoryItem

// Handler do obsługi komunikatów związanych z itemami
class ItemMessageHandler(private val game: MMOGame) : MessageHandler {

    override fun canHandle(messageType: String): Boolean {
        return messageType in listOf(
            "ITEM_LIST",           // Lista dostępnych itemów
            "PLAYER_EQUIPMENT",    // Ekwipunek gracza
            "PLAYER_INVENTORY",    // Inventory gracza
            "ITEM_EQUIPPED",       // Potwierdzenie założenia itemu
            "ITEM_UNEQUIPPED",     // Potwierdzenie zdjęcia itemu
            "ITEM_EQUIP_FAILED",   // Błąd założenia itemu
            "ITEM_UNEQUIP_FAILED", // Błąd zdjęcia itemu
            "ITEM_GIVEN",          // Otrzymanie itemu
            "ITEM_GIVE_FAILED",    // Błąd dania itemu
            "ITEM_MOVED",          // Potwierdzenie przeniesienia itemu
            "ITEM_MOVE_FAILED",    // Błąd przeniesienia itemu
            "ITEM_PICKED_UP",      // Potwierdzenie podniesienia itemu
            "PICKUP_FAILED",       // Błąd podniesienia itemu
            "REMOVE_DROPPED_ITEM", // Usuń item z ziemi
            "DROPPED_ITEMS_LIST",  // Lista dropnietych itemów
            "STATS_UPDATE",        // Aktualizacja statystyk po zmianie itemów
            "CURRENCY_UPDATE"      // Waluta
        )
    }

    override fun handleMessage(parts: List<String>) {

        when (parts[0]) {
            "ITEM_LIST" -> handleItemList(parts)
            "PLAYER_EQUIPMENT" -> handlePlayerEquipment(parts)
            "PLAYER_INVENTORY" -> handlePlayerInventory(parts)
            "ITEM_EQUIPPED" -> handleItemEquipped(parts)
            "ITEM_UNEQUIPPED" -> handleItemUnequipped(parts)
            "ITEM_EQUIP_FAILED" -> handleItemEquipFailed(parts)
            "ITEM_UNEQUIP_FAILED" -> handleItemUnequipFailed(parts)
            "ITEM_GIVEN" -> handleItemGiven(parts)
            "ITEM_GIVE_FAILED" -> handleItemGiveFailed(parts)
            "ITEM_MOVED" -> handleItemMoved(parts)
            "ITEM_MOVE_FAILED" -> handleItemMoveFailed(parts)
            "ITEM_PICKED_UP" -> handleItemPickedUp(parts)
            "PICKUP_FAILED" -> handlePickupFailed(parts)
            "REMOVE_DROPPED_ITEM" -> handleRemoveDroppedItem(parts)
            "DROPPED_ITEMS_LIST" -> handleDroppedItemsList(parts)
            "STATS_UPDATE" -> handleStatsUpdate(parts)
            "CURRENCY_UPDATE" -> handleCurrencyUpdate(parts)
        }
    }

    // Obsługuje listę wszystkich dostępnych itemów w grze
    private fun handleItemList(parts: List<String>) {
        if (parts.size < 2) return

        val itemsData = parts[1]
        if (itemsData.isEmpty()) {
            println("Otrzymano pustą listę itemów")
            return
        }

        val items = itemsData.split(";").mapNotNull { itemData ->
            val itemParts = itemData.split(":")
            if (itemParts.size >= 13) {
                try {
                    ClientItem(
                        id = itemParts[0],
                        name = itemParts[1],
                        type = itemParts[2],
                        rarity = itemParts[3],
                        requiredLevel = itemParts[4].toInt(),
                        strengthBonus = itemParts[5].toInt(),
                        agilityBonus = itemParts[6].toInt(),
                        spellPowerBonus = itemParts[7].toInt(),
                        staminaBonus = itemParts[8].toInt(),
                        manaBonus = itemParts[9].toInt(),
                        armorBonus = itemParts[10].toInt(),
                        attackSpeedBonus = itemParts[11].toInt(),
                        critRatingBonus = itemParts[12].toInt()
                    )
                } catch (e: NumberFormatException) {
                    println("Błąd parsowania itemu: $itemData")
                    null
                }
            } else {
                println("Nieprawidłowy format itemu: $itemData")
                null
            }
        }

        println("Otrzymano listę ${items.size} itemów z serwera")
    }

    // Obsługuje ekwipunek gracza i aktualizuje Player
    private fun handlePlayerEquipment(parts: List<String>) {
        if (parts.size < 6) return

        val player = game.localPlayer

        // Parsuj i ustaw każdy slot
        player.clearAllEquipment()
        setPlayerEquipmentSlot("HELMET", parts[1], player)
        setPlayerEquipmentSlot("ARMOR", parts[2], player)
        setPlayerEquipmentSlot("PANTS", parts[3], player)
        setPlayerEquipmentSlot("BOOTS", parts[4], player)
        setPlayerEquipmentSlot("WEAPON", parts[5], player)

        println("Zaktualizowano ekwipunek gracza z serwera")
    }

    private fun handlePlayerInventory(parts: List<String>) {
        if (parts.size < 2) return

        val inventoryData = parts[1]
        if (inventoryData.isEmpty()) {
            // Wyczyść inventory jeśli puste
            game.itemManager.clearInventory()
            return
        }

        game.itemManager.clearInventory()

        val items = inventoryData.split(";")

        for (itemData in items) {
            val parts = itemData.split(":")
            if (parts.size >= 3) {
                val slot = parts[0].toInt()
                val itemId = parts[1]
                val quantity = parts[2].toInt()

                val itemDef = game.getItemDefinition(itemId)
                if (itemDef != null) {
                    val inventoryItem = InventoryItem(itemId, itemDef.name, false, quantity)
                    game.setInventoryItem(slot, inventoryItem)
                }
            }
        }

        // odswiezanie inventory
        game.refreshInventoryUI()
    }

    // Pomocnicza do ustawiania pojedynczego slotu
    private fun setPlayerEquipmentSlot(itemType: String, itemData: String, player: pl.decodesoft.player.Player) {
        if (itemData == "none" || itemData.isEmpty()) {
            player.unequipItem(itemType)
            return
        }

        val itemParts = itemData.split(":")

        if (itemParts.size >= 13) {
            try {
                val item = ClientItem(
                    id = itemParts[0],
                    name = itemParts[1],
                    type = itemParts[2],
                    rarity = itemParts[3],
                    requiredLevel = itemParts[4].toInt(),
                    strengthBonus = itemParts[5].toInt(),
                    agilityBonus = itemParts[6].toInt(),
                    spellPowerBonus = itemParts[7].toInt(),
                    staminaBonus = itemParts[8].toInt(),
                    manaBonus = itemParts[9].toInt(),
                    armorBonus = itemParts[10].toInt(),
                    attackSpeedBonus = itemParts[11].toInt(),
                    critRatingBonus = itemParts[12].toInt()
                )
                val replacedItem = player.equipItem(item)
                println("Założono ${item.name} w slot $itemType" +
                        if (replacedItem != null) ", zastąpiono: ${replacedItem.name}" else "")
            } catch (e: NumberFormatException) {
                println("Błąd parsowania itemu w slocie $itemType: $itemData")
            }
        } else if (itemParts.size == 1) {
            val itemId = itemParts[0]
            val itemDef = game.getItemDefinition(itemId)
            if (itemDef != null) {
                val replacedItem = player.equipItem(itemDef)
                println("Założono ${itemDef.name} w slot $itemType" +
                        if (replacedItem != null) ", zastąpiono: ${replacedItem.name}" else "")
            } else {
                println("Nie znaleziono definicji itemu $itemId w ItemManager")
            }
        } else {
            println("Nieprawidłowy format itemu w slocie $itemType: $itemData")
        }
    }

    // Obsługuje potwierdzenie założenia itemu
    private fun handleItemEquipped(parts: List<String>) {
        if (parts.size < 3) return

        val itemData = parts[1]
        val previousItemData = if (parts[2] != "none") parts[2] else null

        println("Założono item: $itemData" + if (previousItemData != null) ", zastąpiono: $previousItemData" else "")

        requestPlayerEquipmentUpdate()

        val message = "Założono item!"
        game.addDamageText(game.localPlayer.x, game.localPlayer.y + 30f, message, Color.GREEN)
    }

    private fun requestPlayerEquipmentUpdate() {
        game.sendWebSocketMessage("GET_PLAYER_EQUIPMENT|${game.localPlayer.id}")
    }

    // Obsługuje potwierdzenie zdjęcia itemu
    private fun handleItemUnequipped(parts: List<String>) {
        if (parts.size < 3) return

        val unequippedItemData = if (parts[1] != "none") parts[1] else null
        val itemType = parts[2]

        println("Zdjęto item typu $itemType: $unequippedItemData")

        val message = "Zdjęto item z slotu $itemType"
        game.addDamageText(game.localPlayer.x, game.localPlayer.y + 30f, message, Color.ORANGE)

        game.localPlayer.unequipItem(itemType)
        requestPlayerEquipmentUpdate()
    }

    // Obsługuje błąd założenia itemu
    private fun handleItemEquipFailed(parts: List<String>) {
        if (parts.size < 2) return

        val errorMessage = parts[1]
        println("Błąd założenia itemu: $errorMessage")

        game.addDamageText(game.localPlayer.x, game.localPlayer.y + 30f, "Błąd: $errorMessage", Color.RED)
        game.receiveNetworkChatMessage("SYSTEM", "System", "Błąd założenia itemu: $errorMessage")
    }

    // Obsługuje błąd zdjęcia itemu
    private fun handleItemUnequipFailed(parts: List<String>) {
        if (parts.size < 2) return

        val errorMessage = parts[1]
        println("Błąd zdjęcia itemu: $errorMessage")

        game.addDamageText(game.localPlayer.x, game.localPlayer.y + 30f, "Błąd: $errorMessage", Color.RED)
        game.receiveNetworkChatMessage("SYSTEM", "System", "Błąd zdjęcia itemu: $errorMessage")
    }

    // Obsługuje otrzymanie itemu
    private fun handleItemGiven(parts: List<String>) {
        if (parts.size < 3) return

        val itemData = parts[1]
        val message = parts[2]

        println("Otrzymano item: $itemData - $message")

        game.addDamageText(game.localPlayer.x, game.localPlayer.y + 30f, message, Color.CYAN)
        game.receiveNetworkChatMessage("SYSTEM", "System", message)

        requestPlayerEquipmentUpdate()
    }

    // Obsługuje błąd dania itemu
    private fun handleItemGiveFailed(parts: List<String>) {
        if (parts.size < 2) return

        val errorMessage = parts[1]
        println("Błąd dania itemu: $errorMessage")

        game.receiveNetworkChatMessage("SYSTEM", "System", "Błąd: $errorMessage")
    }

    // Obsługuje potwierdzenie przeniesienia itemu
    private fun handleItemMoved(parts: List<String>) {

        if (parts.size < 6) {
            println("DEBUG: Za mało części w wiadomości ITEM_MOVED")
            return
        }

        val fromType = parts[1]
        val fromSlot = parts[2].toIntOrNull() ?: -1
        val toType = parts[3]
        val toSlot = parts[4].toIntOrNull() ?: -1
        val itemId = parts[5]

        // Powiadom ItemManager o ruchu
        game.handleItemMoved(fromType, fromSlot, toType, toSlot, itemId)

        // Pokaż komunikat graczowi i zaktualizuj inventory gdy potrzeba
        when {
            fromType == "INVENTORY" && toType == "INVENTORY" -> {
                game.messageManager.showMessage("Przeniesiono item w inwentarzu", 2f, Color.YELLOW)
            }
            fromType == "INVENTORY" && toType == "EQUIPMENT" -> {
                // Aktualizuj inventory po założeniu itemu
                game.requestInventoryUpdate()
                game.messageManager.showMessage("Założono item na postać", 2f, Color.GREEN)
            }
            fromType == "EQUIPMENT" && toType == "INVENTORY" -> {
                // Aktualizuj inventory po zdjęciu itemu
                game.requestInventoryUpdate()
                game.messageManager.showMessage("Zdjęto item z postaci", 2f, Color.CYAN)
            }
            fromType == "EQUIPMENT" && toType == "EQUIPMENT" -> {
                game.messageManager.showMessage("Zamieniono itemy miejscami", 2f, Color.YELLOW)
            }
            else -> game.messageManager.showMessage("Przeniesiono item", 2f, Color.YELLOW)
        }

        // Jeśli przenoszenie dotyczyło ekwipunku, odśwież UI ekwipunku
        if (fromType == "EQUIPMENT" || toType == "EQUIPMENT") {

            // Poproś o aktualizację ekwipunku z serwera
            requestPlayerEquipmentUpdate()
        }
    }

    private fun handleItemMoveFailed(parts: List<String>) {
        if (parts.size < 2) return

        val errorMessage = parts[1]
        println("Błąd przeniesienia itemu: $errorMessage")

        game.messageManager.showMessage(errorMessage, 3f, Color.RED)
    }

    private fun handleItemPickedUp(parts: List<String>) {

        if (parts.size >= 3) {
            val itemDropId = parts[1]
            val itemId = parts[2]

            game.removeDroppedItem(itemDropId)

            val itemName = game.getItemDefinition(itemId)?.name ?: itemId
            game.messageManager.showMessage("Podniesiono: $itemName", 2f, Color.GREEN)

            println("DEBUG: Requesting inventory update")
            game.requestInventoryUpdate()
        }
    }

    private fun handlePickupFailed(parts: List<String>) {
        if (parts.size >= 2) {
            val errorMessage = parts[1]
            game.messageManager.showMessage("Nie można podnieść: $errorMessage", 3f, Color.RED)
        }
    }

    private fun handleRemoveDroppedItem(parts: List<String>) {
        if (parts.size >= 2) {
            val itemDropId = parts[1]
            game.removeDroppedItem(itemDropId)
        }
    }

    private fun handleDroppedItemsList(parts: List<String>) {
        if (parts.size < 2) {
            println("Brak itemów na ziemi")
            return
        }

        val itemsData = parts[1]
        if (itemsData.isEmpty()) {
            println("Lista itemów na ziemi jest pusta")
            return
        }

        // Wyczyść aktualną listę itemów na ziemi
        game.clearDroppedItems()

        val items = itemsData.split(";")
        for (itemData in items) {
            val itemParts = itemData.split(":")
            if (itemParts.size >= 4) {
                try {
                    val dropId = itemParts[0]
                    val itemId = itemParts[1]
                    val x = itemParts[2].toFloat()
                    val y = itemParts[3].toFloat()

                    // Dodaj item na ziemi do gry
                    game.addDroppedItem(dropId, itemId, x, y)

                } catch (e: NumberFormatException) {
                    println("Błąd parsowania itemu na ziemi: $itemData")
                }
            }
        }

        println("Załadowano ${items.size} itemów z ziemi")
    }

    private fun handleCurrencyUpdate(parts: List<String>) {
        if (parts.size < 4) return

        val gold = parts[1].toIntOrNull() ?: 0
        val silver = parts[2].toIntOrNull() ?: 0
        val copper = parts[3].toIntOrNull() ?: 0

        game.updatePlayerCurrency(gold, silver, copper)

        println("Waluta zaktualizowana: ${gold}g ${silver}s ${copper}c")
    }

    // Obsługuje aktualizację statystyk po zmianie itemów
    @Suppress("UNUSED_VARIABLE")
    private fun handleStatsUpdate(parts: List<String>) {
        if (parts.size < 10) return

        try {
            val totalStrength = parts[1].toInt()
            val totalAgility = parts[2].toInt()
            val totalSpellPower = parts[3].toInt()
            val totalStamina = parts[4].toInt()
            val totalMana = parts[5].toInt()
            val totalArmor = parts[6].toIntOrNull() ?: 0
            val attackCooldown = parts[7].toFloatOrNull() ?: 2.0f
            val serverDamage = parts[8].toIntOrNull() ?: 0
            val totalCritChance = parts[9].toDoubleOrNull() ?: 0.0

            game.localPlayer.damage = serverDamage

        } catch (e: NumberFormatException) {
            println("Błąd parsowania statystyk: ${parts.joinToString("|")}")
        }
    }
}