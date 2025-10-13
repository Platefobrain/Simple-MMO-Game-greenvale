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

import com.badlogic.gdx.math.Vector2
import pl.decodesoft.items.character.ClientItem
import pl.decodesoft.player.skin.Direction

data class ItemBonusData(
    val strength: Int,
    val agility: Int,
    val spellPower: Int,
    val stamina: Int,
    val armor: Int,
    val attackSpeed: Int,
    val critRating: Int
)

data class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    val id: String,
    val username: String,
    var characterClass: Int = 0, // 0-łucznik, 1-mag, 2-wojownik
    var faction: Faction = Faction.NONE,
    var race: Race = Race.HUMAN,
    var isSelected: Boolean = false,
    val velocity: Vector2 = Vector2(),
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
    var armor: Int = 0,          // bazowy armor
    var attackSpeed: Int = 0,    // bazowy attack speed
    var critChance: Double = 0.0,     // Bazowy crit
    var damage: Int = 0,         // Damage

    // === NOWE POLA DLA ITEMÓW ===
    var equippedHelmet: ClientItem? = null,    // Założony hełm
    var equippedArmor: ClientItem? = null,     // Założona zbroja
    var equippedPants: ClientItem? = null,     // Założone spodnie
    var equippedBoots: ClientItem? = null,     // Założone buty
    var equippedWeapon: ClientItem? = null,     // Założona broń

    var lastDirection: Direction = Direction.DOWN
) {

    companion object {
        const val CRIT_RATING_PER_PERCENT = 22.0  // 22 rating = 1% crita
        const val ATTACK_SPEED_RATING_PER_SECOND = 200.0  // 200 rating = 1s redukcji
    }

    // Zmienne do obsługi ruchu
    var movingState: Boolean = false

    // Zmienne do interpolacji
    private var targetX: Float = x
    private var targetY: Float = y
    private var previousX: Float = x
    private var previousY: Float = y
    private var interpolationProgress: Float = 0f

    fun isDead(): Boolean = currentHealth <= 0

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

    // Zwraca łączną mane (bazowa + bonusy z itemów)
    fun getTotalMana(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.manaBonus }
        return mana + itemBonus
    }

    // NOWE METODY - ARMOR I ATTACK SPEED I DAMAGE

    fun getDamageRange(): String {
        // Główna statystyka postaci
        val primaryStat = getPrimaryStat()

        // Base damage klasy postaci
        val baseDamage = when (characterClass) {
            0 -> 11  // Archer
            1 -> 13  // Mag
            2 -> 9  // Wojownik
            else -> 9
        }

        // Damage modifier w zależności od klasy
        val damageModifier = when (characterClass) {
            0 -> 1.72f
            1 -> 2.22f
            2 -> 1.2f
            else -> 1.0f
        }

        // obliczanie
        val minDamage = baseDamage + primaryStat * damageModifier
        val maxDamage = (baseDamage + 3) + primaryStat * damageModifier

        return "${minDamage.toInt()}-${maxDamage.toInt()}"
    }

    // Zwraca łączny armor (bazowy + bonusy z itemów)
    fun getTotalArmor(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.armorBonus }
        return armor + itemBonus
    }

    // Zwraca łączny attack speed (bazowy + bonusy z itemów)
    private fun getTotalAttackSpeed(): Int {
        val itemBonus = getAllEquippedItems().sumOf { it.attackSpeedBonus }
        return attackSpeed + itemBonus
    }

    fun getTotalCritChance(): Double {
        val itemRating = getAllEquippedItems().sumOf { it.critRatingBonus }
        val critFromRating = itemRating / CRIT_RATING_PER_PERCENT
        return critChance + critFromRating
    }

    // Aktualizuj attackSpeedBonus i przekaż do klasy postaci
    fun updateAttackCooldown(): Float {
        val baseCooldownMs = 2000L
        val totalAttackSpeedRating = getTotalAttackSpeed()
        val cooldownReduction = totalAttackSpeedRating / ATTACK_SPEED_RATING_PER_SECOND
        val adjustedCooldownMs = (baseCooldownMs - (cooldownReduction * 1000)).toLong().coerceAtLeast(100F.toLong())
        return adjustedCooldownMs / 1000f
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
    fun getTotalItemBonuses(): ItemBonusData {
        var totalStr = 0
        var totalAgi = 0
        var totalSP = 0
        var totalSta = 0
        var totalArmor = 0
        var totalAttackSpeed = 0
        var totalCritRating = 0

        getAllEquippedItems().forEach { item ->
            totalStr += item.strengthBonus
            totalAgi += item.agilityBonus
            totalSP += item.spellPowerBonus
            totalSta += item.staminaBonus
            totalArmor += item.armorBonus
            totalAttackSpeed += item.attackSpeedBonus
            totalCritRating += item.critRatingBonus
        }

        return ItemBonusData(totalStr, totalAgi, totalSP, totalSta, totalArmor, totalAttackSpeed, totalCritRating)
    }

    // Czyści cały ekwipunek gracza
    fun clearAllEquipment() {
        equippedHelmet = null
        equippedArmor = null
        equippedPants = null
        equippedBoots = null
        equippedWeapon = null
    }

    // === METODY RUCHU ===

    // Uproszczona metoda do ustawiania docelowej pozycji ruchu
    fun setMoveTarget(newTargetX: Float, newTargetY: Float) {
        previousX = x
        previousY = y

        targetX = newTargetX
        targetY = newTargetY

        movingState = true
        interpolationProgress = 0f
    }

    // Metoda do aktualizacji pozycji gracza (interpolacja bez predykcji)
    fun updatePosition(delta: Float) {
        if (!movingState) {
            velocity.set(0f, 0f)
            return
        }

        interpolationProgress = (interpolationProgress + delta * 20f).coerceAtMost(1f)

        x = previousX + interpolationProgress * (targetX - previousX)
        y = previousY + interpolationProgress * (targetY - previousY)

        velocity.set(targetX - x, targetY - y)

        if (interpolationProgress >= 1f) {
            movingState = false
            velocity.set(0f, 0f)
        }
    }
}