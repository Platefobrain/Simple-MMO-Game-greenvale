package pl.decodesoft.settings

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import pl.decodesoft.MMOGame
import pl.decodesoft.ui.UISkin

class Menu(private val game: MMOGame) {
    private var visible = false
    private lateinit var stage: Stage
    private lateinit var mainTable: Table
    private lateinit var contentTable: Table

    init {
        setupStage()
    }

    private fun setupStage() {
        stage = Stage(ScreenViewport())

        // Główna tabela zamiast okna - daje pełną kontrolę nad tłem
        mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.center()
        mainTable.isVisible = false

        // Opcjonalne: dodaj semi-przezroczyste tło
        val background = createTransparentBackground()
        mainTable.background = background

        // Tabela na zawartość menu
        contentTable = Table()

        // Dodaj tytuł
        val titleLabel = Label("Game Menu", UISkin.skin)
        titleLabel.setFontScale(1.5f)
        titleLabel.color = Color.WHITE
        contentTable.add(titleLabel).pad(20f).row()

        // Tworzymy przyciski
        createMenuButtons()

        // Dodaj tabelę do głównej tabeli
        mainTable.add(contentTable).pad(40f)

        stage.addActor(mainTable)
    }

    private fun createTransparentBackground(): Drawable {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)

        // Semi-przezroczyste ciemne tło
        pixmap.setColor(0f, 0f, 0f, 0.6f)
        // Lub całkowicie przezroczyste: pixmap.setColor(0f, 0f, 0f, 0f)

        pixmap.fill()

        val texture = Texture(pixmap)
        pixmap.dispose()

        return TextureRegionDrawable(TextureRegion(texture))
    }

    private fun createMenuButtons() {
        val buttonWidth = 200f
        val buttonHeight = 30f
        val padding = 5f

        // Resume Button
        val resumeButton = TextButton("Resume Game", UISkin.skin)
        resumeButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                hide()
            }
        })

        // Settings Button
        val settingsButton = TextButton("Settings", UISkin.skin)
        settingsButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                openSettings()
            }
        })

        // Logout Button
        val logoutButton = TextButton("Logout", UISkin.skin)
        logoutButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.startLogoutCountdown()
                hide()
            }
        })

        // Exit Button
        val exitButton = TextButton("Exit Game", UISkin.skin)
        exitButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                showExitConfirmation()
            }
        })

        // Dodajemy wszystkie przyciski do tabeli
        contentTable.add(resumeButton).width(buttonWidth).height(buttonHeight).pad(padding).row()
        contentTable.add(settingsButton).width(buttonWidth).height(buttonHeight).pad(padding).row()
        contentTable.add(logoutButton).width(buttonWidth).height(buttonHeight).pad(padding).row()
        contentTable.add(exitButton).width(buttonWidth).height(buttonHeight).pad(padding).row()

        // Dodajemy separator i informacje
        contentTable.add(Label("", UISkin.skin)).height(20f).row()
        contentTable.add(Label("Game Version: 1.0", UISkin.skin)).pad(5f).row()
    }

    private fun openSettings() {
        println("Opening settings...")
    }

    private fun showExitConfirmation() {
        val dialog = object : Dialog("Exit Game", UISkin.skin) {
            override fun result(result: Any?) {
                if (result as Boolean) {
                    game.exitGame()
                }
            }
        }

        dialog.text("Are you sure you want to exit?")
        dialog.button("Yes", true)
        dialog.button("No", false)
        dialog.show(stage)
    }

    fun toggle() {
        visible = !visible
        mainTable.isVisible = visible

        if (visible) {
            Gdx.input.inputProcessor = stage
        } else {
            Gdx.input.inputProcessor = null
        }
    }

    fun hide() {
        visible = false
        mainTable.isVisible = false
        Gdx.input.inputProcessor = null
    }

    fun show() {
        visible = true
        mainTable.isVisible = true
        Gdx.input.inputProcessor = stage
    }

    fun isVisible(): Boolean = visible

    fun render(delta: Float) {
        if (!visible) return

        stage.act(delta)
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        // Table automatycznie się dostosuje
    }

    fun dispose() {
        stage.dispose()
    }
}