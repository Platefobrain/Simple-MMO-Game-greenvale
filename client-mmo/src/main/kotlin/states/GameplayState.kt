package pl.decodesoft.states

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.player.Faction
import pl.decodesoft.ui.GameUI

// Stan rozgrywki
class PlayingState(game: MMOGame) : BaseGameState(game) {

    private val gameUI: GameUI by lazy {
        game.gameUI ?: GameUI(game).also { game.gameUI = it }
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
            player.updatePosition(delta)
        }

        checkPlayerDeath()
    }

    override fun render(delta: Float) {
        // Czyszczenie ekranu
        Gdx.gl.glClearColor(0.275f, 0.275f, 0.275f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Aktualizacja kamery świata gry
        game.updateCamera()
        game.camera.update()

        // === RENDEROWANIE ŚWIATA GRY ===
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined

        // === WARSTWA 1: MAPA ===
        game.batch.begin()
        game.gameMap.draw(game.batch, game.localPlayer.x, game.localPlayer.y)
        game.batch.end()

        // === WARSTWA 2: OSTATNIA KRATKA PATHFINDINGU ===
        try {
            if (game.pathTiles.isNotEmpty()) {
                game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                game.shapeRenderer.color = Color.YELLOW
                val tileSize = 16f

                for (tile in game.pathTiles) {
                    val px = tile.first * tileSize + tileSize / 2
                    val py = tile.second * tileSize + tileSize / 2

                    // rysuj obramowanie każdej kratki w ścieżce
                    game.shapeRenderer.rect(px - tileSize / 2, py - tileSize / 2, tileSize, tileSize)
                }

                game.shapeRenderer.end()
            }
        } catch (e: Exception) {
            if (game.shapeRenderer.isDrawing) game.shapeRenderer.end()
            Gdx.app.error("Render", "Błąd rysowania celu ścieżki: ${e.message}")
        }

        // === WARSTWA 2.5: OBRAMOWANIA ZAZNACZONYCH OBIEKTÓW ===
        Gdx.gl.glLineWidth(2f) // grubosc lini

        // Obramowania zaznaczonych graczy
        val selectedPlayers = game.players.values.filter { it.isSelected }
        if (selectedPlayers.isNotEmpty()) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedPlayers.forEach { player ->
                val radius = 16f  // Promień okręgu

                game.shapeRenderer.color = Color.GREEN
                game.shapeRenderer.circle(
                    player.x,
                    player.y,
                    radius
                )
            }
            game.shapeRenderer.end()
        }

        // Obramowania zaznaczonych wrogów
        val selectedEnemies = game.enemies.values.toList().filter { it.isSelected }
        if (selectedEnemies.isNotEmpty()) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedEnemies.forEach { enemy ->
                val radius = 20f  // Promień okręgu

                game.shapeRenderer.color = Color.GREEN
                game.shapeRenderer.circle(
                    enemy.x,
                    enemy.y,
                    radius
                )
            }
            game.shapeRenderer.end()
        }

        // Obramowania zaznaczonych NPC
        val selectedNPCs = game.npcs.values.filter { it.isSelected }
        if (selectedNPCs.isNotEmpty()) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedNPCs.forEach { npc ->
                val radius = 16f  // Promień okręgu

                game.shapeRenderer.color = Color.GREEN
                game.shapeRenderer.circle(
                    npc.x,
                    npc.y,
                    radius
                )
            }
            game.shapeRenderer.end()
        }

        // Obramowania zaznaczonych itemów
        val selectedItems = game.droppedItems.values.filter { it.isSelected }
        if (selectedItems.isNotEmpty()) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            selectedItems.forEach { item ->
                val boxSize = 30f  // Rozmiar kwadratu
                val halfSize = boxSize / 2f

                game.shapeRenderer.color = Color.GREEN
                game.shapeRenderer.rect(
                    item.x - halfSize,
                    item.y - halfSize,
                    boxSize,
                    boxSize
                )
            }
            game.shapeRenderer.end()
        }

        // Przywróć normalną grubość linii
        Gdx.gl.glLineWidth(1f)

        // === WARSTWA 3: GRACZE ===
        try {
            // Rysuj wszystkie postacie
            game.batch.begin()
            game.players.values.forEach { player ->
                game.playerSkinManager.renderPlayer(game.batch, player, player.x, player.y, 1f, isMoving = player.movingState, isDead = player.isDead())
            }
            game.batch.end()

            // === WARSTWA 4: WROGOWIE ===
            game.batch.begin()
            game.enemies.values.forEach { enemy ->
                game.enemySkinManager.render(game.batch, enemy)
            }
            game.batch.end()

            // Paski życia nadal można renderować osobno
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.enemies.values.forEach { enemy ->
                if (enemy.isAlive) {
                    drawHealthBar(game.shapeRenderer, enemy.x, enemy.y + 40f,
                        enemy.currentHealth, enemy.maxHealth)
                }
            }
            game.shapeRenderer.end()

            // === WARSTWA 4.1: NPC ===
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.npcs.values.forEach { npc ->
                npc.render(game.shapeRenderer)
                drawHealthBar(game.shapeRenderer, npc.x, npc.y + 28f, npc.currentHealth, npc.maxHealth)
            }
            game.shapeRenderer.end()

            // === WARSTWA 4.5: OBIEKTY MAPY
            game.batch.begin()
            game.gameMap.drawObjects(game.batch, game.localPlayer.x, game.localPlayer.y)
            game.batch.end()

            // === WARSTWA 5: DROPNIĘTE ITEMY ===
            game.batch.begin()
            game.droppedItems.values.forEach { droppedItem ->
                droppedItem.render(game.batch)
                droppedItem.renderItemName(game.batch, game.font)
            }
            game.batch.end()

            // === WARSTWA 6: PASKI ŻYCIA GRACZY ===
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            game.players.values.forEach { player ->
                if (player.id == game.localPlayerId) return@forEach
                drawHealthBar(game.shapeRenderer, player.x, player.y + 55f, player.currentHealth, player.maxHealth)
            }
            game.shapeRenderer.end()

            // === WARSTWA 7: EFEKTY KLAS (strzały itp.) ===
            game.batch.begin()
            game.playerController.renderBatch(game.batch)
            game.batch.end()

        } catch (e: Exception) {
            if (game.shapeRenderer.isDrawing) {
                game.shapeRenderer.end()
            }
            Gdx.app.error("RenderGame", "Error rendering: ${e.message}")
        }

        // === WARSTWA 8: NAZWY ===
        renderPlayerNames()
        renderEnemyNames()
        renderNPCNames()

        // === WARSTWA 9: UI ===
        gameUI.render()

        // przywróć macierz projekcji świata
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined
    }

    override fun handleInput(): Boolean {
        return game.playerController.handleInput()
    }

    override fun resize(width: Int, height: Int) {
        // Aktualizuj kamerę świata gry
        game.camera.setToOrtho(false, width.toFloat(), height.toFloat())
        game.camera.update()

        // Aktualizuj kamerę UI
        game.uiCamera.viewportWidth = width.toFloat()
        game.uiCamera.viewportHeight = height.toFloat()
        game.uiCamera.position.set(game.uiCamera.viewportWidth / 2, game.uiCamera.viewportHeight / 2, 0f)
        game.uiCamera.update()
    }

    private fun checkPlayerDeath() {
        if (game.localPlayer.currentHealth <= 0) {
            game.changeState(DeadState(game))
        }
    }

    private fun resetFontColor() {
        game.font.color = Color.WHITE
    }

    private fun getPlayerNameColor(playerFaction: Faction, localFaction: Faction): Color {
        // Jeśli którakolwiek strona nie ma frakcji - biały
        if (playerFaction == Faction.NONE || localFaction == Faction.NONE) {
            return Color.WHITE
        }

        // Jeśli ta sama frakcja - zielony
        if (playerFaction == localFaction) {
            return Color.WHITE
        }

        // Różne frakcje - CZERWONY
        return Color.RED
    }

    // Renderuje nazwy graczy
    private fun renderPlayerNames() {
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        try {
            game.players.values.forEach { player ->
                // Pomiń własnego gracza
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
                val backgroundY = player.y + 70f

                val textX = backgroundX + padding
                val textY = backgroundY + backgroundHeight - padding

                // Kolor nicku w zależności od frakcji
                val nameColor = getPlayerNameColor(player.faction, game.localPlayer.faction)
                game.font.color = nameColor
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
                val verticalOffset = 55f
                val backgroundY = enemy.y + verticalOffset

                // Rysuj nazwę i poziom
                val textX = backgroundX + padding
                val textY = backgroundY + backgroundHeight - padding

                game.font.color = when (enemy.getState()) {
                    "CHASE" -> Color.RED
                    else -> Color.WHITE
                }
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

    private fun renderNPCNames() {
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()

        try {
            game.npcs.values.forEach { npc ->
                val nameText = npc.name
                val typeText = "<${npc.displayType}>"

                // Oblicz szerokości
                game.layout.setText(game.font, nameText)
                val nameWidth = game.layout.width
                val lineHeight = game.layout.height

                game.layout.setText(game.font, typeText)
                val typeWidth = game.layout.width
                val typeHeight = game.layout.height

                val padding = 4f
                val backgroundWidth = maxOf(nameWidth, typeWidth) + padding * 2
                val backgroundHeight = lineHeight + typeHeight + padding * 3

                val backgroundX = npc.x - backgroundWidth / 2
                val backgroundY = npc.y + 45f

                // Wycentrowanie w poziomie
                val centerX = backgroundX + backgroundWidth / 2
                var textY = backgroundY + backgroundHeight - padding

                // --- 1. LINIA: Nazwa z kolorem w zależności od frakcji ---
                val topX = centerX - nameWidth / 2

                // Określ kolor nicku w zależności od frakcji
                val nameColor = when {
                    npc.currentHealth <= 0 -> Color.LIGHT_GRAY
                    npc.faction == Faction.NONE -> Color.YELLOW
                    npc.faction == game.localPlayer.faction -> Color.GREEN
                    else -> Color.RED
                }

                game.font.color = nameColor
                game.font.draw(game.batch, nameText, topX, textY)

                // --- 2. LINIA: Typ NPC ---
                textY -= lineHeight + padding
                val typeX = centerX - typeWidth / 2

                game.font.color = Color.LIME
                game.font.draw(game.batch, typeText, typeX, textY)
            }
        } catch (e: Exception) {
            Gdx.app.error("PlayingState", "Error rendering NPC names: ${e.message}")
        }

        game.batch.end()
        resetFontColor()
    }

    private fun drawHealthBar(shapeRenderer: ShapeRenderer, x: Float, y: Float, currentHealth: Int, maxHealth: Int) {
        val barWidth = 70f
        val barHeight = 10f
        val segments = 7
        val segmentWidth = (barWidth - (segments - 1) * 1f) / segments
        val segmentGap = 1f

        val ratio = (currentHealth.toFloat() / maxHealth.toFloat()).coerceIn(0f, 1f)
        val filledSegments = (ratio * segments).toInt()
        val partialSegment = (ratio * segments) - filledSegments

        for (i in 0 until segments) {
            val segmentX = x - barWidth / 2 + i * (segmentWidth + segmentGap)

            // Tło segmentu
            shapeRenderer.color = Color.DARK_GRAY
            shapeRenderer.rect(segmentX, y, segmentWidth, barHeight)

            // Wypełnienie segmentu
            when {
                i < filledSegments -> {
                    shapeRenderer.color = when {
                        ratio > 0.5f -> Color.GREEN
                        ratio > 0.25f -> Color.ORANGE
                        else -> Color.RED
                    }
                    shapeRenderer.rect(segmentX, y, segmentWidth, barHeight)
                }
                i == filledSegments && partialSegment > 0 -> {
                    shapeRenderer.color = when {
                        ratio > 0.5f -> Color.GREEN
                        ratio > 0.25f -> Color.ORANGE
                        else -> Color.RED
                    }
                    shapeRenderer.rect(segmentX, y, segmentWidth * partialSegment, barHeight)
                }
            }
        }
    }
}