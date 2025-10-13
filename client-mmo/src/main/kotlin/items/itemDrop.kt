package pl.decodesoft.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import pl.decodesoft.items.character.ClientItem
import kotlin.math.sin

data class ItemDrop(
    var id: String = java.util.UUID.randomUUID().toString(),
    val itemId: String,
    var x: Float,
    var y: Float,
    var itemDefinition: ClientItem? = null,
    var itemManager: ItemManager? = null,
    var isSelected: Boolean = false
) {
    private var animationTime = 0f
    private val bounceHeight = 3f
    private val bounceSpeed = 3f
    private val iconSize = 25f

    fun update(deltaTime: Float) {
        animationTime += deltaTime * bounceSpeed
    }

    fun render(spriteBatch: SpriteBatch) {
        val bounceOffset = sin(animationTime) * bounceHeight
        val renderY = y + bounceOffset

        val renderX = x - iconSize / 2
        val adjustedRenderY = renderY - iconSize / 2

        val textureId = if (itemId.startsWith("currency_")) {
            val parts = itemId.split("_")
            if (parts.size >= 3) {
                "${parts[0]}_${parts[1]}"
            } else {
                itemId
            }
        } else {
            itemId
        }

        val texture = itemManager?.getItemTexture(textureId)
        texture?.let {
            spriteBatch.draw(it, renderX, adjustedRenderY, iconSize, iconSize)
        } ?: run {
            println("DEBUG: Brak tekstury dla $textureId (original: $itemId)")
        }
    }

    fun renderItemName(spriteBatch: SpriteBatch, font: BitmapFont) {
        val bounceOffset = sin(animationTime) * bounceHeight
        val renderY = y + bounceOffset

        val displayName = getDisplayName()

        val glyphLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout()
        glyphLayout.setText(font, displayName)
        val textWidth = glyphLayout.width

        // Kolor w zależności od typu waluty
        font.color = when {
            itemId.startsWith("currency_gold") -> Color.GOLD
            itemId.startsWith("currency_silver") -> Color.LIGHT_GRAY
            itemId.startsWith("currency_copper") -> Color.ORANGE
            else -> Color.WHITE
        }

        font.draw(spriteBatch, displayName, x - textWidth/2, renderY + 35f)
        font.color = Color.WHITE
    }

    fun isInRange(playerX: Float, playerY: Float, range: Float = 32f): Boolean {
        val dx = x - playerX
        val dy = y - playerY
        return (dx * dx + dy * dy) <= (range * range)
    }

    fun getDisplayName(): String {
        if (itemId.startsWith("currency_")) {
            val parts = itemId.split("_")
            if (parts.size >= 3) {
                val amount = parts[2].toIntOrNull() ?: 1
                val currencyType = when(parts[1]) {
                    "copper" -> "Copper"
                    "silver" -> "Silver"
                    "gold" -> "Gold"
                    else -> parts[1].replaceFirstChar { it.uppercase() }
                }
                return "$amount $currencyType"
            }
        }

        return itemDefinition?.name ?: itemId.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}