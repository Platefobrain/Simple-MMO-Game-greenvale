package pl.decodesoft.states

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.TimeUtils
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import pl.decodesoft.MMOGame
import pl.decodesoft.screens.DeathScreen
import pl.decodesoft.ui.GameUI

// Stan rozgrywki
class PlayingState(game: MMOGame) : BaseGameState(game) {
    // GameUI zostanie przekazane z MMOGame lub utworzone tutaj
    private val gameUI: GameUI by lazy {
        // Sprawdź czy game ma już gameUI, jeśli nie to utwórz nowe
        game.gameUI ?: GameUI(game).also { game.gameUI = it }
    }

    private var wasEscDown = false
    private var lastToggleTime = 0L
    private val toggleCooldown = 100L // ms

    // Stałe dla pasków życia
    companion object {
        private const val HEALTH_BAR_WIDTH = 70f
        private const val HEALTH_BAR_HEIGHT = 10f
    }

    // Tło dla nazw graczy - przeniesione z GameUI
    private val transparentBackgroundDrawable: Drawable by lazy {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0.3f)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        TextureRegionDrawable(TextureRegion(texture))
    }

    override fun enter() {
        // Inicjalizacja elementów gry wymaganych do rozgrywki
    }

    override fun exit() {
        // Czyszczenie zasobów
    }

    override fun update(delta: Float) {
        // Aktualizuj MessageManager dla komunikatów cooldown
        game.messageManager.update(delta)

        // Aktualizacja systemu czatu
        game.chatSystem.update(delta)

        // Aktualizacja umiejętności i efektów klas
        game.playerController.update(delta)

        // Aktualizacja przeciwników
        if (game.enemyUpdateTimer >= game.enemyUpdateInterval) {
            game.enemyUpdateTimer = 0f
        }
        game.enemyUpdateTimer += delta

        game.enemies.values.toList().forEach { enemy ->
            enemy.update(delta)
        }

        // Aktualizacja pozycji graczy
        game.players.values.forEach { player ->
            if (player.id == game.localPlayerId) {
                player.updateLocalPosition(delta)
            } else {
                player.updatePosition(delta)
            }
        }

        // Sprawdź śmierć gracza
        checkPlayerDeath()
    }

    override fun render(delta: Float) {
        // Czyszczenie ekranu
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Aktualizacja kamery
        game.updateCamera()
        game.camera.update()

        // WAŻNE: Ustaw macierz projekcji dla świata gry
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined

        // Rysuje mapę
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        game.gameMap.draw(game.batch)
        game.batch.end()

        // Rysowanie graczy
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined

        // Rysowanie graczy z kolorami odpowiadającymi klasie
        try {
            // Rysuj wszystkie wypełnione postacie
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.players.values.forEach { player ->
                game.shapeRenderer.color = player.getClassColor()
                game.shapeRenderer.circle(player.x, player.y, 15f)
            }
            game.shapeRenderer.end()

            // Rysowanie ścieżki pathfindingu
            try {
                game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                game.shapeRenderer.color = Color.YELLOW
                val tileSize = 16f

                game.pathTiles.forEach { (tileX, tileY) ->
                    val px = tileX * tileSize + tileSize / 2
                    val py = tileY * tileSize + tileSize / 2
                    game.shapeRenderer.rect(px - tileSize / 2, py - tileSize / 2, tileSize, tileSize)
                }
                game.shapeRenderer.end()
            } catch (e: Exception) {
                if (game.shapeRenderer.isDrawing) game.shapeRenderer.end()
                Gdx.app.error("Render", "Błąd rysowania ścieżki: ${e.message}")
            }

            // Rysuj obramowania zaznaczonych graczy
            val selectedPlayers = game.players.values.filter { it.isSelected }
            if (selectedPlayers.isNotEmpty()) {
                game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                selectedPlayers.forEach { player ->
                    game.shapeRenderer.color = Color.YELLOW
                    game.shapeRenderer.circle(player.x, player.y, 18f) // Nieco większy promień dla obramowania
                }
                game.shapeRenderer.end()
            }

            // Renderowanie pasków życia
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.players.values.forEach { player ->
                // Pomiń własnego gracza - jego health bar będzie w GameUI
                if (player.id == game.localPlayerId) return@forEach

                drawHealthBar(game.shapeRenderer, player.x, player.y + 28f, player.currentHealth, player.maxHealth)
            }
            game.shapeRenderer.end()

            // Renderowanie strzał i efektów klas
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.playerController.renderShapes(game.shapeRenderer)
            game.shapeRenderer.end()

        } catch (e: Exception) {
            if (game.shapeRenderer.isDrawing) {
                game.shapeRenderer.end()
            }
            Gdx.app.error("RenderGame", "Error rendering: ${e.message}")
        }

        // Renderowanie przeciwników
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.enemies.values.forEach { enemy ->
            // Renderuj kształt przeciwnika
            enemy.render(game.shapeRenderer)

            // Renderuj pasek życia jeśli przeciwnik żyje
            if (enemy.isAlive) {
                drawHealthBar(game.shapeRenderer, enemy.x, enemy.y + 28f, enemy.currentHealth, enemy.maxHealth)
            }
        }
        game.shapeRenderer.end()

        val selectedEnemies = game.enemies.values.toList().filter { it.isSelected }
        if (selectedEnemies.isNotEmpty()) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedEnemies.forEach { enemy ->
                game.shapeRenderer.color = Color.YELLOW
                game.shapeRenderer.circle(enemy.x, enemy.y, 18f) // Nieco większy promień dla obramowania
            }
            game.shapeRenderer.end()
        }

        // *** RENDEROWANIE NAZW GRACZY I PRZECIWNIKÓW - PRZENIESIONE Z GameUI ***
        renderPlayerNames()
        renderEnemyNames()

        // *** WAŻNE: Renderuj teksty DAMAGE PO nazwach i health barach (najwyższy z-index) ***
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        game.playerController.renderBatch(game.batch) // Tutaj są damage texty - teraz na górze wszystkiego
        game.batch.end()

        // Rysowanie UI - teraz używamy klasy GameUI (bez nazw graczy)
        gameUI.render()

        // WAŻNE: Po renderowaniu UI przywróć macierz projekcji świata
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined
    }

    override fun handleInput(): Boolean {
        val escCurrentlyDown = Gdx.input.isKeyPressed(Input.Keys.ESCAPE)

        // Wykrycie puszczenia ESC
        if (wasEscDown && !escCurrentlyDown) {
            if (TimeUtils.timeSinceMillis(lastToggleTime) > toggleCooldown) {
                game.menu.toggle()
                lastToggleTime = TimeUtils.millis()
            }
        }

        // Zapisujemy obecny stan klawisza na następną klatkę
        wasEscDown = escCurrentlyDown

        // Obsługa kliknięć w menu
        if (game.menu.isVisible()) {
            return true
        }

        // Obsługa czatu i ruchu gracza - teraz playerController obsłuży też UI
        val chatHandled = game.chatSystem.handleInput()
        if (!chatHandled) {
            game.playerController.handleInput()
        }

        // Obsługa P - pathfinding
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            val startX = (game.localPlayer.x / 16f).toInt()
            val startY = (game.localPlayer.y / 16f).toInt()
            val endX = 10
            val endY = 10

            game.networkScope.launch {
                game.session?.send(Frame.Text("PATHFIND|$startX|$startY|$endX|$endY"))
            }
        }

        return true
    }

    override fun resize(width: Int, height: Int) {
        game.camera.setToOrtho(false, width.toFloat(), height.toFloat())
        game.camera.update()
    }

    private fun checkPlayerDeath() {
        if (game.localPlayer.currentHealth <= 0) {
            game.changeState(DeadState(game))
        }
    }

    // *** METODY RENDEROWANIA NAZW PRZENIESIONE Z GameUI ***

    private fun resetFontColor() {
        // Ustawia domyślny kolor czcionki na biały (nieprzezroczysty)
        game.font.color = Color.WHITE
    }

    //Renderuje nazwy graczy (bez własnego gracza)
    private fun renderPlayerNames() {
        // Rysujemy tła za pomocą transparentBackgroundDrawable
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        try {
            game.players.values.forEach { player ->
                // Pomiń własnego gracza - jego nick będzie w GameUI
                if (player.id == game.localPlayerId) return@forEach

                val usernameText = player.username
                val levelText = " (${player.level})"

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

                val backgroundX = player.x - backgroundWidth / 2
                val backgroundY = player.y + 40f

                // Rysuj tło z użyciem transparentBackgroundDrawable
                transparentBackgroundDrawable.draw(
                    game.batch,
                    backgroundX,
                    backgroundY,
                    backgroundWidth,
                    backgroundHeight
                )

                // Rysuj nazwę i poziom
                val textX = backgroundX + padding
                val textY = backgroundY + backgroundHeight - padding

                game.font.color = Color.WHITE
                game.font.draw(game.batch, usernameText, textX, textY)

                game.font.color = Color.YELLOW
                game.font.draw(game.batch, levelText, textX + usernameWidth, textY)
            }
        } catch (e: Exception) {
            Gdx.app.error("PlayingState", "Error rendering player names: ${e.message}")
        }

        game.batch.end()
        resetFontColor()
    }

    private fun renderEnemyNames() {
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        try {
            game.enemies.values.forEach { enemy ->
                if (!enemy.isAlive) return@forEach

                val usernameText = enemy.displayName
                val levelText = " (${enemy.level})"

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

                val backgroundX = enemy.x - backgroundWidth / 2
                val verticalOffset = 40f  // Możesz ustawić jak u gracza, np. 50f lub inna wartość
                val backgroundY = enemy.y + verticalOffset

                // Rysuj tło
                transparentBackgroundDrawable.draw(
                    game.batch,
                    backgroundX,
                    backgroundY,
                    backgroundWidth,
                    backgroundHeight
                )

                // Rysuj nazwę i poziom
                val textX = backgroundX + padding
                val textY = backgroundY + backgroundHeight - padding

                game.font.color = Color.WHITE
                game.font.draw(game.batch, usernameText, textX, textY)

                game.font.color = Color.YELLOW
                game.font.draw(game.batch, levelText, textX + usernameWidth, textY)
            }
        } catch (e: Exception) {
            Gdx.app.error("PlayingState", "Error rendering enemy names: ${e.message}")
        }

        game.batch.end()
        resetFontColor()
    }

    // Uproszczona funkcja drawHealthBar bez parametrów width/height
    private fun drawHealthBar(shapeRenderer: ShapeRenderer, x: Float, y: Float, currentHealth: Int, maxHealth: Int) {
        val ratio = currentHealth.toFloat() / maxHealth

        // Tło paska - ciemnoszare
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.rect(x - HEALTH_BAR_WIDTH / 2, y, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT)

        // Wypełnienie paska - kolorowe w zależności od HP
        shapeRenderer.color = when {
            ratio > 0.5f -> Color.GREEN
            ratio > 0.25f -> Color.ORANGE
            else -> Color.RED
        }
        shapeRenderer.rect(x - HEALTH_BAR_WIDTH / 2, y, HEALTH_BAR_WIDTH * ratio, HEALTH_BAR_HEIGHT)
    }
}

// Stan śmierci
class DeadState(game: MMOGame) : BaseGameState(game) {
    private var deathScreen: DeathScreen? = null
    private var deathTimer = 0f
    private val minDeathTime = 0f

    override fun enter() {
        deathTimer = 0f
        deathScreen = DeathScreen(game)
        deathScreen?.show()
    }

    override fun exit() {
        deathScreen?.dispose()
        deathScreen = null
    }

    override fun update(delta: Float) {
        deathTimer += delta

        // obsluga klikniecia
        deathScreen

        // Aktualizacja systemu czatu
        game.chatSystem.update(delta)

        // Aktualizacja pozycji graczy
        game.players.values.forEach { player ->
            if (player.id != game.localPlayerId) {
                player.updatePosition(delta)
            }
        }

        // Aktualizacja przeciwników
        if (game.enemyUpdateTimer >= game.enemyUpdateInterval) {
            game.enemyUpdateTimer = 0f
        }
        game.enemyUpdateTimer += delta

        game.enemies.values.toList().filter { it.isAlive }.forEach { enemy ->
            enemy.update(delta)
        }
    }

    override fun render(delta: Float) {
        // Czyszczenie ekranu
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Aktualizacja kamery
        game.updateCamera()
        game.camera.update()

        // Rysuje mapę
        game.batch.begin()
        game.gameMap.draw(game.batch)
        game.batch.end()

        // Rysowanie graczy
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined

        // Rysowanie graczy z kolorami odpowiadającymi klasie
        try {
            // Rysuj wszystkie wypełnione postacie
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.players.values.forEach { player ->
                game.shapeRenderer.color = player.getClassColor()
                game.shapeRenderer.circle(player.x, player.y, 15f)
            }
            game.shapeRenderer.end()

            // Rysuj obramowania zaznaczonych graczy
            val selectedPlayers = game.players.values.filter { it.isSelected }
            if (selectedPlayers.isNotEmpty()) {
                game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                selectedPlayers.forEach { player ->
                    game.shapeRenderer.color = Color.YELLOW
                    game.shapeRenderer.circle(player.x, player.y, 18f)
                }
                game.shapeRenderer.end()
            }

            // Renderowanie pasków życia
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.players.values.forEach { player ->
                // Rysuj tło paska życia (szary)
                game.shapeRenderer.color = Color.DARK_GRAY
                game.shapeRenderer.rect(player.x - 30f, player.y + 35f, 60f, 8f)

                // Rysuj aktualny stan paska życia (zielony lub czerwony, zależnie od ilości HP)
                val healthRatio = player.currentHealth.toFloat() / player.maxHealth.toFloat()
                if (healthRatio > 0.5f) {
                    game.shapeRenderer.color = Color.GREEN
                } else if (healthRatio > 0.25f) {
                    game.shapeRenderer.color = Color.ORANGE
                } else {
                    game.shapeRenderer.color = Color.RED
                }
                game.shapeRenderer.rect(player.x - 30f, player.y + 35f, 60f * healthRatio, 8f)
            }
            game.shapeRenderer.end()
        } catch (e: Exception) {
            if (game.shapeRenderer.isDrawing) {
                game.shapeRenderer.end()
            }
            Gdx.app.error("RenderGame", "Error rendering: ${e.message}")
        }

        // Renderowanie przeciwników
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.enemies.values.forEach { it.render(game.shapeRenderer) }
        game.shapeRenderer.end()

        val selectedEnemies = game.enemies.values.toList().filter { it.isSelected }
        if (selectedEnemies.isNotEmpty()) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedEnemies.forEach { enemy ->
                game.shapeRenderer.color = Color.YELLOW
                game.shapeRenderer.circle(enemy.x, enemy.y, 18f)
            }
            game.shapeRenderer.end()
        }

        // Renderowanie ekranu śmierci na wierzchu
        Gdx.input.inputProcessor = deathScreen?.stagePublic
        deathScreen?.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        game.camera.viewportWidth = width.toFloat()
        game.camera.viewportHeight = height.toFloat()
        game.camera.update()
        deathScreen?.resize(width, height)
    }

    // Dodana metoda canRespawn() - sprawdza, czy można się odrodzić
    private fun canRespawn(): Boolean {
        return deathTimer >= minDeathTime
    }

    // Dodana metoda handleRespawn() - wykonuje proces odradzania
    fun handleRespawn() {
        if (canRespawn()) {
            Gdx.app.log("Respawn", "Sending respawn message")
            game.sendWebSocketMessage("RESPAWN|${game.localPlayer.id}")

        } else {
            Gdx.app.log("Respawn", "Respawn blocked, death timer not elapsed yet")
        }
    }
}