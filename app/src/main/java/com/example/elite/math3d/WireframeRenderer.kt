package com.example.elite.math3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.elite.flight.SpaceEntity

/**
 * Renders 3D wireframe models faithfully to the BBC Micro LL9 routine.
 * Handles 3D world transform, camera rotation, perspective projection (PROJ),
 * clipping, and vector drawing.
 */
data class WireframeShard(
    var pos: Vector3,
    var velocity: Vector3,
    var angle: Float,
    var angularVel: Float,
    var length: Float,
    val expireTimeMs: Long
)

object WireframeRenderer {

    fun projectPoint(
        worldPos: Vector3,
        viewWidth: Float,
        viewHeight: Float,
        focalLength: Float = 320f
    ): Point2D? {
        if (worldPos.z <= 5f) return null
        val cx = viewWidth / 2f
        val cy = viewHeight / 2f
        val projScale = focalLength / worldPos.z
        val sx = cx + worldPos.x * projScale
        val sy = cy - worldPos.y * projScale
        return Point2D(sx, sy)
    }

    fun renderShip(
        drawScope: DrawScope,
        entity: SpaceEntity,
        viewWidth: Float,
        viewHeight: Float,
        wireColor: Color = Color.White,
        strokeWidth: Float = 1.6f,
        viewMatrix: Matrix3x3 = Matrix3x3.IDENTITY
    ) {
        // Transform entity position by camera view matrix
        val pos = viewMatrix.transform(entity.position)
        // Don't render if behind camera
        if (pos.z <= 10f) return

        val blueprint = entity.blueprint
        val rot = entity.rotation
        val rotMatrix = viewMatrix * Matrix3x3.rotationXYZ(rot.x, rot.y, rot.z)
        val scale = blueprint.baseScale

        val cx = viewWidth / 2f
        val cy = viewHeight / 2f
        val focalLength = 320f

        // Transform vertices into world space
        val projected2D = arrayOfNulls<Point2D>(blueprint.vertices.size)

        for (i in blueprint.vertices.indices) {
            val v = blueprint.vertices[i]
            // Rotate local vertex
            val rotated = rotMatrix.transform(v * scale)
            // Translate to entity position in camera space
            val worldV = rotated + pos

            if (worldV.z > 5f) {
                // Perspective projection (PROJ routine)
                val projScale = focalLength / worldV.z
                val sx = cx + worldV.x * projScale
                val sy = cy - worldV.y * projScale // Invert Y for screen coords
                projected2D[i] = Point2D(sx, sy)
            }
        }

        // Draw edges connecting vertices
        for (edge in blueprint.edges) {
            if (edge.v1 in projected2D.indices && edge.v2 in projected2D.indices) {
                val p1 = projected2D[edge.v1]
                val p2 = projected2D[edge.v2]

                if (p1 != null && p2 != null) {
                    // Check if on screen
                    val inside1 = p1.x in -200f..(viewWidth + 200f) && p1.y in -200f..(viewHeight + 200f)
                    val inside2 = p2.x in -200f..(viewWidth + 200f) && p2.y in -200f..(viewHeight + 200f)

                    if (inside1 || inside2) {
                        drawScope.drawLine(
                            color = wireColor,
                            start = Offset(p1.x, p1.y),
                            end = Offset(p2.x, p2.y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
        }
    }

    /**
     * Renders a blueprint centered in a 2D viewport with interactive 3D rotation,
     * used for the Hologram Viewer / Shipyard encyclopedia.
     */
    fun renderBlueprintAt(
        drawScope: DrawScope,
        blueprint: com.example.elite.ships.ShipBlueprint,
        rotX: Float,
        rotY: Float,
        rotZ: Float,
        center: Offset,
        viewScale: Float = 1.0f,
        wireColor: Color = Color.White,
        strokeWidth: Float = 2.0f
    ) {
        val rotMatrix = Matrix3x3.rotationXYZ(rotX, rotY, rotZ)
        val scale = blueprint.baseScale * viewScale
        val focalLength = 400f
        val cameraZ = 300f

        val projected2D = arrayOfNulls<Point2D>(blueprint.vertices.size)

        for (i in blueprint.vertices.indices) {
            val v = blueprint.vertices[i]
            val rotated = rotMatrix.transform(v * scale)
            val z = cameraZ + rotated.z
            if (z > 20f) {
                val proj = focalLength / z
                val sx = center.x + rotated.x * proj
                val sy = center.y - rotated.y * proj
                projected2D[i] = Point2D(sx, sy)
            }
        }

        for (edge in blueprint.edges) {
            if (edge.v1 in projected2D.indices && edge.v2 in projected2D.indices) {
                val p1 = projected2D[edge.v1]
                val p2 = projected2D[edge.v2]
                if (p1 != null && p2 != null) {
                    drawScope.drawLine(
                        color = wireColor,
                        start = Offset(p1.x, p1.y),
                        end = Offset(p2.x, p2.y),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }

    /**
     * Renders tumbling 3D explosion debris lines (authentic 1984 wireframe explosions).
     */
    fun renderDebris(
        drawScope: DrawScope,
        shards: List<WireframeShard>,
        viewWidth: Float,
        viewHeight: Float,
        color: Color,
        viewMatrix: Matrix3x3 = Matrix3x3.IDENTITY
    ) {
        val cx = viewWidth / 2f
        val cy = viewHeight / 2f
        val focalLength = 320f

        for (shard in shards) {
            val p = viewMatrix.transform(shard.pos)
            if (p.z > 10f) {
                val proj = focalLength / p.z
                val sx = cx + p.x * proj
                val sy = cy - p.y * proj
                val len = shard.length * proj
                val dx = kotlin.math.cos(shard.angle) * len
                val dy = kotlin.math.sin(shard.angle) * len

                drawScope.drawLine(
                    color = color,
                    start = Offset(sx - dx / 2f, sy - dy / 2f),
                    end = Offset(sx + dx / 2f, sy + dy / 2f),
                    strokeWidth = 2f
                )
            }
        }
    }
}
