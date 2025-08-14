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

package pl.decodesoft.player.model

import kotlinx.serialization.Serializable
import pl.decodesoft.items.model.EquippedItems

// Tymczasowy stan gracza podczas gry
@Serializable
data class PlayerData(
    var x: Float,
    var y: Float,
    val id: String,
    val username: String = "",
    val characterClass: CharacterClass = CharacterClass.WARRIOR, // Domyślnie wojownik

    // HEALTH I MANA RAZEM:
    var maxHealth: Int = 100, // Maksymalne zdrowie
    var currentHealth: Int = 100, // Aktualne zdrowie
    var maxMana: Int = 100, // Maksymalna mana
    var currentMana: Int = 100, // Aktualna mana

    var level: Int = 1,
    var experience: Int = 0,
    var isDead: Boolean = false,

    // TIMERY REGENERACJI RAZEM:
    var timeSinceLastDamage: Float = 0f, // Regeneracja dopiero po combat
    var timeSinceLastManaUse: Float = 0f, // Regeneracja many po użyciu

    var spellPower: Int = 0,     // Bazowa moc magiczna dla Maga
    var strength: Int = 0,       // Bazowa siła dla Warriora
    var agility: Int = 0,        // Bazowa zręczność dla Huntera/Archera
    var stamina: Int = 0,        // Bazowa wytrzymałość
    var mana: Int = 0,
    var equippedItems: EquippedItems = EquippedItems() // Założone itemy gracza
) {

    // === SYSTEM ZDROWIA ===

    // Ustawia zdrowie gracza w bezpiecznych granicach
    fun setHealthBase(health: Int) {
        currentHealth = health.coerceIn(0, maxHealth)
        isDead = currentHealth <= 0
    }

    // Zadaje obrażenia graczowi i resetuje timer regeneracji
    fun takeDamage(damage: Int) {
        currentHealth -= damage
        if (currentHealth <= 0) {
            currentHealth = 0
            isDead = true
        }
        timeSinceLastDamage = 0f // Reset timera regeneracji
    }

    // Leczy gracza nie przekraczając maksymalnego zdrowia
    fun heal(amount: Int) {
        currentHealth = (currentHealth + amount).coerceAtMost(maxHealth)
        if (currentHealth > 0) {
            isDead = false
        }
    }

    // Regeneruje zdrowie jeśli gracz nie jest martwy
    fun regenerateHealth(amount: Int) {
        if (!isDead && currentHealth < maxHealth) {
            currentHealth = (currentHealth + amount).coerceAtMost(maxHealth)
        }
    }

    // === SYSTEM MANY ===

    // Ustawia mana gracza w bezpiecznych granicach
    fun setManaBase(mana: Int) {
        currentMana = mana.coerceIn(0, maxMana)
    }

    // Zużywa mana graczowi i resetuje timer regeneracji
    private fun takeMana(amount: Int) {
        currentMana -= amount
        if (currentMana < 0) {
            currentMana = 0
        }
        timeSinceLastManaUse = 0f // Reset timera regeneracji
    }

    // Regeneruje mana jeśli gracz ma mniej niż maksimum
    fun regenerateMana(amount: Int) {
        if (currentMana < maxMana) {
            currentMana = (currentMana + amount).coerceAtMost(maxMana)
        }
    }

    // === FUNKCJE POMOCNICZE MANY (jak useMana z poprzedniej wersji) ===

    // Używa many (zwraca true jeśli udało się użyć)
    fun useMana(amount: Int): Boolean {
        return if (currentMana >= amount) {
            takeMana(amount)
            true
        } else {
            false // Nie ma wystarczająco many
        }
    }

    // === RESPAWN (zaktualizowany o mana) ===

    // Przywraca gracza do życia z pełnym zdrowiem i maną
    fun respawn() {
        currentHealth = maxHealth
        currentMana = maxMana
        isDead = false
        timeSinceLastDamage = 0f
        timeSinceLastManaUse = 0f
    }

    // === KALKULACJA STATYSTYK Z ITEMAMI ===

    // Zwraca łączną siłę (bazowa + bonusy z itemów)
    fun getTotalStrength(): Int {
        return strength + equippedItems.getTotalBonuses().strength
    }

    // Zwraca łączną zręczność (bazowa + bonusy z itemów)
    fun getTotalAgility(): Int {
        return agility + equippedItems.getTotalBonuses().agility
    }

    // Zwraca łączną moc magiczną (bazowa + bonusy z itemów)
    fun getTotalSpellPower(): Int {
        return spellPower + equippedItems.getTotalBonuses().spellPower
    }

    // Zwraca łączną stamine (bazowa + bonusy z itemów)
    fun getTotalStamina(): Int {
        return stamina + equippedItems.getTotalBonuses().stamina
    }

    // Zwraca łączną mane (bazowa + bonusy z itemów)
    private fun getTotalMana(): Int {
        return mana + equippedItems.getTotalBonuses().mana
    }

    // Zwraca główną statystykę klasy uwzględniając bonusy z itemów
    fun getPrimaryStat(): Int {
        return when (characterClass) {
            CharacterClass.ARCHER -> getTotalAgility()      // Archer/Hunter używa zręczności
            CharacterClass.MAGE -> getTotalSpellPower()     // Mage używa mocy magicznej
            CharacterClass.WARRIOR -> getTotalStrength()    // Warrior używa siły
        }
    }

    // Zwiększa bazowe statystyki przy levelup (bez wpływu na itemy)
    fun increasePrimaryStat(amount: Int = 1) {
        when (characterClass) {
            CharacterClass.ARCHER -> agility += amount      // Zwiększ bazową zręczność
            CharacterClass.MAGE -> spellPower += amount     // Zwiększ bazową moc magiczną
            CharacterClass.WARRIOR -> strength += amount    // Zwiększ bazową siłę
        }
        stamina += amount // Zwiększ bazową wytrzymałość
    }

    // === KALKULACJA MAKSYMALNYCH WARTOŚCI ===

    // Oblicza maksymalne zdrowie uwzględniając bonusy z itemów
    fun calculateMaxHealth(): Int {
        return (characterClass.baseHealth + getTotalStamina() * 10 * characterClass.healthModifier).toInt()
    }

    // Oblicza maksymalną mana uwzględniając bonusy z itemów
    private fun calculateMaxMana(): Int {
        return when (characterClass) {
            CharacterClass.MAGE -> (characterClass.baseMana + getTotalMana() * 10 * characterClass.manaModifier).toInt()
            CharacterClass.ARCHER -> (characterClass.baseMana + getTotalMana() * 5 * characterClass.manaModifier).toInt()
            CharacterClass.WARRIOR -> (characterClass.baseMana + getTotalMana() * 2 * characterClass.manaModifier).toInt()
        }
    }

    // === AKTUALIZACJA MAKSYMALNYCH WARTOŚCI ===

    // Aktualizuje maksymalne zdrowie na podstawie aktualnych statystyk i itemów
    private fun updateMaxHealthFromStats() {
        val newMaxHealth = calculateMaxHealth()

        // Zapisz stare wartości
        val oldMaxHealth = maxHealth
        val oldCurrentHealth = currentHealth

        // Ustaw nowe maksymalne HP
        maxHealth = newMaxHealth

        // Nie skaluj automatycznie currentHealth!
        // Pozostaw aktualne HP bez zmian, chyba że przekracza nowe maksimum
        if (!isDead) {
            currentHealth = currentHealth.coerceAtMost(maxHealth)
        }

        println("DEBUG: HP update - Old: $oldCurrentHealth/$oldMaxHealth, New: $currentHealth/$maxHealth")
    }

    // Aktualizuje maksymalną mana na podstawie aktualnych statystyk i itemów
    private fun updateMaxManaFromStats() {
        val newMaxMana = calculateMaxMana()

        // Zapisz stare wartości
        val oldMaxMana = maxMana
        val oldCurrentMana = currentMana

        // Ustaw nowe maksymalne MP
        maxMana = newMaxMana

        // Nie skaluj automatycznie currentMana!
        // Pozostaw aktualną mana bez zmian, chyba że przekracza nowe maksimum
        currentMana = currentMana.coerceAtMost(maxMana)

        println("DEBUG: MP update - Old: $oldCurrentMana/$oldMaxMana, New: $currentMana/$maxMana")
    }

    // Aktualizuje zarówno HP jak i MP
    fun updateMaxStatsFromItems() {
        updateMaxHealthFromStats()
        updateMaxManaFromStats()
    }

    // === INICJALIZACJA ===

    // Ładuje ekwipunek z danych postaci (przy logowaniu)
    fun loadEquipmentFromCharacter(characterInfo: CharacterInfo) {
        equippedItems = characterInfo.equippedItems
        updateMaxStatsFromItems() // Przelicz zarówno zdrowie jak i mana po załadowaniu itemów
    }
}