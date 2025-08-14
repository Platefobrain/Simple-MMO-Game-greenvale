package pl.decodesoft.enemy

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.Strings
import kotlin.math.pow
import kotlin.math.sqrt

class EnemyClient(
    val id: String,
    var x: Float,
    var y: Float,
    private val type: String,
    var currentHealth: Int,
    var maxHealth: Int,
    var level: Int = 1, // Dodany poziom przeciwnika
    private var state: String = "IDLE",
    var isSelected: Boolean = false,
    var isAlive: Boolean = true
) {
    // Czas po śmierci (w sekundach)
    private var timeSinceDeath: Float = 0f

    // Czas po jakim ciało znika (w sekundach) - możesz to dostosować
    private val corpseDecayTime: Float = 10f

    // Czy ciało powinno być usunięte z gry
    private var shouldBeRemoved: Boolean = false

    // Pozyskaj nazwy po polsku bazując na typie
    val displayName: String
        get() = when (type) {
            "Sheep" -> Strings.ENEMY_SHEEP  // "Owca"
            "Wolf" -> Strings.ENEMY_WOLF    // "Wilk"
            "Bear" -> Strings.ENEMY_BEAR    // "Niedźwiedź"
            else -> type
        }

    // Prędkość zależna od stanu
    private val speed: Float
        get() = when (state) {
            "IDLE" -> 30f
            "CHASE" -> 50f
            "RETURN" -> 70f
            else -> 30f
        }

    // Zmienne dla ruchu
    private var targetX: Float = x
    private var targetY: Float = y

    // Aktualizacja docelowej pozycji
    fun updateTargetPosition(newX: Float, newY: Float) {
        // Oblicz kwadraty różnic współrzędnych
        val deltaX = newX - x
        val deltaY = newY - y

        // Oblicz kwadrat odległości (bez pierwiastka)
        val distanceSquared = deltaX * deltaX + deltaY * deltaY

        // Porównaj z kwadratem maksymalnej odległości (50f * 50f = 2500f)
        if (distanceSquared > 2500f) { // 50^2 = 2500
            // Musimy obliczyć pierwiastek tylko raz, gdy faktycznie jest potrzebny
            val distance = sqrt(distanceSquared)

            // Obliczamy kierunek do nowego celu
            val dirX = deltaX / distance
            val dirY = deltaY / distance

            // Ustawiamy cel bliżej obecnej pozycji (maksymalnie 50 jednostek)
            targetX = x + dirX * 50f
            targetY = y + dirY * 50f
        } else {
            // Normalny przypadek - cel jest w rozsądnej odległości
            targetX = newX
            targetY = newY
        }
    }

    // Aktualizacja stanu przeciwnika
    fun updateState(newState: String) {
        state = newState
    }

    // Metoda do oznaczania przeciwnika jako martwego
    fun markAsDead() {
        if (isAlive) {
            isAlive = false
            timeSinceDeath = 0f
        }
    }

    // Aktualizacja pozycji w każdej klatce bazująca na prędkości
    fun update(deltaTimeSeconds: Float) {
        // Jeśli przeciwnik jest martwy, liczmy czas do usunięcia ciała
        if (!isAlive) {
            timeSinceDeath += deltaTimeSeconds
            if (timeSinceDeath >= corpseDecayTime) {
                shouldBeRemoved = true
            }
            return
        }

        // Oblicz odległość do celu
        val distToTarget = sqrt((targetX - x).pow(2) + (targetY - y).pow(2))

        // Jeśli jesteśmy już bardzo blisko celu, możemy go od razu osiągnąć
        if (distToTarget < 1f) {
            x = targetX
            y = targetY
            return
        }

        // Oblicz kierunek do celu
        val dirX = (targetX - x) / distToTarget
        val dirY = (targetY - y) / distToTarget

        // Oblicz dystans do pokonania w tej klatce
        val moveDistance = speed * deltaTimeSeconds

        if (moveDistance >= distToTarget) {
            x = targetX
            y = targetY
        } else {
            // W przeciwnym razie porusz się w kierunku celu z odpowiednią prędkością
            x += dirX * moveDistance
            y += dirY * moveDistance
        }
    }

    fun render(shapeRenderer: ShapeRenderer) {
        if (isAlive) {
            // Renderuj przeciwnika normalnie (tylko kształt, bez paska życia)
            shapeRenderer.color = Color.GRAY
            shapeRenderer.circle(x, y, 15f)
        } else {
            // Renderuj ciało po śmierci z efektem zanikania
            val fadeProgress = timeSinceDeath / corpseDecayTime
            val alpha = 1f - fadeProgress.coerceIn(0f, 1f)

            shapeRenderer.color = Color(0.3f, 0.3f, 0.3f, alpha) // Ciemny kolor z przezroczystością
            shapeRenderer.circle(x, y, 15f)
        }
    }

    // Dodaj tę metodę dla bezpośredniego ustawiania pozycji i celu
    fun teleportToPosition(newX: Float, newY: Float) {
        x = newX
        y = newY
        targetX = newX
        targetY = newY
    }

    // Metoda pomocnicza do sprawdzenia, czy przeciwnik może być usunięty
    fun canBeRemoved(): Boolean {
        return shouldBeRemoved
    }

}