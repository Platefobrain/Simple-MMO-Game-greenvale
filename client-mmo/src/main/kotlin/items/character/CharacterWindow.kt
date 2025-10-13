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

package pl.decodesoft.items.character

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.items.ItemTooltip
import pl.decodesoft.items.ItemTransferSystem

class CharacterWindow(private val game: MMOGame) {
    private var selectedTab = 0 // 0 = Equipment, 1 = Stats
    private val characterEquipment = CharacterEquipment(game)
    private val characterStats = CharacterStats(game)
    private val tabs = arrayOf("Equipment", "Stats")

    // setTransferSystem
    fun setTransferSystem(transferSystem: ItemTransferSystem) {
        characterEquipment.setTransferSystem(transferSystem)
    }

    // tooltip
    fun setItemTooltip(tooltip: ItemTooltip) {
        characterEquipment.setItemTooltip(tooltip)
    }

    fun render(x: Float, y: Float, width: Float, height: Float): Boolean {
        val tabHeight = 30f

        // Render window background
        renderWindowBackground(x, y, width, height)

        // Render tabs
        renderTabs(x, y + height - tabHeight, width)

        // Render active tab content i zwróć czy coś było hoverowane
        return renderActiveTabContent(x, y, width, height - tabHeight)
    }

    private fun renderWindowBackground(x: Float, y: Float, width: Float, height: Float) {
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Main background - ciemnofioletowy jak na screenie
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.9f)
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()

        // Border - złota ramka
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f)
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()
    }

    private fun renderTabs(x: Float, y: Float, width: Float) {
        val tabHeight = 30.0f
        val tabWidth = width / tabs.size

        // Tab backgrounds
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (i in tabs.indices) {
            val tabX = x + i * tabWidth

            if (i == selectedTab) {
                game.shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.9f) // Active tab
            } else {
                game.shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.7f) // Inactive tab
            }

            game.shapeRenderer.rect(tabX, y, tabWidth, tabHeight)
        }
        game.shapeRenderer.end()

        // Tab borders
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        game.shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f)
        for (i in tabs.indices) {
            val tabX = x + i * tabWidth
            game.shapeRenderer.rect(tabX, y, tabWidth, tabHeight)
        }
        game.shapeRenderer.end()

        // Tab text
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        for (i in tabs.indices) {
            val tabX = x + i * tabWidth

            if (i == selectedTab) {
                game.font.color = Color.WHITE
            } else {
                game.font.color = Color.LIGHT_GRAY
            }

            game.layout.setText(game.font, tabs[i])
            val textWidth = game.layout.width
            val textHeight = game.layout.height

            val textX = tabX + (tabWidth - textWidth) / 2
            val textY = y + (tabHeight + textHeight) / 2

            game.font.draw(game.batch, tabs[i], textX, textY)
        }

        game.batch.end()
    }

    private fun renderActiveTabContent(x: Float, y: Float, width: Float, height: Float): Boolean {
        return when (selectedTab) {
            0 -> characterEquipment.render(x, y, width, height) // Equipment tab może mieć hovery
            1 -> {
                characterStats.render(x, y, height)
                false // Stats tab nie ma tooltipów
            }
            else -> false
        }
    }

    // Zaktualizuj handleClick żeby przekazywał isRightClick
    fun handleClick(touchX: Float, touchY: Float, panelX: Float, panelY: Float, panelWidth: Float, panelHeight: Float, isRightClick: Boolean = false): Boolean {
        val tabHeight = 30f
        val tabWidth = panelWidth / tabs.size
        val tabY = panelY + panelHeight - tabHeight

        // Check if click was on tabs
        if (touchY >= tabY && touchY <= tabY + tabHeight &&
            touchX >= panelX && touchX <= panelX + panelWidth) {

            val clickedTab = ((touchX - panelX) / tabWidth).toInt()
            if (clickedTab >= 0 && clickedTab < tabs.size) {
                selectedTab = clickedTab
                return true
            }
        }

        // If Equipment tab is active, check for slot clicks
        if (selectedTab == 0) {
            return characterEquipment.handleClick(touchX, touchY, panelX, panelY, panelWidth, panelHeight - tabHeight, isRightClick)
        }

        return false
    }
}