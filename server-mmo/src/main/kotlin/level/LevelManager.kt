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

package pl.decodesoft.level

import pl.decodesoft.player.model.PlayerData

object LevelManager {
    private fun getXPForNextLevel(level: Int): Int {
        return 100 * level
    }

    data class LevelUpResult(
        val leveledUp: Boolean,
        val newLevel: Int,
        val newMaxHealth: Int,
        val newCurrentHealth: Int,
        val remainingXP: Int,
        val newPrimaryStat: Int = 0,
        val newStamina: Int = 0
    )

    fun addExperience(player: PlayerData, amount: Int): LevelUpResult {
        player.experience += amount
        var leveledUp = false

        while (player.experience >= getXPForNextLevel(player.level)) {
            player.experience -= getXPForNextLevel(player.level)
            player.level++
            leveledUp = true

            // Zwiększ staty
            player.increasePrimaryStat(2) // tu jest ile dmg dodaje na ilosc punktow

            // Zdrowie zależne od staminy
            player.maxHealth = player.calculateMaxHealth()
            player.currentHealth = player.maxHealth
        }

        return LevelUpResult(
            leveledUp = leveledUp,
            newLevel = player.level,
            newMaxHealth = player.maxHealth,
            newCurrentHealth = player.currentHealth,
            remainingXP = player.experience,
            newPrimaryStat = player.getPrimaryStat(),
            newStamina = player.stamina
        )
    }
}