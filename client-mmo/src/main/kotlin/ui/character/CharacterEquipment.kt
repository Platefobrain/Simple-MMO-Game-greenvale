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

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.ui.ItemTransferSystem

data class ClientItem(
    val id: String,
    val name: String,
    val type: String,
    val strengthBonus: Int = 0,
    val agilityBonus: Int = 0,
    val spellPowerBonus: Int = 0,
    val staminaBonus: Int = 0,
    val manaBonus: Int = 0
) {
    fun getBonusDescription(): String {
        val bonuses = mutableListOf<String>()

        if (strengthBonus > 0) bonuses.add("+$strengthBonus Siła")
        if (agilityBonus > 0) bonuses.add("+$agilityBonus Zręczność")
        if (spellPowerBonus > 0) bonuses.add("+$spellPowerBonus Moc Mag.")
        if (staminaBonus > 0) bonuses.add("+$staminaBonus Wytrzym.")

        return if (bonuses.isEmpty()) "Brak bonusów" else bonuses.joinToString(", ")
    }
}

class CharacterEquipment(private val game: MMOGame) {

    private val slotSize = 50f
    private val slotSpacing = 8f

    // Reference do transfer systemu
    private lateinit var transferSystem: ItemTransferSystem

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

    fun render(x: Float, y: Float, width: Float, height: Float) {
        renderEquipmentSlots(x, y, width, height)
        renderStats(x, y, width, height)
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
        val centerX = slotX + slotSize / 2
        val centerY = slotY + slotSize / 2

        // Kolor ikony itemu - jaśniejszy niż pusty slot
        val itemColor = if (isSelected) Color(1f, 1f, 0f, 0.9f) else Color(0.8f, 0.8f, 0.9f, 0.9f)
        game.shapeRenderer.color = itemColor

        when (item.type) {
            "HELMET" -> {
                // Hełm - kształt z detalami
                game.shapeRenderer.rect(centerX - 8f, centerY - 6f, 16f, 12f) // główna część
                game.shapeRenderer.rect(centerX - 6f, centerY + 6f, 12f, 3f) // dolny brzeg
                game.shapeRenderer.rect(centerX - 4f, centerY - 3f, 8f, 2f) // pasek na środku
            }

            "ARMOR" -> {
                // Zbroja - bardziej szczegółowa
                game.shapeRenderer.rect(centerX - 8f, centerY - 8f, 16f, 16f) // tułów
                game.shapeRenderer.rect(centerX - 10f, centerY + 2f, 4f, 8f) // lewe ramię
                game.shapeRenderer.rect(centerX + 6f, centerY + 2f, 4f, 8f) // prawe ramię
                game.shapeRenderer.rect(centerX - 6f, centerY - 2f, 12f, 1f) // pasek środkowy
                game.shapeRenderer.rect(centerX - 6f, centerY + 2f, 12f, 1f) // dolny pasek
            }

            "PANTS" -> {
                // Spodnie - z detalami
                game.shapeRenderer.rect(centerX - 8f, centerY + 4f, 16f, 6f) // pas
                game.shapeRenderer.rect(centerX - 6f, centerY - 8f, 5f, 12f) // lewa nogawka
                game.shapeRenderer.rect(centerX + 1f, centerY - 8f, 5f, 12f) // prawa nogawka
                game.shapeRenderer.rect(centerX - 8f, centerY + 7f, 16f, 1f) // pasek na pasie
            }

            "BOOTS" -> {
                // Buty - bardziej szczegółowe
                game.shapeRenderer.rect(centerX - 12f, centerY - 6f, 10f, 8f) // lewy but
                game.shapeRenderer.rect(centerX - 14f, centerY - 8f, 8f, 4f) // lewa podeszwa
                game.shapeRenderer.rect(centerX + 2f, centerY - 6f, 10f, 8f) // prawy but
                game.shapeRenderer.rect(centerX + 6f, centerY - 8f, 8f, 4f) // prawa podeszwa
                // Sznurówki/detale
                game.shapeRenderer.rect(centerX - 10f, centerY - 3f, 6f, 1f) // lewa linia
                game.shapeRenderer.rect(centerX + 4f, centerY - 3f, 6f, 1f) // prawa linia
            }

            "WEAPON" -> {
                // Miecz - taki sam jak pusty slot ale jaśniejszy
                game.shapeRenderer.rect(centerX - 2f, centerY - 12f, 4f, 20f) // ostrze
                game.shapeRenderer.rect(centerX - 1f, centerY - 15f, 2f, 5f) // ostry czubek
                game.shapeRenderer.rect(centerX - 6f, centerY + 6f, 12f, 3f) // garda
                game.shapeRenderer.rect(centerX - 2.5f, centerY + 9f, 5f, 6f) // rękojeść
                game.shapeRenderer.rect(centerX - 4f, centerY + 15f, 8f, 3f) // gałka
            }

            else -> {
                // Fallback - prosty kwadrat z pierwszą literą (będzie renderowany w batch)
                game.shapeRenderer.rect(centerX - 6f, centerY - 6f, 12f, 12f)
            }
        }
    }

    private fun renderEquipmentSlots(x: Float, y: Float, width: Float, height: Float) {
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

        // Render slotów z podświetleniem
        slotPositions.forEach { (itemType, position) ->
            val (slotX, slotY) = position
            val item = getEquippedItem(itemType)
            val slotIndex = slotIndices[itemType] ?: -1

            // Sprawdź czy to selected slot
            val isSelected = if (::transferSystem.isInitialized) {
                transferSystem.getSelectedItem()?.let { selected ->
                    selected.sourceType == "EQUIPMENT" && selected.sourceSlot == slotIndex
                } ?: false
            } else false

            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

            when {
                isSelected -> {
                    game.shapeRenderer.setColor(0.5f, 0.5f, 0.2f, 0.8f) // Żółtawy dla selected
                }

                item != null -> {
                    game.shapeRenderer.setColor(0.2f, 0.25f, 0.3f, 0.8f)
                }

                ::transferSystem.isInitialized && transferSystem.hasSelectedItem() -> {
                    // Sprawdź czy można tutaj przenieść
                    val selectedItem = transferSystem.getSelectedItem()
                    if (selectedItem != null && canEquipInSlot(selectedItem.itemId, itemType)) {
                        game.shapeRenderer.setColor(0.2f, 0.4f, 0.2f, 0.7f) // Zielony dla możliwego celu
                    } else {
                        game.shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.7f) // Normalny
                    }
                }

                else -> {
                    game.shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.7f)
                }
            }
            game.shapeRenderer.rect(slotX, slotY, slotSize, slotSize)

            // Renderuj cień/sylwetkę tylko dla pustych slotów
            if (item == null) {
                renderSlotShadow(itemType, slotX, slotY)
            } else {
                // Renderuj ikonę itemu
                renderItemIcon(item, slotX, slotY, isSelected)
            }

            game.shapeRenderer.end()

            // Border
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            if (isSelected) {
                game.shapeRenderer.setColor(1f, 1f, 0f, 1f) // Żółta ramka dla selected
            } else {
                game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f)
            }
            game.shapeRenderer.rect(slotX, slotY, slotSize, slotSize)
            game.shapeRenderer.end()
        }

        // Render numerów slotów i bonusów
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

            if (item == null) {
                // Pusty slot - pokaż numer w rogu
                game.font.color = Color.DARK_GRAY
                val originalScale = game.font.data.scaleX
                game.font.data.setScale(0.6f)
                game.font.draw(game.batch, slotNumber, slotX + 3f, slotY + slotSize - 3f)
                game.font.data.setScale(originalScale)
            } else {
                // Slot z itemem - tylko bonusy w rogu
                if (item.strengthBonus > 0 || item.agilityBonus > 0 ||
                    item.spellPowerBonus > 0 || item.staminaBonus > 0) {
                    game.font.color = Color.GREEN
                    val originalScale = game.font.data.scaleX
                    game.font.data.setScale(0.6f)
                    val totalBonus = item.strengthBonus + item.agilityBonus +
                            item.spellPowerBonus + item.staminaBonus
                    val bonus = "+$totalBonus"
                    game.font.draw(game.batch, bonus, slotX + slotSize - 18f, slotY + 12f)
                    game.font.data.setScale(originalScale)
                }
            }
        }

        // Nazwa i level w centrum modelu
        game.font.color = Color.WHITE
        game.layout.setText(game.font, game.username)
        val nameX = centerX - game.layout.width / 2
        val nameY = centerY + characterAreaHeight / 2 + 20f
        game.font.draw(game.batch, game.username, nameX, nameY)

        game.font.color = Color.YELLOW

        // nazwa klasy
        val className = when (game.localPlayer.characterClass) {
            0 -> "Archer"    // CharacterClass.WARRIOR
            1 -> "Mage"     // CharacterClass.ARCHER
            2 -> "Warrior"       // CharacterClass.MAGE
            else -> "BŁĄÐ" // Fallback
        }

        val levelText = "Level ${game.localPlayer.level} $className"
        game.layout.setText(game.font, levelText)
        val levelX = centerX - game.layout.width / 2
        val levelY = nameY - 30f
        game.font.draw(game.batch, levelText, levelX, levelY)

        game.batch.end()
        resetFontColor()
    }

    private fun renderStats(x: Float, y: Float, width: Float, height: Float) {
        val statsY = y + 120f
        val column1X = x + 20f
        val column2X = x + width / 2 + 37f
        val lineHeight = 27f

        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        val player = game.localPlayer

        // Lewa kolumna
        game.font.color = Color.LIGHT_GRAY
        var currentY = statsY

        game.font.draw(game.batch, "Strength:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalStrength()}", column1X + 95f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Agility:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalAgility()}", column1X + 95f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Spell Power:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalSpellPower()}", column1X + 95f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Stamina:", column1X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalStamina()}", column1X + 95f, currentY)
        currentY -= lineHeight

        // Prawa kolumna
        currentY = statsY

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Armor:", column2X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "null", column2X + 125f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Mana:", column2X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.getTotalMana()}", column2X + 125f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Ilość życia:", column2X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "${player.maxHealth}", column2X + 125f, currentY)
        currentY -= lineHeight

        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Szybkość Ataku:", column2X, currentY)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "null", column2X + 125f, currentY)

        game.batch.end()
        resetFontColor()
    }

    private fun getEquippedItem(itemType: String): ClientItem? {
        return game.localPlayer.getEquippedItem(itemType)
    }

    private fun canEquipInSlot(itemId: String, slotType: String): Boolean {
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
                        if (selectedItem != null && canEquipInSlot(selectedItem.itemId, itemType)) {
                            transferSystem.moveItem("EQUIPMENT", slotIndex)
                        }
                    }
                }
                return true
            }
        }

        return false
    }

    private fun unequipItem(itemType: String) {
        game.sendWebSocketMessage("UNEQUIP_ITEM|${game.localPlayer.id}|$itemType")
        println("Wysłano żądanie zdjęcia itemu typu: $itemType")
    }

    fun setEquippedItem(itemType: String, item: ClientItem?) {
        if (item != null) {
            game.localPlayer.equipItem(item)
        } else {
            game.localPlayer.unequipItem(itemType)
        }
        println("Zaktualizowano ekwipunek: $itemType = ${item?.name ?: "none"}")
    }

    private fun resetFontColor() {
        game.font.color = Color.WHITE
    }
}