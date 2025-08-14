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
                val csv = Gdx.files.internal("assets/maps/map.csv").readString("UTF-8")
                game.gameMap = GameMap(120, 120, 16)
                game.gameMap.loadFromCsv(csv)

                loaded = true

                // ukryj menu
                game.menu.hide()

                game.changeState(PlayingState(game))
            }
        }
    }

    override fun render(delta: Float) {
        loadingScreen.render(delta)
    }

    override fun exit() {
        loadingScreen.dispose()
    }
}