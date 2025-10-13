package pl.decodesoft.enemy

import pl.decodesoft.Strings
import kotlin.math.pow
import kotlin.math.sqrt

class EnemyClient(
    val id: String,
    var x: Float,
    var y: Float,
    val type: String,
    var currentHealth: Int,
    var maxHealth: Int,
    var level: Int = 1, // Dodany poziom przeciwnika
    private var state: String = "IDLE",
    var isSelected: Boolean = false,
    var isAlive: Boolean = true,
    var isMoving: Boolean = false
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
            "Spider" -> Strings.ENEMY_SPIDER  // "Owca"
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
    var targetX: Float = x
    var targetY: Float = y

    private var velocityX = 0f
    private var velocityY = 0f

    fun getState(): String = state

    // Aktualizacja docelowej pozycji
    fun updateTargetPosition(newX: Float, newY: Float) {
        // Oblicz kwadraty różnic współrzędnych
        val deltaX = newX - x
        val deltaY = newY - y

        // Oblicz kwadrat odległości (bez pierwiastka)
        val distanceSquared = deltaX * deltaX + deltaY * deltaY

        // Porównaj z kwadratem maksymalnej odległości (50f * 50f = 2500f)
        if (distanceSquared > 2500f) {
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
        if (!isAlive) {
            timeSinceDeath += deltaTimeSeconds
            if (timeSinceDeath >= corpseDecayTime) {
                shouldBeRemoved = true
            }
            velocityX = 0f
            velocityY = 0f
            return
        }

        val distToTarget = sqrt((targetX - x).pow(2) + (targetY - y).pow(2))

        // ZMIEŃ: Tylko gdy jesteś DOKŁADNIE w celu
        if (distToTarget < 0.5f) {  // Zmień z 1f na 0.1f
            x = targetX
            y = targetY
            velocityX = 0f
            velocityY = 0f
            return
        }

        val dirX = (targetX - x) / distToTarget
        val dirY = (targetY - y) / distToTarget
        val moveDistance = speed * deltaTimeSeconds

        val oldX = x
        val oldY = y

        if (moveDistance >= distToTarget) {
            x = targetX
            y = targetY
            velocityX = 0f
            velocityY = 0f
        } else {
            x += dirX * moveDistance
            y += dirY * moveDistance
            velocityX = x - oldX
            velocityY = y - oldY
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