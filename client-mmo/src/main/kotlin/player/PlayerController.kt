package pl.decodesoft.player

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.effects.CombatEffectsManager
import pl.decodesoft.input.PlayerInputHandler
import pl.decodesoft.items.ItemDrop
import pl.decodesoft.klasy.CharacterClass
import pl.decodesoft.klasy.skile.SkileManager
import pl.decodesoft.ui.GameUI
import pl.decodesoft.msg.ChatSystem
import pl.decodesoft.npc.NPCClient
import pl.decodesoft.player.skin.Direction
import pl.decodesoft.player.skin.PlayerSkinManager

// Główna klasa kontrolująca gracza
class PlayerController(
    private val localPlayer: Player,
    players: Map<String, Player>,
    enemies: Map<String, EnemyClient>,
    npcs: Map<String, NPCClient>,
    droppedItems: Map<String, ItemDrop>,
    camera: OrthographicCamera,
    uiCamera: OrthographicCamera,
    private val characterClass: CharacterClass,
    private val skileManager: SkileManager,
    private val font: BitmapFont,
    gameUI: GameUI,
    chatSystem: ChatSystem,
    private val movementController: MovementController,
    private val playerSkinManager: PlayerSkinManager,
    menuToggle: () -> Unit,
    isMenuVisible: () -> Boolean,
    getDroppedItems: () -> Map<String, ItemDrop>,
    onPathfindRequested: (Int, Int, Int, Int) -> Unit
) {

    private val combatEffectsManager = CombatEffectsManager()
    private val targetManager = PlayerTargetManager(
        camera = camera,
        localPlayer = localPlayer,
        players = players,
        enemies = enemies,
        npcs = npcs,
        droppedItems = droppedItems,
        onTargetChanged = { _, _ -> }
    )
    private val inputHandler = PlayerInputHandler(
        camera = camera,
        uiCamera = uiCamera,
        targetManager = targetManager,
        onMoveRequested = ::handleMoveRequest,
        onControlKeyPressed = ::handleControlKey,
        onAttackRequested = ::handleAttackRequest,
        onItemClick = ::handleItemClick,
        onPathfindRequested = onPathfindRequested,
        menuToggle = menuToggle,
        isMenuVisible = isMenuVisible,
        getDroppedItems = getDroppedItems,
        getLocalPlayer = { localPlayer },
        gameUI = gameUI,
        chatSystem = chatSystem
    )

    private var pendingAttackTarget: Any? = null
    private var pendingAttackEntityType: String? = null
    private var isMovingToAttack = false

    // Nowe właściwości dla itemów
    private var pendingItemPickup: String? = null
    private var isMovingToItem = false
    private var itemPickupCallback: ((String) -> Boolean)? = null

    private fun getAttackRange(): Float = when (localPlayer.characterClass) {
        0 -> 295f      // Łucznik
        1 -> 245f      // Mag
        2 ->  40f      // Wojownik
        else -> 0f
    }

    private fun getPickupRange(): Float = 32f

    fun handleInput(): Boolean = if (localPlayer.isDead()) false else inputHandler.handleInput()

    fun update(delta: Float) {
        skileManager.update(delta)
        characterClass.update(delta)
        combatEffectsManager.update(delta)
        checkTargets()
    }

    // Renderowanie z SpriteBatch (tekstury + teksty damage)
    fun renderBatch(batch: SpriteBatch) {
        // Renderuj strzały i inne efekty z teksturą
        skileManager.renderBatch(batch)

        // Renderuj teksty damage
        combatEffectsManager.render(batch, font)
    }

    fun handleMessage(command: String, parts: List<String>) {
        skileManager.handleSkillMessage(command, parts)
    }

    fun addDamageText(x: Float, y: Float, text: String, color: Color) {
        combatEffectsManager.addDamageText(x, y, text, color)
    }

    // Ustawia callback do podnoszenia itemów
    fun setItemPickupCallback(callback: (String) -> Boolean) {
        itemPickupCallback = callback
    }

    // Obsługa kliknięć na itemy
    fun handleItemClick(itemId: String, itemX: Float, itemY: Float) {
        val distance = Vector2.dst(localPlayer.x, localPlayer.y, itemX, itemY)
        val pickupRange = getPickupRange()

        if (distance <= pickupRange) {
            tryPickupItem(itemId)
        } else {
            moveToItem(itemId, itemX, itemY)
            // characterClass.showMessage("Idę do itemu", 2f, Color.CYAN)
        }
    }

    private fun moveToItem(itemId: String, itemX: Float, itemY: Float) {
        cancelPendingAttack() // Anuluj ewentualny atak
        pendingItemPickup = itemId
        isMovingToItem = true

        movementController.handleTargetClick(itemX, itemY, localPlayer.id)
    }

    private fun tryPickupItem(itemId: String) {
        itemPickupCallback?.invoke(itemId)
    }

    private fun handleMoveRequest(x: Float, y: Float) {
        cancelPendingAttack()
        if (!isMovingToItem) {
            cancelPendingItemPickup()
        }
        movementController.handleMovementClick(x, y, localPlayer.id)
    }

    private fun handleAttackRequest(target: Any, entityType: String) {
        cancelPendingItemPickup()

        val (tx, ty) = when (entityType) {
            "player" -> (target as Player).let { it.x to it.y }
            "enemy"  -> (target as EnemyClient).let { it.x to it.y }
            "npc"    -> (target as NPCClient).let { it.x to it.y }
            else     -> return
        }

        val distance = Vector2.dst(localPlayer.x, localPlayer.y, tx, ty)

        // Różne zasięgi dla różnych celów
        val range = if (entityType == "npc") {
            val npc = target as NPCClient
            if (npc.faction == Faction.NONE || localPlayer.faction == npc.faction) {
                35f  // Zasięg rozmowy
            } else {
                getAttackRange()
            }
        } else {
            getAttackRange()
        }

        if (distance > range) {
            characterClass.showMessage("Jesteś za daleko!", 2f, Color.YELLOW)
        }

        if (distance <= range) performAttack(target, entityType) else moveToAttackTarget(target, entityType, tx, ty)
    }

    private fun moveToAttackTarget(target: Any, entityType: String, tx: Float, ty: Float) {
        pendingAttackTarget = target
        pendingAttackEntityType = entityType
        isMovingToAttack = true

        // Użyj odpowiedniego zasięgu dla każdego typu celu
        val range = if (entityType == "npc") {
            val npc = target as NPCClient
            if (npc.faction == Faction.NONE || localPlayer.faction == npc.faction) {
                35f  // Zasięg rozmowy
            } else {
                getAttackRange()
            }
        } else {
            getAttackRange()
        }

        val direction   = Vector2(tx - localPlayer.x, ty - localPlayer.y).nor()
        val moveDist    = Vector2.dst(localPlayer.x, localPlayer.y, tx, ty) - range
        val destX       = localPlayer.x + direction.x * moveDist
        val destY       = localPlayer.y + direction.y * moveDist

        movementController.handleTargetClick(destX, destY, localPlayer.id)
    }

    private fun checkTargets() {
        checkAttackTarget()
        checkItemTarget()
    }

    private fun checkAttackTarget() {
        if (!isMovingToAttack || pendingAttackTarget == null) return
        val (tx, ty) = when (pendingAttackEntityType) {
            "player" -> (pendingAttackTarget as Player).let { it.x to it.y }
            "enemy"  -> (pendingAttackTarget as EnemyClient).let { it.x to it.y }
            "npc"    -> (pendingAttackTarget as NPCClient).let { it.x to it.y }
            else      -> return
        }
        val inRange = Vector2.dst(localPlayer.x, localPlayer.y, tx, ty) <= getAttackRange()
        if (inRange || !localPlayer.movingState) {
            performAttack(pendingAttackTarget!!, pendingAttackEntityType!!)
            cancelPendingAttack()
        }
    }

    private fun checkItemTarget() {
        if (!isMovingToItem || pendingItemPickup == null) return
        if (!localPlayer.movingState) {
            cancelPendingItemPickup()
        }
    }

    private fun calculateDirectionToTarget(target: Any, entityType: String): Direction {
        val (tx, ty) = when (entityType) {
            "player" -> (target as Player).let { it.x to it.y }
            "enemy"  -> (target as EnemyClient).let { it.x to it.y }
            "npc"    -> (target as NPCClient).let { it.x to it.y }
            else     -> return Direction.DOWN
        }

        val dx = tx - localPlayer.x
        val dy = ty - localPlayer.y

        return when {
            kotlin.math.abs(dy) > kotlin.math.abs(dx) -> {
                if (dy > 0) Direction.UP else Direction.DOWN
            }
            dx > 0 -> Direction.RIGHT
            dx < 0 -> Direction.LEFT
            else -> Direction.DOWN
        }
    }

    private fun performAttack(target: Any, entityType: String) {
        // Oblicz kierunek do celu
        val attackDirection = calculateDirectionToTarget(target, entityType)

        // Uruchom animację ataku
        playerSkinManager.startWeaponAnimation(localPlayer.id, "BOW_ATTACK", attackDirection)

        // Wywołaj atak
        when (entityType) {
            "player" -> characterClass.handleTargetClick(target as Player)
            "enemy"  -> characterClass.handleEnemyClick(target as EnemyClient)
            "npc"    -> characterClass.handleNPCClick(target as NPCClient)
        }
    }

    private fun cancelPendingAttack() {
        pendingAttackTarget = null
        pendingAttackEntityType = null
        isMovingToAttack = false
    }

    private fun cancelPendingItemPickup() {
        pendingItemPickup = null
        isMovingToItem = false
    }

    private fun handleControlKey() {
        cancelPendingAttack()
        cancelPendingItemPickup()
    }

    fun getCharacterClass(): CharacterClass {
        return characterClass
    }
}