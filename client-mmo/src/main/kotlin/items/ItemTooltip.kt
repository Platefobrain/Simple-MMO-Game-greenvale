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

package pl.decodesoft.items

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.Strings
import pl.decodesoft.items.character.ClientItem

class ItemTooltip(private val game: MMOGame) {

    private var currentItem: ClientItem? = null
    private var tooltipX = 0f
    private var tooltipY = 0f
    private var isVisible = false
    private var hoverStartTime = 0L

    companion object {
        const val TOOLTIP_DELAY = 100L
        const val MARGIN = 10f
        const val MIN_WIDTH = 250f
        const val LINE_HEIGHT = 20f
        const val PADDING = 12f
        const val TOP_PADDING = 10f

        // Kolory klas
        val COLOR_ARCHER = Color(0.67f, 0.83f, 0.45f, 1f)
        val COLOR_MAGE = Color(0.25f, 0.78f, 0.92f, 1f)
        val COLOR_WARRIOR = Color(0.78f, 0.61f, 0.43f, 1f)
        val COLOR_EQUIP_BONUS = Color(0.12f, 1.0f, 0.0f, 1f)

        // Kolory rzadkości
        val COLOR_COMMON = Color(1f, 1f, 1f, 1f)              // biały
        val COLOR_UNCOMMON = Color(0.12f, 1.0f, 0.0f, 1f)     // #1eff00
        val COLOR_RARE = Color(0.0f, 0.44f, 0.87f, 1f)        // #0070dd
        val COLOR_EPIC = Color(0.58f, 0.27f, 1.0f, 1f)        // #9345ff
    }

    // Dodaj funkcję helper
    private fun getRarityColor(rarity: String): Color {
        return when (rarity) {
            "COMMON" -> COLOR_COMMON
            "UNCOMMON" -> COLOR_UNCOMMON
            "RARE" -> COLOR_RARE
            "EPIC" -> COLOR_EPIC
            else -> COLOR_COMMON
        }
    }

    fun showTooltip(item: ClientItem, mouseX: Float, mouseY: Float) {
        if (currentItem?.id != item.id) {
            currentItem = item
            hoverStartTime = System.currentTimeMillis()
            isVisible = false
        } else {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - hoverStartTime

            if (elapsed >= TOOLTIP_DELAY) {
                isVisible = true
                updateTooltipPosition(mouseX, mouseY)
            }
        }
    }

    fun resetTooltip() {
        currentItem = null
        isVisible = false
        hoverStartTime = 0L
    }

    private fun updateTooltipPosition(mouseX: Float, mouseY: Float) {
        val screenWidth = Gdx.graphics.width.toFloat()
        val estimatedWidth = calculateTooltipWidth()
        val estimatedHeight = calculateTooltipHeight()

        tooltipX = when {
            mouseX + estimatedWidth + MARGIN > screenWidth ->
                mouseX - estimatedWidth - MARGIN
            else -> mouseX + MARGIN
        }

        tooltipY = when {
            mouseY - estimatedHeight - MARGIN < 0 ->
                mouseY + MARGIN
            else -> mouseY - MARGIN
        }
    }

    // Zwraca nazwę slotu (WEAPON -> Weapon, ARMOR -> Chest, itp)
    private fun getItemSlotName(item: ClientItem): String {
        return when (item.type) {
            "WEAPON" -> "Weapon"
            "ARMOR" -> "Chest"
            "HELMET" -> "Head"
            "PANTS" -> "Legs"
            "BOOTS" -> "Feet"
            else -> item.type
        }
    }

    // Zwraca typ materiału (Plate, Leather, Cloth, Sword, Bow, Staff)
    private fun getItemSubtype(item: ClientItem): String? {
        return when {
            item.requiredClass == null -> null

            // WEAPON
            item.type == "WEAPON" && item.requiredClass == 2 -> "Sword"
            item.type == "WEAPON" && item.requiredClass == 0 -> "Bow"
            item.type == "WEAPON" && item.requiredClass == 1 -> "Staff"

            // ARMOR/HELMET/PANTS/BOOTS
            (item.type == "ARMOR" || item.type == "HELMET" || item.type == "PANTS" || item.type == "BOOTS") &&
                    item.requiredClass == 2 -> "Plate"

            (item.type == "ARMOR" || item.type == "HELMET" || item.type == "PANTS" || item.type == "BOOTS") &&
                    item.requiredClass == 0 -> "Leather"

            (item.type == "ARMOR" || item.type == "HELMET" || item.type == "PANTS" || item.type == "BOOTS") &&
                    item.requiredClass == 1 -> "Cloth"

            else -> null
        }
    }

    // Zwraca nazwę klasy na podstawie requiredClass
    private fun getClassName(classId: Int): String {
        return when (classId) {
            0 -> "Archer"
            1 -> "Mage"
            2 -> "Warrior"
            else -> "Unknown"
        }
    }

    // Zwraca kolor dla klasy
    private fun getClassColor(classId: Int): Color {
        return when (classId) {
            0 -> COLOR_ARCHER
            1 -> COLOR_MAGE
            2 -> COLOR_WARRIOR
            else -> Color.WHITE
        }
    }

    private fun calculateTooltipWidth(): Float {
        val item = currentItem ?: return MIN_WIDTH

        val texts = mutableListOf<String>()
        texts.add(item.name)

        // Linia ze slotem i typem
        val slotLine = "${getItemSlotName(item)}${getItemSubtype(item)?.let { "    $it" } ?: ""}"
        texts.add(slotLine)

        // Armor
        if (item.armorBonus > 0) {
            texts.add("${item.armorBonus} Armor")
        }

        // Staty
        if (item.strengthBonus > 0) texts.add("+${item.strengthBonus} ${Strings.STAT_STRENGTH}")
        if (item.agilityBonus > 0) texts.add("+${item.agilityBonus} ${Strings.STAT_AGILITY}")
        if (item.spellPowerBonus > 0) texts.add("+${item.spellPowerBonus} ${Strings.STAT_SPELL_POWER}")
        if (item.staminaBonus > 0) texts.add("+${item.staminaBonus} ${Strings.STAT_STAMINA}")
        if (item.manaBonus > 0) texts.add("+${item.manaBonus} ${Strings.STAT_MANA}")

        // Classes line
        if (item.requiredClass != null) {
            texts.add("Classes: ${getClassName(item.requiredClass)}")
        }

        // Level requirement (przykład)
        texts.add("Requires Level: 40")

        // Equip bonusy
        if (item.critRatingBonus > 0) {
            texts.add("Equip: Improves critical strike rating by ${item.critRatingBonus}.")  // ZMIANA: usunięto .toInt() bo już jest Int
        }
        if (item.attackSpeedBonus > 0) {
            texts.add("Equip: Improves attack speed rating by ${item.attackSpeedBonus}.")
        }

        // Sell price (przykład)
        texts.add("Sell price: 13 Gold 12 Silver 14 Copper")

        var maxWidth = MIN_WIDTH
        texts.forEach { text ->
            game.layout.setText(game.font, text)
            val textWidth = game.layout.width + PADDING * 2
            if (textWidth > maxWidth) {
                maxWidth = textWidth
            }
        }

        return maxWidth
    }

    private fun calculateTooltipHeight(): Float {
        val item = currentItem ?: return LINE_HEIGHT * 2

        var lines = 1 // nazwa
        lines++ // slot + typ
        lines++ // pusta linia

        // Armor
        if (item.armorBonus > 0) lines++

        // Staty
        if (item.strengthBonus > 0) lines++
        if (item.agilityBonus > 0) lines++
        if (item.spellPowerBonus > 0) lines++
        if (item.staminaBonus > 0) lines++
        if (item.manaBonus > 0) lines++

        lines++ // pusta linia

        // Classes
        if (item.requiredClass != null) lines++

        // Level requirement
        lines++

        lines++ // pusta linia

        // Equip bonusy
        if (item.critRatingBonus > 0) lines++
        if (item.attackSpeedBonus > 0) lines++

        lines++ // pusta linia
        lines++ // sell price

        return lines * LINE_HEIGHT + PADDING * 2
    }

    fun render() {
        if (!isVisible || currentItem == null) {
            return
        }

        val item = currentItem!!
        val tooltipWidth = calculateTooltipWidth()
        val tooltipHeight = calculateTooltipHeight()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix

        // Tło
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.05f, 0.05f, 0.15f, 0.95f)
        game.shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight)
        game.shapeRenderer.end()

        // Ramka
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        game.shapeRenderer.setColor(0.7f, 0.7f, 0.2f, 1f)
        game.shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight)
        game.shapeRenderer.end()

        // Tekst
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
        game.batch.begin()

        var currentY = tooltipY + tooltipHeight - PADDING - TOP_PADDING
        val textX = tooltipX + PADDING

        // 1. Nazwa itemu - KOLOR WEDŁUG RZADKOŚCI
        game.font.color = getRarityColor(item.rarity)
        game.font.draw(game.batch, item.name, textX, currentY)
        currentY -= LINE_HEIGHT

        // 2. Slot po lewej | Typ po prawej - białe
        game.font.color = Color.WHITE
        val slotName = getItemSlotName(item)
        game.font.draw(game.batch, slotName, textX, currentY)

        getItemSubtype(item)?.let { subtype ->
            game.layout.setText(game.font, subtype)
            val subtypeX = tooltipX + tooltipWidth - PADDING - game.layout.width
            game.font.draw(game.batch, subtype, subtypeX, currentY)
        }
        currentY -= LINE_HEIGHT

        // Pusta linia
        currentY -= LINE_HEIGHT * 0.5f

        // 3. Armor - biały
        if (item.armorBonus > 0) {
            game.font.color = Color.WHITE
            game.font.draw(game.batch, "${item.armorBonus} Armor", textX, currentY)
            currentY -= LINE_HEIGHT
        }

        // 4. Staty - białe, jeden pod drugim
        game.font.color = Color.WHITE
        if (item.strengthBonus > 0) {
            game.font.draw(game.batch, "+${item.strengthBonus} ${Strings.STAT_STRENGTH}", textX, currentY)
            currentY -= LINE_HEIGHT
        }
        if (item.agilityBonus > 0) {
            game.font.draw(game.batch, "+${item.agilityBonus} ${Strings.STAT_AGILITY}", textX, currentY)
            currentY -= LINE_HEIGHT
        }
        if (item.spellPowerBonus > 0) {
            game.font.draw(game.batch, "+${item.spellPowerBonus} ${Strings.STAT_SPELL_POWER}", textX, currentY)
            currentY -= LINE_HEIGHT
        }
        if (item.staminaBonus > 0) {
            game.font.draw(game.batch, "+${item.staminaBonus} ${Strings.STAT_STAMINA}", textX, currentY)
            currentY -= LINE_HEIGHT
        }
        if (item.manaBonus > 0) {
            game.font.draw(game.batch, "+${item.manaBonus} ${Strings.STAT_MANA}", textX, currentY)
            currentY -= LINE_HEIGHT
        }

        // Pusta linia
        currentY -= LINE_HEIGHT * 0.5f

        // 5. Classes: - biały "Classes:", potem klasa kolorem
        if (item.requiredClass != null) {
            game.font.color = Color.WHITE
            game.font.draw(game.batch, "Classes: ", textX, currentY)

            game.layout.setText(game.font, "Classes: ")
            val classNameX = textX + game.layout.width

            game.font.color = getClassColor(item.requiredClass)
            game.font.draw(game.batch, getClassName(item.requiredClass), classNameX, currentY)
            currentY -= LINE_HEIGHT
        }

        // 6. Requires Level - biały lub czerwony jeśli za niski
        val playerLevel = game.localPlayer.level
        val canEquip = playerLevel >= item.requiredLevel

        game.font.color = if (canEquip) Color.WHITE else Color.RED
        game.font.draw(game.batch, "Requires Level: ${item.requiredLevel}", textX, currentY)
        currentY -= LINE_HEIGHT

        // Pusta linia
        currentY -= LINE_HEIGHT * 0.5f

        // 7. Equip bonusy - zielone
        game.font.color = COLOR_EQUIP_BONUS
        if (item.critRatingBonus > 0) {
            game.font.draw(game.batch, "Equip: Improves critical strike rating by ${item.critRatingBonus}.", textX, currentY)
            currentY -= LINE_HEIGHT
        }
        if (item.attackSpeedBonus > 0) {
            game.font.draw(game.batch, "Equip: Improves attack speed rating by ${item.attackSpeedBonus}.", textX, currentY)
            currentY -= LINE_HEIGHT
        }

        // Pusta linia
        currentY -= LINE_HEIGHT * 0.5f

        // 8. Sell price - biały
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "Sell price: 13 Gold 12 Silver 14 Copper", textX, currentY)

        game.batch.end()

        // Reset koloru
        game.font.color = Color.WHITE

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }
}