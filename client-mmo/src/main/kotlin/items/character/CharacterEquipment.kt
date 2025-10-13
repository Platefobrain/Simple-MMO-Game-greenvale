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
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.items.ItemTransferSystem
import pl.decodesoft.items.ItemTooltip

data class ClientItem(
    val id: String,
    val name: String,
    val type: String,
    val rarity: String,
    val requiredLevel: Int = 1,
    val strengthBonus: Int = 0,
    val agilityBonus: Int = 0,
    val spellPowerBonus: Int = 0,
    val staminaBonus: Int = 0,
    val manaBonus: Int = 0,
    val armorBonus: Int = 0,
    val attackSpeedBonus: Int = 0,
    val critRatingBonus: Int = 0,
    val icon: String? = null,
    val requiredClass: Int? = null
) {
    fun getBonusDescription(): String {
        val bonuses = mutableListOf<String>()

        if (strengthBonus > 0) bonuses.add("+$strengthBonus Siła")
        if (agilityBonus > 0) bonuses.add("+$agilityBonus Zręczność")
        if (spellPowerBonus > 0) bonuses.add("+$spellPowerBonus Moc Mag.")
        if (staminaBonus > 0) bonuses.add("+$staminaBonus Wytrzym.")
        if (manaBonus > 0) bonuses.add("+$manaBonus Mana")
        if (armorBonus > 0) bonuses.add("+$armorBonus Armor")
        if (attackSpeedBonus > 0) bonuses.add("+$attackSpeedBonus Szybk.")
        if (critRatingBonus > 0) bonuses.add("+$critRatingBonus Crit Rating")

        return if (bonuses.isEmpty()) "Brak bonusów" else bonuses.joinToString(", ")
    }
}

class CharacterEquipment(private val game: MMOGame) {

    private val slotSize = 50f

    // Reference do transfer systemu i tooltipa
    private lateinit var transferSystem: ItemTransferSystem
    private var itemTooltip: ItemTooltip? = null

    // Mapowanie slotów na indeksy
    private val slotIndices = mapOf(
        "HELMET" to 0,     // ← slot 0 = HELMET
        "ARMOR" to 1,    // ← slot 1 = ARMOR
        "PANTS" to 2,    // ← slot 2 = PANTS
        "BOOTS" to 3,   // ← slot 3 = BOOTS
        "WEAPON" to 4    // ← slot 4 = WEAPON
    )

    fun setTransferSystem(transferSystem: ItemTransferSystem) {
        this.transferSystem = transferSystem
    }

    fun setItemTooltip(tooltip: ItemTooltip) {
        this.itemTooltip = tooltip
    }

    fun render(x: Float, y: Float, width: Float, height: Float): Boolean {
        // NAJPIERW sprawdź hovery
        val anyItemHovered = renderEquipmentSlots(x, y, width, height)
        renderStats(x, y, width)

        return anyItemHovered
    }

    private fun renderSlotShadow(slotType: String, slotX: Float, slotY: Float) {
        val shadowColor = Color(0.4f, 0.4f, 0.4f, 0.6f)
        game.shapeRenderer.color = shadowColor

        val centerX = slotX + slotSize / 2
        val centerY = slotY + slotSize / 2

        when (slotType) {
            "HELMET" -> {
                // Hełm - prosty kształt głowy
                game.shapeRenderer.rect(centerX - 8f, centerY - 6f, 16f, 12f) // główna część
                game.shapeRenderer.rect(centerX - 6f, centerY + 6f, 12f, 3f) // dolny brzeg
            }

            "ARMOR" -> {
                // Zbroja - kształt koszuli/zbroi
                game.shapeRenderer.rect(centerX - 8f, centerY - 8f, 16f, 16f) // tułów
                game.shapeRenderer.rect(centerX - 10f, centerY + 2f, 4f, 8f) // lewe ramię
                game.shapeRenderer.rect(centerX + 6f, centerY + 2f, 4f, 8f) // prawe ramię
            }

            "PANTS" -> {
                // Spodnie - kształt spodni
                game.shapeRenderer.rect(centerX - 8f, centerY + 4f, 16f, 6f) // pas
                game.shapeRenderer.rect(centerX - 6f, centerY - 8f, 5f, 12f) // lewa nogawka
                game.shapeRenderer.rect(centerX + 1f, centerY - 8f, 5f, 12f) // prawa nogawka
            }

            "BOOTS" -> {
                // Buty - bardziej realistyczny kształt
                game.shapeRenderer.rect(centerX - 12f, centerY - 6f, 10f, 8f) // lewy but - główna część
                game.shapeRenderer.rect(centerX - 14f, centerY - 8f, 8f, 4f) // lewy but - podeszwa/przód
                game.shapeRenderer.rect(centerX + 2f, centerY - 6f, 10f, 8f) // prawy but - główna część
                game.shapeRenderer.rect(centerX + 6f, centerY - 8f, 8f, 4f) // prawy but - podeszwa/przód
            }

            "WEAPON" -> {
                // Miecz - grubszy z wyraźną końcówką
                game.shapeRenderer.rect(centerX - 2f, centerY - 12f, 4f, 20f) // ostrze - grubsze
                game.shapeRenderer.rect(centerX - 1f, centerY - 15f, 2f, 5f) // ostry czubek
                game.shapeRenderer.rect(centerX - 6f, centerY + 6f, 12f, 3f) // garda - grubsza
                game.shapeRenderer.rect(centerX - 2.5f, centerY + 9f, 5f, 6f) // rękojeść
                game.shapeRenderer.rect(centerX - 4f, centerY + 15f, 8f, 3f) // gałka - większa
            }
        }
    }

    private fun renderItemIcon(item: ClientItem, slotX: Float, slotY: Float, isSelected: Boolean) {
        val texture = game.itemManager.getItemTexture(item.id)
            ?: try { Texture(Gdx.files.internal("items/icons/error.png")) } catch (e: Exception) { null }

        if (texture != null) {
            // Renderuj teksturę itemu (lub itemerror.png)
            game.shapeRenderer.end() // Zakończ shape rendering

            // Przełącz na batch dla tekstur
            game.batch.projectionMatrix = game.uiBatch.projectionMatrix
            game.batch.begin()

            // Kolor ikony - jaśniejszy jeśli selected
            val alpha = if (isSelected) 1.0f else 0.9f
            game.batch.setColor(1f, 1f, 1f, alpha)

            // Renderuj teksturę wyśrodkowaną w slocie
            val iconSize = slotSize * 0.8f // 80% rozmiaru slotu
            val iconX = slotX + (slotSize - iconSize) / 2
            val iconY = slotY + (slotSize - iconSize) / 2

            game.batch.draw(texture, iconX, iconY, iconSize, iconSize)
            game.batch.setColor(1f, 1f, 1f, 1f) // Reset color

            game.batch.end()

            // Wróć do shape renderingu (dla dalszych slotów)
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        }
        // Jeśli texture == null, nie renderuj nic (pusty slot)
    }

    private fun renderEquipmentSlots(x: Float, y: Float, width: Float, height: Float): Boolean {
        val centerX = x + width / 2
        val centerY = y + height - 125f

        val characterAreaWidth = 160f
        val characterAreaHeight = 190f

        val slotPositions = mapOf(
            "HELMET" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY + 55f),
            "ARMOR" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY - 0f),
            "PANTS" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY - 55f),
            "BOOTS" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY - 110f),
            "WEAPON" to Pair(centerX + characterAreaWidth / 2 + 20f, centerY - 110f)
        )

        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Render obszaru modelu postaci
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.5f)
        game.shapeRenderer.rect(
            centerX - characterAreaWidth / 2,
            centerY - characterAreaHeight / 2,
            characterAreaWidth,
            characterAreaHeight
        )
        game.shapeRenderer.end()

        var anyItemHovered = false

        // Render slotów z podświetleniem
        slotPositions.forEach { (itemType, position) ->
            val (slotX, slotY) = position
            val item = getEquippedItem(itemType)
            val slotIndex = slotIndices[itemType] ?: -1

            val isSelected = if (::transferSystem.isInitialized) {
                transferSystem.getSelectedItem()?.let { selected ->
                    selected.sourceType == "EQUIPMENT" && selected.sourceSlot == slotIndex
                } ?: false
            } else false

            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

            when {
                isSelected -> {
                    game.shapeRenderer.setColor(0.5f, 0.5f, 0.2f, 0.8f)
                }
                item != null -> {
                    game.shapeRenderer.setColor(0.2f, 0.25f, 0.3f, 0.8f)
                }
                ::transferSystem.isInitialized && transferSystem.hasSelectedItem() -> {
                    val selectedItem = transferSystem.getSelectedItem()
                    if (selectedItem != null && canEquipInSlot()) {
                        game.shapeRenderer.setColor(0.2f, 0.4f, 0.2f, 0.7f)
                    } else {
                        game.shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.7f)
                    }
                }
                else -> {
                    game.shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.7f)
                }
            }
            game.shapeRenderer.rect(slotX, slotY, slotSize, slotSize)

            if (item == null) {
                renderSlotShadow(itemType, slotX, slotY)
            } else {
                renderItemIcon(item, slotX, slotY, isSelected)
            }

            game.shapeRenderer.end()

            // Border
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            if (isSelected) {
                game.shapeRenderer.setColor(1f, 1f, 0f, 1f)
            } else {
                game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f)
            }
            game.shapeRenderer.rect(slotX, slotY, slotSize, slotSize)
            game.shapeRenderer.end()
        }

        // Render numerów slotów i bonusów + sprawdzanie tooltipa
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        val slotNumbers = mapOf(
            "HELMET" to "1",
            "ARMOR" to "2",
            "PANTS" to "3",
            "BOOTS" to "4",
            "WEAPON" to "5"
        )

        slotPositions.forEach { (itemType, position) ->
            val (slotX, slotY) = position
            val item = getEquippedItem(itemType)
            val slotNumber = slotNumbers[itemType] ?: ""

            // Sprawdź hover dla tooltipa
            if (item != null) {
                val mouseX = Gdx.input.x.toFloat()
                val mouseY = Gdx.graphics.height - Gdx.input.y.toFloat()

                if (mouseX >= slotX && mouseX <= slotX + slotSize &&
                    mouseY >= slotY && mouseY <= slotY + slotSize) {

                    val itemDef = try {
                        game.itemManager.getItemDefinition(item.id)
                    } catch (e: Exception) {
                        null
                    }

                    if (itemDef != null) {
                        itemTooltip?.showTooltip(itemDef, mouseX, mouseY)
                    } else {
                        itemTooltip?.showTooltip(item, mouseX, mouseY)
                    }

                    anyItemHovered = true
                }
            }

            if (item == null) {
                game.font.color = Color.DARK_GRAY
                val originalScale = game.font.data.scaleX
                game.font.data.setScale(0.6f)
                game.font.draw(game.batch, slotNumber, slotX + 3f, slotY + slotSize - 3f)
                game.font.data.setScale(originalScale)
            } else {
                if (item.strengthBonus > 0 || item.agilityBonus > 0 ||
                    item.spellPowerBonus > 0 || item.staminaBonus > 0) {
                    game.font.color = Color.GREEN
                    val originalScale = game.font.data.scaleX
                    game.font.data.setScale(0.6f)
                    val totalBonus = item.strengthBonus + item.agilityBonus +
                            item.spellPowerBonus + item.staminaBonus + item.manaBonus
                    val bonus = "+$totalBonus"
                    game.font.draw(game.batch, bonus, slotX + slotSize - 18f, slotY + 12f)
                    game.font.data.setScale(originalScale)
                }
            }
        }

        // === ZMODYFIKOWANE: Nazwa, frakcja i level w centrum modelu ===
        var currentTextY = centerY + characterAreaHeight / 2 + 20f

        // 1. Nick gracza (na górze)
        game.font.color = Color.WHITE
        game.layout.setText(game.font, game.username)
        val nameX = centerX - game.layout.width / 2
        game.font.draw(game.batch, game.username, nameX, currentTextY)
        currentTextY -= 20f // Przesunięcie w dół

        // 2. Frakcja (pod nickiem)
        val factionText = when (game.localPlayer.faction.name) {
            "WATAHA" -> "(WATAHA)"
            "ZAKON" -> "(ZAKON)"
            else -> ""
        }

        if (factionText.isNotEmpty()) {
            val factionColor = when (game.localPlayer.faction.name) {
                "WATAHA" -> Color(0.8f, 0.3f, 0.1f, 1f) // Pomarańczowy
                "ZAKON" -> Color(0.2f, 0.4f, 0.8f, 1f)  // Niebieski
                else -> Color.GRAY
            }

            game.font.color = factionColor
            game.layout.setText(game.font, factionText)
            val factionX = centerX - game.layout.width / 2
            game.font.draw(game.batch, factionText, factionX, currentTextY)
            currentTextY -= 20f // Przesunięcie w dół
        }

        // 3. Level i klasa (na dole) - ZMODYFIKOWANE
        game.font.color = Color.YELLOW
        val className = when (game.localPlayer.characterClass) {
            0 -> "Archer"
            1 -> "Mage"
            2 -> "Warrior"
            else -> "BŁĄD"
        }

        val levelText = "Level ${game.localPlayer.level} $className"
        game.layout.setText(game.font, levelText)
        val levelX = centerX - game.layout.width / 2
        game.font.draw(game.batch, levelText, levelX, currentTextY)

        game.batch.end()
        resetFontColor()

        return anyItemHovered
    }

    private fun renderStats(x: Float, y: Float, width: Float) {
        val statsY = y + 120f
        val column1X = x + 50f
        val column2X = x + width / 2 + 20f
        val lineHeight = 23f //odstep gora dol pomiedzy

        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        val player = game.localPlayer

        // Lewa kolumna
        game.font.color = Color.LIGHT_GRAY
        var currentY = statsY

        game.font.draw(game.batch, "Strength:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalStrength()}", column1X + 100f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Agility:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalAgility()}", column1X + 100f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Spell Power:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalSpellPower()}", column1X + 100f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Stamina:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalStamina()}", column1X + 100f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Mana:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalMana()}", column1X + 100f, currentY)
        currentY -= lineHeight

        // Prawa kolumna
        currentY = statsY

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Damage:", column2X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, player.getDamageRange(), column2X + 115f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Armor:", column2X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalArmor()}", column2X + 115f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Critical:", column2X, currentY)
        game.font.color = Color.WHITE
        val crit = player.getTotalCritChance()
        game.font.draw(game.batch, "${String.format("%.1f", crit)}%", column2X + 115f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Attack Speed:", column2X, currentY)
        game.font.color = Color.WHITE
        val cooldownSeconds = player.updateAttackCooldown()
        game.font.draw(game.batch, "${String.format("%.2f", cooldownSeconds)}s", column2X + 115f, currentY)
        currentY -= lineHeight

        game.batch.end()
        resetFontColor()
    }

    private fun getEquippedItem(itemType: String): ClientItem? {
        return game.localPlayer.getEquippedItem(itemType)
    }

    private fun canEquipInSlot(): Boolean {
        // Tutaj możesz dodać logikę sprawdzania czy item pasuje do slotu
        // Na razie zakładamy że tak
        return true
    }

    fun handleClick(touchX: Float, touchY: Float, panelX: Float, panelY: Float, panelWidth: Float, panelHeight: Float, isRightClick: Boolean): Boolean {
        val centerX = panelX + panelWidth / 2
        val centerY = panelY + panelHeight - 125f

        val characterAreaWidth = 160f

        val slotPositions = mapOf(
            "HELMET" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY + 55f),
            "ARMOR" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY - 0f),
            "PANTS" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY - 55f),
            "BOOTS" to Pair(centerX - characterAreaWidth / 2 - slotSize - 20f, centerY - 110f),
            "WEAPON" to Pair(centerX + characterAreaWidth / 2 + 20f, centerY - 110f)
        )

        slotPositions.forEach { (itemType, position) ->
            val (slotX, slotY) = position
            if (touchX >= slotX && touchX <= slotX + slotSize &&
                touchY >= slotY && touchY <= slotY + slotSize) {

                val item = getEquippedItem(itemType)
                val slotIndex = slotIndices[itemType] ?: -1

                if (!::transferSystem.isInitialized) return false

                if (isRightClick && item != null) {
                    // Prawy klik - natychmiast zdejmij
                    transferSystem.rightClickUnequip(item.id)
                    return true
                }

                if (item != null) {
                    // Lewy klik na item w slocie
                    if (transferSystem.hasSelectedItem()) {
                        val selectedItem = transferSystem.getSelectedItem()
                        if (selectedItem?.sourceType == "EQUIPMENT" && selectedItem.sourceSlot == slotIndex) {
                            // Klik na ten sam item - anuluj
                            transferSystem.clearSelection()
                        } else {
                            // Przenieś/zamień na ten slot
                            transferSystem.moveItem("EQUIPMENT", slotIndex)
                        }
                    } else {
                        // Wybierz ten item
                        transferSystem.selectItem(item.id, item.name, "EQUIPMENT", slotIndex)
                    }
                } else {
                    // Lewy klik na pusty slot
                    if (transferSystem.hasSelectedItem()) {
                        // Przenieś tutaj (jeśli to możliwe)
                        val selectedItem = transferSystem.getSelectedItem()
                        if (selectedItem != null && canEquipInSlot()) {
                            transferSystem.moveItem("EQUIPMENT", slotIndex)
                        }
                    }
                }
                return true
            }
        }

        return false
    }

    private fun resetFontColor() {
        game.font.color = Color.WHITE
    }
}