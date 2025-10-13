package pl.decodesoft.player.skin

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import pl.decodesoft.player.Player
import pl.decodesoft.player.Race
import java.util.concurrent.ConcurrentHashMap

class PlayerSkinManager {

    // Pozycja skina
    companion object {
        private const val RENDER_OFFSET_Y = 10f
    }

    // Cache skinów dla kombinacji rasa
    private val raceSkinCache = mutableMapOf<Pair<Race, Int>, PlayerSkin>()

    data class WeaponAnimationState(
        var isAnimating: Boolean = false,
        var animationTime: Float = 0f,
        val animationDuration: Float = 0.3f,
        var attackType: String = "MELEE_ATTACK",
        var attackDirection: Direction? = null
    )

    private val weaponAnimations = ConcurrentHashMap<String, WeaponAnimationState>()

    init {
        // Załaduj skiny dla każdej rasy
        loadRaceSkins()
    }

    fun getSkinForRace(race: Race, characterClass: Int): PlayerSkin? {
        val key = race to characterClass
        return raceSkinCache[key]
    }

    private fun loadRaceSkins() {
        // Załaduj skiny dla każdej kombinacji rasy i klasy
        Race.entries.forEach { race ->
            for (classIndex in 0..2) {  // 0=Archer, 1=Mage, 2=Warrior
                val key = race to classIndex
                raceSkinCache[key] = RaceSkin(race, classIndex)
            }
        }
    }

    fun update(deltaTime: Float) {
        // Update animacji broni
        weaponAnimations.entries.removeIf { (_, animState) ->
            if (animState.isAnimating) {
                animState.animationTime += deltaTime
                if (animState.animationTime >= animState.animationDuration) {
                    animState.isAnimating = false
                    animState.animationTime = 0f
                    animState.attackDirection = null
                    true
                } else false
            } else false
        }

        // Update wszystkich skinów
        raceSkinCache.values.forEach { it.update(deltaTime) }
    }

    fun startWeaponAnimation(playerId: String, attackType: String, attackDirection: Direction? = null) {
        val animState = weaponAnimations.getOrPut(playerId) { WeaponAnimationState() }
        animState.isAnimating = true
        animState.animationTime = 0f
        animState.attackType = attackType
        animState.attackDirection = attackDirection
    }

    fun renderPlayer(
        batch: SpriteBatch,
        player: Player,
        x: Float,
        y: Float,
        scale: Float = 1f,
        isMoving: Boolean = false,
        isDead: Boolean = false
    ) {
        // Przesunięcie renderowania w górę
        val adjustedY = y + RENDER_OFFSET_Y

        // Pobierz skin dla rasy gracza
        val skinKey = player.race to player.characterClass
        val skin = raceSkinCache[skinKey] ?: raceSkinCache[Race.HUMAN to 0]!!

        // Oblicz kierunek z velocity tylko gdy się porusza
        if (isMoving && (player.velocity.x != 0f || player.velocity.y != 0f)) {
            player.lastDirection = getDirectionFromVelocity(player.velocity.x, player.velocity.y)
        }

        // Sprawdź czy jest aktywna animacja ataku
        val animState = weaponAnimations[player.id]
        val isAttacking = animState?.isAnimating == true

        // Użyj kierunku ataku jeśli atakuje, w przeciwnym razie użyj lastDirection
        val direction = if (isAttacking && animState?.attackDirection != null) {
            animState.attackDirection!!
        } else {
            player.lastDirection
        }

        // Ustaw animację broni przed renderowaniem
        if (!isDead) {
            val animationProgress = if (isAttacking) {
                animState!!.animationTime / animState.animationDuration
            } else {
                0f
            }
            val attackType = animState?.attackType ?: "MELEE_ATTACK"
            skin.setWeaponAnimation(animationProgress, attackType)
        }

        when {
            isDead -> skin.renderDead(batch, x, adjustedY, scale, direction)
            isMoving && !isAttacking -> skin.renderMoving(batch, x, adjustedY, scale, direction)
            else -> skin.renderIdle(batch, x, adjustedY, scale, direction)
        }
    }

    private fun getDirectionFromVelocity(velocityX: Float, velocityY: Float): Direction {
        if (velocityX == 0f && velocityY == 0f) return Direction.DOWN

        return when {
            kotlin.math.abs(velocityY) > kotlin.math.abs(velocityX) -> {
                if (velocityY > 0) Direction.UP else Direction.DOWN
            }
            velocityX != 0f -> {
                if (velocityX > 0) Direction.RIGHT else Direction.LEFT
            }
            else -> Direction.DOWN
        }
    }

    fun calculateDirectionToTarget(fromX: Float, fromY: Float, toX: Float, toY: Float): Direction {
        val dx = toX - fromX
        val dy = toY - fromY

        return when {
            kotlin.math.abs(dy) > kotlin.math.abs(dx) -> {
                if (dy > 0) Direction.UP else Direction.DOWN
            }
            dx > 0 -> Direction.RIGHT
            dx < 0 -> Direction.LEFT
            else -> Direction.DOWN
        }
    }

    fun dispose() {
        raceSkinCache.values.forEach { it.dispose() }
    }
}