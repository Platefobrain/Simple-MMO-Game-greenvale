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

// Informacje o postaci zapisywane do bazy danych
@Serializable
data class CharacterInfo(
    val id: String,
    val nickname: String,
    val characterClass: Int,
    var maxHealth: Int = 100,       // stamina
    var currentHealth: Int = 100,   // stamina
    var maxMana: Int = 100,           // mana
    var currentMana: Int = 100,       // mana
    var level: Int = 1,
    var experience: Int = 0,
    var lastX: Float = 500f,        // Ostatnia pozycja X
    var lastY: Float = 600f,        // Ostatnia pozycja Y
    var spellPower: Int = 0,
    var strength: Int = 0,
    var agility: Int = 0,
    var stamina: Int = 0,
    var mana: Int = 0,
    var equippedItems: EquippedItems = EquippedItems(), // itemy
    var inventory: MutableMap<Int, String> = mutableMapOf(),
    var hasReceivedStarterItems: Boolean = false
)