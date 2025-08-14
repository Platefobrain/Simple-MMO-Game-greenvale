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

// Model pojedynczego itemu w grze
@Serializable
data class Item(
    val id: String,
    val name: String,
    val type: ItemType,
    val strengthBonus: Int = 0,
    val agilityBonus: Int = 0,
    val spellPowerBonus: Int = 0,
    val staminaBonus: Int = 0,
    val manaBonus: Int = 0
)