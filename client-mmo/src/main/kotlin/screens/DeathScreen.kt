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

package pl.decodesoft.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import pl.decodesoft.MMOGame
import pl.decodesoft.ui.UISkin

class DeathScreen(private val game: MMOGame) {
    val stagePublic get() = stage
    private var visible = false
    private var stage: Stage = Stage(ScreenViewport())
    private val mainTable: Table = Table()
    private val contentTable: Table = Table()

    init {
        setupStage()
    }

    private fun setupStage() {
        mainTable.setFillParent(true)
        mainTable.center()
        mainTable.isVisible = false

        mainTable.background = createTransparentBackground()

        // Label z tekstem "Zginąłeś!" na czerwono i powiększony
        val deathLabel = Label("Zginąłeś!", UISkin.skin)
        deathLabel.color = Color.RED
        deathLabel.setFontScale(2f)
        contentTable.add(deathLabel).pad(20f).row()

        // Przycisk Respawn z ChangeListener
        val buttonWidth = 300f
        val buttonHeight = 60f
        val padding = 5f

        val respawnButton = TextButton("Respawn", UISkin.skin)
        respawnButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.respawnPlayer()
                hide()
            }
        })
        contentTable.add(respawnButton).width(buttonWidth).height(buttonHeight).pad(padding).row()

        mainTable.add(contentTable).pad(40f)
        stage.addActor(mainTable)
    }

    private fun createTransparentBackground(): Drawable {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0.6f)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    fun show() {
        visible = true
        mainTable.isVisible = true
        Gdx.input.inputProcessor = stage
    }

    private fun hide() {
        visible = false
        mainTable.isVisible = false
        Gdx.input.inputProcessor = null
    }

    fun render(delta: Float) {
        if (!visible) return
        stage.act(delta)
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    fun dispose() {
        stage.dispose()
    }
}