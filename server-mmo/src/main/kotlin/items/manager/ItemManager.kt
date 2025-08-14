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

package pl.decodesoft.items.manager

import pl.decodesoft.items.model.Item
import pl.decodesoft.items.model.ItemType
import pl.decodesoft.player.manager.UserManager
import pl.decodesoft.player.model.PlayerData
import java.util.concurrent.ConcurrentHashMap

// Menedżer zarządzający itemami w grze
class ItemManager {

    // Predefiniowane itemy w grze - baza danych itemów
    private val gameItems = mutableMapOf<String, Item>()

    init {
        initializeGameItems() // Inicjalizuj wszystkie itemy w grze
    }

    // Inicjalizuje wszystkie dostępne itemy w grze
    private fun initializeGameItems() {
        // === PODSTAWOWE ITEMY (dla przykładów) ===
        addGameItem(Item("sword_01", "Iron Sword", ItemType.WEAPON,
            strengthBonus = 5, staminaBonus = 10)) // Żelazny miecz z przykładu

        addGameItem(Item("armor_01", "Leather Armor", ItemType.ARMOR,
            strengthBonus = 2, agilityBonus = 1, staminaBonus = 20)) // Skórzana zbroja

        // === BRONIE STARTOWE DLA KAŻDEJ KLASY ===
        addGameItem(Item("weapon_starter_warrior", "Żelazny Miecz Nowicjusza", ItemType.WEAPON,
            strengthBonus = 2, staminaBonus = 2)) // Miecz dla wojownika
        addGameItem(Item("weapon_starter_archer", "Drewniany Łuk Nowicjusza", ItemType.WEAPON,
            agilityBonus = 1, staminaBonus = 2)) // Łuk dla łucznika
        addGameItem(Item("weapon_starter_mage", "Różdżka Nowicjusza", ItemType.WEAPON,
            spellPowerBonus = 2, staminaBonus = 1, manaBonus = 20)) // Różdżka dla maga

        // === HEŁMY ===
        addGameItem(Item("helmet_basic_warrior", "Żelazny Hełm", ItemType.HELMET,
            strengthBonus = 2, staminaBonus = 2)) // Hełm dla wojownika
        addGameItem(Item("helmet_basic_archer", "Skórzana Czapka", ItemType.HELMET,
            agilityBonus = 1, staminaBonus = 2)) // Hełm dla łucznika
        addGameItem(Item("helmet_basic_mage", "Magiczny Kaptur", ItemType.HELMET,
            spellPowerBonus = 2, staminaBonus = 1)) // Hełm dla maga

        // === ZBROJE ===
        addGameItem(Item("armor_basic_warrior", "Płytowa Zbroja", ItemType.ARMOR,
            strengthBonus = 2, staminaBonus = 3)) // Zbroja dla wojownika
        addGameItem(Item("armor_basic_archer", "Skórzana Zbroja", ItemType.ARMOR,
            agilityBonus = 1, staminaBonus = 2)) // Zbroja dla łucznika
        addGameItem(Item("armor_basic_mage", "Magiczna Szata", ItemType.ARMOR,
            spellPowerBonus = 2, staminaBonus = 1)) // Zbroja dla maga

        // === SPODNIE ===
        addGameItem(Item("pants_basic_warrior", "Żelazne Nogawice", ItemType.PANTS,
            strengthBonus = 2, staminaBonus = 3)) // Spodnie dla wojownika
        addGameItem(Item("pants_basic_archer", "Skórzane Spodnie", ItemType.PANTS,
            agilityBonus = 1, staminaBonus = 2)) // Spodnie dla łucznika
        addGameItem(Item("pants_basic_mage", "Magiczne Spodnie", ItemType.PANTS,
            spellPowerBonus = 1, staminaBonus = 2)) // Spodnie dla maga

        // === BUTY ===
        addGameItem(Item("boots_basic_warrior", "Ciężkie Buty", ItemType.BOOTS,
            strengthBonus = 2, staminaBonus = 2)) // Buty dla wojownika
        addGameItem(Item("boots_basic_archer", "Szybkie Buty", ItemType.BOOTS,
            agilityBonus = 1, staminaBonus = 2)) // Buty dla łucznika
        addGameItem(Item("boots_basic_mage", "Magiczne Buty", ItemType.BOOTS,
            spellPowerBonus = 1, staminaBonus = 2)) // Buty dla maga

        // === BRONIE PODSTAWOWE (zostaw jak były) ===
        addGameItem(Item("weapon_basic_warrior", "Żelazny Miecz", ItemType.WEAPON,
            strengthBonus = 2, staminaBonus = 2)) // Miecz dla wojownika
        addGameItem(Item("weapon_basic_archer", "Drewniany Łuk", ItemType.WEAPON,
            agilityBonus = 1, staminaBonus = 2)) // Łuk dla łucznika
        addGameItem(Item("weapon_basic_mage", "Magiczna Różdżka", ItemType.WEAPON,
            spellPowerBonus = 2, staminaBonus = 1)) // Różdżka dla maga

        // === UNIWERSALNE ITEMY ===
        addGameItem(Item("helmet_universal", "Hełm Wędrowca", ItemType.HELMET,
            strengthBonus = 1, agilityBonus = 1, spellPowerBonus = 1, staminaBonus = 2)) // Uniwersalny hełm
        addGameItem(Item("armor_universal", "Zbroja Poszukiwacza", ItemType.ARMOR,
            strengthBonus = 2, agilityBonus = 2, spellPowerBonus = 2, staminaBonus = 3)) // Uniwersalna zbroja

        println("Załadowano ${gameItems.size} itemów do gry") // Log liczby itemów
    }

    // Dodaje item do bazy itemów gry
    private fun addGameItem(item: Item) {
        gameItems[item.id] = item
    }

    // Pobiera item z bazy danych gry po ID
    private fun getItemById(itemId: String): Item? {
        return gameItems[itemId]
    }

    // Zwraca wszystkie dostępne itemy w grze
    fun getAllItems(): Collection<Item> {
        return gameItems.values
    }

    // Zakłada item graczowi i zwraca poprzedni item z tego slotu
    fun equipItem(
        playerId: String,
        itemId: String,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<Item?> {
        // Pobierz item z bazy danych gry
        val item = getItemById(itemId)
            ?: return Result.failure(Exception("Nie znaleziono itemu o ID: $itemId"))

        // Pobierz dane gracza
        val playerData = playerStates[playerId]
            ?: return Result.failure(Exception("Gracz nie jest online"))

        val user = userManager.getUserById(playerId)
            ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        val character = user.getSelectedCharacter()
            ?: return Result.failure(Exception("Gracz nie ma wybranej postaci"))

        try {
            // Zakładamy item w PlayerData
            val previousItem = playerData.equippedItems.equipItem(item)

            // Synchronizuj z danymi postaci
            character.equippedItems = playerData.equippedItems

            // Przelicz statystyki po założeniu itemu
            playerData.updateMaxStatsFromItems()

            // Zapisz zmiany do bazy danych
            userManager.updateUser(user)

            println("Gracz ${character.nickname} założył item: ${item.name}") // Log założenia itemu

            return Result.success(previousItem) // Zwróć poprzedni item jeśli był

        } catch (e: Exception) {
            return Result.failure(Exception("Błąd podczas zakładania itemu: ${e.message}"))
        }
    }

    // Zdejmuje item z gracza i zwraca zdjęty item
    fun unequipItem(
        playerId: String,
        itemType: ItemType,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<Item?> {
        // Pobierz dane gracza
        val playerData = playerStates[playerId]
            ?: return Result.failure(Exception("Gracz nie jest online"))

        val user = userManager.getUserById(playerId)
            ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        val character = user.getSelectedCharacter()
            ?: return Result.failure(Exception("Gracz nie ma wybranej postaci"))

        try {
            // Zdejmij item z PlayerData
            val removedItem = playerData.equippedItems.unequipItem(itemType)
                ?: return Result.failure(Exception("Brak itemu do zdjęcia w tym slocie"))

            // Synchronizuj z danymi postaci
            character.equippedItems = playerData.equippedItems

            // Przelicz statystyki po zdjęciu itemu
            playerData.updateMaxStatsFromItems()

            // Zapisz zmiany do bazy danych
            userManager.updateUser(user)

            println("Gracz ${character.nickname} zdjął item: ${removedItem.name}") // Log zdjęcia itemu

            return Result.success(removedItem)

        } catch (e: Exception) {
            return Result.failure(Exception("Błąd podczas zdejmowania itemu: ${e.message}"))
        }
    }

    // Zwraca szczegółowe informacje o ekwipunku gracza
    fun getDetailedEquipment(playerId: String, playerStates: ConcurrentHashMap<String, PlayerData>): Map<ItemType, Item?> {
        val playerData = playerStates[playerId] ?: return emptyMap()

        return mapOf(
            ItemType.HELMET to playerData.equippedItems.helmet,
            ItemType.ARMOR to playerData.equippedItems.armor,
            ItemType.PANTS to playerData.equippedItems.pants,
            ItemType.BOOTS to playerData.equippedItems.boots,
            ItemType.WEAPON to playerData.equippedItems.weapon
        )
    }

    // Daje graczowi item (w przyszłości z bossów)
    fun giveItemToPlayer(
        playerId: String,
        itemId: String,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<String> {
        val item = getItemById(itemId)
            ?: return Result.failure(Exception("Nie znaleziono itemu"))

        // Na razie automatycznie zakładamy item - w przyszłości można dodać inwentarz
        return equipItem(playerId, itemId, playerStates, userManager).map {
            "Otrzymałeś item: ${item.name}"
        }
    }

    //
    private fun moveItemInInventory(
        playerId: String,
        fromSlot: Int,
        toSlot: Int,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<String> {
        // Sprawdź czy gracz jest online
        playerStates[playerId] ?: return Result.failure(Exception("Gracz nie jest online"))

        val user = userManager.getUserById(playerId)
            ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        val character = user.getSelectedCharacter()
            ?: return Result.failure(Exception("Gracz nie ma wybranej postaci"))

        try {
            // Walidacja slotów
            if (fromSlot < 0 || fromSlot >= 30 || toSlot < 0 || toSlot >= 30) {
                return Result.failure(Exception("Nieprawidłowy numer slotu"))
            }

            if (fromSlot == toSlot) {
                return Result.failure(Exception("Nie można przenieść na ten sam slot"))
            }

            // Jeśli inventory puste, zainicjalizuj startowe itemy
            if (character.inventory.isEmpty()) {
                return Result.failure(Exception("Inventory jest puste - skontaktuj się z administracją"))
            }

            val inventory = character.inventory

            // Pobierz item z fromSlot
            val fromItemId = inventory[fromSlot]
            if (fromItemId == null) {
                println("DEBUG: Serwer - brak itemu w slocie $fromSlot")
                println("DEBUG: Dostępne sloty: ${inventory.keys}")
                return Result.failure(Exception("Brak itemu w slocie źródłowym"))
            }

            // Pobierz item z toSlot (może być null)
            val toItemId = inventory[toSlot]

            if (toItemId == null) {
                // Przenieś na pusty slot
                inventory.remove(fromSlot)
                inventory[toSlot] = fromItemId
                println("DEBUG: Serwer - przeniesiono $fromItemId z slotu $fromSlot na pusty slot $toSlot")
            } else {
                // Zamień miejscami
                inventory[fromSlot] = toItemId
                inventory[toSlot] = fromItemId
                println("DEBUG: Serwer - zamieniono miejscami: $fromItemId ↔ $toItemId")
            }

            // Zapisz do bazy
            userManager.updateUser(user)

            println("Przeniesiono item z slotu $fromSlot do $toSlot dla gracza ${character.nickname}")
            return Result.success("Item przeniesiony pomyślnie")

        } catch (e: Exception) {
            return Result.failure(Exception("Błąd podczas przenoszenia itemu: ${e.message}"))
        }
    }

    private fun moveItemInventoryToEquipment(
        playerId: String,
        inventorySlot: Int,
        equipmentSlot: Int,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<String> {
        val playerData = playerStates[playerId]
            ?: return Result.failure(Exception("Gracz nie jest online"))

        val user = userManager.getUserById(playerId)
            ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        val character = user.getSelectedCharacter()
            ?: return Result.failure(Exception("Gracz nie ma wybranej postaci"))

        try {
            // 1. USUŃ Z INVENTORY
            val itemId = character.inventory.remove(inventorySlot)
                ?: return Result.failure(Exception("Brak itemu w slocie $inventorySlot"))

            // 2. POBIERZ DEFINICJĘ ITEMU
            val item = getItemById(itemId)
                ?: return Result.failure(Exception("Nie znaleziono definicji itemu"))

            // 3. ZAŁÓŻ ITEM (może być zamiana)
            val previousItem = playerData.equippedItems.equipItem(item)

            // 4. JEŚLI BYŁ POPRZEDNI ITEM, DODAJ DO INVENTORY
            if (previousItem != null) {
                // Znajdź wolny slot
                val freeSlot = (0..29).find { !character.inventory.containsKey(it) }
                if (freeSlot != null) {
                    character.inventory[freeSlot] = previousItem.id
                    println("DEBUG: Poprzedni item ${previousItem.name} dodany do slotu $freeSlot")
                } else {
                    // Inventory pełne - przywróć stan
                    character.inventory[inventorySlot] = itemId
                    return Result.failure(Exception("Brak miejsca w inventory"))
                }
            }

            // 5. SYNCHRONIZUJ WSZYSTKO
            character.equippedItems = playerData.equippedItems
            playerData.updateMaxStatsFromItems()
            userManager.updateUser(user)

            println("DEBUG: Przeniesiono ${item.name} z inventory[$inventorySlot] do equipment[$equipmentSlot]")
            return Result.success("Założono ${item.name}")

        } catch (e: Exception) {
            return Result.failure(Exception("Błąd: ${e.message}"))
        }
    }

    private fun moveItemEquipmentToInventory(
        playerId: String,
        equipmentSlot: Int,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<String> {
        val playerData = playerStates[playerId]
            ?: return Result.failure(Exception("Gracz nie jest online"))

        val user = userManager.getUserById(playerId)
            ?: return Result.failure(Exception("Nie znaleziono użytkownika"))

        val character = user.getSelectedCharacter()
            ?: return Result.failure(Exception("Gracz nie ma wybranej postaci"))

        try {
            val itemType = mapSlotToItemType(equipmentSlot)
                ?: return Result.failure(Exception("Nieprawidłowy slot"))

            // 1. ZDEJMIJ Z EQUIPMENT
            val removedItem = playerData.equippedItems.unequipItem(itemType)
                ?: return Result.failure(Exception("Brak itemu do zdjęcia"))

            // 2. ZNAJDŹ WOLNE MIEJSCE W INVENTORY
            val freeSlot = (0..29).find { !character.inventory.containsKey(it) }
                ?: return Result.failure(Exception("Brak miejsca w inventory"))

            // 3. DODAJ DO INVENTORY
            character.inventory[freeSlot] = removedItem.id

            // 4. SYNCHRONIZUJ
            character.equippedItems = playerData.equippedItems
            playerData.updateMaxStatsFromItems()
            userManager.updateUser(user)

            println("DEBUG: Przeniesiono ${removedItem.name} z equipment[$equipmentSlot] do inventory[$freeSlot]")
            return Result.success("Zdjęto ${removedItem.name}")

        } catch (e: Exception) {
            return Result.failure(Exception("Błąd: ${e.message}"))
        }
    }

    // Przenosi item między ekwipunkiem a inwentarzem lub w obrębie każdego z nich
    fun moveItemBetweenSlots(
        playerId: String,
        fromType: String,
        fromSlot: Int,
        toType: String,
        toSlot: Int,
        itemId: String,
        playerStates: ConcurrentHashMap<String, PlayerData>,
        userManager: UserManager
    ): Result<String> {
        return when {
            fromType == "INVENTORY" && toType == "INVENTORY" -> {
                // Przenoszenie w obrębie inwentarza
                moveItemInInventory(playerId, fromSlot, toSlot, playerStates, userManager)
            }

            fromType == "INVENTORY" && toType == "EQUIPMENT" -> {
                // Zakładanie z inwentarza na postać
                moveItemInventoryToEquipment(playerId, fromSlot, toSlot, playerStates, userManager)
            }

            fromType == "EQUIPMENT" && toType == "INVENTORY" -> {
                moveItemEquipmentToInventory(playerId, fromSlot, playerStates, userManager)
            }

            fromType == "EQUIPMENT" && toType == "EQUIPMENT" -> {
                // Zamiana między slotami ekwipunku (rzadko używane)
                Result.success("Zamiana w ekwipunku - nie zaimplementowana")
            }

            else -> {
                Result.failure(Exception("Nieobsługiwany typ przenoszenia: $fromType -> $toType"))
            }
        }
    }

    // Mapuje indeks slotu na ItemType (już istnieje w serwerze, ale dodaj także tutaj)
    private fun mapSlotToItemType(slotIndex: Int): ItemType? {
        return when (slotIndex) {
            0 -> ItemType.HELMET
            1 -> ItemType.ARMOR
            2 -> ItemType.PANTS
            3 -> ItemType.BOOTS
            4 -> ItemType.WEAPON
            else -> null
        }
    }
}