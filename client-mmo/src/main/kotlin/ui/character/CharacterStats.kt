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

package pl.decodesoft.ui.character

import com.badlogic.gdx.graphics.Color
import pl.decodesoft.MMOGame

class CharacterStats(private val game: MMOGame) {

    fun render(x: Float, y: Float, height: Float) {
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        val textX = x + 10f
        var currentY = y + height - 20f
        val lineHeight = 25f

        // Nagłówek
        game.font.color = Color.YELLOW
        game.font.draw(game.batch, "Statystyki", textX, currentY)
        currentY -= lineHeight * 1.5f

        // Pobierz bazowe statystyki gracza
        val player = game.localPlayer
        val baseStr = player.strength
        val baseAgi = player.agility
        val baseSP = player.spellPower
        val baseSta = player.stamina

        // Oblicz bonusy z itemów
        val itemBonuses = player.getTotalItemBonuses()
        val strBonus = itemBonuses[0]
        val agiBonus = itemBonuses[1]
        val spBonus = itemBonuses[2]
        val staBonus = itemBonuses[3]

        // Oblicz łączne statystyki
        val totalStr = player.getTotalStrength()
        val totalAgi = player.getTotalAgility()
        val totalSP = player.getTotalSpellPower()
        val totalSta = player.getTotalStamina()

        // === WYŚWIETL STATYSTYKI Z BONUSAMI ===
        game.font.color = Color.WHITE

        // Siła
        val strText = if (strBonus > 0) {
            "Siła: $baseStr (+$strBonus) = $totalStr"
        } else {
            "Siła: $baseStr"
        }
        game.font.draw(game.batch, strText, textX, currentY)
        currentY -= lineHeight

        // Zręczność
        val agiText = if (agiBonus > 0) {
            "Zręczność: $baseAgi (+$agiBonus) = $totalAgi"
        } else {
            "Zręczność: $baseAgi"
        }
        game.font.draw(game.batch, agiText, textX, currentY)
        currentY -= lineHeight

        // Moc Magiczna
        val spText = if (spBonus > 0) {
            "Moc Magiczna: $baseSP (+$spBonus) = $totalSP"
        } else {
            "Moc Magiczna: $baseSP"
        }
        game.font.draw(game.batch, spText, textX, currentY)
        currentY -= lineHeight

        // Wytrzymałość
        val staText = if (staBonus > 0) {
            "Wytrzymałość: $baseSta (+$staBonus) = $totalSta"
        } else {
            "Wytrzymałość: $baseSta"
        }
        game.font.draw(game.batch, staText, textX, currentY)
        currentY -= lineHeight * 1.5f

        // === DODATKOWE INFORMACJE ===

        // Główna statystyka klasy
        game.font.color = Color.CYAN
        val primaryStatName = player.getPrimaryStatName()
        val primaryStatValue = player.getPrimaryStat()
        game.font.draw(game.batch, "Główny stat ($primaryStatName): $primaryStatValue", textX, currentY)
        currentY -= lineHeight

        // Maksymalne zdrowie z bonusami
        game.font.color = Color.GREEN
        game.font.draw(game.batch, "Maksymalne Zdrowie: ${player.maxHealth}", textX, currentY)
        currentY -= lineHeight

        // Liczba założonych itemów
        val equippedCount = player.getAllEquippedItems().size
        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Założonych itemów: $equippedCount/5", textX, currentY)
        currentY -= lineHeight * 1.5f

        // === OPIS BONUSÓW Z ITEMÓW ===
        if (player.hasAnyEquippedItems()) {
            game.font.color = Color.YELLOW
            game.font.draw(game.batch, "Bonusy z ekwipunku:", textX, currentY)
            currentY -= lineHeight

            game.font.color = Color.WHITE
            player.getAllEquippedItems().forEach { item ->
                val itemBonus = item.getBonusDescription()
                if (itemBonus != "Brak bonusów") {
                    game.font.draw(game.batch, "• ${item.name}: $itemBonus", textX + 10f, currentY)
                    currentY -= lineHeight * 0.8f
                }
            }
        } else {
            game.font.color = Color.GRAY
            game.font.draw(game.batch, "Brak założonych itemów", textX, currentY)
            currentY -= lineHeight

            game.font.color = Color.LIGHT_GRAY
            game.font.draw(game.batch, "Użyj /give [item_id] aby otrzymać item", textX, currentY)
        }

        game.batch.end()
        resetFontColor()
    }

    private fun resetFontColor() {
        game.font.color = Color.WHITE
    }
}