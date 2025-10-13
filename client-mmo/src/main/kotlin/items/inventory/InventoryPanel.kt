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

package pl.decodesoft.items.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.items.ItemManager
import pl.decodesoft.items.ItemTransferSystem
import pl.decodesoft.items.ItemTooltip

data class InventoryItem(
    val id: String,
    val name: String,
    val stackable: Boolean = false,
    val quantity: Int = 1,
    val icon: String? = null
)

class InventoryPanel(private val game: MMOGame) {

    private val slotsPerRow = 6
    private val slotsPerColumn = 5

    private val slotSize = 50f
    private val slotSpacing = 5f

    private var itemManager: ItemManager? = null
    private var transferSystem: ItemTransferSystem? = null
    private var itemTooltip: ItemTooltip? = null

    fun setItemManager(itemManager: ItemManager) {
        this.itemManager = itemManager
    }

    fun setTransferSystem(transferSystem: ItemTransferSystem) {
        this.transferSystem = transferSystem
    }

    fun setItemTooltip(tooltip: ItemTooltip) {
        this.itemTooltip = tooltip
    }

    fun render(x: Float, y: Float, width: Float, height: Float): Boolean {
        renderBackground(x, y, width, height)

        // Najpierw sprawdź hovery
        val anyItemHovered = renderInventoryGrid(x, y, width, height)

        renderBottomInfo(x, y, width)

        // ZWRÓĆ wynik
        return anyItemHovered
    }

    private fun renderBackground(x: Float, y: Float, width: Float, height: Float) {
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.9f)
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f)
        game.shapeRenderer.rect(x, y, width, height)
        game.shapeRenderer.end()
    }

    private fun renderInventoryGrid(x: Float, y: Float, width: Float, height: Float): Boolean {
        val gridStartX = x + 14f
        val gridStartY = y + height - 80f
        var anyItemHovered = false

        // Render slots
        for (row in 0 until slotsPerColumn) {
            for (col in 0 until slotsPerRow) {
                val slotIndex = row * slotsPerRow + col
                val slotX = gridStartX + col * (slotSize + slotSpacing)
                val slotY = gridStartY - row * (slotSize + slotSpacing)

                val wasHovered = renderSlot(slotX, slotY, slotIndex)
                if (wasHovered) anyItemHovered = true
            }
        }

        // Render title
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        game.font.color = Color.YELLOW
        val title = "Inventory"
        game.layout.setText(game.font, title)
        val titleX = x + (width - game.layout.width) / 2
        val titleY = y + height - 10f
        game.font.draw(game.batch, title, titleX, titleY)

        game.batch.end()
        resetFontColor()

        return anyItemHovered
    }

    private fun renderSlot(x: Float, y: Float, slotIndex: Int): Boolean {
        val item = itemManager?.getInventoryItem(slotIndex)
        var isHovered = false

        // Sprawdź czy to selected item
        val isSelected = transferSystem?.getSelectedItem()?.let { selected ->
            selected.sourceType == "INVENTORY" && selected.sourceSlot == slotIndex
        } ?: false

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Slot background - podświetl jeśli selected
        when {
            isSelected -> {
                game.shapeRenderer.setColor(0.5f, 0.5f, 0.2f, 0.8f) // Żółtawy dla selected
            }
            item != null -> {
                game.shapeRenderer.setColor(0.2f, 0.3f, 0.2f, 0.8f) // Zielonkawy jeśli ma item
            }
            transferSystem?.hasSelectedItem() == true -> {
                game.shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 0.7f) // Jaśniejszy dla możliwego celu
            }
            else -> {
                game.shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 0.7f) // Ciemnoszary jeśli pusty
            }
        }
        game.shapeRenderer.rect(x, y, slotSize, slotSize)
        game.shapeRenderer.end()

        // Slot border
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        if (isSelected) {
            game.shapeRenderer.setColor(1f, 1f, 0f, 1f) // Żółta ramka dla selected
        } else {
            game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f)
        }
        game.shapeRenderer.rect(x, y, slotSize, slotSize)
        game.shapeRenderer.end()

        // Sprawdź hover dla tooltipa
        if (item != null) {
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat() // odwróć Y

            if (mouseX >= x && mouseX <= x + slotSize &&
                mouseY >= y && mouseY <= y + slotSize) {

                val itemDef = itemManager?.getItemDefinition(item.id)
                if (itemDef != null) {
                    itemTooltip?.showTooltip(itemDef, mouseX, mouseY)
                    isHovered = true
                }
            }
        }

        // Render item if exists
        if (item != null) {
            // Pobierz teksturę jeśli istnieje
            val texture = itemManager?.getItemTexture(item.id)

            game.batch.projectionMatrix = game.uiBatch.projectionMatrix
            game.batch.begin()

            if (texture != null) {
                // Renderuj grafikę itemu
                val iconSize = slotSize * 0.8f // 80% rozmiaru slotu
                val iconX = x + (slotSize - iconSize) / 2
                val iconY = y + (slotSize - iconSize) / 2

                game.batch.draw(texture, iconX, iconY, iconSize, iconSize)
            } else {
                // Fallback - renderuj literę jak wcześniej
                game.font.color = if (isSelected) Color.YELLOW else Color.WHITE
                val itemInitial = item.name.first().toString().uppercase()
                game.layout.setText(game.font, itemInitial)
                val textX = x + (slotSize - game.layout.width) / 2
                val textY = y + (slotSize + game.layout.height) / 2
                game.font.draw(game.batch, itemInitial, textX, textY)
            }

            // Stack count
            if (item.stackable && item.quantity > 1) {
                game.font.color = Color.YELLOW
                val originalScale = game.font.data.scaleX
                game.font.data.setScale(0.7f)

                val quantityText = item.quantity.toString()
                game.layout.setText(game.font, quantityText)
                val qX = x + slotSize - game.layout.width - 3f
                val qY = y + game.layout.height + 3f
                game.font.draw(game.batch, quantityText, qX, qY)

                game.font.data.setScale(originalScale)
            }

            game.batch.end()
        }

        return isHovered
    }

    private fun renderBottomInfo(x: Float, y: Float, width: Float) {
        val infoY = y + 20f // Zmień tę wartość aby przesunąć tekst w górę/dół (większa = wyżej)
        val iconOffsetY = 5f // Zmień aby przesunąć TYLKO obrazki w górę/dół (większa = wyżej)
        val iconSize = 20f // Rozmiar ikon monet
        val spaceBetweenCoins = 15f // Odstęp między różnymi walutami
        val iconPadding = 0f // Odstęp między liczbą a ikoną (zmniejsz dla mniejszego odstępu)
        var currentX = x + 30f // Zmień aby przesunąć w lewo/prawo (większa = bardziej w prawo)

        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        // Gold
        game.font.color = Color.GOLD
        val goldText = game.playerGold.toString()
        game.layout.setText(game.font, goldText)
        game.font.draw(game.batch, goldText, currentX, infoY)
        currentX += game.layout.width + iconPadding

        val goldTexture = itemManager?.getCoinTexture("gold")
        if (goldTexture != null) {
            val iconY = infoY - iconSize + iconOffsetY
            game.batch.draw(goldTexture, currentX, iconY, iconSize, iconSize)
            currentX += iconSize + spaceBetweenCoins
        } else {
            currentX += spaceBetweenCoins
        }

        // Silver
        game.font.color = Color.LIGHT_GRAY
        val silverText = game.playerSilver.toString()
        game.layout.setText(game.font, silverText)
        game.font.draw(game.batch, silverText, currentX, infoY)
        currentX += game.layout.width + iconPadding

        val silverTexture = itemManager?.getCoinTexture("silver")
        if (silverTexture != null) {
            val iconY = infoY - iconSize + iconOffsetY
            game.batch.draw(silverTexture, currentX, iconY, iconSize, iconSize)
            currentX += iconSize + spaceBetweenCoins
        } else {
            currentX += spaceBetweenCoins
        }

        // Copper
        game.font.color = Color(0.8f, 0.5f, 0.2f, 1f) // Copper color
        val copperText = game.playerCopper.toString()
        game.layout.setText(game.font, copperText)
        game.font.draw(game.batch, copperText, currentX, infoY)
        currentX += game.layout.width + iconPadding

        val copperTexture = itemManager?.getCoinTexture("copper")
        if (copperTexture != null) {
            val iconY = infoY - iconSize + iconOffsetY
            game.batch.draw(copperTexture, currentX, iconY, iconSize, iconSize)
        }

        // Capacity (pozostaje bez zmian)
        game.font.color = Color.LIGHT_GRAY
        val capacityText = itemManager?.let { manager ->
            "${manager.getInventoryItemCount()}/${ItemManager.INVENTORY_SIZE}"
        } ?: "0/30"
        game.font.draw(game.batch, capacityText, x + width - 85f, infoY)

        game.batch.end()
        resetFontColor()
    }

    fun handleClick(touchX: Float, touchY: Float, panelX: Float, panelY: Float, isRightClick: Boolean = false): Boolean {
        val gridStartX = panelX + 14f
        val gridStartY = panelY + 320f - 80f

        // Check which slot was clicked
        for (row in 0 until slotsPerColumn) {
            for (col in 0 until slotsPerRow) {
                val slotIndex = row * slotsPerRow + col
                val slotX = gridStartX + col * (slotSize + slotSpacing)
                val slotY = gridStartY - row * (slotSize + slotSpacing)

                if (touchX >= slotX && touchX <= slotX + slotSize &&
                    touchY >= slotY && touchY <= slotY + slotSize) {

                    val item = itemManager?.getInventoryItem(slotIndex)
                    val transfer = transferSystem ?: return false

                    if (isRightClick && item != null) {
                        itemManager?.rightClickEquipFromInventory(item.id)
                        return true
                    }

                    if (item != null) {
                        // Lewy klik na item
                        if (transfer.hasSelectedItem()) {
                            // Już coś trzymamy - próbuj przenieść tutaj
                            val selectedItem = transfer.getSelectedItem()
                            if (selectedItem?.sourceType == "INVENTORY" && selectedItem.sourceSlot == slotIndex) {
                                // Klik na ten sam item - anuluj
                                transfer.clearSelection()
                            } else {
                                // Przenieś na ten slot
                                transfer.moveItem("INVENTORY", slotIndex)
                            }
                        } else {
                            // Wybierz ten item
                            transfer.selectItem(item.id, item.name, "INVENTORY", slotIndex)
                        }
                    } else {
                        // Lewy klik na pusty slot
                        if (transfer.hasSelectedItem()) {
                            transfer.moveItem("INVENTORY", slotIndex)
                        }
                    }
                    return true
                }
            }
        }

        return false
    }

    private fun resetFontColor() {
        game.font.color = Color.WHITE
    }
}