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

package pl.decodesoft.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import pl.decodesoft.MMOGame
import pl.decodesoft.ui.character.ClientItem
import pl.decodesoft.ui.inventory.InventoryItem

// Manager itemów z ładowaniem z plików i pełnym inventory
class ItemManager(private val game: MMOGame) {

    // Definicje itemów w grze - id -> item (ładowane z plików)
    private val itemDefinitions = mutableMapOf<String, ClientItem>()

    // Inventory gracza - slot -> item
    private val playerInventory = mutableMapOf<Int, InventoryItem>()

    companion object {
        const val INVENTORY_SIZE = 30 // 6x5 slotów
        const val ITEMS_FOLDER = "items/"
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

        val itemFiles = itemsFolder.list(".txt")

        for (file in itemFiles) {
            try {
                val item = parseItemFromFile(file)
                if (item != null) {
                    itemDefinitions[item.id] = item
                    println("Załadowano item: ${item.name} z pliku ${file.name()}")
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
            ClientItem(
                id = data["id"] ?: return null,
                name = data["name"] ?: return null,
                type = data["type"] ?: return null,
                strengthBonus = data["strengthBonus"]?.toInt() ?: 0,
                agilityBonus = data["agilityBonus"]?.toInt() ?: 0,
                spellPowerBonus = data["spellPowerBonus"]?.toInt() ?: 0,
                staminaBonus = data["staminaBonus"]?.toInt() ?: 0
            )
        } catch (e: Exception) {
            println("Błąd parsowania pliku ${file.name()}: ${e.message}")
            null
        }
    }

    private fun createExampleItemFiles() {
        println("Informacja: Utwórz folder 'assets/items/' i dodaj pliki itemów.")
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
        """.trimIndent())

        // Fallback - ładuj przykładowe itemy do pamięci
        loadFallbackItems()
    }

    private fun loadFallbackItems() {
        val fallbackItems = listOf(
            // POPRAWIONE - dokładnie jak na serwerze:
            ClientItem("sword_01", "Iron Sword", "WEAPON", 5, 0, 0, 10),
            ClientItem("armor_01", "Leather Armor", "ARMOR", 2, 1, 0, 20),
            ClientItem("helmet_01", "Iron Helmet", "HELMET", 3, 0, 0, 2),
            ClientItem("pants_01", "Chain Pants", "PANTS", 2, 0, 0, 3),
            ClientItem("boots_01", "Leather Boots", "BOOTS", 0, 3, 0, 1),

            // BRONIE STARTOWE DLA KAŻDEJ KLASY
            ClientItem("weapon_starter_warrior", "Żelazny Miecz Nowicjusza", "WEAPON", 2, 0, 0, 2),
            ClientItem("weapon_starter_archer", "Drewniany Łuk Nowicjusza", "WEAPON", 0, 1, 0, 2),
            ClientItem("weapon_starter_mage", "Różdżka Nowicjusza", "WEAPON", 0, 0, 2, 1, 20),

            // PODSTAWOWE ITEMY KLASOWE
            ClientItem("helmet_basic_warrior", "Żelazny Hełm", "HELMET", 2, 0, 0, 2),
            ClientItem("helmet_basic_archer", "Skórzana Czapka", "HELMET", 0, 1, 0, 2),
            ClientItem("helmet_basic_mage", "Magiczny Kaptur", "HELMET", 0, 0, 2, 1),

            ClientItem("armor_basic_warrior", "Płytowa Zbroja", "ARMOR", 2, 0, 0, 3),
            ClientItem("armor_basic_archer", "Skórzana Zbroja", "ARMOR", 0, 1, 0, 2),
            ClientItem("armor_basic_mage", "Magiczna Szata", "ARMOR", 0, 0, 1, 2),

            ClientItem("pants_basic_warrior", "Żelazne Nogawice", "PANTS", 2, 0, 0, 3),
            ClientItem("pants_basic_archer", "Skórzane Spodnie", "PANTS", 0, 1, 0, 2),
            ClientItem("pants_basic_mage", "Magiczne Spodnie", "PANTS", 0, 0, 1, 2),

            ClientItem("boots_basic_warrior", "Ciężkie Buty", "BOOTS", 2, 0, 0, 2),
            ClientItem("boots_basic_archer", "Szybkie Buty", "BOOTS", 0, 1, 0, 2),
            ClientItem("boots_basic_mage", "Magiczne Buty", "BOOTS", 0, 0, 1, 2),

            ClientItem("weapon_basic_warrior", "Żelazny Miecz", "WEAPON", 2, 0, 0, 2),
            ClientItem("weapon_basic_archer", "Drewniany Łuk", "WEAPON", 0, 1, 0, 2),
            ClientItem("weapon_basic_mage", "Magiczna Różdżka", "WEAPON", 0, 0, 2, 1),

            ClientItem("helmet_universal", "Hełm Wędrowca", "HELMET", 1, 1, 1, 2),
            ClientItem("armor_universal", "Zbroja Poszukiwacza", "ARMOR", 2, 2, 2, 3)
        )

        fallbackItems.forEach { item ->
            itemDefinitions[item.id] = item
        }

        println("Załadowano ${fallbackItems.size} fallback itemów z nowymi broniami startowymi")
    }

    fun setInventoryItem(slot: Int, item: InventoryItem) {
        playerInventory[slot] = item
    }

    fun getItemDefinition(itemId: String): ClientItem? {
        return itemDefinitions[itemId]
    }

    fun clearInventory() {
        playerInventory.clear()
        println("DEBUG: Wyczyszczono lokalne inventory")
    }

    // === INVENTORY OPERATIONS ===

    fun addItemToInventory(itemId: String, quantity: Int = 1): Boolean {
        val itemDef = getItemDefinition(itemId) ?: return false

        // Znajdź wolny slot
        val freeSlot = findFreeSlot()
        if (freeSlot == -1) return false

        playerInventory[freeSlot] = InventoryItem(itemId, itemDef.name, false, quantity)
        println("Dodano ${itemDef.name} do inventory slot $freeSlot")
        return true
    }

    private fun removeItemFromInventory(slot: Int): InventoryItem? {
        val removed = playerInventory.remove(slot)
        if (removed != null) {
            println("Usunięto ${removed.name} z inventory slot $slot")
        }
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
        println("DEBUG: moveItemInventoryToEquipment($fromSlot -> equipment slot $equipmentSlot)")

        val item = playerInventory[fromSlot] ?: return false
        val itemDef = getItemDefinition(item.id) ?: return false

        // Sprawdź czy item pasuje do slotu
        if (!canEquipInSlot(itemDef, equipmentSlot)) {
            println("DEBUG: Item ${itemDef.name} nie pasuje do equipment slot $equipmentSlot")
            return false
        }

        println("DEBUG: Przenoszenie ${itemDef.name} z inventory slot $fromSlot na equipment slot $equipmentSlot")

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

        println("Przenoszenie ${equippedItem.name} z equipment slot $equipmentSlot do inventory slot $freeSlot")
        return true
    }

    fun moveItemWithinInventory(fromSlot: Int, toSlot: Int): Boolean {
        println("DEBUG: moveItemWithinInventory($fromSlot -> $toSlot)")

        val fromItem = playerInventory[fromSlot]
        if (fromItem == null) {
            println("DEBUG: Brak itemu w źródłowym slocie $fromSlot")
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
        println("DEBUG: Wysłano do serwera: $message")

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

        println("DEBUG: Sprawdzam slot $equipmentSlot (oczekuje $requiredType) vs item type ${itemDef.type}")
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
        println("Razem itemów: ${playerInventory.size}/${INVENTORY_SIZE}")
        println("=======================")
    }

    fun handleItemMoved(fromType: String, fromSlot: Int, toType: String, toSlot: Int, itemId: String) {
        println("DEBUG: handleItemMoved($fromType:$fromSlot -> $toType:$toSlot, itemId=$itemId)")

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
                        println("DEBUG: Zaktualizowano po serwerze: ${fromItem.name} z slot $fromSlot -> slot $toSlot")
                    } else {
                        // Zamień miejscami
                        playerInventory[fromSlot] = toItem
                        playerInventory[toSlot] = fromItem
                        println("DEBUG: Zamieniono po serwerze: ${fromItem.name} ↔ ${toItem.name}")
                    }
                } else {
                    println("DEBUG: BŁĄD: Item $itemId nie znaleziony w slocie $fromSlot")
                }
            }
            fromType == "INVENTORY" && toType == "EQUIPMENT" -> {
                // Usuń z inventory (został założony)
                val removed = removeItemFromInventory(fromSlot)
                println("DEBUG: Usunięto z inventory po założeniu: ${removed?.name}")
            }
            fromType == "EQUIPMENT" && toType == "INVENTORY" -> {
                // Dodaj do inventory (został zdjęty)
                addItemToInventory(itemId, 1)
                println("DEBUG: Dodano do inventory po zdjęciu: $itemId")
            }
        }
    }
}