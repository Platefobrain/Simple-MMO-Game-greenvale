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

import pl.decodesoft.MMOGame

// Stan przenoszenia itemów
data class ItemInTransfer(
    val itemId: String,
    val itemName: String,
    val sourceType: String, // "INVENTORY" lub "EQUIPMENT"
    val sourceSlot: Int
)

// System zarządzania transferem itemów
class ItemTransferSystem(private val game: MMOGame) {
    private var selectedItem: ItemInTransfer? = null

    // Reference do ItemManager - ustawiana później
    private var itemManager: ItemManager? = null

    fun setItemManager(itemManager: ItemManager) {
        this.itemManager = itemManager
    }

    fun selectItem(itemId: String, itemName: String, sourceType: String, sourceSlot: Int) {
        selectedItem = ItemInTransfer(itemId, itemName, sourceType, sourceSlot)
    }

    fun getSelectedItem(): ItemInTransfer? = selectedItem

    fun clearSelection() {
        selectedItem?.let {
        }
        selectedItem = null
    }

    fun hasSelectedItem(): Boolean = selectedItem != null

    fun moveItem(toType: String, toSlot: Int): ItemInTransfer? {
        val item = selectedItem
        if (item != null) {

            // Używaj ItemManager jeśli dostępny
            val manager = itemManager
            if (manager != null) {
                when {
                    item.sourceType == "INVENTORY" && toType == "EQUIPMENT" -> {
                        // Z inventory na equipment
                        manager.moveItemInventoryToEquipment(item.sourceSlot, toSlot)
                    }
                    item.sourceType == "EQUIPMENT" && toType == "INVENTORY" -> {
                        // Z equipment do inventory
                        manager.moveItemEquipmentToInventory(item.sourceSlot)
                    }
                    item.sourceType == "INVENTORY" && toType == "INVENTORY" -> {
                        // W obrębie inventory
                        val success = manager.moveItemWithinInventory(item.sourceSlot, toSlot)
                        if (!success) {
                            println("BŁĄD: moveItemWithinInventory zwróciło false")
                        }
                    }
                    item.sourceType == "EQUIPMENT" && toType == "EQUIPMENT" -> {
                        // Zamiana w equipment (rzadko używane)
                        println("Zamiana w equipment nie jest jeszcze obsługiwana")
                        // Fallback - wyślij do serwera
                        sendMoveMessage(item, toType, toSlot)
                    }
                }
            } else {
                // Fallback - stary system bez ItemManager
                sendMoveMessage(item, toType, toSlot)
            }

            // Zawsze wyczyść selection po moveItem
            clearSelection()
        }
        return item
    }

    private fun sendMoveMessage(item: ItemInTransfer, toType: String, toSlot: Int) {
        val message = "MOVE_ITEM|${game.localPlayer.id}|${item.sourceType}|${item.sourceSlot}|${toType}|${toSlot}|${item.itemId}"
        game.sendWebSocketMessage(message)
    }

    fun rightClickUnequip(itemId: String) {
        println("Prawy klik - zdejmowanie: $itemId")

        val manager = itemManager
        if (manager != null) {
            manager.rightClickUnequipToInventory(itemId)
        } else {
            // Fallback - stary system
            val message = "RIGHT_CLICK_ITEM|${game.localPlayer.id}|${itemId}|UNEQUIP"
            game.sendWebSocketMessage(message)
        }
    }

}