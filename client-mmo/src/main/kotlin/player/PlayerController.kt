package pl.decodesoft.player

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.effects.CombatEffectsManager
import pl.decodesoft.input.PlayerInputHandler
import pl.decodesoft.klasy.CharacterClass
import pl.decodesoft.klasy.skile.SkileManager
import pl.decodesoft.network.PlayerNetworkManager
import pl.decodesoft.ui.GameUI
import pl.decodesoft.msg.ChatSystem

// Główna klasa kontrolująca gracza - fasada koordynująca wszystkie komponenty
class PlayerController(
    private val localPlayer: Player,
    players: Map<String, Player>,
    enemies: Map<String, EnemyClient>,
    camera: OrthographicCamera,
    networkScope: CoroutineScope,
    getSession: () -> DefaultWebSocketSession?,
    private val characterClass: CharacterClass,
    private val skileManager: SkileManager,
    private val font: BitmapFont,
    gameUI: GameUI, // Wymagane GameUI (bez nullable)
    chatSystem: ChatSystem // Dodaj ChatSystem
) {

    private val combatEffectsManager = CombatEffectsManager()
    private val networkManager = PlayerNetworkManager(
        networkScope = networkScope,
        getSession = getSession
    )
    private val targetManager = PlayerTargetManager(
        camera = camera,
        localPlayer = localPlayer,
        players = players,
        enemies = enemies,
        characterClass = characterClass,
        onTargetChanged = { _, _ -> }
    )
    private val inputHandler = PlayerInputHandler(
        camera = camera,
        targetManager = targetManager,
        onMoveRequested = ::handleMoveRequest,
        onControlKeyPressed = ::handleControlKey,
        onAttackRequested = ::handleAttackRequest,
        gameUI = gameUI, // Bez null check
        chatSystem = chatSystem // Przekaż ChatSystem
    )

    private var pendingAttackTarget: Any? = null
    private var pendingAttackEntityType: String? = null
    private var isMovingToAttack = false

    private fun getAttackRange(): Float = when (localPlayer.characterClass) {
        0 -> 295f      // Łucznik
        1 -> 245f      // Mag
        2 ->  40f      // Wojownik
        else -> 0f
    }

    fun handleInput(): Boolean = if (localPlayer.isDead()) false else inputHandler.handleInput()

    fun update(delta: Float) {
        skileManager.update(delta)
        characterClass.update(delta)
        combatEffectsManager.update(delta)
        checkAttackTarget()
    }


    fun renderShapes(shapeRenderer: ShapeRenderer) {
        skileManager.render(shapeRenderer)
    }

    fun renderBatch(batch: SpriteBatch) {
        combatEffectsManager.render(batch, font)
    }

    fun handleMessage(command: String, parts: List<String>) {
        skileManager.handleSkillMessage(command, parts)
    }

    fun addDamageText(x: Float, y: Float, text: String, color: Color) {
        combatEffectsManager.addDamageText(x, y, text, color)
    }

    private fun handleMoveRequest(x: Float, y: Float) {
        cancelPendingAttack()
        networkManager.sendMoveRequest(x, y, localPlayer.id)
    }

    private fun handleAttackRequest(target: Any, entityType: String) {
        val (tx, ty) = when (entityType) {
            "player" -> (target as Player).let { it.x to it.y }
            "enemy"  -> (target as EnemyClient).let { it.x to it.y }
            else      -> return
        }
        val distance = Vector2.dst(localPlayer.x, localPlayer.y, tx, ty)
        val range    = getAttackRange()
        if (distance <= range) performAttack(target, entityType) else moveToAttackTarget(target, entityType, tx, ty)
    }

    private fun moveToAttackTarget(target: Any, entityType: String, tx: Float, ty: Float) {
        pendingAttackTarget = target
        pendingAttackEntityType = entityType
        isMovingToAttack = true

        val direction   = Vector2(tx - localPlayer.x, ty - localPlayer.y).nor()
        val moveDist    = Vector2.dst(localPlayer.x, localPlayer.y, tx, ty) - getAttackRange()
        val destX       = localPlayer.x + direction.x * moveDist
        val destY       = localPlayer.y + direction.y * moveDist

        networkManager.sendMoveRequest(destX, destY, localPlayer.id)
    }

    private fun checkAttackTarget() {
        if (!isMovingToAttack || pendingAttackTarget == null) return
        val (tx, ty) = when (pendingAttackEntityType) {
            "player" -> (pendingAttackTarget as Player).let { it.x to it.y }
            "enemy"  -> (pendingAttackTarget as EnemyClient).let { it.x to it.y }
            else      -> return
        }
        val inRange = Vector2.dst(localPlayer.x, localPlayer.y, tx, ty) <= getAttackRange()
        if (inRange || !localPlayer.movingState) {
            performAttack(pendingAttackTarget!!, pendingAttackEntityType!!)
            cancelPendingAttack()
        }
    }

    private fun performAttack(target: Any, entityType: String) {
        when (entityType) {
            "player" -> characterClass.handleTargetClick(target as Player)
            "enemy"  -> characterClass.handleEnemyClick(target as EnemyClient)
        }
    }

    private fun cancelPendingAttack() {
        pendingAttackTarget = null
        pendingAttackEntityType = null
        isMovingToAttack = false
    }

    private fun handleControlKey() {
        cancelPendingAttack()
        // dodatkowa logika pod klawisze umiejętności
    }

    fun getCharacterClass(): CharacterClass {
        return characterClass
    }
}