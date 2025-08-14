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

package pl.decodesoft.items.model

import kotlinx.serialization.Serializable

// Klasa reprezentująca założone itemy postaci
@Serializable
data class EquippedItems(
    var helmet: Item? = null,
    var armor: Item? = null,
    var pants: Item? = null,
    var boots: Item? = null,
    var weapon: Item? = null
) {
    // Zakłada item w odpowiedni slot
    fun equipItem(item: Item): Item? {
        val previousItem = when (item.type) {
            ItemType.HELMET -> helmet.also { helmet = item }
            ItemType.ARMOR -> armor.also { armor = item }
            ItemType.PANTS -> pants.also { pants = item }
            ItemType.BOOTS -> boots.also { boots = item }
            ItemType.WEAPON -> weapon.also { weapon = item }
        }
        return previousItem
    }

    // Zdejmuje item z odpowiedniego slotu
    fun unequipItem(itemType: ItemType): Item? {
        return when (itemType) {
            ItemType.HELMET -> helmet.also { helmet = null }
            ItemType.ARMOR -> armor.also { armor = null }
            ItemType.PANTS -> pants.also { pants = null }
            ItemType.BOOTS -> boots.also { boots = null }
            ItemType.WEAPON -> weapon.also { weapon = null }
        }
    }

    // Zwraca łączne bonusy ze wszystkich założonych itemów
    fun getTotalBonuses(): ItemBonuses {
        var totalStrength = 0
        var totalAgility = 0
        var totalSpellPower = 0
        var totalStamina = 0
        var totalMana = 0

        listOf(helmet, armor, pants, boots, weapon).forEach { item ->
            item?.let {
                totalStrength += it.strengthBonus
                totalAgility += it.agilityBonus
                totalSpellPower += it.spellPowerBonus
                totalStamina += it.staminaBonus
                totalMana += it.manaBonus
            }
        }

        return ItemBonuses(
            strength = totalStrength,
            agility = totalAgility,
            spellPower = totalSpellPower,
            stamina = totalStamina,
            mana = totalMana
        )
    }

    // Zwraca listę wszystkich założonych itemów
    fun getAllEquippedItems(): List<Item> {
        return listOfNotNull(helmet, armor, pants, boots, weapon)
    }
}

// Klasa pomocnicza do przechowywania łącznych bonusów z itemów
@Serializable
data class ItemBonuses(
    val strength: Int = 0,
    val agility: Int = 0,
    val spellPower: Int = 0,
    val stamina: Int = 0,
    val mana: Int = 0
)