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

package pl.decodesoft.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

object UISkin {
    private var _skin: Skin? = null
    private var initialized = false

    val skin: Skin
        get() {
            if (!initialized) {
                initialize()
            }
            return _skin!!
        }

    private fun initialize() {
        if (initialized) return

        try {
            // Próbujemy załadować skin z pliku
            _skin = Skin(Gdx.files.internal("assets/uiskin.json"))
            println("Załadowano UI skin z pliku")
        } catch (e: Exception) {
            println("Nie można załadować UI skin z pliku, tworzę programowo: ${e.message}")
            _skin = createProgrammaticSkin()
        }

        initialized = true
    }

    private fun createProgrammaticSkin(): Skin {
        val skin = Skin()

        // Tworzymy font
        val font = BitmapFont()
        font.color = Color.WHITE
        skin.add("default-font", font)

        // Tworzymy tekstury programowo
        val buttonTexture = createButtonTexture()
        val buttonPressedTexture = createButtonPressedTexture()
        val buttonHoverTexture = createButtonHoverTexture()
        val windowTexture = createWindowTexture()

        // Tworzymy drawable z tekstur
        val buttonDrawable = TextureRegionDrawable(TextureRegion(buttonTexture))
        val buttonPressedDrawable = TextureRegionDrawable(TextureRegion(buttonPressedTexture))
        val buttonHoverDrawable = TextureRegionDrawable(TextureRegion(buttonHoverTexture))
        val windowDrawable = TextureRegionDrawable(TextureRegion(windowTexture))

        // Style dla przycisków
        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.font = font
        textButtonStyle.fontColor = Color.WHITE
        textButtonStyle.downFontColor = Color.LIGHT_GRAY
        textButtonStyle.overFontColor = Color.CYAN
        textButtonStyle.up = buttonDrawable
        textButtonStyle.down = buttonPressedDrawable
        textButtonStyle.over = buttonHoverDrawable
        skin.add("default", textButtonStyle)

        // Style dla okna
        val windowStyle = Window.WindowStyle()
        windowStyle.titleFont = font
        windowStyle.titleFontColor = Color.WHITE
        windowStyle.background = windowDrawable
        skin.add("default", windowStyle)

        // Style dla labeli
        val labelStyle = Label.LabelStyle()
        labelStyle.font = font
        labelStyle.fontColor = Color.WHITE
        skin.add("default", labelStyle)

        // Style dla checkbox
        val checkBoxStyle = CheckBox.CheckBoxStyle()
        checkBoxStyle.font = font
        checkBoxStyle.fontColor = Color.WHITE
        checkBoxStyle.checkboxOn = createCheckboxDrawable(true)
        checkBoxStyle.checkboxOff = createCheckboxDrawable(false)
        skin.add("default", checkBoxStyle)

        // Style dla TextField
        val textFieldStyle = TextField.TextFieldStyle()
        textFieldStyle.font = font
        textFieldStyle.fontColor = Color.WHITE
        textFieldStyle.background = createTextFieldDrawable()
        textFieldStyle.cursor = createCursorDrawable()
        textFieldStyle.selection = createSelectionDrawable()
        skin.add("default", textFieldStyle)

        return skin
    }

    private fun createButtonTexture(): Texture {
        val pixmap = Pixmap(100, 40, Pixmap.Format.RGBA8888)

        // Gradient background
        for (y in 0 until 40) {
            val intensity = 0.3f + (y / 40f) * 0.2f
            pixmap.setColor(intensity, intensity, intensity + 0.1f, 1f)
            pixmap.drawLine(0, y, 99, y)
        }

        // Border
        pixmap.setColor(0.6f, 0.6f, 0.8f, 1f)
        pixmap.drawRectangle(0, 0, 100, 40)

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun createButtonPressedTexture(): Texture {
        val pixmap = Pixmap(100, 40, Pixmap.Format.RGBA8888)

        // Darker gradient for pressed state
        for (y in 0 until 40) {
            val intensity = 0.1f + (y / 40f) * 0.1f
            pixmap.setColor(intensity, intensity, intensity + 0.05f, 1f)
            pixmap.drawLine(0, y, 99, y)
        }

        // Border
        pixmap.setColor(0.4f, 0.4f, 0.6f, 1f)
        pixmap.drawRectangle(0, 0, 100, 40)

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun createButtonHoverTexture(): Texture {
        val pixmap = Pixmap(100, 40, Pixmap.Format.RGBA8888)

        // Lighter gradient for hover state
        for (y in 0 until 40) {
            val intensity = 0.4f + (y / 40f) * 0.2f
            pixmap.setColor(intensity, intensity + 0.1f, intensity + 0.2f, 1f)
            pixmap.drawLine(0, y, 99, y)
        }

        // Bright border
        pixmap.setColor(0.7f, 0.8f, 1f, 1f)
        pixmap.drawRectangle(0, 0, 100, 40)

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun createWindowTexture(): Texture {
        val pixmap = Pixmap(200, 150, Pixmap.Format.RGBA8888)

        // Semi-transparent dark background
        pixmap.setColor(0.1f, 0.1f, 0.2f, 0.9f)
        pixmap.fill()

        // Border
        pixmap.setColor(0.5f, 0.5f, 0.7f, 1f)
        pixmap.drawRectangle(0, 0, 200, 150)

        // Title bar
        pixmap.setColor(0.2f, 0.2f, 0.4f, 1f)
        pixmap.fillRectangle(1, 130, 198, 19)

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun createCheckboxDrawable(checked: Boolean): Drawable {
        val pixmap = Pixmap(20, 20, Pixmap.Format.RGBA8888)

        // Background
        pixmap.setColor(0.3f, 0.3f, 0.3f, 1f)
        pixmap.fill()

        // Border
        pixmap.setColor(0.6f, 0.6f, 0.6f, 1f)
        pixmap.drawRectangle(0, 0, 20, 20)

        if (checked) {
            // Checkmark
            pixmap.setColor(0f, 1f, 0f, 1f)
            pixmap.drawLine(4, 10, 8, 6)
            pixmap.drawLine(8, 6, 16, 14)
            pixmap.drawLine(5, 10, 9, 6)
            pixmap.drawLine(9, 6, 17, 14)
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    private fun createTextFieldDrawable(): Drawable {
        val pixmap = Pixmap(100, 30, Pixmap.Format.RGBA8888)

        // Background
        pixmap.setColor(0.1f, 0.1f, 0.1f, 1f)
        pixmap.fill()

        // Border
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1f)
        pixmap.drawRectangle(0, 0, 100, 30)

        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    private fun createCursorDrawable(): Drawable {
        val pixmap = Pixmap(2, 20, Pixmap.Format.RGBA8888)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.fill()

        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    private fun createSelectionDrawable(): Drawable {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0.3f, 0.6f, 1f, 0.5f)
        pixmap.fill()

        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    fun dispose() {
        _skin?.dispose()
        _skin = null
        initialized = false
    }
}