package pl.decodesoft.states

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame
import pl.decodesoft.screens.DeathScreen

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
        Gdx.gl.glClearColor(0.275f, 0.275f, 0.275f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Aktualizacja kamery
        game.updateCamera()
        game.camera.update()

        // Rysuje mapę
        game.batch.begin()
        game.gameMap.draw(game.batch, game.localPlayer.x, game.localPlayer.y)
        game.batch.end()

        // Rysowanie graczy
        game.batch.projectionMatrix = game.camera.combined
        game.shapeRenderer.projectionMatrix = game.camera.combined

        // Rysowanie graczy z użyciem PlayerSkinManager
        try {
            game.batch.begin()
            game.players.values.forEach { player ->
                game.playerSkinManager.renderPlayer(game.batch, player, player.x, player.y, 1f)
            }
            game.batch.end()

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

        } catch (e: Exception) {
            if (game.shapeRenderer.isDrawing) {
                game.shapeRenderer.end()
            }
            Gdx.app.error("RenderDeadState", "Error rendering players: ${e.message}")
        }

        // === RENDEROWANIE PRZECIWNIKÓW ===
        try {
            // Użyj EnemySkinManager do renderowania sprite'ów
            game.batch.begin()
            game.enemies.values.forEach { enemy ->
                game.enemySkinManager.render(game.batch, enemy)
            }
            game.batch.end()

            // Obramowania zaznaczonych wrogów
            val selectedEnemies = game.enemies.values.toList().filter { it.isSelected }
            if (selectedEnemies.isNotEmpty()) {
                game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                selectedEnemies.forEach { enemy ->
                    game.shapeRenderer.color = Color.YELLOW
                    game.shapeRenderer.circle(enemy.x, enemy.y, 18f)
                }
                game.shapeRenderer.end()
            }
        } catch (e: Exception) {
            if (game.shapeRenderer.isDrawing) {
                game.shapeRenderer.end()
            }
            Gdx.app.error("RenderDeadState", "Error rendering enemies: ${e.message}")
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