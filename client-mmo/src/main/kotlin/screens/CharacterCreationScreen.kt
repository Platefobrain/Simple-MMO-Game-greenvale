package pl.decodesoft.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.decodesoft.MMOGame
import pl.decodesoft.Strings.IP_ADDRESS
import pl.decodesoft.player.skin.Direction
import pl.decodesoft.player.skin.PlayerSkinManager

@Serializable
data class CharacterCreateRequest(
    val userId: String,
    val characterClass: Int,
    val nickname: String,
    val slotIndex: Int,
    val faction: String,
    val race: String
)

@Serializable
data class CharacterCreateResponse(
    val success: Boolean,
    val message: String
)

class CharacterCreationScreen(
    private val game: MMOGame,
    private val userId: String,
    private val username: String,
    private val slotIndex: Int
) : Screen {
    private lateinit var stage: Stage
    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var font: BitmapFont
    private lateinit var smallFont: BitmapFont
    private lateinit var skin: Skin
    private var skinPreviewBatch: SpriteBatch? = null
    private var skinManager: PlayerSkinManager? = null

    private lateinit var archerTexture: Texture
    private lateinit var mageTexture: Texture
    private lateinit var warriorTexture: Texture
    private lateinit var archerLogoTexture: Texture
    private lateinit var mageLogoTexture: Texture
    private lateinit var warriorLogoTexture: Texture
    private lateinit var goblinTexture: Texture
    private lateinit var humanTexture: Texture
    private lateinit var undeadTexture: Texture
    private lateinit var elfTexture: Texture

    private var selectedClass = 0
    private var selectedFaction = "NONE"
    private var selectedRace = "NONE"
    private var nicknameField: TextField? = null
    private var playerNickname: String = username

    private lateinit var characterImageContainer: Container<Actor>

    private var creationScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun show() {
        camera = OrthographicCamera()
        viewport = FitViewport(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera)
        batch = SpriteBatch()

        skinPreviewBatch = SpriteBatch()
        skinManager = PlayerSkinManager()

        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"))

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 32
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE
        }
        font = generator.generateFont(parameter)

        val smallParameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 20
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
            color = Color.WHITE
        }
        smallFont = generator.generateFont(smallParameter)

        generator.dispose()

        try {
            archerTexture = Texture(Gdx.files.internal("textures/archer/archer.png"))
            mageTexture = Texture(Gdx.files.internal("textures/mage/mage.png"))
            warriorTexture = Texture(Gdx.files.internal("textures/warrior/warrior.png"))

            archerLogoTexture = Texture(Gdx.files.internal("textures/archer/archer_logo.png"))
            mageLogoTexture = Texture(Gdx.files.internal("textures/mage/mage_logo.png"))
            warriorLogoTexture = Texture(Gdx.files.internal("textures/warrior/warrior_logo.png"))

            goblinTexture = Texture(Gdx.files.internal("textures/races/goblin.png"))
            humanTexture = Texture(Gdx.files.internal("textures/races/human.png"))
            undeadTexture = Texture(Gdx.files.internal("textures/races/undead.png"))
            elfTexture = Texture(Gdx.files.internal("textures/races/elf.png"))
        } catch (e: Exception) {
            Gdx.app.error("CharacterCreation", "Nie można załadować tekstur: ${e.message}")
        }

        stage = Stage(viewport, batch)
        Gdx.input.inputProcessor = stage

        skin = runCatching {
            Skin(Gdx.files.internal("assets/uiskin.json")).also {
                if (!it.has("title", Label.LabelStyle::class.java)) {
                    val titleStyle = Label.LabelStyle().apply {
                        font = this@CharacterCreationScreen.font
                        fontColor = Color(1f, 0.9f, 0.6f, 1f)
                    }
                    it.add("title", titleStyle)
                }
            }
        }.getOrElse {
            createBasicSkin()
        }

        createUI()
    }

    private fun createBasicSkin(): Skin {
        val skin = Skin()

        skin.add("default-font", font)
        skin.add("small-font", smallFont)

        val textButtonStyle = TextButton.TextButtonStyle().apply {
            font = smallFont
            fontColor = Color.WHITE
            downFontColor = Color.LIGHT_GRAY
            up = skin.newDrawable("white", Color(0.2f, 0.2f, 0.2f, 0.8f))
            down = skin.newDrawable("white", Color(0.3f, 0.3f, 0.3f, 0.9f))
            over = skin.newDrawable("white", Color(0.4f, 0.4f, 0.4f, 0.9f))
        }
        skin.add("default", textButtonStyle)

        val labelStyle = Label.LabelStyle().apply {
            font = smallFont
            fontColor = Color.WHITE
        }
        skin.add("default", labelStyle)

        val titleStyle = Label.LabelStyle().apply {
            font = this@CharacterCreationScreen.font
            fontColor = Color(1f, 0.9f, 0.6f, 1f)
        }
        skin.add("title", titleStyle)

        val textFieldStyle = TextField.TextFieldStyle().apply {
            font = smallFont
            fontColor = Color.WHITE
            background = skin.newDrawable("white", Color(0.2f, 0.2f, 0.2f, 1f))
            cursor = skin.newDrawable("white", Color.WHITE)
            selection = skin.newDrawable("white", Color(0.3f, 0.3f, 0.7f, 1f))
        }
        skin.add("default", textFieldStyle)

        return skin
    }

    private fun createUI() {
        stage.clear()

        val mainTable = Table()
        mainTable.setFillParent(true)
        mainTable.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))

        // LEWY PANEL - Opis wybranej klasy
        val leftPanel = Table()
        leftPanel.background = skin.newDrawable("white", Color(0.149f, 0.149f, 0.149f, 1f))
        leftPanel.align(Align.top)

        val classInfoTable = Table()
        classInfoTable.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))
        classInfoTable.pad(20f)
        classInfoTable.align(Align.top)

        val classNameLabel = Label(getClassName(selectedClass), skin, "title")
        classNameLabel.setFontScale(1.0f)
        classNameLabel.color = getClassColor(selectedClass)
        classInfoTable.add(classNameLabel).pad(10f).top()
        classInfoTable.row()

        val classDesc = Label(getClassDescription(selectedClass), skin)
        classDesc.wrap = true
        classDesc.setAlignment(Align.topLeft)
        classInfoTable.add(classDesc).width(260f).expandY().fillY().pad(10f).top()

        leftPanel.add(classInfoTable).expand().fill().pad(10f, 10f, 10f, 10f)

        // ŚRODKOWY PANEL - Podgląd postaci
        val centerPanel = Table()
        centerPanel.align(Align.center)

        characterImageContainer = Container<Actor>()
        updateCharacterImage()
        centerPanel.add(characterImageContainer).size(400f, 400f).center()
        centerPanel.row()

        // Pole nicku
        val nicknameTable = Table()
        val nicknameLabel = Label("Nazwa postaci:", skin)
        nicknameField = TextField(username, skin)
        nicknameField?.maxLength = 20
        nicknameField?.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                playerNickname = nicknameField?.text ?: username
            }
        })

        nicknameTable.add(nicknameLabel).padRight(10f)
        nicknameTable.add(nicknameField).width(200f)
        centerPanel.add(nicknameTable).pad(20f)
        centerPanel.row()

        // Przyciski akcji
        val createButton = TextButton("Stwórz postać", skin)
        createButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                createCharacter()
            }
        })
        centerPanel.add(createButton).width(300f).height(60f).pad(10f)

        // PRAWY PANEL - Wybór rasy i klasy
        val rightPanel = Table()
        rightPanel.background = skin.newDrawable("white", Color(0.149f, 0.149f, 0.149f, 1f))
        rightPanel.align(Align.top)

        // === SEKCJA WYBORU RASY ===
        val raceTitle = Label("Wybór rasy", skin, "title")
        raceTitle.setFontScale(0.8f)
        rightPanel.add(raceTitle).pad(20f, 10f, 15f, 10f).top().colspan(2)
        rightPanel.row()

        // Tabela z dwoma kolumnami dla ras
        val raceTable = Table()
        raceTable.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))
        raceTable.pad(10f)

        // Lewa kolumna - Zakon (Człowiek i Elf)
        val zakonColumn = Table()
        val zakonLabel = Label("Zakon", skin)
        zakonLabel.color = Color(0.2f, 0.4f, 0.8f, 1f)
        zakonColumn.add(zakonLabel).pad(5f)
        zakonColumn.row()

        val humanRaceItem = createRaceItem(humanTexture, "HUMAN", "ZAKON")
        zakonColumn.add(humanRaceItem).size(100f).pad(5f)
        zakonColumn.row()

        val elfRaceItem = createRaceItem(elfTexture, "ELF", "ZAKON")
        zakonColumn.add(elfRaceItem).size(100f).pad(5f)

        // Prawa kolumna - Wataha (Goblin i Undead)
        val watahaColumn = Table()
        val watahaLabel = Label("Wataha", skin)
        watahaLabel.color = Color(0.8f, 0.3f, 0.1f, 1f)
        watahaColumn.add(watahaLabel).pad(5f)
        watahaColumn.row()

        val goblinRaceItem = createRaceItem(goblinTexture, "GOBLIN", "WATAHA")
        watahaColumn.add(goblinRaceItem).size(100f).pad(5f)
        watahaColumn.row()

        val undeadRaceItem = createRaceItem(undeadTexture, "UNDEAD", "WATAHA")
        watahaColumn.add(undeadRaceItem).size(100f).pad(5f)

        raceTable.add(zakonColumn).pad(10f)
        raceTable.add(watahaColumn).pad(10f)

        rightPanel.add(raceTable).pad(10f).colspan(2)
        rightPanel.row()

        val separator1 = Table()
        separator1.background = skin.newDrawable("white", Color(0.3f, 0.3f, 0.3f, 1f))
        rightPanel.add(separator1).height(2f).width(280f).pad(15f, 0f, 15f, 0f).colspan(2)
        rightPanel.row()

        // === SEKCJA WYBORU KLASY ===
        val classTitle = Label("Wybierz klasę", skin, "title")
        classTitle.setFontScale(0.8f)
        rightPanel.add(classTitle).pad(10f, 10f, 15f, 10f).top().colspan(2)
        rightPanel.row()

        val classListTable = Table()
        classListTable.background = skin.newDrawable("white", Color(0.1176f, 0.1176f, 0.1176f, 1f))
        classListTable.pad(10f)
        classListTable.align(Align.top)

        val archerLogo = createClassLogoItem(0, archerLogoTexture)
        val mageLogo = createClassLogoItem(1, mageLogoTexture)
        val warriorLogo = createClassLogoItem(2, warriorLogoTexture)

        classListTable.add(archerLogo).size(90f).pad(5f)
        classListTable.add(mageLogo).size(90f).pad(5f)
        classListTable.add(warriorLogo).size(90f).pad(5f)
        classListTable.row()

        rightPanel.add(classListTable).pad(10f).colspan(2)
        rightPanel.row()

        rightPanel.add().expand().fill().colspan(2)
        rightPanel.row()

        // Przycisk powrotu
        val buttonPanel = Table()
        val backButton = TextButton("Anuluj", skin)
        backButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.showCharacterSelectionScreen()
            }
        })
        buttonPanel.add(backButton).width(280f).height(40f).pad(5f)
        rightPanel.add(buttonPanel).pad(10f).bottom().colspan(2)

        // Dodanie paneli do głównej tabeli
        mainTable.add(leftPanel).width(350f).expandY().fillY().pad(10f)
        mainTable.add(centerPanel).expand().fill()
        mainTable.add(rightPanel).width(350f).expandY().fillY().pad(10f)

        stage.addActor(mainTable)
    }

    private fun createRaceItem(raceTexture: Texture, raceId: String, factionId: String): Container<Image> {
        val container = Container<Image>()

        val isSelected = selectedRace == raceId

        val backgroundColor = if (isSelected) {
            when (factionId) {
                "WATAHA" -> Color(0.6f, 0.25f, 0.12f, 1f)
                "ZAKON" -> Color(0.2f, 0.35f, 0.65f, 1f)
                else -> Color(0.4f, 0.5f, 0.4f, 1f)
            }
        } else {
            Color(0.2f, 0.2f, 0.25f, 0.8f)
        }

        container.background = skin.newDrawable("white", backgroundColor)

        if (::goblinTexture.isInitialized && ::humanTexture.isInitialized && ::undeadTexture.isInitialized && ::elfTexture.isInitialized) {
            val image = Image(raceTexture)
            container.actor = image
            container.fill()
        } else {
            val fallbackColor = when (factionId) {
                "WATAHA" -> Color(0.8f, 0.3f, 0.1f, 1f)
                "ZAKON" -> Color(0.2f, 0.4f, 0.8f, 1f)
                else -> Color(0.5f, 0.5f, 0.5f, 1f)
            }
            val placeholder = Image(skin.newDrawable("white", fallbackColor))
            container.actor = placeholder
            container.fill()
        }

        container.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
        container.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
            override fun touchDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return true
            }

            override fun touchUp(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                selectedRace = raceId
                selectedFaction = factionId
                createUI()
            }
        })

        return container
    }

    private fun createClassLogoItem(classIndex: Int, logoTexture: Texture): Container<Image> {
        val container = Container<Image>()

        val isSelected = selectedClass == classIndex

        // Jeśli wybrany - lekkie białe tło, jeśli nie - przezroczyste
        val backgroundColor = if (isSelected) {
            Color(1f, 1f, 1f, 0.05f)
        } else {
            Color(0f, 0f, 0f, 0f)
        }

        container.background = skin.newDrawable("white", backgroundColor)

        if (::archerLogoTexture.isInitialized && ::mageLogoTexture.isInitialized && ::warriorLogoTexture.isInitialized) {
            val image = Image(logoTexture)
            container.actor = image
            container.fill()
        } else {
            val placeholder = Image(skin.newDrawable("white", getClassColor(classIndex)))
            container.actor = placeholder
            container.fill()
        }

        container.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
        container.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
            override fun touchDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return true
            }

            override fun touchUp(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                selectedClass = classIndex
                createUI()
            }
        })

        return container
    }

    private fun updateCharacterImage() {
        characterImageContainer.clear()

        if (selectedRace != "NONE" && skinManager != null) {
            // Stwórz custom actor do renderowania skina
            val skinPreviewActor = object : Actor() {
                override fun draw(batch: Batch?, parentAlpha: Float) {
                    batch?.end()

                    // Użyj osobnego batcha dla skina
                    skinPreviewBatch?.let { skinBatch ->
                        if (batch != null) {
                            skinBatch.projectionMatrix = batch.projectionMatrix
                        }
                        skinBatch.begin()

                        // Stwórz tymczasowego gracza do podglądu
                        val previewRace = when (selectedRace) {
                            "GOBLIN" -> pl.decodesoft.player.Race.GOBLIN
                            "HUMAN" -> pl.decodesoft.player.Race.HUMAN
                            "UNDEAD" -> pl.decodesoft.player.Race.UNDEAD
                            "ELF" -> pl.decodesoft.player.Race.ELF
                            else -> pl.decodesoft.player.Race.HUMAN
                        }

                        // Pobierz skin z cache
                        val skin = skinManager!!.getSkinForRace(previewRace, selectedClass)

                        // Renderuj w centrum aktora
                        val centerX = x + width / 2
                        val centerY = y + height / 2

                        skin?.renderIdle(skinBatch, centerX, centerY, 3f, Direction.DOWN)

                        skinBatch.end()
                    }

                    batch?.begin()
                }
            }

            skinPreviewActor.setSize(400f, 400f)
            characterImageContainer.actor = skinPreviewActor
            characterImageContainer.size(400f, 400f)
            characterImageContainer.fill()
            return
        }

        // Fallback - użyj tekstur ras jeśli dostępne
        if (selectedRace != "NONE") {
            val raceTexture = when (selectedRace) {
                "GOBLIN" -> goblinTexture
                "HUMAN" -> humanTexture
                "UNDEAD" -> undeadTexture
                "ELF" -> elfTexture
                else -> null
            }

            if (raceTexture != null && ::goblinTexture.isInitialized) {
                val image = Image(raceTexture)
                characterImageContainer.actor = image
                characterImageContainer.size(400f, 400f)
                return
            }
        }

        // Ostateczny fallback - tekstury klas
        if (::archerTexture.isInitialized && ::mageTexture.isInitialized && ::warriorTexture.isInitialized) {
            val texture = when (selectedClass) {
                0 -> archerTexture
                1 -> mageTexture
                else -> warriorTexture
            }
            val image = Image(texture)
            characterImageContainer.actor = image
            characterImageContainer.size(400f, 400f)
        } else {
            val placeholder = Table()
            placeholder.background = skin.newDrawable("white", getClassColor(selectedClass))
            characterImageContainer.actor = placeholder
            characterImageContainer.size(400f, 400f)
        }
    }

    private fun getClassName(classIndex: Int): String {
        return when (classIndex) {
            0 -> "Łucznik"
            1 -> "Mag"
            else -> "Wojownik"
        }
    }

    private fun getClassDescription(classIndex: Int): String {
        return when (classIndex) {
            0 -> """
                Łucznik to mistrz walki na dystans, specjalizujący się w precyzyjnych atakach z łuku. 
                
                Jego strzały są śmiertelnie skuteczne przeciwko pojedynczym celom. Posiada wysoką zwinność, 
                co pozwala mu unikać ataków wrogów i szybko zmieniać pozycję na polu bitwy.
                
                Idealny dla graczy ceniących strategiczne podejście i wysokie obrażenia.
            """.trimIndent()
            1 -> """
                Mag włada potężną magią, która może zmienić bieg każdej bitwy. 
                
                Specjalizuje się w zaklęciach obszarowych, które zadają obrażenia wielu wrogom jednocześnie. 
                Potrafi kontrolować pole bitwy poprzez efekty kontroli i osłabienia przeciwników.
                
                Wybór dla graczy lubiących taktyczne podejście i niszczycielską moc magii.
            """.trimIndent()
            else -> """
                Wojownik to wytrzymały czempion walki wręcz, który staje na pierwszej linii frontu.
                
                Posiada najwyższe punkty zdrowia i może przyjmować na siebie najwięcej obrażeń, 
                chroniąc tym samym swoich sojuszników. Jego siła w bezpośrednim starciu jest niezrównana.
                
                Doskonały wybór dla tych, którzy lubią być w centrum akcji i chronić drużynę.
            """.trimIndent()
        }
    }

    private fun getClassColor(classIndex: Int): Color {
        return when (classIndex) {
            0 -> Color(0.67f, 0.83f, 0.45f, 1f)
            1 -> Color(0.41f, 0.8f, 0.94f, 1f)
            else -> Color(0.78f, 0.61f, 0.43f, 1f)
        }
    }

    private fun createCharacter() {
        if (playerNickname.isBlank() || playerNickname.length < 3) {
            showError("Nazwa postaci musi mieć co najmniej 3 znaki")
            return
        }

        if (selectedFaction == "NONE" || selectedRace == "NONE") {
            showError("Musisz wybrać rasę!")
            return
        }

        creationScope.launch {
            try {
                val response = httpClient.post("http://$IP_ADDRESS/character/create") {
                    contentType(ContentType.Application.Json)
                    setBody(CharacterCreateRequest(
                        userId,
                        selectedClass,
                        playerNickname,
                        slotIndex,
                        selectedFaction,
                        selectedRace
                    ))
                }

                val createResponse = Json.decodeFromString<CharacterCreateResponse>(response.bodyAsText())

                if (createResponse.success) {
                    Gdx.app.postRunnable {
                        game.showCharacterSelectionScreen()
                    }
                } else {
                    Gdx.app.error("CharacterCreation", "Błąd tworzenia postaci: ${createResponse.message}")
                    withContext(Dispatchers.Default) {
                        showError(createResponse.message)
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("CharacterCreation", "Błąd połączenia: ${e.message}")
                withContext(Dispatchers.Default) {
                    showError("Błąd połączenia z serwerem")
                }
            }
        }
    }

    private fun showError(message: String) {
        val dialog = Dialog("Błąd", skin)
        dialog.text(message)
        dialog.button("OK")
        dialog.show(stage)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        // Update animacji skinów
        skinManager?.update(delta)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        stage.dispose()
        batch.dispose()
        skinPreviewBatch?.dispose()
        skinManager?.dispose()
        font.dispose()
        smallFont.dispose()

        if (::archerTexture.isInitialized) archerTexture.dispose()
        if (::mageTexture.isInitialized) mageTexture.dispose()
        if (::warriorTexture.isInitialized) warriorTexture.dispose()
        if (::archerLogoTexture.isInitialized) archerLogoTexture.dispose()
        if (::mageLogoTexture.isInitialized) mageLogoTexture.dispose()
        if (::warriorLogoTexture.isInitialized) warriorLogoTexture.dispose()
        if (::goblinTexture.isInitialized) goblinTexture.dispose()
        if (::humanTexture.isInitialized) humanTexture.dispose()
        if (::undeadTexture.isInitialized) undeadTexture.dispose()
        if (::elfTexture.isInitialized) elfTexture.dispose()

        creationScope.cancel()
        httpClient.close()
    }
}