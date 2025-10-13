package pl.decodesoft.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.MMOGame

class LoadingScreen(val game: MMOGame) : Screen {

    private var progress = 0f
    private val shapeRenderer = ShapeRenderer()
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

    override fun show() {
        batch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.WHITE
        font.data.setScale(1f)
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.275f, 0.275f, 0.275f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()

        val barWidth = screenWidth * 0.6f
        val barHeight = 20f
        val barX = (screenWidth - barWidth) / 2f
        val barY = screenHeight / 2f - barHeight / 2f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.rect(barX, barY, barWidth, barHeight)

        shapeRenderer.color = Color.GREEN
        shapeRenderer.rect(barX, barY, barWidth * progress, barHeight)
        shapeRenderer.end()

        val percent = (progress * 100).toInt().coerceIn(0, 100)
        val text = "Loading... $percent%"
        val layout = GlyphLayout(font, text)

        batch.begin()
        font.draw(
            batch,
            text,
            (screenWidth - layout.width) / 2f,
            barY + barHeight + 40f
        )
        batch.end()
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        font.dispose()
    }
}
