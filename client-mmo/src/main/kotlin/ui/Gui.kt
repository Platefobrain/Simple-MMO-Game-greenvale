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
 * MERCHANTABILITY or.PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with [GreenVale].  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.decodesoft.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import pl.decodesoft.MMOGame
import pl.decodesoft.Strings
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.items.ItemTooltip
import pl.decodesoft.items.ItemTransferSystem
import pl.decodesoft.player.Player
import pl.decodesoft.items.character.CharacterWindow
import pl.decodesoft.items.inventory.InventoryPanel
import pl.decodesoft.npc.NPCClient

// Klasa odpowiedzialna za renderowanie elementów interfejsu użytkownika
class GameUI(private val game: MMOGame) {

    //kolory
    private val customGreen = Color(30f / 255f, 134f / 255f, 30f / 255f, 1f)
    private val customBlue = Color(46f / 255f, 77f / 255f, 131f / 255f, 1f)

    // fps
    private var showFPS = true
    private var fpsTimer = 0f
    private var frameCount = 0
    private var currentFPS = 0

    // characterpanel
    private val characterWindow = CharacterWindow(game)
    private val inventoryPanel = InventoryPanel(game)
    private var isCharacterWindowVisible = false
    private var isInventoryVisible = false
    private val itemTooltip = ItemTooltip(game)
    //transfer system itemy
    private val itemTransferSystem = ItemTransferSystem(game)

    init {
        characterWindow.setTransferSystem(itemTransferSystem)
        inventoryPanel.setTransferSystem(itemTransferSystem)

        inventoryPanel.setItemTooltip(itemTooltip)
        characterWindow.setItemTooltip(itemTooltip)
    }

    // Klasa do przechowywania obrażeń gracza (UI)
    private class PlayerDamageText(
        var y: Float,
        val text: String,
        val color: Color,
        var alpha: Float = 1.0f,
        var lifetime: Float = 0f
    )

    fun toggleFPS() {
        showFPS = !showFPS
    }

    private fun updateFPS(delta: Float) {
        frameCount++
        fpsTimer += delta

        if (fpsTimer >= 1f) {
            currentFPS = frameCount
            frameCount = 0
            fpsTimer = 0f
        }
    }

    private fun renderFPS() {
        if (!showFPS) return

        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined

        game.batch.begin()

        val oldFontColor = game.font.color.cpy()
        game.font.color = Color.WHITE

        // Pozycja: prawy górny róg
        val text = "FPS: $currentFPS"
        game.layout.setText(game.font, text)
        val textWidth = game.layout.width

        val x = game.uiCamera.viewportWidth - textWidth - 10f
        val y = game.uiCamera.viewportHeight - 10f

        game.font.draw(game.batch, text, x, y)

        game.font.color = oldFontColor
        game.batch.end()
        game.batch.projectionMatrix = oldBatchProj
    }

    // Metoda wywoływana z MMOGame po inicjalizacji pItemManager
    fun connectItemManager() {
        try {
            val manager = game.itemManager // Spróbuj pobrać
            inventoryPanel.setItemManager(manager)
            itemTransferSystem.setItemManager(manager)
        } catch (e: Exception) {
            println("BŁĄD: ItemManager nie jest zainicjowany: ${e.message}")
        }
    }

    // Lista obrażeń gracza (UI)
    private val playerDamageTexts = mutableListOf<PlayerDamageText>()

    // tło ui
    private val transparentBackgroundDrawable: Drawable by lazy {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0.3f)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        TextureRegionDrawable(TextureRegion(texture))
    }

    private fun resetFontColor() {
        // Ustawia domyślny kolor czcionki na biały (nieprzezroczysty)
        game.font.color = Color.WHITE
    }

    // Publiczna metoda do dodawania obrażeń gracza (UI)
    fun addPlayerDamageText(text: String, color: Color) {
        val startY = game.uiCamera.viewportHeight / 2 + 0f
        playerDamageTexts.add(PlayerDamageText(startY, text, color))
    }

    // Aktualizacja obrażeń gracza
    private fun updatePlayerDamage(delta: Float) {
        // Aktualizacja pozycji i przezroczystości
        playerDamageTexts.forEach { text ->
            text.y += 30f * delta // Unoszenie w górę (szybciej niż w świecie)
            text.alpha -= delta * 0.5f // Szybsze zanikanie
            text.lifetime += delta
        }

        // Usuwanie starych efektów
        playerDamageTexts.removeAll { it.lifetime > 1.6f } // Krótsze życie
    }

    // Metoda do przełączania panelu statystyk
    fun toggleCharacterWindow() {
        isCharacterWindowVisible = !isCharacterWindowVisible
    }

    // Metoda do przelaczania inv
    fun toggleInventory() {
        isInventoryVisible = !isInventoryVisible
    }

    // Sprawdza czy kliknięto w przycisk statystyk "C"
    fun isClickOnCharacterButton(mouseX: Float, mouseY: Float): Boolean {
        val xpBarWidth = game.uiCamera.viewportWidth * 0.8f
        val xpBarX = (game.uiCamera.viewportWidth - xpBarWidth) / 2

        val buttonX = xpBarX + (xpBarWidth - 45f) / 2 - 25f
        val buttonY = 35f
        val buttonWidth = 20f
        val buttonHeight = 30f

        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight
    }

    // Sprawdza czy kliknięto w przycisk inventory "I"
    fun isClickOnInventoryButton(mouseX: Float, mouseY: Float): Boolean {
        // Przycisk "I" obok przycisku "C"
        val xpBarWidth = game.uiCamera.viewportWidth * 0.8f
        val xpBarX = (game.uiCamera.viewportWidth - xpBarWidth) / 2

        val buttonX = xpBarX + (xpBarWidth - 45f) / 2 + 5f // Przesunięty w prawo
        val buttonY = 35f
        val buttonWidth = 20f
        val buttonHeight = 30f

        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight
    }

    // Sprawdza czy kliknięto w przycisk Chat
    fun isClickOnChatButton(mouseX: Float, mouseY: Float): Boolean {
        val chatButtonX = 20f
        val buttonY = 60f + 250f // chatY + 250f
        val buttonWidth = 60f
        val buttonHeight = 25f

        return mouseX >= chatButtonX && mouseX <= chatButtonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight
    }

    // Sprawdza czy kliknięto w przycisk Log
    fun isClickOnLogButton(mouseX: Float, mouseY: Float): Boolean {
        val buttonSpacing = 5f
        val buttonWidth = 60f
        val chatButtonX = 20f
        val logButtonX = chatButtonX + buttonWidth + buttonSpacing
        val buttonY = 60f + 250f // chatY + 250f
        val buttonHeight = 25f

        return mouseX >= logButtonX && mouseX <= logButtonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight
    }

    // Renderuje wszystkie elementy UI
    fun render() {
        updateFPS(Gdx.graphics.deltaTime)

        // Zakończ jakiekolwiek aktywne operacje renderowania
        if (game.uiBatch.isDrawing) game.uiBatch.end()
        if (game.batch.isDrawing) game.batch.end()
        if (game.shapeRenderer.isDrawing) game.shapeRenderer.end()

        // projekcja
        val originalBatchProjection = game.batch.projectionMatrix.cpy()
        val originalShapeProjection = game.shapeRenderer.projectionMatrix.cpy()

        // Aktualizuj obrażenia gracza
        updatePlayerDamage(Gdx.graphics.deltaTime)

        // Renderowanie panelu gracza
        renderPlayerUnitFrame()

        // Renderowanie panelu przeciwnika
        val selectedPlayer = game.players.values.find { it.isSelected }
        if (selectedPlayer != null) {
            renderEnemyPlayerUnitFrame(selectedPlayer)
        }

        // renderowanie panelu moba
        val selectedEnemies = game.enemies.values.find { it.isSelected }
        if (selectedEnemies != null) {
            renderEnemyMobUnitFrame(selectedEnemies)
        }

        // renderowanie panelu NPC
        val selectedNPC = game.npcs.values.find { it.isSelected }
        if (selectedNPC != null) {
            renderNPCUnitFrame(selectedNPC)
        }

        // Renderuj pasek XP
        renderXPBar()

        // Renderuj panel statystyk jeśli jest widoczny
        renderUIButtons()

        // Sprawdź hovery w panelach
        var anyPanelHasHover = false

        if (isCharacterWindowVisible) {
            val characterHover = renderCharacterWindow()
            if (characterHover) anyPanelHasHover = true
        }

        if (isInventoryVisible) {
            val inventoryHover = renderInventoryPanel()
            if (inventoryHover) anyPanelHasHover = true
        }

        if (!anyPanelHasHover) {
            itemTooltip.resetTooltip()
        }

        // Renderuj własny nick gracza
        renderPlayerNickname()

        // Renderuj własny health bar gracza
        //renderPlayerHealthBar()

        // Renderuj pasek
        renderCooldownBar()

        // Renderuj obrażenia gracza
        renderPlayerDamageTexts()

        // chat
        renderChatBackground()
        renderChatButtons()

        // uiBatch przed renderowaniem tesktu menu
        game.uiBatch.projectionMatrix = game.uiCamera.combined
        game.uiBatch.begin()

        // Renderuj komunikaty
        renderCooldownMessage()
        renderSelectedItemCursor()

        // Renderowanie czatu
        game.chatSystem.render(game.uiBatch, game.font)

        // Zakończ uiBatch
        game.uiBatch.end()

        // fps
        renderFPS()

        // tooltip
        itemTooltip.render()

        // oryginalne macierze projekcji świata
        game.batch.projectionMatrix = originalBatchProjection
        game.shapeRenderer.projectionMatrix = originalShapeProjection
    }

    private fun renderUIButtons() {
        val xpBarWidth = game.uiCamera.viewportWidth * 0.8f
        val xpBarX = (game.uiCamera.viewportWidth - xpBarWidth) / 2

        // Character button (C)
        val charButtonX = xpBarX + (xpBarWidth - 45f) / 2 - 25f
        val invButtonX = xpBarX + (xpBarWidth - 45f) / 2 + 5f
        val buttonY = 35f
        val buttonWidth = 20f
        val buttonHeight = 30f

        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Character button background
        if (isCharacterWindowVisible) {
            game.shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 0.8f)
        } else {
            game.shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.7f)
        }
        game.shapeRenderer.rect(charButtonX, buttonY, buttonWidth, buttonHeight)

        // Inventory button background
        if (isInventoryVisible) {
            game.shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 0.8f)
        } else {
            game.shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.7f)
        }
        game.shapeRenderer.rect(invButtonX, buttonY, buttonWidth, buttonHeight)

        game.shapeRenderer.end()

        // Borders
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        game.shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 1f)
        game.shapeRenderer.rect(charButtonX, buttonY, buttonWidth, buttonHeight)
        game.shapeRenderer.rect(invButtonX, buttonY, buttonWidth, buttonHeight)
        game.shapeRenderer.end()

        // Button text
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()

        game.font.color = Color.WHITE

        // "C" button
        game.layout.setText(game.font, "C")
        var textX = charButtonX + (buttonWidth - game.layout.width) / 2
        var textY = buttonY + (buttonHeight + game.layout.height) / 2
        game.font.draw(game.batch, "C", textX, textY)

        // "I" button
        game.layout.setText(game.font, "I")
        textX = invButtonX + (buttonWidth - game.layout.width) / 2
        textY = buttonY + (buttonHeight + game.layout.height) / 2
        game.font.draw(game.batch, "I", textX, textY)

        game.batch.end()
        resetFontColor()
    }

    // Renderuj okno postaci (po lewej od środka)
    private fun renderCharacterWindow(): Boolean {
        val windowWidth = 400f
        val windowHeight = 400f
        val spacing = 70f

        val windowX = (game.uiCamera.viewportWidth / 2) - windowWidth - spacing
        val windowY = (game.uiCamera.viewportHeight - windowHeight) / 2

        return characterWindow.render(windowX, windowY, windowWidth, windowHeight)
    }

    // Renderuj panel ekwipunku (po prawej od środka)
    private fun renderInventoryPanel(): Boolean {
        val panelWidth = 350f
        val panelHeight = 330f
        val spacing = 70f

        val panelX = (game.uiCamera.viewportWidth / 2) + spacing
        val panelY = (game.uiCamera.viewportHeight - panelHeight) / 2

        return inventoryPanel.render(panelX, panelY, panelWidth, panelHeight)
    }

    // Obsługa kliknięć w okno postaci
    fun handleCharacterWindowClick(touchX: Float, touchY: Float, isRightClick: Boolean = false) {
        if (!isCharacterWindowVisible) return

        val windowWidth = 400f
        val windowHeight = 400f
        val spacing = 70f

        val windowX = (game.uiCamera.viewportWidth / 2) - windowWidth - spacing
        val windowY = (game.uiCamera.viewportHeight - windowHeight) / 2

        characterWindow.handleClick(touchX, touchY, windowX, windowY, windowWidth, windowHeight, isRightClick)
    }

    // Obsługa kliknięć w panel ekwipunku
    fun handleInventoryClick(touchX: Float, touchY: Float, isRightClick: Boolean = false) {
        if (!isInventoryVisible) return

        val panelHeight = 330f
        val spacing = 70f

        val panelX = (game.uiCamera.viewportWidth / 2) + spacing
        val panelY = (game.uiCamera.viewportHeight - panelHeight) / 2

        inventoryPanel.handleClick(touchX, touchY, panelX, panelY, isRightClick)
    }

    private fun renderChatBackground() {
        // blending on
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.projectionMatrix = game.uiCamera.combined
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        val chatX = 10f
        val chatY = 60f
        val chatWidth = 600f
        val chatHeight = 245f

        // Tło główne czatu
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.5f)
        game.shapeRenderer.rect(chatX, chatY, chatWidth, chatHeight)

        // Przyciski nad chatem - zawsze widoczne
        val buttonY = chatY + 250f // background gora-dol
        val buttonWidth = 60f
        val buttonHeight = 25f
        val buttonSpacing = 5f // odstep pomiedzy buttonami
        val chatButtonX = 20f // background prawo-lewo
        val logButtonX = chatButtonX + buttonWidth + buttonSpacing

        // Tło przycisku Chat
        if (game.chatSystem.getCurrentMode().name == "CHAT") {
            game.shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.8f) // Aktywny
        } else {
            game.shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.6f) // Nieaktywny
        }
        game.shapeRenderer.rect(chatButtonX, buttonY, buttonWidth, buttonHeight)

        // Tło przycisku Log
        if (game.chatSystem.getCurrentMode().name == "LOG") {
            game.shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.8f) // Aktywny
        } else {
            game.shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.6f) // Nieaktywny
        }
        game.shapeRenderer.rect(logButtonX, buttonY, buttonWidth, buttonHeight)

        game.shapeRenderer.end()

        // Obramowanie przycisków
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        // Obramowanie przycisku Chat
        if (game.chatSystem.getCurrentMode().name == "CHAT") {
            game.shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 1f)
        } else {
            game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f)
        }
        game.shapeRenderer.rect(chatButtonX, buttonY, buttonWidth, buttonHeight)

        // Obramowanie przycisku Log
        if (game.chatSystem.getCurrentMode().name == "LOG") {
            game.shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 1f)
        } else {
            game.shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0.8f)
        }
        game.shapeRenderer.rect(logButtonX, buttonY, buttonWidth, buttonHeight)

        game.shapeRenderer.end()

        // blending off
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun renderChatButtons() {
        val mouseX = Gdx.input.x.toFloat()
        val mouseY = (game.uiCamera.viewportHeight - Gdx.input.y)

        val buttonWidth = 60f
        val buttonHeight = 25f
        val buttonY = 310f

        val chatButtonX = 20f
        val logButtonX = 85f

        fun isMouseOver(x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float): Boolean {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        }

        // Sprawdź hover (opcjonalnie możesz to wykorzystać do zmiany koloru)
        val chatHover = isMouseOver(chatButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)
        val logHover = isMouseOver(logButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)

        // Ustawienie projekcji UI
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()

        val oldFontColor = game.font.color.cpy()

        // Renderuj tekst buttona Chat
        val chatText = "[Chat]"
        game.layout.setText(game.font, chatText)
        val chatTextX = chatButtonX + (buttonWidth - game.layout.width) / 2f
        val chatTextY = buttonY + (buttonHeight + game.layout.height) / 2f

        // Zmień kolor jeśli aktywny lub hover
        game.font.color = if (game.chatSystem.getCurrentMode().name == "CHAT") {
            Color.WHITE
        } else if (chatHover) {
            Color.LIGHT_GRAY
        } else {
            Color.GRAY
        }
        game.font.draw(game.batch, chatText, chatTextX, chatTextY)

        // Renderuj tekst buttona Log
        val logText = "[Log]"
        game.layout.setText(game.font, logText)
        val logTextX = logButtonX + (buttonWidth - game.layout.width) / 2f
        val logTextY = buttonY + (buttonHeight + game.layout.height) / 2f

        game.font.color = if (game.chatSystem.getCurrentMode().name == "LOG") {
            Color.WHITE
        } else if (logHover) {
            Color.LIGHT_GRAY
        } else {
            Color.GRAY
        }
        game.font.draw(game.batch, logText, logTextX, logTextY)

        // Przywróć kolor czcionki
        game.font.color = oldFontColor
        game.batch.end()
        game.batch.projectionMatrix = oldBatchProj
    }

    private fun renderXPBar() {
        val xpBarWidth = game.uiCamera.viewportWidth * 0.8f
        val xpBarHeight = 10f
        val segments = 15
        val segmentGap = 2f
        val segmentWidth = (xpBarWidth - (segments - 1) * segmentGap) / segments

        val xpBarX = (game.uiCamera.viewportWidth - xpBarWidth) / 2
        val xpBarY = 10f

        val currentXP = game.localPlayer.experience
        val currentLevel = game.localPlayer.level
        val xpToNextLevel = 100 * currentLevel
        val xpPercent = (currentXP.toFloat() / xpToNextLevel.toFloat()).coerceIn(0f, 1f)

        val oldProjection = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Render tła segmentów
        for (i in 0 until segments) {
            val segmentX = xpBarX + i * (segmentWidth + segmentGap)
            game.shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 0.7f)
            game.shapeRenderer.rect(segmentX, xpBarY, segmentWidth, xpBarHeight)
        }

        // Render wypełnienia segmentów
        val filledSegmentsF = xpPercent * segments
        val filledSegments = filledSegmentsF.toInt()
        val partialSegment = filledSegmentsF - filledSegments

        for (i in 0 until filledSegments) {
            val segmentX = xpBarX + i * (segmentWidth + segmentGap)
            game.shapeRenderer.color = Color(0.6f, 0.2f, 0.8f, 0.8f)
            game.shapeRenderer.rect(segmentX, xpBarY, segmentWidth, xpBarHeight)
        }

        // Częściowo wypełniony segment
        if (partialSegment > 0 && filledSegments < segments) {
            val segmentX = xpBarX + filledSegments * (segmentWidth + segmentGap)
            game.shapeRenderer.color = Color(0.6f, 0.2f, 0.8f, 0.8f)
            game.shapeRenderer.rect(segmentX, xpBarY, segmentWidth * partialSegment, xpBarHeight)
        }

        game.shapeRenderer.end()
        game.shapeRenderer.projectionMatrix = oldProjection

        // Render tekstu XP
        val oldBatchProjection = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()

        val originalFontColor = game.font.color.cpy()
        val xpText = "XP: $currentXP / $xpToNextLevel"

        game.layout.setText(game.font, xpText)
        val textX = xpBarX + (xpBarWidth - game.layout.width) / 2
        val textY = xpBarY + (xpBarHeight + game.layout.height) / 2

        game.font.color = Color.WHITE
        game.font.draw(game.batch, xpText, textX, textY)
        game.font.color = originalFontColor

        game.batch.end()
        game.batch.projectionMatrix = oldBatchProjection

        resetFontColor()
    }

    // Renderuje nick własnego gracza
    private fun renderPlayerNickname() {
        val usernameText = game.characterNickname
        val levelText = " (${game.localPlayer.level})"

        // Oblicz szerokości tekstu
        game.layout.setText(game.font, usernameText)
        val usernameWidth = game.layout.width

        game.layout.setText(game.font, levelText)
        val levelWidth = game.layout.width

        val totalWidth = usernameWidth + levelWidth
        val textHeight = game.layout.height

        val padding = 4f
        val backgroundWidth = totalWidth + padding * 2
        val backgroundHeight = textHeight + padding * 2

        // Pozycja - nad graczem (środek ekranu + offset w górę)
        val backgroundX = (game.uiCamera.viewportWidth - backgroundWidth) / 2
        val backgroundY = (game.uiCamera.viewportHeight / 2) + 60f

        // Ustawienie projekcji UI
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined

        game.batch.begin()

        // Rysuj nick i poziom
        val textX = backgroundX + padding
        val textY = backgroundY + backgroundHeight - padding

        val oldFontColor = game.font.color.cpy()

        game.font.color = Color.WHITE
        game.font.draw(game.batch, usernameText, textX, textY)

        game.font.color = Color.YELLOW
        game.font.draw(game.batch, levelText, textX + usernameWidth, textY)

        // Przywróć kolory i projekcje
        game.font.color = oldFontColor
        game.batch.end()
        game.batch.projectionMatrix = oldBatchProj
    }

    // Renderuje health bar własnego gracza nad graczem (środek ekranu) z podziałem na segmenty
    private fun renderPlayerHealthBar() {
        val barWidth = 70f
        val barHeight = 10f
        val segments = 7 // Liczba segmentów
        val segmentWidth = (barWidth - (segments - 1) * 1f) / segments // Szerokość segmentu minus przerwy
        val segmentGap = 1f // Przerwa między segmentami

        // Pozycja - nad graczem, pod nickiem
        val barX = (game.uiCamera.viewportWidth - barWidth) / 2
        val barY = (game.uiCamera.viewportHeight / 2) + 45f // 25px nad środkiem (pod nickiem)

        val current = game.localPlayer.currentHealth.toFloat()
        val max = game.localPlayer.maxHealth.toFloat()
        val ratio = (current / max).coerceIn(0f, 1f)

        // Ile segmentów powinno być wypełnionych
        val filledSegments = (ratio * segments).toInt()
        val partialSegment = (ratio * segments) - filledSegments // Część ostatniego segmentu

        // Ustawienie projekcji UI dla ShapeRenderer
        val oldShapeProj = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        // Włączamy blending
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Renderuj każdy segment
        for (i in 0 until segments) {
            val segmentX = barX + i * (segmentWidth + segmentGap)

            // Tło segmentu - ciemnoszare
            game.shapeRenderer.color = Color.DARK_GRAY
            game.shapeRenderer.rect(segmentX, barY, segmentWidth, barHeight)

            // Wypełnienie segmentu
            when {
                i < filledSegments -> {
                    // Pełny segment - kolorowe w zależności od HP
                    game.shapeRenderer.color = when {
                        ratio > 0.5f -> Color.GREEN
                        ratio > 0.25f -> Color.ORANGE
                        else -> Color.RED
                    }
                    game.shapeRenderer.rect(segmentX, barY, segmentWidth, barHeight)
                }
                i == filledSegments && partialSegment > 0 -> {
                    // Częściowo wypełniony segment
                    game.shapeRenderer.color = when {
                        ratio > 0.5f -> Color.GREEN
                        ratio > 0.25f -> Color.ORANGE
                        else -> Color.RED
                    }
                    game.shapeRenderer.rect(segmentX, barY, segmentWidth * partialSegment, barHeight)
                }
                // Pozostałe segmenty pozostają tylko z tłem (ciemnoszare)
            }
        }

        game.shapeRenderer.end()

        // Przywróć projekcję
        game.shapeRenderer.projectionMatrix = oldShapeProj
    }

    // Renderuje pasek cooldownu
    private fun renderCooldownBar() {
        // Sprawdź czy gracz ma klasę postaci
        val characterClass = game.playerController.getCharacterClass()

        val barWidth = 200f
        val barHeight = 20f

        // Pozycja
        val barX = (game.uiCamera.viewportWidth - barWidth) / 2
        val barY = 250f

        val progress = characterClass.getCooldownProgress()
        val isOnCooldown = characterClass.isOnCooldown()

        // Ustawienie projekcji UI dla ShapeRenderer
        val oldShapeProj = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        // Włączamy blending
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tło paska
        game.shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 0.7f)
        game.shapeRenderer.rect(barX, barY, barWidth, barHeight)

        // Wypełnienie paska
        val color = if (isOnCooldown) {
            val attackColor = characterClass.getCurrentAttackColor()
            Color(attackColor.r, attackColor.g, attackColor.b, 0.9f)
        } else {
            Color(0f, 0.8f, 0f, 0.9f) // Zielony gdy gotowy
        }
        game.shapeRenderer.color = color
        game.shapeRenderer.rect(barX, barY, barWidth * progress, barHeight)

        game.shapeRenderer.end()

        // Renderowanie tekstu
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined

        game.batch.begin()

        val text = if (isOnCooldown) {
            "${characterClass.getCurrentAttackName()}: ${"%.1f".format(characterClass.getRemainingCooldownTime())}s"
        } else {
            "Gotowy do ataku!"
        }

        val oldFontColor = game.font.color.cpy()
        game.font.color = Color.WHITE

        game.layout.setText(game.font, text)
        val textWidth = game.layout.width
        val textHeight = game.layout.height

        val textX = barX + (barWidth - textWidth) / 2
        val textY = barY + (barHeight + textHeight) / 2

        game.font.draw(game.batch, text, textX, textY)

        // Przywróć kolory i projekcje
        game.font.color = oldFontColor
        game.batch.end()
        game.batch.projectionMatrix = oldBatchProj
        game.shapeRenderer.projectionMatrix = oldShapeProj
    }

    // Renderuje obrażenia gracza
    private fun renderPlayerDamageTexts() {
        if (playerDamageTexts.isEmpty()) return

        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined

        game.batch.begin()

        val originalColor = game.font.color.cpy()
        val originalScaleX = game.font.data.scaleX
        val originalScaleY = game.font.data.scaleY

        // ZMIEŃ ROZMIAR CZCIONKI
        game.font.data.setScale(1.5f) // 1.5x większy tekst (możesz zmienić na 2.0f, 0.8f itp.)

        playerDamageTexts.forEach { damageText ->
            // Ustaw kolor z przezroczystością
            game.font.color = Color(
                damageText.color.r,
                damageText.color.g,
                damageText.color.b,
                damageText.alpha.coerceIn(0f, 1f)
            )

            // Wyśrodkuj tekst poziomo
            game.layout.setText(game.font, damageText.text)
            val textWidth = game.layout.width
            val textX = (game.uiCamera.viewportWidth - textWidth) / 2

            game.font.draw(game.batch, damageText.text, textX, damageText.y)
        }

        // Przywróć oryginalny rozmiar i kolor czcionki
        game.font.data.setScale(originalScaleX, originalScaleY)
        game.font.color = originalColor
        game.batch.end()
        game.batch.projectionMatrix = oldBatchProj
    }

    //player
    private fun renderPlayerUnitFrame() {
        val marginLeft = 10f // margines od lewej ekranu
        val marginTop = 10f  // margines od góry ekranu

        val backgroundWidth = 250f  // szerokość tła całego panelu
        val backgroundHeight = 51f  // wysokosc tła

        val healthBarWidth = 248f   // szerokość health bara
        val healthBarHeight = 30f   // wysokość health bara
        val manaBarWidth = 248f     // szerokość mana bara
        val manaBarHeight = 18f     // NOWA wysokość mana bara (było 25f)

        val paddingLeft = 1f  // padding paska względem tła
        val paddingTop = 1f

        // Pozycja tła: 10px od lewej, 10px od góry
        val xBackground = marginLeft
        val yBackground = game.uiCamera.viewportHeight - marginTop - backgroundHeight

        // Pozycja pasków wewnątrz tła
        val xBar = xBackground + paddingLeft
        val yHealthBar = yBackground + backgroundHeight - paddingTop - healthBarHeight // Health bar na górze
        val yManaBar = yHealthBar - manaBarHeight - 1f // Mana bar pod health barem z 1px odstępem

        val currentHP = game.localPlayer.currentHealth.toFloat()
        val maxHP = game.localPlayer.maxHealth.toFloat()
        val hpRatio = (currentHP / maxHP).coerceIn(0f, 1f)

        val currentMP = game.localPlayer.currentMana.toFloat()
        val maxMP = game.localPlayer.maxMana.toFloat()
        val mpRatio = (currentMP / maxMP).coerceIn(0f, 1f)

        // Rysujemy tło całego panelu
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy paski
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tło health bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yHealthBar, healthBarWidth, healthBarHeight)

        // Wypełnienie health bara
        game.shapeRenderer.color = when {
            hpRatio > 0.5f -> customGreen
            hpRatio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(xBar, yHealthBar, healthBarWidth * hpRatio, healthBarHeight)

        // Tło mana bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yManaBar, manaBarWidth, manaBarHeight)

        // Wypełnienie mana bara
        game.shapeRenderer.color = customBlue
        game.shapeRenderer.rect(xBar, yManaBar, manaBarWidth * mpRatio, manaBarHeight)

        game.shapeRenderer.end()
        game.shapeRenderer.projectionMatrix = oldProjShape

        // Rysujemy teksty
        val leftText = "${game.username} (${Strings.LEVEL} ${game.localPlayer.level})"
        val hpText = "${game.localPlayer.currentHealth} / ${game.localPlayer.maxHealth}"
        val mpText = "${game.localPlayer.currentMana} / ${game.localPlayer.maxMana}"

        val oldFontColor = game.font.color.cpy()
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined
        game.font.color = Color.WHITE

        game.batch.begin()

        // Nick i poziom na górze panelu
        game.font.draw(game.batch, leftText, xBar + 5f, yBackground + backgroundHeight - 10f)

        // HP text na health barze
        val hpTextWidth = game.font.draw(game.batch, hpText, 0f, 0f).width
        game.font.draw(game.batch, hpText, xBar + healthBarWidth - hpTextWidth - 5f, yHealthBar + healthBarHeight - 10f)

        // MP text na mana barze
        val mpTextWidth = game.font.draw(game.batch, mpText, 0f, 0f).width
        game.font.draw(game.batch, mpText, xBar + manaBarWidth - mpTextWidth - 5f, yManaBar + manaBarHeight - 4f) // Zmniejsz offset tekstu

        game.batch.end()

        game.font.color = oldFontColor
        game.batch.projectionMatrix = oldBatchProj
    }

    //enemy player
    private fun renderEnemyPlayerUnitFrame(enemy: Player) {
        val xBackground = 270f // po prawej stronie własnego panelu
        val marginTop = 10f

        val backgroundWidth = 250f
        val backgroundHeight = 51f // wysokość

        val healthBarWidth = 248f   // szerokość health bara
        val healthBarHeight = 30f   // wysokość health bara
        val manaBarWidth = 248f     // szerokość mana bara
        val manaBarHeight = 18f     // wysokość mana bara

        val paddingLeft = 1f
        val paddingTop = 1f

        val yBackground = game.uiCamera.viewportHeight - marginTop - backgroundHeight

        val xBar = xBackground + paddingLeft
        val yHealthBar = yBackground + backgroundHeight - paddingTop - healthBarHeight // Health bar na górze
        val yManaBar = yHealthBar - manaBarHeight - 1f // Mana bar pod health barem z 1px odstępem

        val currentHP = enemy.currentHealth.toFloat()
        val maxHP = enemy.maxHealth.toFloat()
        val hpRatio = (currentHP / maxHP).coerceIn(0f, 1f)

        val currentMP = enemy.currentMana.toFloat()
        val maxMP = enemy.maxMana.toFloat()
        val mpRatio = (currentMP / maxMP).coerceIn(0f, 1f)

        // Rysowanie tła całego panelu
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy paski
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tło health bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yHealthBar, healthBarWidth, healthBarHeight)

        // Wypełnienie health bara
        game.shapeRenderer.color = when {
            hpRatio > 0.5f -> customGreen
            hpRatio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(xBar, yHealthBar, healthBarWidth * hpRatio, healthBarHeight)

        // Tło mana bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yManaBar, manaBarWidth, manaBarHeight)

        // Wypełnienie mana bara
        game.shapeRenderer.color = customBlue
        game.shapeRenderer.rect(xBar, yManaBar, manaBarWidth * mpRatio, manaBarHeight)

        game.shapeRenderer.end()
        game.shapeRenderer.projectionMatrix = oldProjShape

        // Rysowanie tekstów
        val leftText = "${enemy.username} (${Strings.LEVEL} ${enemy.level})"
        val hpText = "${enemy.currentHealth} / ${enemy.maxHealth}"
        val mpText = "${enemy.currentMana} / ${enemy.maxMana}"

        val oldFontColor = game.font.color.cpy()
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined
        game.font.color = Color.WHITE

        game.batch.begin()

        // Nick i poziom na górze panelu
        game.font.draw(game.batch, leftText, xBar + 5f, yBackground + backgroundHeight - 10f)

        // HP text na health barze
        val hpTextWidth = game.font.draw(game.batch, hpText, 0f, 0f).width
        game.font.draw(game.batch, hpText, xBar + healthBarWidth - hpTextWidth - 5f, yHealthBar + healthBarHeight - 10f)

        // MP text na mana barze
        val mpTextWidth = game.font.draw(game.batch, mpText, 0f, 0f).width
        game.font.draw(game.batch, mpText, xBar + manaBarWidth - mpTextWidth - 5f, yManaBar + manaBarHeight - 4f) // Zmniejsz offset tekstu

        game.batch.end()

        game.font.color = oldFontColor
        game.batch.projectionMatrix = oldBatchProj
    }

    //enemy mob
    private fun renderEnemyMobUnitFrame(enemy: EnemyClient) {
        val xBackground = 270f // po prawej stronie własnego panelu
        val marginTop = 10f

        val backgroundWidth = 250f
        val backgroundHeight = 32f

        val barWidth = 248f
        val barHeight = 30f

        val paddingLeft = 1f
        val paddingTop = 1f

        val yBackground = game.uiCamera.viewportHeight - marginTop - backgroundHeight

        val xBar = xBackground + paddingLeft
        val yBar = yBackground + backgroundHeight - paddingTop - barHeight

        val current = enemy.currentHealth.toFloat()
        val max = enemy.maxHealth.toFloat()
        val ratio = (current / max).coerceIn(0f, 1f)

        // Rysowanie tła całego panelu (transparentne lub inne)
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy ciemnoszare tło paska życia
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yBar, barWidth, barHeight)
        game.shapeRenderer.end()

        // Rysujemy pasek życia
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = when {
            ratio > 0.5f -> customGreen
            ratio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(xBar, yBar, barWidth * ratio, barHeight)
        game.shapeRenderer.end()

        game.shapeRenderer.projectionMatrix = oldProjShape

        // Rysowanie tekstów
        val leftText = "${enemy.displayName} (${Strings.LEVEL} ${enemy.level})"
        val rightText = "${enemy.currentHealth} / ${enemy.maxHealth}"

        val oldFontColor = game.font.color.cpy()
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined
        game.font.color = Color.WHITE

        game.batch.begin()
        game.font.draw(game.batch, leftText, xBar + 5f, yBar + barHeight - 9f)
        val rightTextWidth = game.font.draw(game.batch, rightText, 0f, 0f).width
        game.font.draw(game.batch, rightText, xBar + barWidth - rightTextWidth - 5f, yBar + barHeight - 9f)
        game.batch.end()

        game.font.color = oldFontColor
        game.batch.projectionMatrix = oldBatchProj
    }

    // npc unit frame
    private fun renderNPCUnitFrame(npc: NPCClient) {
        val xBackground = 270f // po prawej stronie panelu wroga
        val marginTop = 10f

        val backgroundWidth = 250f
        val backgroundHeight = 32f

        val barWidth = 248f
        val barHeight = 30f

        val paddingLeft = 1f
        val paddingTop = 1f

        val yBackground = game.uiCamera.viewportHeight - marginTop - backgroundHeight

        val xBar = xBackground + paddingLeft
        val yBar = yBackground + backgroundHeight - paddingTop - barHeight

        val current = npc.currentHealth.toFloat()
        val max = npc.maxHealth.toFloat()
        val ratio = (current / max).coerceIn(0f, 1f)

        // Rysowanie tła całego panelu
        game.batch.projectionMatrix = game.uiCamera.combined
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy ciemnoszare tło paska życia
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiCamera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yBar, barWidth, barHeight)
        game.shapeRenderer.end()

        // Rysujemy pasek życia
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = when {
            ratio > 0.5f -> customGreen
            ratio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(xBar, yBar, barWidth * ratio, barHeight)
        game.shapeRenderer.end()

        game.shapeRenderer.projectionMatrix = oldProjShape

        // Rysowanie tekstów
        val leftText = "${npc.name} (${Strings.LEVEL} ${npc.level})"
        val rightText = "${npc.currentHealth} / ${npc.maxHealth}"

        val oldFontColor = game.font.color.cpy()
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined
        game.font.color = Color.WHITE

        game.batch.begin()
        game.font.draw(game.batch, leftText, xBar + 5f, yBar + barHeight - 9f)
        val rightTextWidth = game.font.draw(game.batch, rightText, 0f, 0f).width
        game.font.draw(game.batch, rightText, xBar + barWidth - rightTextWidth - 5f, yBar + barHeight - 9f)
        game.batch.end()

        game.font.color = oldFontColor
        game.batch.projectionMatrix = oldBatchProj
    }

    // Renderuj komunikaty na środku ekranu (wszystkie aktywne)
    private fun renderCooldownMessage() {
        val messageManager = game.messageManager
        val activeMessages = messageManager.getActiveMessages()

        if (activeMessages.isEmpty()) return

        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiCamera.combined

        game.batch.begin()

        val oldColor = game.font.color.cpy()
        val oldScaleX = game.font.data.scaleX
        val oldScaleY = game.font.data.scaleY

        // Ustaw większy rozmiar czcionki
        game.font.data.setScale(2.0f)

        activeMessages.forEach { message ->
            // Oblicz alpha na podstawie pozostałego czasu (fade out effect)
            val alpha = if (message.timer < 0.5f) message.timer / 0.5f else 1f

            // Ustaw kolor z przezroczystością z wiadomości
            game.font.color = Color(
                message.color.r,
                message.color.g,
                message.color.b,
                alpha
            )

            // Oblicz pozycję na środku ekranu + animowana pozycja Y
            game.layout.setText(game.font, message.text)
            val textWidth = game.layout.width
            val textHeight = game.layout.height

            val x = (game.uiCamera.viewportWidth - textWidth) / 2
            val baseY = (game.uiCamera.viewportHeight + textHeight) / 2 + 250f
            val y = baseY + message.currentY // ZMIENIONE: Używaj currentY zamiast yOffset

            // Renderuj tekst
            game.font.draw(game.batch, message.text, x, y)
        }

        // Przywróć oryginalny rozmiar i kolor
        game.font.data.setScale(oldScaleX, oldScaleY)
        game.font.color = oldColor

        game.batch.end()
        game.batch.projectionMatrix = oldBatchProj
    }

    private fun renderSelectedItemCursor() {
        val selectedItem = itemTransferSystem.getSelectedItem()
        if (selectedItem != null) {
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = game.uiCamera.viewportHeight - Gdx.input.y.toFloat() // Odwróć Y

            game.batch.projectionMatrix = game.uiCamera.combined
            game.batch.begin()

            val texture = game.itemManager.getItemTexture(selectedItem.itemId)
                ?: try { Texture(Gdx.files.internal("items/icons/error.png")) } catch (e: Exception) { null }

            val originalColor = game.batch.color.cpy()

            if (texture != null) {
                // Renderuj teksturę itemu (lub itemerror.png)
                val iconSize = 40f // Rozmiar ikony
                val iconX = mouseX - iconSize / 2
                val iconY = mouseY - iconSize / 2

                // Semi-transparent
                game.batch.setColor(1f, 1f, 1f, 0.8f)
                game.batch.draw(texture, iconX, iconY, iconSize, iconSize)
            }

            // Przywróć kolor batcha
            game.batch.color = originalColor

            // Narysuj nazwę itemu pod ikoną (zawsze)
            val originalFontColor = game.font.color.cpy()
            game.font.color = Color(1f, 1f, 0f, 0.9f)
            game.font.data.setScale(0.8f)

            game.layout.setText(game.font, selectedItem.itemName)
            val nameWidth = game.layout.width
            game.font.draw(game.batch, selectedItem.itemName, mouseX - nameWidth/2, mouseY - 25f)

            game.font.data.setScale(1f)
            game.font.color = originalFontColor

            game.batch.end()
        }
    }
}