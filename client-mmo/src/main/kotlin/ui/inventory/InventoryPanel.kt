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

package pl.decodesoft.ui.inventory

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.ui.ItemManager
import pl.decodesoft.ui.ItemTransferSystem

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
    private val totalSlots = slotsPerRow * slotsPerColumn

    private val slotSize = 50f
    private val slotSpacing = 5f

    private var itemManager: ItemManager? = null

    // Reference do transfer systemu
    private var transferSystem: ItemTransferSystem? = null

    fun setItemManager(itemManager: ItemManager) {
        this.itemManager = itemManager
        println("ItemManager podłączony do InventoryPanel")
    }

    fun setTransferSystem(transferSystem: ItemTransferSystem) {
        this.transferSystem = transferSystem
        println("TransferSystem podłączony do InventoryPanel")
    }

    fun render(x: Float, y: Float, width: Float, height: Float) {
        renderBackground(x, y, width, height)
        renderInventoryGrid(x, y, width, height)
        renderBottomInfo(x, y, width)
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

    private fun renderInventoryGrid(x: Float, y: Float, width: Float, height: Float) {
        val gridStartX = x + 14f
        val gridStartY = y + height - 80f

        // Render slots
        for (row in 0 until slotsPerColumn) {
            for (col in 0 until slotsPerRow) {
                val slotIndex = row * slotsPerRow + col
                val slotX = gridStartX + col * (slotSize + slotSpacing)
                val slotY = gridStartY - row * (slotSize + slotSpacing)

                renderSlot(slotX, slotY, slotIndex)
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
    }

    private fun renderSlot(x: Float, y: Float, slotIndex: Int) {
        // POPRAWIONE: Używamy ItemManager zamiast lokalnej mapy
        val item = itemManager?.getInventoryItem(slotIndex)

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

        // Render item if exists
        if (item != null) {
            game.batch.projectionMatrix = game.uiBatch.projectionMatrix
            game.batch.begin()

            // Item initial
            game.font.color = if (isSelected) Color.YELLOW else Color.WHITE
            val itemInitial = item.name.first().toString().uppercase()
            game.layout.setText(game.font, itemInitial)
            val textX = x + (slotSize - game.layout.width) / 2
            val textY = y + (slotSize + game.layout.height) / 2
            game.font.draw(game.batch, itemInitial, textX, textY)

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
    }

    private fun renderBottomInfo(x: Float, y: Float, width: Float) {
        val infoY = y + 20f

        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        game.font.color = Color.GOLD
        val goldText = "Gold: 3142"
        game.font.draw(game.batch, goldText, x + 30f, infoY)

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

                    // POPRAWIONE: Używamy temManager zamiast lokalnej mapy
                    val item = itemManager?.getInventoryItem(slotIndex)
                    val transfer = transferSystem ?: return false

                    if (isRightClick && item != null) {
                        // POPRAWIONE: Używamy ItemManager
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
                            // Przenieś tutaj
                            println("DEBUG: Przenoszenie na pusty slot $slotIndex")
                            transfer.moveItem("INVENTORY", slotIndex)
                        } else {
                            println("DEBUG: Klik na pusty slot $slotIndex bez wybranego itemu")
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