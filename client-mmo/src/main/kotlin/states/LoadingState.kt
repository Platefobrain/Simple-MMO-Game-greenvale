package pl.decodesoft.states

import com.badlogic.gdx.Gdx
import pl.decodesoft.MMOGame
import pl.decodesoft.map.GameMap
import pl.decodesoft.screens.LoadingScreen

class LoadingState(game: MMOGame) : BaseGameState(game) {
    private lateinit var loadingScreen: LoadingScreen
    private var progress = 0f
    private var loaded = false

    override fun enter() {
        loadingScreen = LoadingScreen(game)
        loadingScreen.show()
        loaded = false
        progress = 0f
    }

    override fun update(delta: Float) {
        if (!loaded) {
            progress += delta
            loadingScreen.setProgress(progress)

            if (progress >= 1.1f) {
                game.gameMap = GameMap(120, 120, 16)

                // Załaduj wszystkie chunki
                loadChunk("greenshire")
                loadChunk("forest")
                loadChunk("desert")
                loadChunk("mountains")
                loadChunk("swamp")

                loaded = true
                game.menu.hide()
                game.changeState(PlayingState(game))
            }
        }
    }

    private fun loadChunk(chunkName: String) {
        try {
            val csv = Gdx.files.internal("assets/maps/$chunkName.csv").readString("UTF-8")
            game.gameMap.loadFromCsv(csv, chunkName)
        } catch (e: Exception) {
            println("Nie można załadować chunka $chunkName: ${e.message}")
        }
    }

    override fun render(delta: Float) {
        loadingScreen.render(delta)
    }

    override fun exit() {
        loadingScreen.dispose()
    }
}