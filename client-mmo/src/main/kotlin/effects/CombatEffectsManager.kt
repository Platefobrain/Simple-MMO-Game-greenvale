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

package pl.decodesoft.effects

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

// Klasa zarządzająca efektami walki, jak teksty obrażeń
class CombatEffectsManager {
    // Klasa do przechowywania efektów tekstowych obrażeń
    private class DamageText(
        var x: Float,
        var y: Float,
        val text: String,
        val color: Color,
        var alpha: Float = 1.5f,
        var lifetime: Float = 0f
    )

    // Lista efektów tekstowych obrażeń
    private val damageTexts = mutableListOf<DamageText>()

    // Dodaj nowy efekt tekstowy obrażeń
    fun addDamageText(x: Float, y: Float, text: String, color: Color) {
        damageTexts.add(DamageText(x, y, text, color))
    }

    // Aktualizacja efektów tekstowych obrażeń
    fun update(delta: Float) {
        // Aktualizacja pozycji i przezroczystości
        damageTexts.forEach { text ->
            text.y += 30f * delta // Unoszenie w górę
            text.alpha -= delta // Zanikanie
            text.lifetime += delta
        }

        // Usuwanie starych efektów
        damageTexts.removeAll { it.lifetime > 1.5f }
    }

    // Renderowanie efektów tekstowych obrażeń
    fun render(batch: SpriteBatch, font: BitmapFont) {
        if (damageTexts.isEmpty()) return

        // Zapisz oryginalny kolor i rozmiar czcionki
        val originalColor = font.color.cpy()
        val originalScaleX = font.data.scaleX
        val originalScaleY = font.data.scaleY

        // ZWIĘKSZ ROZMIAR CZCIONKI
        font.data.setScale(1.3f) // 2x większy tekst (możesz zmienić na 1.5f, 2.5f itp.)

        damageTexts.forEach { text ->
            font.color = Color(text.color.r, text.color.g, text.color.b, text.alpha)
            font.draw(batch, text.text, text.x - 10f, text.y)
        }

        // Przywróć oryginalny rozmiar i kolor czcionki po zakończeniu renderowania
        font.data.setScale(originalScaleX, originalScaleY)
        font.color = originalColor
    }
}