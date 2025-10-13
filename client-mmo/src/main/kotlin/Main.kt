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

package pl.decodesoft

import pl.decodesoft.items.ItemManager
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import pl.decodesoft.Strings.IP_ADDRESS
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.enemy.EnemySkinManager
import pl.decodesoft.klasy.Archer
import pl.decodesoft.klasy.Mage
import pl.decodesoft.klasy.Warrior
import pl.decodesoft.klasy.skile.SkileManager
import pl.decodesoft.map.GameMap
import pl.decodesoft.msg.ChatSystem
import pl.decodesoft.network.MessageManager
import pl.decodesoft.screens.CharacterCreationScreen
import pl.decodesoft.screens.CharacterSelectionScreen
import pl.decodesoft.screens.DeathScreen
import pl.decodesoft.screens.LoginScreen
import pl.decodesoft.settings.Menu
import pl.decodesoft.states.*
import pl.decodesoft.ui.GameUI
import pl.decodesoft.ui.UISkin
import pl.decodesoft.items.ItemTooltip
import pl.decodesoft.items.character.ClientItem
import pl.decodesoft.items.inventory.InventoryItem
import pl.decodesoft.items.ItemDrop
import pl.decodesoft.klasy.projectiles.ProjectileTextures
import pl.decodesoft.npc.NPCClient
import pl.decodesoft.player.*
import pl.decodesoft.player.skin.PlayerSkinManager
import java.util.concurrent.ConcurrentHashMap

// Główny kod gry
class MMOGame : ApplicationAdapter() {

    // === PODSTAWOWE KOMPONENTY RENDEROWANIA ===
    private lateinit var currentState: GameState
    lateinit var batch: SpriteBatch
    lateinit var uiBatch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var font: BitmapFont
    lateinit var camera: OrthographicCamera
    lateinit var uiCamera: OrthographicCamera

    // === MANAGERY GIER ===
    lateinit var gameMap: GameMap
    lateinit var chatSystem: ChatSystem
    lateinit var menu: Menu
    lateinit var messageManager: MessageManager
    private lateinit var skileManager: SkileManager
    lateinit var itemManager: ItemManager
    private lateinit var itemTooltip: ItemTooltip
    lateinit var playerSkinManager: PlayerSkinManager
    lateinit var enemySkinManager: EnemySkinManager
    var gameUI: GameUI? = null

    // === DANE GRACZA ===
    var localPlayerId = "player_${System.currentTimeMillis()}"
    var username = ""
    var characterNickname: String = ""
    lateinit var localPlayer: Player
    val layout = GlyphLayout()

    // === MAPY GRACZY I OBIEKTÓW ===
    val players = ConcurrentHashMap<String, Player>()
    val pathTiles = mutableListOf<Pair<Int, Int>>()
    val enemies = ConcurrentHashMap<String, EnemyClient>()
    val npcs = ConcurrentHashMap<String, NPCClient>()
    val droppedItems = ConcurrentHashMap<String, ItemDrop>()

    // === WALUTA ===
    var playerGold = 0
    var playerSilver = 0
    var playerCopper = 0

    // === SIEĆ I KOMUNIKACJA ===
    private lateinit var networkScope: CoroutineScope
    private var client: HttpClient? = null
    private var session: DefaultWebSocketSession? = null

    // === KONTROLERY ===
    lateinit var playerController: PlayerController
    lateinit var movementController: MovementController

    // === WROGOWIE ===
    var enemyUpdateTimer = 0f
    val enemyUpdateInterval = 1f

    // === EKRANY ===
    private var deathScreen: DeathScreen? = null
    private var loginScreen: LoginScreen? = null
    private var characterSelectionScreen: CharacterSelectionScreen? = null
    private var characterCreationScreen: CharacterCreationScreen? = null

    // Metoda wywoływana z ItemMessageHandler
    fun handleItemMoved(fromType: String, fromSlot: Int, toType: String, toSlot: Int, itemId: String) {
        try {
            itemManager.handleItemMoved(fromType, fromSlot, toType, toSlot, itemId)
            itemManager.debugPrintInventory()
        } catch (e: Exception) {
            println("DEBUG: ItemManager nie jest dostępny: ${e.message}")
        }
    }

    fun getItemDefinition(itemId: String): ClientItem? {
        return itemManager.getItemDefinition(itemId)
    }

    fun setInventoryItem(slot: Int, item: InventoryItem) {
        itemManager.setInventoryItem(slot, item)
    }

    // Poproś serwer o aktualizację inventory
    fun requestInventoryUpdate() {
        try {
            sendWebSocketMessage("GET_PLAYER_INVENTORY|${localPlayer.id}")
        } catch (e: Exception) {
            println("DEBUG: Błąd wysyłania żądania inventory: ${e.message}")
        }
    }

    fun refreshInventoryUI() {
        try {
        } catch (e: Exception) {
            println("DEBUG: Błąd odświeżania UI inventory: ${e.message}")
        }
    }

    fun updatePlayerCurrency(gold: Int, silver: Int, copper: Int) {
        playerGold = gold
        playerSilver = silver
        playerCopper = copper
    }

    // Metoda do resetowania pozycji przy respawnie
    private fun resetMovementPosition(x: Float, y: Float) {
        if (::movementController.isInitialized) {
            movementController.resetLastPosition(x, y)
        }
    }

    override fun create() {
        //xd
        deathScreen = DeathScreen(this)
        batch = SpriteBatch()
        uiBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"))
        val parameter = FreeTypeFontParameter().apply {
            size = 15
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE

            borderWidth = 1f // BOLD czcionki
            borderColor = Color(0.275f, 0.275f, 0.275f, 1f)
        }
        font = generator.generateFont(parameter)
        font.data.markupEnabled = true
        generator.dispose()

        // Kamera świata gry
        camera = OrthographicCamera()
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        // Kamera UI
        uiCamera = OrthographicCamera()
        uiCamera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        uiCamera.position.set(uiCamera.viewportWidth / 2, uiCamera.viewportHeight / 2, 0f)
        uiCamera.update()

        // Inicjalizacja zakresu coroutine dla komunikacji sieciowej
        networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        //spam
        movementController = MovementController(networkScope) { session }

        // Inicjalizacja ekranu logowania
        loginScreen = LoginScreen(this)
        loginScreen?.show()

        // wiadomosci graczy
        messageManager = MessageManager(this)

        // menu gry
        menu = Menu(this)

        // itemmanager
        itemManager = ItemManager(this)
        itemTooltip = ItemTooltip(this)
        itemManager.initialize()
        itemManager.loadCoinTextures()

        // manager kolorów skórek graczy
        playerSkinManager = PlayerSkinManager()

        // manager skórek wrogów
        enemySkinManager = EnemySkinManager()

        // gameui
        gameUI = GameUI(this)

        // łaczenie itemmanager game ui
        gameUI!!.connectItemManager()

        // Ustaw początkowy stan gry
        changeState(LoginState(this))
    }

    // metoda zmian stanu
    fun changeState(newState: GameState) {
        // Jeśli już istnieje stan, wywołaj wyjście
        if (::currentState.isInitialized) {
            currentState.exit()
        }

        // Ustawienie nowego stanu
        currentState = newState

        // Wywołanie wejścia do nowego stanu
        currentState.enter()
    }

    // Pokazuje ekran wyboru postaci
    fun showCharacterSelectionScreen() {
        changeState(CharacterSelectionState(this))
    }

    // Pokazuje ekran tworzenia postaci
    fun showCharacterCreationScreen(slotIndex: Int) {
        changeState(CharacterCreationState(this, slotIndex))
    }

    // Przełącza na ekran logowania
    fun switchToLoginScreen() {
        changeState(LoginState(this))
    }

    // Chat
    fun receiveNetworkChatMessage(senderId: String, senderName: String, content: String) {
        chatSystem.receiveMessage(senderId, senderName, content)
    }

    // combat log
    fun receiveNetworkCombatLog(content: String) {
        chatSystem.addLogMessage(content)
    }

    // metoda do aktualizacji przeciwników (w tym usuwania ciał)
    private fun updateEnemies(deltaTime: Float) {
        // Aktualizuj wszystkich przeciwników
        enemies.values.forEach { enemy ->
            enemy.update(deltaTime)
        }

        // Usuń przeciwników, których ciała powinny zniknąć
        val toRemove = enemies.filter { (_, enemy) -> enemy.canBeRemoved() }
        toRemove.forEach { (id, _) ->
            enemies.remove(id)
        }

        // Opcjonalnie: loguj usuwanie ciał (do debugowania)
        if (toRemove.isNotEmpty()) {
            Gdx.app.debug("EnemyManager", "Removed ${toRemove.size} corpses")
        }
    }

    // Aktualizuje istniejącego przeciwnika lub tworzy nowego
    fun updateEnemy(id: String, x: Float, y: Float, type: String, hp: Int, maxHp: Int, level: Int = 1, state: String = "IDLE", isMoving: Boolean = false): EnemyClient {

        return enemies[id]?.let { existingEnemy ->
            existingEnemy.updateTargetPosition(x, y)
            existingEnemy.currentHealth = hp
            existingEnemy.maxHealth = maxHp
            existingEnemy.level = level
            existingEnemy.updateState(state)
            existingEnemy.isMoving = isMoving
            existingEnemy.isAlive = hp > 0
            existingEnemy

        } ?: run {
            val newEnemy = EnemyClient(id, x, y, type, hp, maxHp, level, state, isMoving = isMoving)
            newEnemy.isAlive = hp > 0
            enemies[id] = newEnemy
            newEnemy
        }
    }

    // Teleportuje przeciwnika do nowej pozycji
    fun respawnEnemy(id: String, x: Float, y: Float, type: String, hp: Int, maxHp: Int, level: Int = 1, state: String): EnemyClient {
        return enemies[id]?.let { enemy ->
            enemy.teleportToPosition(x, y)
            enemy.currentHealth = hp
            enemy.maxHealth = maxHp
            enemy.level = level
            enemy.updateState(state)
            enemy.isAlive = true
            enemy
        } ?: run {
            val newEnemy = EnemyClient(id, x, y, type, hp, maxHp, level, state)
            newEnemy.isAlive = true
            enemies[id] = newEnemy
            newEnemy
        }
    }

    // Aktualizuje zdrowie przeciwnika
    fun updateEnemyHealth(id: String, damage: Int): Boolean {
        return enemies[id]?.let { enemy ->
            enemy.currentHealth -= damage
            if (enemy.currentHealth <= 0) {
                enemy.currentHealth = 0
                enemy.markAsDead()
            }
            true
        } ?: false
    }

    // Oznacza przeciwnika jako martwego
    fun markEnemyAsDead(id: String): Boolean {
        return enemies[id]?.let { enemy ->
            enemy.markAsDead()
            enemy.isSelected = false
            true
        } ?: false
    }

    // Dodaje nowego gracza do gry
    fun addPlayer(
        id: String,
        x: Float,
        y: Float,
        username: String,
        characterClass: Int,
        currentHealth: Int,
        maxHealth: Int,
        currentMana: Int,
        maxMana: Int,
        level: Int = 1,
        experience: Int = 0,
        faction: Faction = Faction.NONE,
        race: Race = Race.HUMAN
    ) {
        if (id != localPlayerId) {
            val newPlayer = Player(
                x, y, id, username, characterClass,
                level = level,
                experience = experience,
                faction = faction,
                race = race
            )
            newPlayer.currentHealth = currentHealth
            newPlayer.maxHealth = maxHealth
            newPlayer.currentMana = currentMana
            newPlayer.maxMana = maxMana
            players[id] = newPlayer
        }
    }

    // Dodanie nowego npc do gry
    fun addNPC(id: String, name: String, type: String, x: Float, y: Float, currentHealth: Int = 100, maxHealth: Int = 100, level: Int = 1, faction: Faction = Faction.NONE) {
        val npc = NPCClient(id, name, type, x, y, currentHealth, maxHealth, level, false, faction)
        npcs[id] = npc
        println("Dodano NPC: $name ($type), faction: ${faction.displayName}")
    }

    fun respawnNPC(id: String, name: String, type: String, x: Float, y: Float, currentHealth: Int, maxHealth: Int, level: Int, faction: Faction = Faction.NONE) {
        val npc = NPCClient(id, name, type, x, y, currentHealth, maxHealth, level, false, faction)
        npcs[id] = npc
        println("Respawn NPC: $name, faction: ${faction.displayName}")
    }

    fun markNPCAsDead(npcId: String) {
        npcs[npcId]?.let { npc ->
            npcs[npcId] = NPCClient(npc.id, npc.name, npc.type, npc.x, npc.y, 0, npc.maxHealth, npc.level, false, npc.faction)
        }
    }

    // Wyświetla notyfikację graczowi
    fun showNotification(message: String, type: String) {
        val senderId = "system"
        val senderName = "System"

        val formattedMessage = when (type) {
            "levelup" -> message
            else -> message
        }

        println(formattedMessage)
        receiveNetworkChatMessage(senderId, senderName, formattedMessage)
    }

    fun startLogoutCountdown() {
        networkScope.launch {
            for (i in 5 downTo 1) {
                Gdx.app.postRunnable {
                    receiveNetworkChatMessage("SYSTEM", "System", "Wylogowanie za $i...")
                }
                delay(1000L)
            }
            Gdx.app.postRunnable {
                logoutToCharacterSelection()
            }
        }
    }

    private fun logoutToCharacterSelection() {
        // Wyślij informację do serwera
        session?.let {
            networkScope.launch {
                try {
                    it.send(Frame.Text("LEAVE_WORLD|${localPlayer.id}"))
                    delay(100) // Daj czas na wysłanie
                } catch (e: Exception) {
                    println("Błąd przy wysyłaniu LEAVE_WORLD: ${e.message}")
                } finally {
                    closeConnection()
                }
            }
        }

        removePlayer(localPlayer.id)
        showCharacterSelectionScreen()
    }

    fun exitGame() {
        // Wyślij informację do serwera przed wyjściem
        session?.let {
            networkScope.launch {
                try {
                    it.send(Frame.Text("LEAVE_WORLD|${localPlayer.id}"))
                    delay(100) // Daj czas na wysłanie
                } catch (e: Exception) {
                    println("Błąd przy wysyłaniu LEAVE_WORLD podczas wyjścia: ${e.message}")
                } finally {
                    closeConnection()
                    Gdx.app.exit()
                }
            }
        } ?: run {
            Gdx.app.exit()
        }
    }

    // Aktualizuje pozycję gracza
    fun updatePlayerPosition(id: String, x: Float, y: Float) {
        players[id]?.setMoveTarget(x, y)
    }

    //Obsługuje nieudany ruch gracza
    fun handleMoveFailed(playerId: String) {
        if (playerId == localPlayerId) {
            // Anuluj ruch lokalnego gracza
            localPlayer.setMoveTarget(localPlayer.x, localPlayer.y)
            messageManager.showMessage("Nie mogę tam dojść!", 2f, Color.YELLOW)
        }
    }

    // Usuwa gracza z gry
    fun removePlayer(id: String) {
        if (players.containsKey(id)) {
            players.remove(id)
            println("Gracz $username wylogował się z gry (players size=${players.size})")
        } else {
            println("Nie znaleziono gracza $username do wylogowania (players size=${players.size})")
        }
    }

    // Aktualizuje zdrowie gracza
    fun updatePlayerHealth(playerId: String, currentHealth: Int, maxHealth: Int) {
        players[playerId]?.let { player ->
            player.currentHealth = currentHealth
            player.maxHealth = maxHealth
        }
    }

    // Aktualizuje zdrowie gracza
    fun updatePlayerMana(playerId: String, currentMana: Int, maxMana: Int) {
        players[playerId]?.let { player ->
            player.currentMana = currentMana
            player.maxMana = maxMana
        }
    }

    // Bezpośrednio ustawia zdrowie przeciwnika
    fun updateEnemyHealthExplicit(enemyId: String, currentHealth: Int, maxHealth: Int) {
        enemies[enemyId]?.let { enemy ->
            enemy.currentHealth = currentHealth
            enemy.maxHealth = maxHealth
            if (currentHealth <= 0) {
                enemy.markAsDead()
            } else {
                enemy.isAlive = true
            }
        }
    }

    // Pobiera gracza po ID
    fun getPlayer(playerId: String): Player? {
        return players[playerId]
    }

    // Pobiera przeciwnika po ID
    fun getEnemy(enemyId: String): EnemyClient? {
        return enemies[enemyId]
    }

    // Dodaje efekt tekstowy obrażeń
    fun addDamageText(x: Float, y: Float, text: String, color: Color) {
        playerController.addDamageText(x, y, text, color)
    }

    // Obsługuje śmierć gracza
    fun handlePlayerDeath(playerId: String) {
        if (playerId == localPlayerId) {
            Gdx.app.postRunnable {
                changeState(DeadState(this))
                deathScreen?.show()
            }
        }
    }

    // Dodaje dropnięty item do świata gry
    fun addDroppedItem(dropId: String, itemId: String, x: Float, y: Float) {

        val droppedItem = ItemDrop(
            itemId = itemId,
            x = x,
            y = y,
            itemDefinition = itemManager.getItemDefinition(itemId),
            itemManager = itemManager
        )

        // Ustaw konkretny dropId zamiast automatycznego
        droppedItem.id = dropId

        droppedItems[dropId] = droppedItem
    }

    // Dodaj też metodę clearDroppedItems
    fun clearDroppedItems() {
        droppedItems.clear()
    }

    // Próbuje podnieść item z ziemi
    private fun tryPickupItem(itemDropId: String): Boolean {

        val droppedItem = droppedItems[itemDropId] ?: run {
            return false
        }

        if (!droppedItem.isInRange(localPlayer.x, localPlayer.y)) {
            messageManager.showMessage("Za daleko od itemu!", 2f, Color.RED)
            return false
        }

        println("DEBUG: Sending PICKUP_ITEM message: PICKUP_ITEM|${localPlayer.id}|$itemDropId|${droppedItem.itemId}")
        sendWebSocketMessage("PICKUP_ITEM|${localPlayer.id}|$itemDropId|${droppedItem.itemId}")

        return true
    }

    // Usuwa item z ziemi (po udanym podniesieniu)
    fun removeDroppedItem(itemDropId: String) {
        droppedItems.remove(itemDropId)?.let { item ->
            println("Client: Usunięto dropnięty item ${item.getDisplayName()}")
        }
    }

    // pozycja gracza po respie
    fun respawnPlayerWithPosition(playerId: String, currentHealth: Int, maxHealth: Int, x: Float, y: Float) {
        players[playerId]?.let { player ->
            player.currentHealth = currentHealth
            player.maxHealth = maxHealth
            player.x = x
            player.y = y

            if (playerId == localPlayerId) {
                localPlayer.x = x
                localPlayer.y = y
                camera.position.set(x, y, 0f)
                camera.update()

                //spam
                resetMovementPosition(x, y)

                //  zmiana stanu gry
                if (currentState is DeadState && playerId == localPlayerId) {
                    Gdx.app.postRunnable {
                        changeState(PlayingState(this))
                    }
                }
            }
        }
    }

    // Obsługuje odrodzenie gracza
    fun respawnPlayerHealth(playerId: String, currentHealth: Int, maxHealth: Int) {
        players[playerId]?.let { player ->
            player.currentHealth = currentHealth
            player.maxHealth = maxHealth
        }
    }

    // Obsługuje wiadomość o ataku
    fun handleAttackMessage(attackType: String, parts: List<String>) {
        playerController.handleMessage(attackType, parts)
        if (::skileManager.isInitialized) {
            skileManager.handleSkillMessage(attackType, parts)
        }
    }

    // Aktualizuje dane ścieżki pathfinding
    fun updatePathTiles(pathData: List<Pair<Int, Int>>) {
        pathTiles.clear()
        pathTiles.addAll(pathData)
    }

    fun sendWebSocketMessage(message: String) {
        networkScope.launch {
            try {
                val currentSession = session
                if (currentSession != null) {
                    currentSession.send(Frame.Text(message))
                } else {
                    Gdx.app.error("WebSocket", "Cannot send message - no active session")
                }
            } catch (e: Exception) {
                Gdx.app.error("WebSocket", "Error sending message: ${e.message}")
            }
        }
    }

    // Metoda wywoływana po pomyślnym wyborze postaci
    fun startGame(userUsername: String, userId: String, characterClass: Int = 2, nickname: String = userUsername, level: Int = 1, experience: Int = 0) {
        networkScope.coroutineContext.cancelChildren()
        networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        username = userUsername
        localPlayerId = userId

        // tekstury pociskow
        ProjectileTextures.load()

        // Tworzenie gracza z wybraną klasą postaci i nickiem
        localPlayer = Player(
            id = localPlayerId,
            username = nickname,
            characterClass = characterClass,
            level = level,
            experience = experience
        )

        // Inicjalizacja czatu
        chatSystem = ChatSystem(
            localPlayerId = localPlayerId,
            username = nickname,
            networkScope = networkScope,
            getSession = { session }
        )

        // Dodajemy lokalnego gracza do mapy
        players[localPlayerId] = localPlayer

        // Inicjalizacja menedżera umiejętności
        skileManager = SkileManager(
            localPlayerId = localPlayerId,
            enemies = enemies,
            networkScope = networkScope,
            getSession = { session }
        )

        // Inicjalizacja GameUI
        val gameUIInstance = GameUI(this)
        gameUI = gameUIInstance

        //itemy
        gameUI!!.connectItemManager()

        // Inicjalizacja obsługi gracza i umiejętności
        playerController = PlayerController(
            localPlayer = localPlayer,
            players = players,
            enemies = enemies,
            npcs = npcs,
            droppedItems = droppedItems,
            camera = camera,
            uiCamera = uiCamera,
            characterClass = when (localPlayer.characterClass) {
                0 -> Archer(localPlayer, networkScope, { session }, skileManager, messageManager)
                1 -> Mage(localPlayer, networkScope, { session }, skileManager, messageManager)
                else -> Warrior(localPlayer, networkScope, { session }, skileManager, messageManager)
            },
            skileManager = skileManager,
            font = font,
            gameUI = gameUIInstance,
            chatSystem = chatSystem,
            movementController = movementController,
            playerSkinManager = playerSkinManager,
            menuToggle = { menu.toggle() },
            isMenuVisible = { menu.isVisible() },
            getDroppedItems = { droppedItems },
            onPathfindRequested = { startX, startY, endX, endY ->
                networkScope.launch {
                    session?.send(Frame.Text("PATHFIND|$startX|$startY|$endX|$endY"))
                }
            }
        )

        // callback podchodzenia do itemów
        playerController.setItemPickupCallback { itemId -> tryPickupItem(itemId) }

        // Uruchomienie połączenia websocket
        connectToServer()

        // Zmiana stanu gry
        changeState(LoadingState(this))
    }

    private fun connectToServer() {
        networkScope.launch {
            try {
                client = HttpClient(CIO) {
                    install(WebSockets)
                }

                client?.webSocket("ws://$IP_ADDRESS/ws") {
                    session = this

                    // Podstawowe dane
                    val playerId = localPlayer.id
                    send(Frame.Text("JOIN|0|0|$playerId|$username"))

                    // Załaduj eq
                    send(Frame.Text("GET_PLAYER_EQUIPMENT|$playerId"))

                    // Załaduj przeciwników
                    send(Frame.Text("GET_ENEMIES"))

                    // Załaduj itemy na ziemi
                    send(Frame.Text("GET_DROPPED_ITEMS"))

                    // Załaduj NPC
                    send(Frame.Text("GET_NPCS"))

                    // Odbieraj wiadomości
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            processMessage(message)
                        }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("WebSocket", "Błąd połączenia: ${e.message}")
            }
        }
    }

    // zamyka połączenie bez reconnect
    private suspend fun closeConnection() {
        try {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Logout"))
            println("Sesja została zamknięta")
        } catch (e: Exception) {
            println("Błąd przy zamykaniu sesji: ${e.message}")
        }

        session = null
        client?.close()
        client = null
    }

    private fun processMessage(message: String) {
        messageManager.processMessage(message)
    }

    override fun render() {

        // Aktualizacja animacji enemy skinów
        enemySkinManager.update(Gdx.graphics.deltaTime)

        // Aktualizacja animacji broni
        playerSkinManager.update(Gdx.graphics.deltaTime)

        // Czyścimy ekran
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Obsługa wejścia aktualnego stanu
        currentState.handleInput()

        // Aktualizacja logiki
        updateEnemies(Gdx.graphics.deltaTime)

        // DropItems
        droppedItems.values.forEach { item ->
            item.update(Gdx.graphics.deltaTime)
        }

        currentState.update(Gdx.graphics.deltaTime)

        // Renderowanie aktualnego stanu
        currentState.render(Gdx.graphics.deltaTime)

        // Renderujemy menu
        menu.render(Gdx.graphics.deltaTime)
    }

    // podazanie za graczem
    fun updateCamera() {
        // Interpolacja liniowa dla płynnego śledzenia
        val lerpFactor = 0.5f
        val targetX = localPlayer.x
        val targetY = localPlayer.y

        camera.position.x += (targetX - camera.position.x) * lerpFactor
        camera.position.y += (targetY - camera.position.y) * lerpFactor
    }

    // Respawn graczy
    fun respawnPlayer() {
        Gdx.app.log("Respawn", "Respawn requested")

        // Pobierz aktualny stan gry
        val currentDeadState = currentState as? DeadState

        if (currentDeadState != null) {
            currentDeadState.handleRespawn()
        } else {
            Gdx.app.error("Respawn", "Cannot respawn - not in death state")
        }
    }

    override fun resize(width: Int, height: Int) {
        // Aktualizuj kamerę świata
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()

        // Aktualizuj kamerę UI
        uiCamera.viewportWidth = width.toFloat()
        uiCamera.viewportHeight = height.toFloat()
        uiCamera.position.set(uiCamera.viewportWidth / 2, uiCamera.viewportHeight / 2, 0f)
        uiCamera.update()

        currentState.resize(width, height)
        menu.resize(width, height)
    }

    override fun dispose() {
        println("Aplikacja się zamyka...")

        // Wyślij informację do serwera o opuszczeniu świata
        if (::localPlayer.isInitialized) {
            session?.let {
                try {
                    runBlocking {
                        it.send(Frame.Text("LEAVE_WORLD|${localPlayer.id}"))
                        delay(50)
                    }
                    println("Wysłano LEAVE_WORLD dla gracza ${localPlayer.id}")
                } catch (e: Exception) {
                    println("Błąd przy wysyłaniu LEAVE_WORLD w dispose: ${e.message}")
                }
            }
        }

        // Standardowe sprzątanie zasobów - sprawdź inicjalizację każdego
        if (::batch.isInitialized) batch.dispose()
        if (::uiBatch.isInitialized) uiBatch.dispose()
        if (::shapeRenderer.isInitialized) shapeRenderer.dispose()
        if (::font.isInitialized) font.dispose()

        loginScreen?.dispose()
        characterSelectionScreen?.dispose()
        characterCreationScreen?.dispose()
        deathScreen?.dispose()

        if (::gameMap.isInitialized) gameMap.dispose()
        if (::itemManager.isInitialized) itemManager.dispose()
        if (::menu.isInitialized) menu.dispose()
        if (::enemySkinManager.isInitialized) enemySkinManager.dispose()
        if (::playerSkinManager.isInitialized) playerSkinManager.dispose()

        // Sprawdź czy statyczne obiekty są zainicjalizowane
        runCatching { UISkin.dispose() }
        runCatching { ProjectileTextures.dispose() }

        // Zamknięcie połączenia websocket
        runBlocking {
            try {
                session?.close(CloseReason(CloseReason.Codes.NORMAL, "App dispose"))
                client?.close()
            } catch (e: Exception) {
                println("Błąd przy zamykaniu połączenia: ${e.message}")
            }
        }

        // Anulowanie wszystkich koroutyn
        if (::networkScope.isInitialized) {
            networkScope.cancel()
        }
    }

    // Punkt wejścia dla aplikacji desktopowej
    object Launcher {
        @JvmStatic
        fun main(args: Array<String>) {
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("MMO Game")
            //config.setWindowedMode(1000, 800)
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
            config.setForegroundFPS(60)
            Lwjgl3Application(MMOGame(), config)
        }
    }
}