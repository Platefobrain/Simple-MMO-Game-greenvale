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

package pl.decodesoft.klasy.skile

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.klasy.projectiles.Arrow
import pl.decodesoft.klasy.projectiles.Fireball
import pl.decodesoft.klasy.projectiles.Sword

// Klasa zarządzająca wszystkimi umiejętnościami (pociskami) w grze
class SkileManager(
    private val localPlayerId: String,
    private val enemies: Map<String, EnemyClient> = emptyMap()
) {
    // Lista wszystkich aktywnych umiejętności
    private val activeSkills = mutableListOf<Skile>()

    // Mapa przechowująca informacje o trafionych przeciwnikach i odpowiadających im pociskach
    private val hitEnemies = mutableMapOf<String, MutableList<String>>()

    // Dodaj nową umiejętność do listy
    fun addSkill(skill: Skile) {
        activeSkills.add(skill)
    }

    // Aktualizacja wszystkich umiejętności
    fun update(delta: Float) {
        val toRemove = mutableListOf<Skile>()

        activeSkills.forEach { skill ->
            val alive = skill.update(delta)
            if (!alive) toRemove.add(skill)
            else {

                // kolizje z przeciwnikami
                enemies.values.forEach { enemy ->
                    if (enemy.isSelected &&
                        skill.checkCollision(enemy) &&
                        !isEnemyHitBySkill(enemy.id, skill.id)
                    ) {
                        handleEnemyHit(enemy.id, skill.casterId)
                    }
                }
            }
        }

        activeSkills.removeAll(toRemove)
    }

    // Sprawdź czy dany przeciwnik został już trafiony przez konkretną umiejętność
    private fun isEnemyHitBySkill(enemyId: String, skillId: String): Boolean {
        return hitEnemies[enemyId]?.contains(skillId) ?: false
    }

    // Renderowanie wszystkich umiejętności
    fun render(shapeRenderer: ShapeRenderer) {
        activeSkills.forEach { it.render(shapeRenderer) }
    }

    // Obsługa trafiania w przeciwników
    private fun handleEnemyHit(enemyId: String, attackerId: String) {
        // Znajdź umiejętność, która mogła trafić w przeciwnika
        activeSkills.filter { it.casterId == attackerId }.forEach { skill ->
            enemies[enemyId]?.let { enemy ->
                if (skill.checkCollision(enemy)) {
                    // Zarejestruj trafienie, aby nie usuwać tej samej umiejętności wielokrotnie
                    val skillsForEnemy = hitEnemies.getOrPut(enemyId) { mutableListOf() }
                    skillsForEnemy.add(skill.id)

                    // Usuń umiejętność przy następnej aktualizacji
                    skill.markForRemoval()
                }
            }
        }
    }

    fun handleSkillMessage(msgType: String, parts: List<String>) {
        // Gdx.app.log("NET‑DEBUG", "msg=$msgType parts=$parts")
        when (msgType) {
            "RANGED_ATTACK", "SPELL_ATTACK", "MELEE_ATTACK" -> {
                if (parts.size >= 6) {
                    val startX   = parts[1].toFloat()
                    val startY   = parts[2].toFloat()
                    val targetX  = parts[3].toFloat()
                    val targetY  = parts[4].toFloat()
                    val casterId = parts[5]
                    val targetId = parts.getOrNull(6)

                    if (casterId != localPlayerId) {
                        val dir = Vector2(targetX - startX, targetY - startY).nor()
                        when (msgType) {
                            "RANGED_ATTACK" -> addSkill(
                                Arrow(startX, startY, dir.x, dir.y,
                                    casterId, targetId, targetX, targetY)
                            )
                            "SPELL_ATTACK"  -> addSkill(
                                Fireball(startX, startY, dir.x, dir.y,
                                    casterId, targetId, targetX, targetY)
                            )
                            "MELEE_ATTACK"  -> addSkill(
                                Sword(startX, startY, dir.x, dir.y,
                                    casterId, targetId, targetX, targetY)
                            )
                        }
                    }
                }
            }
            // Obsługa wiadomości o trafieniu przeciwnika
            "HIT", "HIT_DETAILED" -> {
                if (parts.size >= 3 && parts[1].startsWith("enemy_")) {
                    val enemyId = parts[1].substring(6)
                    val attackerId = parts[2]

                    // Zatrzymaj pocisk trafiony w przeciwnika
                    handleEnemyHit(enemyId, attackerId)
                }
            }
        }
    }
}
