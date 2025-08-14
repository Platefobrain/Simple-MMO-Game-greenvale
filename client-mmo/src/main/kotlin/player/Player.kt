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

package pl.decodesoft.player

import com.badlogic.gdx.graphics.Color
import pl.decodesoft.ui.character.ClientItem
import kotlin.math.sqrt

data class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    val id: String,
    val username: String,
    val characterClass: Int = 0, // 0-łucznik, 1-mag, 2-wojownik
    var isSelected: Boolean = false,
    var maxHealth: Int = 100,
    var currentHealth: Int = 100,
    var maxMana: Int = 100,
    var currentMana: Int = 100,
    var level: Int = 1,
    var experience: Int = 0,
    var spellPower: Int = 0,     // Bazowa moc magiczna dla Maga
    var strength: Int = 0,       // Bazowa siła dla Warriora
    var agility: Int = 0,        // Bazowa zręczność dla Archera
    var stamina: Int = 0,        // Bazowa stamina
    var mana: Int = 0,           // Bazowa mana

    // === NOWE POLA DLA ITEMÓW ===
    var equippedHelmet: ClientItem? = null,    // Założony hełm
    var equippedArmor: ClientItem? = null,     // Założona zbroja
    var equippedPants: ClientItem? = null,     // Założone spodnie
    var equippedBoots: ClientItem? = null,     // Założone buty
    var equippedWeapon: ClientItem? = null     // Założona broń

) {
    // Zmienne do obsługi ruchu - uproszczone
    var movingState: Boolean = false
    private var moveSpeed: Float = 120f

    // Zmienne do interpolacji (dla płynnego ruchu gracza)
    private var targetX: Float = x
    private var targetY: Float = y
    private var previousX: Float = x
    private var previousY: Float = y
    private var interpolationProgress: Float = 0f

    fun isDead(): Boolean = currentHealth <= 0

    fun getClassColor(): Color {
        return when (characterClass) {
            0 -> Color(0.2f, 0.8f, 0.2f, 1f) // Zielony dla łucznika
            1 -> Color(0.2f, 0.2f, 0.9f, 1f) // Niebieski dla maga
            else -> Color(0.9f, 0.2f, 0.2f, 1f) // Czerwony dla wojownika
        }
    }

    // === NOWE METODY - KALKULACJA STATYSTYK Z ITEMAMI ===

    // Zwraca łączną siłę (bazowa + bonusy z itemów)
    fun getTotalStrength(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.strengthBonus }
        return strength + itemBonus
    }

    // Zwraca łączną zręczność (bazowa + bonusy z itemów)
    fun getTotalAgility(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.agilityBonus }
        return agility + itemBonus
    }

    // Zwraca łączną moc magiczną (bazowa + bonusy z itemów)
    fun getTotalSpellPower(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.spellPowerBonus }
        return spellPower + itemBonus
    }

    // Zwraca łączną wytrzymałość (bazowa + bonusy z itemów)
    fun getTotalStamina(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.staminaBonus }
        return stamina + itemBonus
    }

    // Zwraca łączną wytrzymałość (bazowa + bonusy z itemów)
    fun getTotalMana(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.manaBonus }
        return mana + itemBonus
    }

    // Funkcja do pobierania nazwy głównego statu
    fun getPrimaryStatName(): String {
        return when (characterClass) {
            0 -> "Agility"
            1 -> "Spell Power"
            2 -> "Strength"
            else -> "Unknown"
        }
    }

    // Zwraca główną statystykę klasy uwzględniając bonusy z itemów
    fun getPrimaryStat(): Int {
        return when (characterClass) {
            0 -> getTotalAgility()      // Łucznik używa zręczności
            1 -> getTotalSpellPower()   // Mag używa mocy magicznej
            2 -> getTotalStrength()     // Wojownik używa siły
            else -> 0
        }
    }

    // === METODY ZARZĄDZANIA ITEMAMI ===

    // Ustawia item w odpowiednim slocie
    fun equipItem(item: ClientItem): ClientItem? {
        val previousItem = when (item.type) {
            "HELMET" -> {
                val old = equippedHelmet
                equippedHelmet = item
                old
            }
            "ARMOR" -> {
                val old = equippedArmor
                equippedArmor = item
                old
            }
            "PANTS" -> {
                val old = equippedPants
                equippedPants = item
                old
            }
            "BOOTS" -> {
                val old = equippedBoots
                equippedBoots = item
                old
            }
            "WEAPON" -> {
                val old = equippedWeapon
                equippedWeapon = item
                old
            }
            else -> null
        }
        return previousItem
    }

    // Zdejmuje item z odpowiedniego slotu
    fun unequipItem(itemType: String): ClientItem? {
        return when (itemType) {
            "HELMET" -> {
                val old = equippedHelmet
                equippedHelmet = null
                old
            }
            "ARMOR" -> {
                val old = equippedArmor
                equippedArmor = null
                old
            }
            "PANTS" -> {
                val old = equippedPants
                equippedPants = null
                old
            }
            "BOOTS" -> {
                val old = equippedBoots
                equippedBoots = null
                old
            }
            "WEAPON" -> {
                val old = equippedWeapon
                equippedWeapon = null
                old
            }
            else -> null
        }
    }

    // Pobiera item z danego slotu
    fun getEquippedItem(itemType: String): ClientItem? {
        return when (itemType) {
            "HELMET" -> equippedHelmet
            "ARMOR" -> equippedArmor
            "PANTS" -> equippedPants
            "BOOTS" -> equippedBoots
            "WEAPON" -> equippedWeapon
            else -> null
        }
    }

    // Zwraca listę wszystkich założonych itemów
    fun getAllEquippedItems(): List<ClientItem> {
        return listOfNotNull(equippedHelmet, equippedArmor, equippedPants, equippedBoots, equippedWeapon)
    }

    // Sprawdza czy gracz ma założony jakikolwiek item
    fun hasAnyEquippedItems(): Boolean {
        return equippedHelmet != null || equippedArmor != null ||
                equippedPants != null || equippedBoots != null || equippedWeapon != null
    }

    // Oblicza łączne bonusy ze wszystkich itemów
    fun getTotalItemBonuses(): Array<Int> {
        var totalStr = 0
        var totalAgi = 0
        var totalSP = 0
        var totalSta = 0

        getAllEquippedItems().forEach { item ->
            totalStr += item.strengthBonus
            totalAgi += item.agilityBonus
            totalSP += item.spellPowerBonus
            totalSta += item.staminaBonus
        }

        return arrayOf(totalStr, totalAgi, totalSP, totalSta)
    }

    // Czyści cały ekwipunek gracza
    fun clearAllEquipment() {
        equippedHelmet = null
        equippedArmor = null
        equippedPants = null
        equippedBoots = null
        equippedWeapon = null
    }

    // === METODY RUCHU (BEZ ZMIAN) ===

    // Uproszczona metoda do ustawiania docelowej pozycji ruchu
    fun setMoveTarget(newTargetX: Float, newTargetY: Float) {
        // Zapisz aktualną pozycję jako poprzednią
        previousX = x
        previousY = y

        // Ustaw nowy cel
        targetX = newTargetX
        targetY = newTargetY

        // Rozpocznij ruch
        movingState = true
        interpolationProgress = 0f
    }

    // Metoda do aktualizacji pozycji gracza (interpolacja bez predykcji)
    fun updatePosition(delta: Float) {
        if (!movingState) return

        // Zwiększ postęp interpolacji
        interpolationProgress += delta * 10f // Mnożnik wpływa na płynność
        interpolationProgress = interpolationProgress.coerceIn(0f, 1f)

        // Interpoluj między poprzednią a docelową pozycją
        x = lerp(previousX, targetX, interpolationProgress)
        y = lerp(previousY, targetY, interpolationProgress)

        // Jeśli osiągnęliśmy cel, zatrzymaj interpolację
        if (interpolationProgress >= 1f) {
            movingState = false
        }
    }

    // Metoda do aktualizacji pozycji lokalnego gracza (predykcja po stronie klienta)
    fun updateLocalPosition(delta: Float) {
        if (!movingState) return

        // Oblicz odległość do celu
        val distX = targetX - x
        val distY = targetY - y

        // Używanie square magnitude zamiast pełnego obliczania dystansu
        val distSquared = distX * distX + distY * distY

        // Używanie kwadratu odległości zamiast samej odległości (unikamy kosztownego sqrt)
        if (distSquared < 25f) { // 5^2 = 25
            movingState = false
            return
        }

        // Obliczamy faktyczny dystans tylko jeśli potrzebujemy
        val distance = sqrt(distSquared.toDouble()).toFloat()

        // Oblicz znormalizowany wektor kierunku
        val dirX = distX / distance
        val dirY = distY / distance

        // Oblicz odległość ruchu
        val moveDistance = moveSpeed * delta

        // Zabezpieczenie przed "przeskoczeniem" celu
        val actualMoveDistance = minOf(moveDistance, distance)

        // Aktualizuj pozycję
        x += dirX * actualMoveDistance
        y += dirY * actualMoveDistance
    }

    // Metoda do bezpośredniego ustawienia pozycji otrzymanej z serwera
    fun setServerPosition(serverX: Float, serverY: Float) {
        // Jeśli nie jesteśmy w ruchu, od razu ustaw pozycję
        if (!movingState) {
            x = serverX
            y = serverY
            previousX = serverX
            previousY = serverY
        } else {
            // Jeśli jesteśmy w ruchu, ustaw nowy cel
            previousX = x
            previousY = y
            targetX = serverX
            targetY = serverY
            interpolationProgress = 0f
        }
    }

    // Metoda pomocnicza do interpolacji liniowej
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + t * (end - start)
    }
}