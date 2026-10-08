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
        viewMatrix: Matrix3x3 = Matrix3x3.IDENTITY,
        renderStyle: RenderStyle = RenderStyle.AUTHENTIC_BACKFACE_CULLED
    ) {
        val center = Offset(viewWidth / 2f, viewHeight / 2f)
        WireframeProjectionEngine.renderModel(
            drawScope = drawScope,
            blueprint = entity.blueprint,
            worldPos = entity.position,
            rotationXYZ = entity.rotation,
            camMatrix = viewMatrix,
            center = center,
            viewScale = 1.0f,
            focalLength = 320f,
            renderStyle = renderStyle,
            overrideColor = wireColor,
            strokeWidth = strokeWidth,
            enableDepthFade = true,
            enableBeaconLights = true
        )
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
        strokeWidth: Float = 2.0f,
        renderStyle: RenderStyle = RenderStyle.AUTHENTIC_BACKFACE_CULLED
    ) {
        WireframeProjectionEngine.renderModel(
            drawScope = drawScope,
            blueprint = blueprint,
            worldPos = Vector3(0f, 0f, 300f),
            rotationXYZ = Vector3(rotX, rotY, rotZ),
            camMatrix = Matrix3x3.IDENTITY,
            center = center,
            viewScale = viewScale,
            focalLength = 400f,
            renderStyle = renderStyle,
            overrideColor = wireColor,
            strokeWidth = strokeWidth,
            enableDepthFade = false,
            enableBeaconLights = true
        )
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
