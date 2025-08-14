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

package pl.decodesoft.msg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.StringBuilder
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class ChatSystem(
    private val localPlayerId: String,
    private val username: String,
    private val networkScope: CoroutineScope,
    private val getSession: () -> DefaultWebSocketSession?
) {
    companion object {
        private const val MAX_MESSAGES = 20
        private const val MAX_MESSAGE_LIFETIME = 20f
        private const val MAX_DISPLAY_LINES = 9
        private const val FADE_DURATION = 3f
        private const val MAX_INPUT_LENGTH = 200
        private const val LINE_HEIGHT_MULTIPLIER = 1.2f
        private const val CHAT_WIDTH = 580f
        private const val SCROLL_SPEED = 1

        // buttons
        private const val BUTTON_WIDTH = 60f
        private const val BUTTON_HEIGHT = 25f
        private const val BUTTONS_Y_OFFSET = 255f
        private const val BUTTONS_X_OFFSET = 6f
    }

    private val activeMessages = ConcurrentLinkedQueue<ChatMessage>()
    private val allMessages = mutableListOf<ChatMessage>()
    private val logMessages = mutableListOf<ChatMessage>()
    private val isTyping = AtomicBoolean(false)
    private val currentInput = StringBuilder()
    private val chatInputProcessor = ChatInputProcessor()
    private var originalInputProcessor: com.badlogic.gdx.InputProcessor? = null
    private val glyphLayout = GlyphLayout()

    // Zmienne do obsługi przewijania
    private var scrollOffset = 0
    private var maxScrollOffset = 0
    private var allProcessedLines = mutableListOf<Pair<ChatMessage, String>>()

    // Lepsze zarządzanie stanem klawiatury
    private var enterJustReleased = false
    private var wasEnterDown = false

    // Zmienne dla trybu wyświetlania
    private var currentMode = ChatMode.CHAT
    private var mouseX = 0f
    private var mouseY = 0f

    enum class ChatMode {
        CHAT, LOG
    }

    data class ChatMessage(
        val senderId: String,
        val senderName: String,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
        var lifetime: Float = 0f,
        val messageType: MessageType = MessageType.PLAYER,
        val isLogMessage: Boolean = false
    )

    enum class MessageType {
        PLAYER, SYSTEM, LOG
    }

    inner class ChatInputProcessor : InputAdapter() {
        override fun keyTyped(character: Char): Boolean {
            if (currentMode == ChatMode.LOG) return false

            if (character >= ' ' && currentInput.length < MAX_INPUT_LENGTH) {
                currentInput.append(character)
                return true
            }
            return false
        }

        override fun keyDown(keycode: Int): Boolean {
            when (keycode) {
                Input.Keys.BACKSPACE -> {
                    if (currentMode == ChatMode.CHAT && currentInput.isNotEmpty()) {
                        currentInput.deleteCharAt(currentInput.length - 1)
                    }
                    return true
                }
                Input.Keys.ESCAPE -> {
                    currentInput.clear()
                    endTyping()
                    return true
                }
                Input.Keys.ENTER -> {
                    if (currentMode == ChatMode.CHAT) {
                        val message = currentInput.toString().trim()
                        if (message.isNotEmpty()) {
                            sendMessage(message)
                            currentInput.clear()
                        }
                    }
                    return true
                }
                Input.Keys.UP -> { scrollUp(); return true }
                Input.Keys.DOWN -> { scrollDown(); return true }
                Input.Keys.PAGE_UP -> { scrollDown(MAX_DISPLAY_LINES); return true }
                Input.Keys.PAGE_DOWN -> { scrollUp(MAX_DISPLAY_LINES); return true }
                Input.Keys.TAB -> {
                    switchMode()
                    return true
                }
            }
            return false
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (amountY > 0) {
                scrollDown((amountY * SCROLL_SPEED).toInt())
            } else if (amountY < 0) {
                scrollUp((-amountY * SCROLL_SPEED).toInt())
            }
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val buttonY = 60f + BUTTONS_Y_OFFSET
            val chatButtonX = 20f + BUTTONS_X_OFFSET     // uwzględniony offset X
            val logButtonX = chatButtonX + BUTTON_WIDTH

            val worldY = Gdx.graphics.height - screenY.toFloat()
            val worldX = screenX.toFloat()

            if (worldX >= chatButtonX && worldX <= chatButtonX + BUTTON_WIDTH &&
                worldY >= buttonY && worldY <= buttonY + BUTTON_HEIGHT) {
                currentMode = ChatMode.CHAT
                scrollToBottom()
                return true
            }

            if (worldX >= logButtonX && worldX <= logButtonX + BUTTON_WIDTH &&
                worldY >= buttonY && worldY <= buttonY + BUTTON_HEIGHT) {
                currentMode = ChatMode.LOG
                scrollToBottom()
                return true
            }

            return false
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            mouseX = screenX.toFloat()
            mouseY = Gdx.graphics.height - screenY.toFloat()
            return false
        }
    }

    fun handleInput(): Boolean {
        val enterCurrentlyDown = Gdx.input.isKeyPressed(Input.Keys.ENTER)

        if (wasEnterDown && !enterCurrentlyDown) {
            enterJustReleased = true
        }
        wasEnterDown = enterCurrentlyDown

        if (enterJustReleased) {
            enterJustReleased = false
            return isTyping.get()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (!isTyping.get()) {
                // Blokuj pisanie w trybie LOG
                if (currentMode == ChatMode.CHAT) {
                    startTyping()
                    return true // Zwróć true TYLKO jeśli faktycznie zaczął pisać
                }
                return false // Zwróć false jeśli chat się nie aktywował (tryb LOG)
            } else {
                endTyping()
                return true // Chat był aktywny i się wyłączył
            }
        }

        // POPRAWKA: Ustaw inputProcessor TYLKO gdy chat jest aktywny
        if (isTyping.get()) {
            if (Gdx.input.inputProcessor != chatInputProcessor) {
                Gdx.input.inputProcessor = chatInputProcessor
            }
        } else {
            // Gdy chat nie jest aktywny, wyczyść inputProcessor
            if (Gdx.input.inputProcessor == chatInputProcessor) {
                Gdx.input.inputProcessor = null
            }
        }

        return isTyping.get() // Zwróć true tylko jeśli chat jest aktywny
    }

    private fun startTyping() {
        if (!isTyping.compareAndSet(false, true)) return

        originalInputProcessor = Gdx.input.inputProcessor
        Gdx.input.inputProcessor = chatInputProcessor
        currentInput.clear()
        scrollToBottom()
    }

    private fun endTyping() {
        if (!isTyping.compareAndSet(true, false)) return

        // Zachowaj input processor dla przycisków
        scrollToBottom()
    }

    fun switchMode() {
        currentMode = if (currentMode == ChatMode.CHAT) ChatMode.LOG else ChatMode.CHAT
        scrollToBottom()
    }

    private fun scrollUp(amount: Int = 1) {
        updateProcessedLines()
        scrollOffset = min(scrollOffset + amount, maxScrollOffset)
    }

    private fun scrollDown(amount: Int = 1) {
        scrollOffset = max(scrollOffset - amount, 0)
    }

    private fun scrollToBottom() {
        scrollOffset = 0
    }

    private fun updateProcessedLines() {
        val messages = when {
            isTyping.get() -> {
                if (currentMode == ChatMode.LOG) {
                    logMessages.toList()
                } else {
                    allMessages.toList()
                }
            }
            else -> activeMessages.toList()
        }

        allProcessedLines.clear()

        for (message in messages) {
            val fullText = formatMessage(message)
            val lines = wrapText(fullText, BitmapFont(), CHAT_WIDTH)
            for (line in lines) {
                allProcessedLines.add(Pair(message, line))
            }
        }

        maxScrollOffset = max(0, allProcessedLines.size - MAX_DISPLAY_LINES)
    }

    private fun sendMessage(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty() || trimmedContent.length > MAX_INPUT_LENGTH) {
            addSystemMessage("Wiadomość jest zbyt długa lub pusta")
            return
        }

        val message = ChatMessage(localPlayerId, username, trimmedContent)
        addMessage(message)

        networkScope.launch {
            try {
                getSession()?.send("CHAT|$localPlayerId|$username|$trimmedContent")
            } catch (e: Exception) {
                Gdx.app.error("Chat", "Error sending message: ${e.message}")
                addSystemMessage("Błąd wysyłania wiadomości")
            }
        }
    }

    fun receiveMessage(senderId: String, senderName: String, content: String) {
        if (senderId != localPlayerId) {
            addMessage(ChatMessage(senderId, senderName, content))
        }
    }

    private fun addSystemMessage(content: String) {
        addMessage(
            ChatMessage(
                "system",
                "System",
                content,
                messageType = MessageType.SYSTEM,
                isLogMessage = true
            )
        )
    }

    fun addLogMessage(content: String) {
        val logMessage = ChatMessage(
            "system",
            "Log",
            content,
            messageType = MessageType.LOG,
            isLogMessage = true
        )
        addMessage(logMessage)
    }

    @Synchronized
    private fun addMessage(message: ChatMessage) {
        if (message.isLogMessage) {
            logMessages.add(message)
            while (logMessages.size > 200) {
                logMessages.removeAt(0)
            }
        } else {
            activeMessages.offer(message)
            allMessages.add(message)

            while (activeMessages.size > MAX_MESSAGES) {
                activeMessages.poll()
            }

            while (allMessages.size > 100) {
                allMessages.removeAt(0)
            }
        }

        if (scrollOffset == 0) {
            scrollToBottom()
        }
    }

    fun update(delta: Float) {
        if (isTyping.get()) return

        synchronized(this) {
            val iterator = activeMessages.iterator()
            while (iterator.hasNext()) {
                val message = iterator.next()
                message.lifetime += delta
                if (message.lifetime > MAX_MESSAGE_LIFETIME) {
                    iterator.remove()
                }
            }
        }
    }

    private fun wrapText(text: String, font: BitmapFont, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            glyphLayout.setText(font, testLine)

            if (glyphLayout.width <= maxWidth) {
                if (currentLine.isNotEmpty()) {
                    currentLine.append(" ")
                }
                currentLine.append(word)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                }

                glyphLayout.setText(font, word)
                if (glyphLayout.width <= maxWidth) {
                    currentLine.append(word)
                } else {
                    var remainingWord = word
                    while (remainingWord.isNotEmpty()) {
                        var charCount = 1
                        var testSubstring = remainingWord.substring(0, charCount)

                        while (charCount < remainingWord.length) {
                            val nextTestSubstring = remainingWord.substring(0, charCount + 1)
                            glyphLayout.setText(font, nextTestSubstring)

                            if (glyphLayout.width > maxWidth) {
                                break
                            }
                            testSubstring = nextTestSubstring
                            charCount++
                        }

                        lines.add(testSubstring)
                        remainingWord = remainingWord.substring(testSubstring.length)
                    }
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines.ifEmpty { listOf("") }
    }

    fun render(batch: SpriteBatch, font: BitmapFont) {
        val inputX = 20f
        val inputY = 80f
        val originalScale = font.data.scaleX
        val lineHeight = font.lineHeight * LINE_HEIGHT_MULTIPLIER

        font.data.setScale(1.0f)

        try {
            // Zawsze renderuj przyciski
            renderButtons(batch, font)

            if (isTyping.get()) {
                renderTypingMode(batch, font, inputX, inputY, lineHeight)
            } else {
                renderNormalMode(batch, font, inputX, inputY, lineHeight)
            }
        } finally {
            font.data.setScale(originalScale)
        }
    }

    private fun renderTypingMode(batch: SpriteBatch, font: BitmapFont, inputX: Float, inputY: Float, lineHeight: Float) {
        // Renderuj pole wejścia tylko w trybie CHAT
        if (currentMode == ChatMode.CHAT) {
            font.color = Color.WHITE
            val cursorVisible = ((System.currentTimeMillis() / 500) % 2 == 0L)
            val cursor = if (cursorVisible) "_" else " "
            val fullInputText = "Say: ${currentInput}$cursor"

            glyphLayout.setText(font, fullInputText)
            val displayText = if (glyphLayout.width > CHAT_WIDTH) {
                val prefix = "Say: "
                glyphLayout.setText(font, prefix)
                val prefixWidth = glyphLayout.width
                val availableWidth = CHAT_WIDTH - prefixWidth

                val inputWithCursor = "${currentInput}$cursor"
                var startIndex = 0

                for (i in inputWithCursor.indices) {
                    val substring = inputWithCursor.substring(i)
                    glyphLayout.setText(font, substring)
                    if (glyphLayout.width <= availableWidth) {
                        startIndex = i
                        break
                    }
                }

                prefix + inputWithCursor.substring(startIndex)
            } else {
                fullInputText
            }

            font.draw(batch, displayText, inputX, inputY)
        }

        // Pokaż wiadomości z przewijaniem
        val messages = if (currentMode == ChatMode.LOG) logMessages.toList() else allMessages.toList()
        val linesToShow = getLinesToShowWithScroll(messages, font)
        renderLines(batch, font, linesToShow, inputX, inputY, lineHeight, true, 0)
    }

    private fun renderNormalMode(
        batch: SpriteBatch,
        font: BitmapFont,
        inputX: Float,
        inputY: Float,
        lineHeight: Float
    ) {
        // podpowiedź zależna od trybu
        font.color = Color(0.7f, 0.7f, 0.7f, 0.5f)
        val hint = if (currentMode == ChatMode.CHAT)
            "Naciśnij Enter, aby czatować"
        else
            "TAB aby wrócić do czatu"
        font.draw(batch, hint, inputX, inputY)

        val messages = if (currentMode == ChatMode.LOG)
            logMessages.toList()
        else
            activeMessages.toList()

        val linesToShow = getLinesToShow(messages, font)
        renderLines(batch, font, linesToShow, inputX, inputY, lineHeight,
            isTypingMode = false, inputLineOffset = 0)
    }

    private fun renderButtons(batch: SpriteBatch, font: BitmapFont) {
        val mouseX = Gdx.input.x.toFloat()
        val mouseY = (Gdx.graphics.height - Gdx.input.y).toFloat() // zamiana y do układu GUI (0,0 na dole)

        val buttonWidth = 60f
        val buttonHeight = 25f
        val buttonY = 310f

        val chatButtonX = 20f
        val logButtonX = 85f

        fun isMouseOver(x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float): Boolean {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        }

        isMouseOver(chatButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)
        isMouseOver(logButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)

        val chatText = "[Chat]"
        glyphLayout.setText(font, chatText)
        val chatTextX = chatButtonX + (buttonWidth - glyphLayout.width) / 2f
        val chatTextY = buttonY + (buttonHeight + glyphLayout.height) / 2f
        font.draw(batch, chatText, chatTextX, chatTextY)

        val logText = "[Log]"
        glyphLayout.setText(font, logText)
        val logTextX = logButtonX + (buttonWidth - glyphLayout.width) / 2f
        val logTextY = buttonY + (buttonHeight + glyphLayout.height) / 2f
        font.draw(batch, logText, logTextX, logTextY)
    }

    private fun getLinesToShowWithScroll(messages: List<ChatMessage>, font: BitmapFont): List<Pair<ChatMessage, String>> {
        val allLines = mutableListOf<Pair<ChatMessage, String>>()

        for (message in messages) {
            val fullText = formatMessage(message)
            val lines = wrapText(fullText, font, CHAT_WIDTH)
            for (line in lines) {
                allLines.add(Pair(message, line))
            }
        }

        if (allLines.isEmpty()) return emptyList()

        val totalLines = allLines.size
        val startIndex = max(0, totalLines - MAX_DISPLAY_LINES - scrollOffset)
        val endIndex = min(totalLines, startIndex + MAX_DISPLAY_LINES)

        return if (startIndex < endIndex) {
            allLines.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    private fun getLinesToShow(messages: List<ChatMessage>, font: BitmapFont): List<Pair<ChatMessage, String>> {
        val allLines = mutableListOf<Pair<ChatMessage, String>>()

        for (message in messages) {
            val fullText = formatMessage(message)
            val lines = wrapText(fullText, font, CHAT_WIDTH)
            for (line in lines) {
                allLines.add(Pair(message, line))
            }
        }

        return if (allLines.size > MAX_DISPLAY_LINES) {
            allLines.takeLast(MAX_DISPLAY_LINES)
        } else {
            allLines
        }
    }

    private fun renderLines(
        batch: SpriteBatch,
        font: BitmapFont,
        lines: List<Pair<ChatMessage, String>>,
        inputX: Float,
        inputY: Float,
        lineHeight: Float,
        isTypingMode: Boolean,
        inputLineOffset: Int
    ) {
        for (i in lines.indices) {
            val (message, lineText) = lines[i]
            val currentY = if (isTypingMode) {
                inputY + (lines.size - i) * lineHeight
            } else {
                inputY + (inputLineOffset + lines.size - i) * lineHeight
            }

            val alpha = if (!isTypingMode && message.lifetime > MAX_MESSAGE_LIFETIME - FADE_DURATION) {
                ((MAX_MESSAGE_LIFETIME - message.lifetime) / FADE_DURATION).coerceIn(0f, 1f)
            } else if (isTypingMode) {
                0.8f
            } else {
                1f
            }

            font.color = getMessageColor(message, alpha)
            font.draw(batch, lineText, inputX, currentY)
        }
    }

    private fun getMessageColor(message: ChatMessage, alpha: Float): Color {
        return when {
            message.messageType == MessageType.SYSTEM -> Color(1f, 0.8f, 0f, alpha)
            message.messageType == MessageType.LOG -> Color(0.8f, 0.8f, 0.8f, alpha)
            message.senderId == localPlayerId -> Color(0.2f, 0.8f, 0.2f, alpha)
            else -> Color(0.8f, 0.8f, 1f, alpha)
        }
    }

    private fun formatMessage(message: ChatMessage): String {
        return if (message.messageType == MessageType.LOG) {
            "[SERVER] ${message.content}"
        } else {
            "${message.senderName}: ${message.content}"
        }
    }

    // Pomocnicze metody
    fun getCurrentMode(): ChatMode = currentMode
}