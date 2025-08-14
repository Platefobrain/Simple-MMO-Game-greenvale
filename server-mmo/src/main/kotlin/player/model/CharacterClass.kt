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

// Wyliczenie dostępnych klas postaci
enum class CharacterClass(
    val healthModifier: Double,
    val baseHealth: Int,
    val manaModifier: Double,
    val baseMana: Int,
    val damageModifier: Double,
    val baseDamage: Int
) {
    ARCHER(1.7, 60, 1.0, 55, 1.5, 7),
    MAGE(1.5, 52, 1.0, 75, 2.0, 5),
    WARRIOR(1.9, 66, 1.0, 20, 1.0, 9)
}