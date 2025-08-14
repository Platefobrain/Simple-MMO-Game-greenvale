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
import pl.decodesoft.player.Player
import pl.decodesoft.ui.character.CharacterWindow
import pl.decodesoft.ui.inventory.InventoryPanel

// Klasa odpowiedzialna za renderowanie elementów interfejsu użytkownika
class GameUI(private val game: MMOGame) {

    //kolory
    private val customGreen = Color(30f / 255f, 134f / 255f, 30f / 255f, 1f)

    // characterpanel
    private val characterWindow = CharacterWindow(game)
    private val inventoryPanel = InventoryPanel(game)
    private var isCharacterWindowVisible = false
    private var isInventoryVisible = false
    //transfer system itemy
    private val itemTransferSystem = ItemTransferSystem(game)

    init {
        characterWindow.setTransferSystem(itemTransferSystem)
        inventoryPanel.setTransferSystem(itemTransferSystem)
    }

    // Klasa do przechowywania obrażeń gracza (UI)
    private class PlayerDamageText(
        var y: Float,
        val text: String,
        val color: Color,
        var alpha: Float = 1.0f,
        var lifetime: Float = 0f
    )

    // Metoda wywoływana z MMOGame po inicjalizacji pItemManager
    fun connectItemManager() {
        try {
            val manager = game.itemManager // Spróbuj pobrać
            inventoryPanel.setItemManager(manager)
            itemTransferSystem.setItemManager(manager)
            println("ItemManager podłączony do wszystkich komponentów UI")
        } catch (e: Exception) {
            println("BŁĄD: ItemManager nie jest zainicjowany: ${e.message}")
        }
    }

    // Debug - dodaj metodę do testowania
    fun handleDebugKeys() {
        try {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F1)) {
                game.itemManager.debugPrintInventory()
            }

            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F2)) {
                println("=== TEST: Przenoszenie slot 0 -> slot 10 ===")
                val success = game.itemManager.moveItemWithinInventory(0, 10)
                println("Wynik: $success")
            }

            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F3)) {
                println("=== TEST: Dodaję nowy item ===")
                val success = game.itemManager.addItemToInventory("sword_01", 1)
                println("Wynik: $success")
            }
        } catch (_: Exception) {
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
        val startY = game.camera.viewportHeight / 2 + 0f
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
        val xpBarWidth = game.camera.viewportWidth * 0.8f
        val xpBarX = (game.camera.viewportWidth - xpBarWidth) / 2

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
        val xpBarWidth = game.camera.viewportWidth * 0.8f
        val xpBarX = (game.camera.viewportWidth - xpBarWidth) / 2

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

    // Odświeża UI okna postaci
    fun refreshCharacterWindow() {
        try {
            if (isCharacterWindowVisible) {
                println("DEBUG: Odświeżanie okna postaci - UI")
            }
            println("DEBUG: refreshCharacterWindow() wywołane pomyślnie")
        } catch (e: Exception) {
            println("DEBUG: Błąd odświeżania okna postaci: ${e.message}")
        }
    }

    // Renderuje wszystkie elementy UI
    fun render() {
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

        // Renderuj pasek XP
        renderXPBar()

        // Renderuj panel statystyk jeśli jest widoczny
        renderUIButtons()

        if (isCharacterWindowVisible) {
            renderCharacterWindow()
        }

        if (isInventoryVisible) {
            renderInventoryPanel()
        }

        // Renderuj własny nick gracza
        renderPlayerNickname()

        // Renderuj własny health bar gracza
        renderPlayerHealthBar()

        // Renderuj własny mana bar gracza
        renderPlayerManaBar()

        // Renderuj pasek cooldownu
        renderCooldownBar()

        // Renderuj obrażenia gracza (nad cooldownem)
        renderPlayerDamageTexts()

        // Najpierw narysuj tło chatu i przycisków
        renderChatBackground()

        // Rozpocznij uiBatch przed renderowaniem tesktu menu
        game.uiBatch.begin()

        // Renderuj komunikaty cooldown na końcu (żeby były na wierzchu)
        renderCooldownMessage()

        // Renderuj komunikaty cooldown na końcu (żeby były na wierzchu)
        renderCooldownMessage()

        renderSelectedItemCursor()

        // Renderowanie czatu
        game.chatSystem.render(game.uiBatch, game.font)

        // Zakończ uiBatch
        game.uiBatch.end()

        // WAŻNE: Przywróć oryginalne macierze projekcji świata
        game.batch.projectionMatrix = originalBatchProjection
        game.shapeRenderer.projectionMatrix = originalShapeProjection
    }

    private fun renderUIButtons() {
        val xpBarWidth = game.camera.viewportWidth * 0.8f
        val xpBarX = (game.camera.viewportWidth - xpBarWidth) / 2

        // Character button (C)
        val charButtonX = xpBarX + (xpBarWidth - 45f) / 2 - 25f
        val invButtonX = xpBarX + (xpBarWidth - 45f) / 2 + 5f
        val buttonY = 35f
        val buttonWidth = 20f
        val buttonHeight = 30f

        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix

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
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix
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
    private fun renderCharacterWindow() {
        val windowWidth = 400f
        val windowHeight = 400f
        val spacing = 70f // Połowa odstępu (10px / 2)

        // Character Window będzie po lewej od środka ekranu
        val windowX = (game.camera.viewportWidth / 2) - windowWidth - spacing
        val windowY = (game.camera.viewportHeight - windowHeight) / 2 // Wyśrodkowane w pionie

        characterWindow.render(windowX, windowY, windowWidth, windowHeight)
    }

    // Renderuj panel ekwipunku (po prawej od środka)
    private fun renderInventoryPanel() {
        val panelWidth = 350f
        val panelHeight = 330f
        val spacing = 70f // Połowa odstępu (10px / 2)

        // Inventory Panel będzie po prawej od środka ekranu
        val panelX = (game.camera.viewportWidth / 2) + spacing
        val panelY = (game.camera.viewportHeight - panelHeight) / 2 // Wyśrodkowane w pionie

        inventoryPanel.render(panelX, panelY, panelWidth, panelHeight)
    }

    // Obsługa kliknięć w okno postaci
    fun handleCharacterWindowClick(touchX: Float, touchY: Float, isRightClick: Boolean = false) {
        if (!isCharacterWindowVisible) return

        val windowWidth = 400f
        val windowHeight = 400f
        val spacing = 70f

        val windowX = (game.camera.viewportWidth / 2) - windowWidth - spacing
        val windowY = (game.camera.viewportHeight - windowHeight) / 2

        characterWindow.handleClick(touchX, touchY, windowX, windowY, windowWidth, windowHeight, isRightClick)
    }

    // Obsługa kliknięć w panel ekwipunku
    fun handleInventoryClick(touchX: Float, touchY: Float, isRightClick: Boolean = false) {
        if (!isInventoryVisible) return

        val panelHeight = 330f
        val spacing = 70f

        val panelX = (game.camera.viewportWidth / 2) + spacing
        val panelY = (game.camera.viewportHeight - panelHeight) / 2

        inventoryPanel.handleClick(touchX, touchY, panelX, panelY, isRightClick)
    }

    private fun renderChatBackground() {
        // blending on
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix
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

    // Renderuje pasek XP
    private fun renderXPBar() {
        val xpBarWidth = game.camera.viewportWidth * 0.8f
        val xpBarHeight = 10f

        // Wyśrodkowanie na ekranie i 100px od dołu
        val xpBarX = (game.camera.viewportWidth - xpBarWidth) / 2  // wyśrodkowanie poziome
        val xpBarY = 10f

        val currentXP = game.localPlayer.experience
        val currentLevel = game.localPlayer.level
        val xpToNextLevel = 100 * currentLevel

        val xpPercent = currentXP.toFloat() / xpToNextLevel.toFloat()

        // Ustawienie projekcji na widok ekranu (UI), a nie na widok świata
        val oldProjection = game.shapeRenderer.projectionMatrix.cpy()

        // Ustawienie macierzy projekcji na identyczną z widokiem UI (zazwyczaj ortograficzna projekcja ekranu)
        val uiProjection = game.uiBatch.projectionMatrix.cpy()  // zakładamy, że uiBatch używa projekcji UI
        game.shapeRenderer.projectionMatrix = uiProjection

        // Włączamy blending dla przezroczystości
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tło paska (ciemnoszare z przezroczystością)
        game.shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 0.7f)
        game.shapeRenderer.rect(xpBarX, xpBarY, xpBarWidth, xpBarHeight)

        // Wypełnienie paska
        game.shapeRenderer.color = Color(0.6f, 0.2f, 0.8f, 0.4f)
        game.shapeRenderer.rect(xpBarX, xpBarY, xpBarWidth * xpPercent.coerceIn(0f, 1f), xpBarHeight)

        game.shapeRenderer.end()

        // Przywróć oryginalną macierz projekcji
        game.shapeRenderer.projectionMatrix = oldProjection

        // Teraz narysuj tekst również używając projekcji UI
        val oldBatchProjection = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = uiProjection
        game.batch.begin()

        // Zapisz oryginalny kolor czcionki
        val originalFontColor = game.font.color.cpy()

        // Przygotuj tekst XP
        val xpText = "XP: $currentXP / $xpToNextLevel"

        // Oblicz wymiary tekstu, aby móc go wycentrować
        game.layout.setText(game.font, xpText)
        val textWidth = game.layout.width
        val textHeight = game.layout.height

        // Ustaw kolor czcionki na biały dla lepszego kontrastu na ciemnoszarym tle
        game.font.color = Color.WHITE

        // Oblicz pozycję tekstu, aby był wyśrodkowany w pasku
        val textX = xpBarX + (xpBarWidth - textWidth) / 2
        val textY = xpBarY + (xpBarHeight + textHeight) / 2

        // Narysuj tekst wyśrodkowany w pasku XP
        game.font.draw(game.batch, xpText, textX, textY)

        // Przywróć oryginalny kolor czcionki
        game.font.color = originalFontColor

        game.batch.end()

        // Przywróć oryginalną macierz projekcji
        game.batch.projectionMatrix = oldBatchProjection

        // blending
        // Gdx.gl.glDisable(GL20.GL_BLEND);
        resetFontColor()
    }

    // Renderuje nick własnego gracza nad graczem (środek ekranu)
    private fun renderPlayerNickname() {
        val usernameText = game.username
        val levelText = " (${Strings.LEVEL} ${game.localPlayer.level})"

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
        val backgroundX = (game.camera.viewportWidth - backgroundWidth) / 2
        val backgroundY = (game.camera.viewportHeight / 2) + 40f // 40px nad środkiem ekranu

        // Ustawienie projekcji UI
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

        game.batch.begin()

        // Rysuj tło
        transparentBackgroundDrawable.draw(
            game.batch,
            backgroundX,
            backgroundY,
            backgroundWidth,
            backgroundHeight
        )

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

    // Renderuje health bar własnego gracza nad graczem (środek ekranu)
    private fun renderPlayerHealthBar() {
        val barWidth = 70f
        val barHeight = 10f

        // Pozycja - nad graczem, pod nickiem
        val barX = (game.camera.viewportWidth - barWidth) / 2
        val barY = (game.camera.viewportHeight / 2) + 25f // 25px nad środkiem (pod nickiem)

        val current = game.localPlayer.currentHealth.toFloat()
        val max = game.localPlayer.maxHealth.toFloat()
        val ratio = (current / max).coerceIn(0f, 1f)

        // Ustawienie projekcji UI dla ShapeRenderer
        val oldShapeProj = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

        // Włączamy blending
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tło paska - ciemnoszare
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(barX, barY, barWidth, barHeight)

        // Wypełnienie paska - kolorowe w zależności od HP
        game.shapeRenderer.color = when {
            ratio > 0.5f -> Color.GREEN
            ratio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(barX, barY, barWidth * ratio, barHeight)

        game.shapeRenderer.end()

        // Przywróć projekcję
        game.shapeRenderer.projectionMatrix = oldShapeProj
    }

    // Renderuje mana bar własnego gracza pod health barem
    private fun renderPlayerManaBar() {
        val barWidth = 70f
        val barHeight = 10f

        // Pozycja - pod health barem
        val barX = (game.camera.viewportWidth - barWidth) / 2
        val barY = (game.camera.viewportHeight / 2) + 13f // 13px nad środkiem (pod health barem)

        val current = game.localPlayer.currentMana.toFloat()
        val max = game.localPlayer.maxMana.toFloat()
        val ratio = (current / max).coerceIn(0f, 1f)

        // Ustawienie projekcji UI dla ShapeRenderer
        val oldShapeProj = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

        // Włączamy blending
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Tło paska - ciemnoszare
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(barX, barY, barWidth, barHeight)

        // Wypełnienie paska - niebieski/cyan dla many
        game.shapeRenderer.color = when {
            ratio > 0.5f -> Color.CYAN
            ratio > 0.25f -> Color.BLUE
            else -> Color.PURPLE
        }
        game.shapeRenderer.rect(barX, barY, barWidth * ratio, barHeight)

        game.shapeRenderer.end()

        // Przywróć projekcję
        game.shapeRenderer.projectionMatrix = oldShapeProj
    }

    // Renderuje pasek cooldownu nad XP barem
    private fun renderCooldownBar() {
        // Sprawdź czy gracz ma klasę postaci
        val characterClass = game.playerController.getCharacterClass()

        val barWidth = 200f
        val barHeight = 20f

        // Pozycja - nad XP barem
        val barX = (game.camera.viewportWidth - barWidth) / 2
        val barY = 250f // nad XP barem (XP bar jest na y=10, wysokość 10px, więc 65px da odstęp)

        val progress = characterClass.getCooldownProgress()
        val isOnCooldown = characterClass.isOnCooldown()

        // Ustawienie projekcji UI dla ShapeRenderer
        val oldShapeProj = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

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
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

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

    // Renderuje obrażenia gracza nad paskiem cooldownu
    private fun renderPlayerDamageTexts() {
        if (playerDamageTexts.isEmpty()) return

        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

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
            val textX = (game.camera.viewportWidth - textWidth) / 2

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
        val backgroundHeight = 105f  // ZWIĘKSZ wysokość (było 80f) żeby zmieścić oba paski

        val barWidth = 248f   // szerokość paska
        val barHeight = 25f   // wysokość paska (zmniejsz z 30f)

        val paddingLeft = 1f  // padding paska względem tła
        val paddingTop = 1f

        // Pozycja tła: 10px od lewej, 10px od góry
        val xBackground = marginLeft
        val yBackground = game.camera.viewportHeight - marginTop - backgroundHeight

        // Pozycja pasków wewnątrz tła
        val xBar = xBackground + paddingLeft
        val yHealthBar = yBackground + backgroundHeight - paddingTop - barHeight // Health bar na górze
        val yManaBar = yHealthBar - barHeight - 5f // Mana bar pod health barem z 5px odstępem

        val currentHP = game.localPlayer.currentHealth.toFloat()
        val maxHP = game.localPlayer.maxHealth.toFloat()
        val hpRatio = (currentHP / maxHP).coerceIn(0f, 1f)

        val currentMP = game.localPlayer.currentMana.toFloat()
        val maxMP = game.localPlayer.maxMana.toFloat()
        val mpRatio = (currentMP / maxMP).coerceIn(0f, 1f)

        // Rysujemy tło całego panelu
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy paski
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // === HEALTH BAR ===
        // Tło health bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yHealthBar, barWidth, barHeight)

        // Wypełnienie health bara
        game.shapeRenderer.color = when {
            hpRatio > 0.5f -> customGreen
            hpRatio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(xBar, yHealthBar, barWidth * hpRatio, barHeight)

        // === MANA BAR ===
        // Tło mana bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yManaBar, barWidth, barHeight)

        // Wypełnienie mana bara
        game.shapeRenderer.color = when {
            mpRatio > 0.5f -> Color.CYAN
            mpRatio > 0.25f -> Color.BLUE
            else -> Color.PURPLE
        }
        game.shapeRenderer.rect(xBar, yManaBar, barWidth * mpRatio, barHeight)

        game.shapeRenderer.end()
        game.shapeRenderer.projectionMatrix = oldProjShape

        // Rysujemy teksty
        val leftText = "${game.username} (${Strings.LEVEL} ${game.localPlayer.level})"
        val hpText = "${game.localPlayer.currentHealth} / ${game.localPlayer.maxHealth}"
        val mpText = "${game.localPlayer.currentMana} / ${game.localPlayer.maxMana}"

        val oldFontColor = game.font.color.cpy()
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()
        game.font.color = Color.WHITE

        game.batch.begin()

        // Nick i poziom na górze panelu
        game.font.draw(game.batch, leftText, xBar + 5f, yBackground + backgroundHeight - 5f)

        // HP text na health barze
        val hpTextWidth = game.font.draw(game.batch, hpText, 0f, 0f).width
        game.font.draw(game.batch, hpText, xBar + barWidth - hpTextWidth - 5f, yHealthBar + barHeight - 7f)

        // MP text na mana barze
        val mpTextWidth = game.font.draw(game.batch, mpText, 0f, 0f).width
        game.font.draw(game.batch, mpText, xBar + barWidth - mpTextWidth - 5f, yManaBar + barHeight - 7f)

        game.batch.end()

        game.font.color = oldFontColor
        game.batch.projectionMatrix = oldBatchProj
    }

    //enemy player
    private fun renderEnemyPlayerUnitFrame(enemy: Player) {
        val xBackground = 270f // po prawej stronie własnego panelu
        val marginTop = 10f

        val backgroundWidth = 250f
        val backgroundHeight = 105f // ZWIĘKSZ z 80f żeby zmieścić oba paski

        val barWidth = 248f
        val barHeight = 25f // ZMNIEJSZ z 30f żeby zmieścić oba paski

        val paddingLeft = 1f
        val paddingTop = 1f

        val yBackground = game.camera.viewportHeight - marginTop - backgroundHeight

        val xBar = xBackground + paddingLeft
        val yHealthBar = yBackground + backgroundHeight - paddingTop - barHeight // Health bar na górze
        val yManaBar = yHealthBar - barHeight - 5f // Mana bar pod health barem z 5px odstępem

        val currentHP = enemy.currentHealth.toFloat()
        val maxHP = enemy.maxHealth.toFloat()
        val hpRatio = (currentHP / maxHP).coerceIn(0f, 1f)

        val currentMP = enemy.currentMana.toFloat()
        val maxMP = enemy.maxMana.toFloat()
        val mpRatio = (currentMP / maxMP).coerceIn(0f, 1f)

        // Rysowanie tła całego panelu
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy paski
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // === HEALTH BAR ===
        // Tło health bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yHealthBar, barWidth, barHeight)

        // Wypełnienie health bara
        game.shapeRenderer.color = when {
            hpRatio > 0.5f -> customGreen
            hpRatio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        game.shapeRenderer.rect(xBar, yHealthBar, barWidth * hpRatio, barHeight)

        // === MANA BAR ===
        // Tło mana bara
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yManaBar, barWidth, barHeight)

        // Wypełnienie mana bara
        game.shapeRenderer.color = when {
            mpRatio > 0.5f -> Color.CYAN
            mpRatio > 0.25f -> Color.BLUE
            else -> Color.PURPLE
        }
        game.shapeRenderer.rect(xBar, yManaBar, barWidth * mpRatio, barHeight)

        game.shapeRenderer.end()
        game.shapeRenderer.projectionMatrix = oldProjShape

        // Rysowanie tekstów
        val leftText = "${enemy.username} (${Strings.LEVEL} ${enemy.level})"
        val hpText = "${enemy.currentHealth} / ${enemy.maxHealth}"
        val mpText = "${enemy.currentMana} / ${enemy.maxMana}"

        val oldFontColor = game.font.color.cpy()
        val oldBatchProj = game.batch.projectionMatrix.cpy()
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()
        game.font.color = Color.WHITE

        game.batch.begin()

        // Nick i poziom na górze panelu
        game.font.draw(game.batch, leftText, xBar + 5f, yBackground + backgroundHeight - 5f)

        // HP text na health barze
        val hpTextWidth = game.font.draw(game.batch, hpText, 0f, 0f).width
        game.font.draw(game.batch, hpText, xBar + barWidth - hpTextWidth - 5f, yHealthBar + barHeight - 7f)

        // MP text na mana barze
        val mpTextWidth = game.font.draw(game.batch, mpText, 0f, 0f).width
        game.font.draw(game.batch, mpText, xBar + barWidth - mpTextWidth - 5f, yManaBar + barHeight - 7f)

        game.batch.end()

        game.font.color = oldFontColor
        game.batch.projectionMatrix = oldBatchProj
    }

    //enemy mob
    private fun renderEnemyMobUnitFrame(enemy: EnemyClient) {
        val xBackground = 270f // po prawej stronie własnego panelu
        val marginTop = 10f

        val backgroundWidth = 250f
        val backgroundHeight = 80f

        val barWidth = 248f
        val barHeight = 30f

        val paddingLeft = 1f
        val paddingTop = 1f

        val yBackground = game.camera.viewportHeight - marginTop - backgroundHeight

        val xBar = xBackground + paddingLeft
        val yBar = yBackground + backgroundHeight - paddingTop - barHeight

        val current = enemy.currentHealth.toFloat()
        val max = enemy.maxHealth.toFloat()
        val ratio = (current / max).coerceIn(0f, 1f)

        // Rysowanie tła całego panelu (transparentne lub inne)
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()
        game.batch.begin()
        transparentBackgroundDrawable.draw(game.batch, xBackground, yBackground, backgroundWidth, backgroundHeight)
        game.batch.end()

        // Rysujemy ciemnoszare tło paska życia
        val oldProjShape = game.shapeRenderer.projectionMatrix.cpy()
        game.shapeRenderer.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.color = Color.DARK_GRAY
        game.shapeRenderer.rect(xBar, yBar, barWidth, barHeight)
        game.shapeRenderer.end()

        // Rysujemy pasek życia w kolorze zależnym od poziomu HP
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
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()
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
        game.batch.projectionMatrix = game.uiBatch.projectionMatrix.cpy()

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

            // Oblicz pozycję na środku ekranu + offset w górę
            game.layout.setText(game.font, message.text)
            val textWidth = game.layout.width
            val textHeight = game.layout.height

            val x = (game.camera.viewportWidth - textWidth) / 2
            val baseY = (game.camera.viewportHeight + textHeight) / 2 + 250f
            val y = baseY + message.yOffset // Dodaj offset w górę

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
            val mouseY = game.camera.viewportHeight - Gdx.input.y.toFloat() // Odwróć Y

            game.batch.projectionMatrix = game.uiBatch.projectionMatrix
            game.batch.begin()

            // Semi-transparent item under cursor
            val originalColor = game.font.color.cpy()
            game.font.color = Color(1f, 1f, 1f, 0.7f)

            val itemInitial = selectedItem.itemName.first().toString().uppercase()
            game.layout.setText(game.font, itemInitial)
            val textWidth = game.layout.width
            val textHeight = game.layout.height

            // Draw item letter under cursor
            game.font.draw(game.batch, itemInitial, mouseX - textWidth/2, mouseY + textHeight/2)

            // Draw item name below
            game.font.color = Color(1f, 1f, 0f, 0.8f)
            game.font.data.setScale(0.8f)
            game.layout.setText(game.font, selectedItem.itemName)
            val nameWidth = game.layout.width
            game.font.draw(game.batch, selectedItem.itemName, mouseX - nameWidth/2, mouseY - 15f)
            game.font.data.setScale(1f)

            game.font.color = originalColor
            game.batch.end()
        }
    }
}