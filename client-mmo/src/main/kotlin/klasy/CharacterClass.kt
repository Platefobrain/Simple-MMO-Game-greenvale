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

package pl.decodesoft.klasy

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.player.Player
import pl.decodesoft.klasy.skile.SkileManager
import pl.decodesoft.network.MessageManager
import pl.decodesoft.npc.NPCClient
import pl.decodesoft.player.Faction

// Abstrakcyjna klasa bazowa dla wszystkich klas postaci w grze
abstract class CharacterClass(
    protected val player: Player,
    private val networkScope: CoroutineScope,
    protected val session: () -> DefaultWebSocketSession?,
    val skileManager: SkileManager,
    private val messageManager: MessageManager
) {
    // obliczanie bonus speed
    protected var attackSpeedBonus: Int = 0
        private set

    // Wspólne właściwości dla wszystkich klas postaci
    private var targetPlayer: Player? = null
    private var targetEnemy: EnemyClient? = null
    private var targetNPC: NPCClient? = null

    // Właściwości do nadpisania przez klasy potomne
    protected abstract val attackCooldown: Float
    protected abstract var attackTimer: Float
    protected abstract val attackRange: Float
    protected abstract val attackName: String
    protected abstract val attackColor: Color

    // Aktualizacja stanu postaci
    open fun update(delta: Float) {
        // Odliczaj czas cooldownu
        if (attackTimer > 0) {
            attackTimer -= delta
        }

        // Sprawdź czy mamy cel gracza do podejścia
        targetPlayer?.let { target ->
            // Oblicz dystans do celu
            val distance = Vector2.dst(player.x, player.y, target.x, target.y)

            // Jeśli jesteśmy w zasięgu ataku i cooldown się skończył
            if (distance <= attackRange && attackTimer <= 0) {
                performAttack(target.x, target.y, target.id)
                targetPlayer = null
            }
        }

        // Sprawdź czy mamy cel przeciwnika do podejścia
        targetEnemy?.let { target ->
            val distance = Vector2.dst(player.x, player.y, target.x, target.y)

            if (distance <= attackRange && attackTimer <= 0) {
                performAttack(target.x, target.y, "enemy_${target.id}")
                targetEnemy = null
            }
        }

        // === POPRAWIONA LOGIKA DLA NPC ===
        targetNPC?.let { target ->
            val distance = Vector2.dst(player.x, player.y, target.x, target.y)

            // Sprawdź czy to NPC z tej samej frakcji lub neutralny (NONE)
            if (target.faction == Faction.NONE || player.faction == target.faction) {
                // Rozmowa - zasięg interakcji 40f
                val interactionRange = 40f
                if (distance <= interactionRange) {
                    targetNPC = null
                    messageManager.showMessage("Rozmawiasz z ${target.name}", 2f, Color.GREEN)
                }
            } else {
                // Atak - przeciwna frakcja
                if (distance <= attackRange && attackTimer <= 0) {
                    performAttack(target.x, target.y, "npc_${target.id}")
                    targetNPC = null
                }
            }
        }
    }

    // Obsługa kliknięcia na przeciwnika
    open fun handleEnemyClick(targetEnemy: EnemyClient): Boolean {
        if (isOnCooldown()) {
            messageManager.showMessage("Masz cooldown!", 2f, Color.RED)
            return false
        }

        this.targetEnemy = targetEnemy
        this.targetPlayer = null

        val distance = Vector2.dst(player.x, player.y, targetEnemy.x, targetEnemy.y)

        if (distance <= attackRange && attackTimer <= 0) {
            performAttack(targetEnemy.x, targetEnemy.y, "enemy_${targetEnemy.id}")
            this.targetEnemy = null
            return true
        }

        return false
    }

    // Obsługa kliknięcia na NPC
    open fun handleNPCClick(targetNPC: NPCClient): Boolean {
        // Sprawdź czy NPC jest z tej samej frakcji lub neutralny
        if (targetNPC.faction == Faction.NONE || player.faction == targetNPC.faction) {
            this.targetNPC = targetNPC
            this.targetPlayer = null
            this.targetEnemy = null
            return false
        }

        if (isOnCooldown()) {
            messageManager.showMessage("Masz cooldown!", 2f, Color.RED)
            return false
        }

        this.targetNPC = targetNPC
        this.targetPlayer = null
        this.targetEnemy = null

        val distance = Vector2.dst(player.x, player.y, targetNPC.x, targetNPC.y)

        if (distance <= attackRange && attackTimer <= 0) {
            performAttack(targetNPC.x, targetNPC.y, "npc_${targetNPC.id}")
            this.targetNPC = null
            return true
        }

        return false
    }

    // Obsługa kliknięcia na gracza
    open fun handleTargetClick(targetPlayer: Player): Boolean {
        // Sprawdź czy jest cooldown - jeśli tak, zablokuj atak
        if (isOnCooldown()) {
            messageManager.showMessage("Masz cooldown!", 2f, Color.RED)
            return false
        }

        // Ustaw gracza jako cel do podejścia
        this.targetPlayer = targetPlayer
        this.targetEnemy = null

        // Oblicz dystans do celu
        val distance = Vector2.dst(player.x, player.y, targetPlayer.x, targetPlayer.y)

        // Jeśli jesteśmy już w zasięgu i cooldown się skończył, atakuj od razu
        if (distance <= attackRange && attackTimer <= 0) {
            performAttack(targetPlayer.x, targetPlayer.y, targetPlayer.id)
            // USUNIĘTO: attackTimer = getEffectiveAttackCooldown()
            // Timer będzie zresetowany przez serwer

            // Wyczyść cel po wykonaniu ataku
            this.targetPlayer = null
            return true
        }

        // Zwróć false, jeśli nie wykonaliśmy natychmiastowego ataku
        return false
    }

    // out of range
    fun showMessage(text: String, duration: Float, color: Color = Color.WHITE) {
        messageManager.showMessage(text, duration, color)
    }

    // Metoda abstrakcyjna do wykonania ataku
    protected abstract fun performAttack(targetX: Float, targetY: Float, targetId: String)

    // Metoda do wysyłania wiadomości o ataku na serwer
    protected fun sendAttackMessage(
        attackType: String,
        targetX: Float,
        targetY: Float,
        normalizedDirX: Float,
        normalizedDirY: Float,
        targetId: String
    ) {
        networkScope.launch {
            try {
                // Najpierw definiujemy zmienną message
                val message = "$attackType|${player.x}|${player.y}|$normalizedDirX|$normalizedDirY|${player.id}|$targetId"

                val currentSession: DefaultWebSocketSession? = session()
                if (currentSession != null) {
                    currentSession.send(Frame.Text(message))
                } else {
                    Gdx.app.error(this::class.java.simpleName, "Błąd wysyłania informacji o ataku: sesja jest null")
                }
            } catch (e: Exception) {
                Gdx.app.error(this::class.java.simpleName, "Błąd wysyłania informacji o ataku: ${e.message}")
            }
        }
    }

    // Oblicza rzeczywisty cooldown z uwzględnieniem attack speed bonus
    private fun getEffectiveAttackCooldown(): Float {
        return player.updateAttackCooldown()
    }

    // === NOWA PUBLICZNA METODA DO RESETOWANIA COOLDOWNU ===
    // Wywoływana przez CombatMessageHandler gdy serwer zatwierdzi atak
    fun resetAttackCooldown() {
        attackTimer = getEffectiveAttackCooldown()
    }

    // Gettery do odczytu stanu cooldownu przez GameUI
    fun getCooldownProgress(): Float {
        val effectiveCooldown = getEffectiveAttackCooldown()
        return if (attackTimer > 0) 1 - (attackTimer / effectiveCooldown) else 1f
    }

    fun getRemainingCooldownTime(): Float {
        return attackTimer
    }

    fun isOnCooldown(): Boolean {
        return attackTimer > 0
    }

    fun getCurrentAttackName(): String {
        return attackName
    }

    fun getCurrentAttackColor(): Color {
        return attackColor
    }
}