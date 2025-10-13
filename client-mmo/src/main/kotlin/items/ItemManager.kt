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
 *
 * You should have received a copy of the GNU General Public License
 * along with [GreenVale].  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.decodesoft.items

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import pl.decodesoft.MMOGame
import pl.decodesoft.items.character.ClientItem
import pl.decodesoft.items.inventory.InventoryItem

// Manager itemów z ładowaniem z plików i pełnym inventory
class ItemManager(private val game: MMOGame) {

    // Definicje itemów w grze - id -> item (ładowane z plików)
    private val itemDefinitions = mutableMapOf<String, ClientItem>()

    // Inventory gracza - slot -> item
    private val playerInventory = mutableMapOf<Int, InventoryItem>()

    // Tekstury itemów - id -> texture
    private val itemTextures = mutableMapOf<String, Texture>()

    // Tekstury waluty
    private val coinTextures = mutableMapOf<String, Texture>()

    companion object {
        const val INVENTORY_SIZE = 30 // 6x5 slotów
        const val ITEMS_FOLDER = "items/"
    }

    fun loadCoinTextures() {
        coinTextures["gold"] = Texture(Gdx.files.internal("items/icons/currency/gold.png"))
        coinTextures["silver"] = Texture(Gdx.files.internal("items/icons/currency/silver.png"))
        coinTextures["copper"] = Texture(Gdx.files.internal("items/icons/currency/copper.png"))
    }

    // === ITEM DEFINITIONS - LOADING FROM FILES ===

    private fun loadItemsFromFiles() {
        itemDefinitions.clear()

        val itemsFolder = Gdx.files.internal(ITEMS_FOLDER)
        if (!itemsFolder.exists()) {
            println("Folder items/ nie istnieje, tworzę przykładowe itemy...")
            createExampleItemFiles()
            return
        }

        // Listuj pliki .txt w folderze
        val itemFiles = Gdx.files.internal("items/items.txt")
            .readString()
            .lines()
            .filter { it.isNotBlank() }
            .map { Gdx.files.internal("items/$it") }

        for (file in itemFiles) {
            try {
                val item = parseItemFromFile(file)
                if (item != null) {
                    itemDefinitions[item.id] = item
                    //println("Załadowano item: ${item.name} z pliku ${file.name()}")
                }
            } catch (e: Exception) {
                println("Błąd przy ładowaniu ${file.name()}: ${e.message}")
            }
        }

        println("Załadowano ${itemDefinitions.size} itemów z plików")
    }

    private fun parseItemFromFile(file: FileHandle): ClientItem? {
        val lines = file.readString().lines().filter { it.isNotBlank() }

        val data = mutableMapOf<String, String>()
        for (line in lines) {
            if (line.contains("=")) {
                val (key, value) = line.split("=", limit = 2)
                data[key.trim()] = value.trim()
            }
        }

        return try {
            val item = ClientItem(
                id = data["id"] ?: return null,
                name = data["name"] ?: return null,
                type = data["type"] ?: return null,
                rarity = data["rarity"] ?: "COMMON",
                requiredLevel = data["requiredLevel"]?.toIntOrNull() ?: 1,
                strengthBonus = data["strengthBonus"]?.toInt() ?: 0,
                agilityBonus = data["agilityBonus"]?.toInt() ?: 0,
                spellPowerBonus = data["spellPowerBonus"]?.toInt() ?: 0,
                staminaBonus = data["staminaBonus"]?.toInt() ?: 0,
                manaBonus = data["manaBonus"]?.toInt() ?: 0,
                armorBonus = data["armorBonus"]?.toInt() ?: 0,
                attackSpeedBonus = data["attackSpeedBonus"]?.toInt() ?: 0,
                critRatingBonus = data["critRatingBonus"]?.toInt() ?: 0,
                icon = data["icon"],
                requiredClass = data["requiredClass"]?.toIntOrNull()
            )

            // Załaduj teksturę jeśli podano ścieżkę
            item.icon?.let { iconPath ->
                try {
                    val texture = Texture(Gdx.files.internal("items/icons/$iconPath"))
                    itemTextures[item.id] = texture
                    // println("Załadowano teksturę dla ${item.name}: $iconPath")
                } catch (e: Exception) {
                    println("Nie można załadować tekstury $iconPath: ${e.message}")
                }
            }

            item
        } catch (e: Exception) {
            println("Błąd parsowania pliku ${file.name()}: ${e.message}")
            null
        }
    }

    // Dodaj metodę do pobierania tekstury
    fun getItemTexture(itemId: String): Texture? {
        return itemTextures[itemId]
    }

    fun getCoinTexture(coinType: String): Texture? {
        return coinTextures[coinType]
    }

    private fun createExampleItemFiles() {
        println("Informacja: Utwórz folder 'items/' i dodaj pliki itemów.")
        println("Przykład pliku sword_01.txt:")
        println("""
        id=sword_01
        name=Iron Sword
        type=WEAPON
        strengthBonus=8
        agilityBonus=0
        spellPowerBonus=0
        staminaBonus=2
        manaBonus=20
        armorBonus=0
        attackSpeedBonus=3
        critRatingBonus=1.0
        icon=sword.png
        requiredClass=0
    """.trimIndent())

        // Fallback - ładuj przykładowe itemy do pamięci
        loadFallbackItems()
    }

    private fun loadFallbackItems() {
        val fallbackItems = listOf(
            // === WALUTY (bez zmian) ===
            ClientItem("currency_copper", "Copper Coin", "CURRENCY", "COMMON", 1, 0, 0, 0, 0, 0, 0, 0, 0, "currency/copper.png", null),
            ClientItem("currency_silver", "Silver Coin", "CURRENCY", "COMMON", 1, 0, 0, 0, 0, 0, 0, 0, 0, "currency/silver.png", null),
            ClientItem("currency_gold", "Gold Coin", "CURRENCY", "COMMON", 1, 0, 0, 0, 0, 0, 0, 0, 0, "currency/gold.png", null),

            // === PODSTAWOWE (zmienione wartości crit) ===
            ClientItem("sword_01", "Iron Sword", "WEAPON", "UNCOMMON", 1, 5, 0, 0, 10, 0, 0, 3, 22, "sword.png", null),  // było 1.0
            ClientItem("armor_01", "Leather Armor", "ARMOR", "UNCOMMON", 1, 2, 1, 0, 20, 0, 8, 0, 22, "armor.png", null),  // było 1.0
            ClientItem("helmet_universal", "Hełm Wędrowca", "HELMET", "UNCOMMON", 1, 1, 1, 1, 2, 0, 3, 0, 0, null, null),
            ClientItem("armor_universal", "Zbroja Poszukiwacza", "ARMOR", "UNCOMMON", 1, 2, 2, 2, 3, 0, 8, 0, 0, null, null),

            // === WOJOWNIK ===
            ClientItem("wooden_sword", "Drewniany Miecz", "WEAPON", "EPIC", 1, 2, 0, 0, 2, 0, 0, 18, 22, "sword.png", 2),  // było 1.0
            ClientItem("helmet_basic_warrior", "Żelazny Hełm", "HELMET", "UNCOMMON", 1, 2, 0, 0, 2, 0, 5, 0, 0, null, 2),
            ClientItem("armor_basic_warrior", "Płytowa Zbroja", "ARMOR", "UNCOMMON", 1, 2, 0, 0, 3, 0, 15, 0, 0, null, 2),
            ClientItem("pants_basic_warrior", "Żelazne Nogawice", "PANTS", "UNCOMMON", 1, 2, 0, 0, 3, 0, 8, 0, 0, null, 2),
            ClientItem("boots_basic_warrior", "Ciężkie Buty", "BOOTS", "UNCOMMON", 1, 2, 0, 0, 2, 0, 3, 0, 0, null, 2),
            ClientItem("weapon_basic_warrior", "Żelazny Miecz", "WEAPON", "UNCOMMON", 1, 2, 0, 0, 2, 0, 0, 2, 0, null, 2),

            // === ŁUCZNIK ===
            ClientItem("wooden_bow", "Drewniany Łuk", "WEAPON", "RARE", 2, 0, 1, 0, 2, 1, 0, 26, 22, "bow.png", 0),  // było 1.0
            ClientItem("leather_helm", "Skórzany Hełm", "HELMET", "UNCOMMON", 1, 0, 1, 0, 2, 1, 2, 0, 22, "leather_helm.png", 0),  // było 1.0
            ClientItem("leather_chest", "Skórzana Zbroja", "ARMOR", "UNCOMMON", 1, 0, 1, 0, 2, 1, 4, 0, 22, "leather_chest.png", 0),  // było 1.0
            ClientItem("pants_basic_archer", "Skórzane Spodnie", "PANTS", "UNCOMMON", 1, 0, 1, 0, 2, 1, 3, 0, 22, "leather_pants.png", 0),  // było 1.0
            ClientItem("boots_basic_archer", "Szybkie Buty", "BOOTS", "UNCOMMON", 1, 0, 1, 0, 2, 1, 2, 1, 22, "leather_boots.png", 0),  // było 1.0

            // === MAG ===
            ClientItem("wooden_staff", "Drewniana Laska", "WEAPON", "UNCOMMON", 1, 0, 0, 2, 1, 20, 0, 21, 22, "staff.png", 1),  // było 1.0
            ClientItem("cloth_helm", "Magiczny Kaptur", "HELMET", "UNCOMMON", 1, 0, 0, 1, 1, 2, 1, 0, 40, "cloth_helm.png", 1),  // było 1.8 → 40 (1.8 * 22 ≈ 40)
            ClientItem("armor_basic_mage", "Magiczna Szata", "ARMOR", "UNCOMMON", 1, 0, 0, 1, 2, 0, 2, 0, 0, null, 1),
            ClientItem("pants_basic_mage", "Magiczne Spodnie", "PANTS", "UNCOMMON", 1, 0, 0, 1, 2, 0, 1, 0, 0, null, 1),
            ClientItem("boots_basic_mage", "Magiczne Buty", "BOOTS", "UNCOMMON", 1, 0, 0, 1, 2, 0, 1, 0, 0, null, 1),
            ClientItem("weapon_basic_mage", "Magiczna Różdżka", "WEAPON", "UNCOMMON", 1, 0, 0, 2, 1, 0, 0, 2, 0, null, 1),
        )

        fallbackItems.forEach { item ->
            itemDefinitions[item.id] = item
            item.icon?.let { iconPath ->
                try {
                    val texture = Texture(Gdx.files.internal("items/icons/$iconPath"))
                    itemTextures[item.id] = texture
                } catch (e: Exception) {
                    println("Nie można załadować tekstury fallback $iconPath")
                }
            }
        }

        println("Załadowano ${fallbackItems.size} fallback itemów")
    }

    fun setInventoryItem(slot: Int, item: InventoryItem) {
        playerInventory[slot] = item
    }

    fun getItemDefinition(itemId: String): ClientItem? {
        if (itemId.startsWith("currency_")) {
            val parts = itemId.split("_")
            if (parts.size >= 3) {
                val baseCurrencyId = "${parts[0]}_${parts[1]}"
                return itemDefinitions[baseCurrencyId]
            }
        }

        return itemDefinitions[itemId]
    }

    fun clearInventory() {
        playerInventory.clear()
    }

    // === INVENTORY OPERATIONS ===

    private fun addItemToInventory(itemId: String): Boolean {
        val itemDef = getItemDefinition(itemId) ?: return false
        val freeSlot = findFreeSlot()
        if (freeSlot == -1) return false

        playerInventory[freeSlot] = InventoryItem(itemId, itemDef.name, false, 1)
        println("Założono ${itemDef.name} w slot $freeSlot")
        return true
    }

    private fun removeItemFromInventory(slot: Int): InventoryItem? {
        val removed = playerInventory.remove(slot)
        return removed
    }

    fun getInventoryItem(slot: Int): InventoryItem? {
        return playerInventory[slot]
    }

    private fun findFreeSlot(): Int {
        for (i in 0 until INVENTORY_SIZE) {
            if (!playerInventory.containsKey(i)) {
                return i
            }
        }
        return -1
    }

    // === ITEM MOVEMENT OPERATIONS ===

    fun moveItemInventoryToEquipment(fromSlot: Int, equipmentSlot: Int): Boolean {

        val item = playerInventory[fromSlot] ?: return false
        val itemDef = getItemDefinition(item.id) ?: return false

        // Sprawdź czy item pasuje do slotu
        if (!canEquipInSlot(itemDef, equipmentSlot)) {
            println("DEBUG: Item ${itemDef.name} nie pasuje do equipment slot $equipmentSlot")
            return false
        }

        // Wyślij do serwera
        val message = "MOVE_ITEM|${game.localPlayer.id}|INVENTORY|$fromSlot|EQUIPMENT|$equipmentSlot|${item.id}"
        game.sendWebSocketMessage(message)

        return true
    }

    fun moveItemEquipmentToInventory(equipmentSlot: Int): Boolean {
        val equippedItem = getEquippedItemBySlot(equipmentSlot) ?: return false

        // Znajdź wolne miejsce w inventory
        val freeSlot = findFreeSlot()
        if (freeSlot == -1) {
            println("Brak miejsca w inventory!")
            return false
        }

        // Wyślij do serwera
        val message = "MOVE_ITEM|${game.localPlayer.id}|EQUIPMENT|$equipmentSlot|INVENTORY|$freeSlot|${equippedItem.id}"
        game.sendWebSocketMessage(message)

        return true
    }

    fun moveItemWithinInventory(fromSlot: Int, toSlot: Int): Boolean {

        val fromItem = playerInventory[fromSlot]
        if (fromItem == null) {
            debugPrintInventory()
            return false
        }

        if (toSlot >= INVENTORY_SIZE) {
            println("DEBUG: Docelowy slot $toSlot jest poza zakresem")
            return false
        }

        if (fromSlot == toSlot) {
            println("DEBUG: Źródłowy i docelowy slot są takie same ($fromSlot)")
            return false
        }

        // move item do serwera
        val message = "MOVE_ITEM|${game.localPlayer.id}|INVENTORY|$fromSlot|INVENTORY|$toSlot|${fromItem.id}"
        game.sendWebSocketMessage(message)

        return true
    }

    // === EQUIPMENT HELPERS ===

    private fun canEquipInSlot(itemDef: ClientItem, equipmentSlot: Int): Boolean {
        val requiredType = when (equipmentSlot) {
            0 -> "HELMET"  // HEAD slot
            1 -> "ARMOR"   // ARMOR slot
            2 -> "PANTS"   // PANTS slot
            3 -> "BOOTS"   // BOOTS slot
            4 -> "WEAPON"  // WEAPON slot
            else -> return false
        }

        return itemDef.type == requiredType
    }

    private fun getEquippedItemBySlot(slot: Int): ClientItem? {
        val slotType = when (slot) {
            0 -> "HELMET"
            1 -> "ARMOR"
            2 -> "PANTS"
            3 -> "BOOTS"
            4 -> "WEAPON"
            else -> return null
        }

        return game.localPlayer.getEquippedItem(slotType)
    }

    // === RIGHT CLICK OPERATIONS ===

    fun rightClickEquipFromInventory(itemId: String) {
        // Znajdź item w inventory
        val slot = playerInventory.entries.find { it.value.id == itemId }?.key ?: return

        // Znajdź odpowiedni slot equipment
        val itemDef = getItemDefinition(itemId) ?: return
        val equipmentSlot = when (itemDef.type) {
            "HELMET" -> 0
            "ARMOR" -> 1
            "PANTS" -> 2
            "BOOTS" -> 3
            "WEAPON" -> 4
            else -> return
        }

        moveItemInventoryToEquipment(slot, equipmentSlot)
    }

    fun rightClickUnequipToInventory(itemId: String) {
        // Znajdź item w equipment
        val equippedItems = game.localPlayer.getAllEquippedItems()
        val item = equippedItems.find { it.id == itemId } ?: return

        val equipmentSlot = when (item.type) {
            "HELMET" -> 0
            "ARMOR" -> 1
            "PANTS" -> 2
            "BOOTS" -> 3
            "WEAPON" -> 4
            else -> return
        }

        moveItemEquipmentToInventory(equipmentSlot)
    }

    // === INITIALIZATION ===

    fun initialize() {
        loadItemsFromFiles()
        println("ItemManager zainicjalizowany")
    }

    // === PUBLIC API FOR UI ===

    fun getInventoryItemCount(): Int {
        return playerInventory.size
    }

    // === REFRESH METHODS ===

    fun debugPrintInventory() {
        println("=== DEBUG INVENTORY ===")
        if (playerInventory.isEmpty()) {
            println("Inventory jest puste")
        } else {
            playerInventory.forEach { (slot, item) ->
                println("Slot $slot: ${item.name} x${item.quantity}")
            }
        }
        println("Razem itemów: ${playerInventory.size}/$INVENTORY_SIZE")
        println("=======================")
    }

    fun handleItemMoved(fromType: String, fromSlot: Int, toType: String, toSlot: Int, itemId: String) {

        when {
            fromType == "INVENTORY" && toType == "INVENTORY" -> {
                // TERAZ dopiero aktualizuj lokalnie po potwierdzeniu serwera
                val fromItem = playerInventory[fromSlot]
                val toItem = playerInventory[toSlot]

                if (fromItem != null && fromItem.id == itemId) {
                    if (toItem == null) {
                        // Przenieś na pusty slot
                        playerInventory.remove(fromSlot)
                        playerInventory[toSlot] = fromItem
                    } else {
                        // Zamień miejscami
                        playerInventory[fromSlot] = toItem
                        playerInventory[toSlot] = fromItem
                    }
                } else {
                    println("DEBUG: BŁĄD: Item $itemId nie znaleziony w slocie $fromSlot")
                }
            }
            fromType == "INVENTORY" && toType == "EQUIPMENT" -> {
                // Usuń z inventory (został założony)
                removeItemFromInventory(fromSlot)
            }
            fromType == "EQUIPMENT" && toType == "INVENTORY" -> {
                // Dodaj do inventory (został zdjęty)
                addItemToInventory(itemId)
            }
        }
    }

    fun dispose() {
        itemTextures.values.forEach { it.dispose() }
        itemTextures.clear()
        coinTextures.values.forEach { it.dispose() }
        coinTextures.clear()
    }
}