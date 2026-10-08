package com.example.elite.math3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.elite.ships.Edge
import com.example.elite.ships.Face
import com.example.elite.ships.ShipBlueprint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Wireframe Projection Engine faithful to David Braben and Ian Bell's
 * 1984 BBC Micro 6502 assembly routines (LL9, PROJ, MVS, DVID3, TUCK).
 *
 * Implements:
 * - 3D Perspective Projection (focal length division, camera view transform)
 * - David Braben's revolutionary 1984 Hidden Surface / Backface Culling algorithm
 * - Dual-pass CRT Vector Phosphor Bloom & Glow
 * - Docking Slot navigation beacon flashing logic for Coriolis & Dodecahedron stations
 * - Engine exhaust vector trails and distance depth fading
 */
enum class RenderStyle(val label: String, val description: String) {
    AUTHENTIC_BACKFACE_CULLED("1984 ELITE (CULLED)", "David Braben's face-normal hidden line removal"),
    FULL_WIREFRAME("X-RAY WIREFRAME", "All structural vertices and interior edges visible"),
    GLOWING_CRT_VECTOR("CRT PHOSPHOR GLOW", "Authentic arcade vector monitor bloom and halo"),
    FACET_SHADED("FACET SHADED", "Retro flat-shaded polygons with vector boundary lines")
}

enum class ColorTheme(val label: String, val primary: Color, val secondary: Color, val accent: Color) {
    BBC_WHITE("BBC MODE 7 WHITE", Color(0xFFEEEEEE), Color(0xFF888888), Color(0xFFFFCC00)),
    BBC_MODE1("BBC MODE 1 COLOR", Color(0xFF00FFCC), Color(0xFF00FF00), Color(0xFFFF0055)),
    AMBER_CRT("AMBER CRT PHOSPHOR", Color(0xFFFFB000), Color(0xFF996600), Color(0xFFFFDD44)),
    PHOSPHOR_GREEN("GREEN CRT VECTOR", Color(0xFF33FF33), Color(0xFF117711), Color(0xFF88FF88)),
    CYBER_NEON("NEON TACTICAL", Color(0xFF00F0FF), Color(0xFFFF0077), Color(0xFFFFEE00))
}

data class ProjectedVertex(
    val world3D: Vector3,
    val cam3D: Vector3,
    val screen2D: Point2D?,
    val isVisible: Boolean
)

object WireframeProjectionEngine {

    /**
     * Recreates the BBC Micro PROJ routine:
     * ScreenX = CenterX + (CamX * FocalLength) / CamZ
     * ScreenY = CenterY - (CamY * FocalLength) / CamZ (inverting Y for screen space)
     */
    fun project(
        camPos: Vector3,
        centerX: Float,
        centerY: Float,
        focalLength: Float = 340f,
        nearClipZ: Float = 8f
    ): Point2D? {
        if (camPos.z <= nearClipZ) return null
        val scale = focalLength / camPos.z
        val sx = centerX + camPos.x * scale
        val sy = centerY - camPos.y * scale
        return Point2D(sx, sy)
    }

    /**
     * Core 3D Wireframe rendering pass for any ship or space station blueprint.
     */
    fun renderModel(
        drawScope: DrawScope,
        blueprint: ShipBlueprint,
        worldPos: Vector3 = Vector3(0f, 0f, 0f),
        rotationXYZ: Vector3 = Vector3(0f, 0f, 0f),
        camMatrix: Matrix3x3 = Matrix3x3.IDENTITY,
        center: Offset,
        viewScale: Float = 1.0f,
        focalLength: Float = 340f,
        renderStyle: RenderStyle = RenderStyle.AUTHENTIC_BACKFACE_CULLED,
        colorTheme: ColorTheme = ColorTheme.BBC_WHITE,
        overrideColor: Color? = null,
        strokeWidth: Float = 1.8f,
        enableDepthFade: Boolean = true,
        enableBeaconLights: Boolean = true
    ) {
        val totalVertices = blueprint.vertices.size
        if (totalVertices == 0) return

        val rotModel = Matrix3x3.rotationXYZ(rotationXYZ.x, rotationXYZ.y, rotationXYZ.z)
        val combinedMatrix = camMatrix * rotModel
        val scale = blueprint.baseScale * viewScale

        val camPosEntity = camMatrix.transform(worldPos)
        // Skip if entirely behind camera
        if (camPosEntity.z <= 5f && worldPos.z <= 0f && worldPos != Vector3(0f, 0f, 0f)) return

        // 1. Transform all vertices into Camera Space
        val projVertices = ArrayList<ProjectedVertex>(totalVertices)
        for (v in blueprint.vertices) {
            val rotatedLocal = combinedMatrix.transform(v * scale)
            val camV = rotatedLocal + camPosEntity
            val pt2D = project(camV, center.x, center.y, focalLength)
            projVertices.add(ProjectedVertex(rotatedLocal, camV, pt2D, pt2D != null))
        }

        // 2. Hidden Surface / Backface Culling Determination
        // In original 1984 Elite, faces whose transformed surface normal points away
        // from the line-of-sight vector are culled.
        val visibleFaces = BooleanArray(blueprint.faces.size)
        val visibleFaceEdges = HashSet<Pair<Int, Int>>()

        if (renderStyle == RenderStyle.AUTHENTIC_BACKFACE_CULLED || renderStyle == RenderStyle.FACET_SHADED) {
            for (fIdx in blueprint.faces.indices) {
                val face = blueprint.faces[fIdx]
                if (face.vertexIndices.size < 3) continue

                // Compute normal in camera space
                val camNormal = combinedMatrix.transform(face.normal)

                // Compute centroid of face in camera space
                var cX = 0f
                var cY = 0f
                var cZ = 0f
                for (vIdx in face.vertexIndices) {
                    if (vIdx in projVertices.indices) {
                        val cv = projVertices[vIdx].cam3D
                        cX += cv.x
                        cY += cv.y
                        cZ += cv.z
                    }
                }
                val count = face.vertexIndices.size.toFloat()
                val centroidCam = Vector3(cX / count, cY / count, cZ / count)

                // Face is visible if normal points towards camera:
                // View direction is from camera (0,0,0) to centroid, so dot(camNormal, -centroidCam) > 0
                val dot = camNormal.dot(Vector3(-centroidCam.x, -centroidCam.y, -centroidCam.z))
                if (dot > 0f) {
                    visibleFaces[fIdx] = true
                    // Mark constituent edges of this front-facing face as visible
                    for (i in face.vertexIndices.indices) {
                        val v1 = face.vertexIndices[i]
                        val v2 = face.vertexIndices[(i + 1) % face.vertexIndices.size]
                        val edgeKey = if (v1 < v2) Pair(v1, v2) else Pair(v2, v1)
                        visibleFaceEdges.add(edgeKey)
                    }
                }
            }
        }

        // 3. Facet Shading Pass (if enabled)
        if (renderStyle == RenderStyle.FACET_SHADED && blueprint.faces.isNotEmpty()) {
            for (fIdx in blueprint.faces.indices) {
                if (!visibleFaces[fIdx]) continue
                val face = blueprint.faces[fIdx]
                val path = Path()
                var first = true
                var allValid = true

                for (vIdx in face.vertexIndices) {
                    val p = projVertices.getOrNull(vIdx)?.screen2D
                    if (p != null) {
                        if (first) {
                            path.moveTo(p.x, p.y)
                            first = false
                        } else {
                            path.lineTo(p.x, p.y)
                        }
                    } else {
                        allValid = false
                        break
                    }
                }

                if (allValid && !first) {
                    path.close()
                    // Simple light source from upper-left
                    val lightDir = Vector3(-0.4f, 0.6f, -0.7f).normalized()
                    val norm = combinedMatrix.transform(face.normal).normalized()
                    val intensity = (norm.dot(lightDir) * 0.5f + 0.5f).coerceIn(0.12f, 0.75f)
                    val baseCol = overrideColor ?: colorTheme.primary
                    val faceColor = baseCol.copy(alpha = intensity * 0.35f)
                    drawScope.drawPath(path, color = faceColor)
                }
            }
        }

        // 4. Determine which edges to draw
        val hasDefinedFaces = blueprint.faces.isNotEmpty()
        val wireBaseColor = overrideColor ?: colorTheme.primary

        for (edge in blueprint.edges) {
            val v1 = edge.v1
            val v2 = edge.v2
            if (v1 !in projVertices.indices || v2 !in projVertices.indices) continue

            val pv1 = projVertices[v1]
            val pv2 = projVertices[v2]
            if (!pv1.isVisible || !pv2.isVisible) continue

            val p1 = pv1.screen2D ?: continue
            val p2 = pv2.screen2D ?: continue

            // Backface culling filter:
            if (renderStyle == RenderStyle.AUTHENTIC_BACKFACE_CULLED) {
                if (hasDefinedFaces) {
                    val edgeKey = if (v1 < v2) Pair(v1, v2) else Pair(v2, v1)
                    val isPartFace = visibleFaceEdges.contains(edgeKey)
                    // If blueprint has faces, only draw edges of visible front faces
                    // or special non-face edges (like docking ports or thruster vents)
                    val isSpecialFeature = isSpecialFeatureEdge(blueprint, edge)
                    if (!isPartFace && !isSpecialFeature) {
                        continue
                    }
                }
            }

            // Distance attenuation
            val avgZ = (pv1.cam3D.z + pv2.cam3D.z) / 2f
            val depthAlpha = if (enableDepthFade && avgZ > 100f) {
                ((1200f - avgZ) / 1100f).coerceIn(0.25f, 1.0f)
            } else {
                1.0f
            }
            val edgeColor = wireBaseColor.copy(alpha = depthAlpha)

            // Vector Glow effect (Dual stroke pass)
            if (renderStyle == RenderStyle.GLOWING_CRT_VECTOR) {
                // Outer glow
                drawScope.drawLine(
                    color = edgeColor.copy(alpha = depthAlpha * 0.28f),
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = strokeWidth * 2.8f
                )
            }

            // Core vector line
            drawScope.drawLine(
                color = edgeColor,
                start = Offset(p1.x, p1.y),
                end = Offset(p2.x, p2.y),
                strokeWidth = strokeWidth
            )
        }

        // 5. Special Station Features: Docking Bay Nav Lights
        if (enableBeaconLights && (blueprint.name.contains("Coriolis") || blueprint.name.contains("Dodec"))) {
            renderStationDockingBeacons(drawScope, blueprint, projVertices, combinedMatrix)
        }
    }

    /**
     * Checks if an edge represents a docking slot aperture, cockpit glass, or thruster
     * which must always be drawn even in backface culling mode.
     */
    private fun isSpecialFeatureEdge(blueprint: ShipBlueprint, edge: Edge): Boolean {
        val name = blueprint.name.lowercase()
        // Docking slot on Coriolis (vertices 12..15)
        if (name.contains("coriolis") && (edge.v1 in 12..15 || edge.v2 in 12..15)) return true
        // Docking slot on Dodec (vertices 20..23)
        if (name.contains("dodec") && (edge.v1 in 20..23 || edge.v2 in 20..23)) return true
        // Exhaust vents or cockpit canopies
        if (name.contains("cobra") && (edge.v1 in 12..19 || edge.v2 in 12..19)) return true
        if (name.contains("sidewinder") && (edge.v1 in 6..9 || edge.v2 in 6..9)) return true
        if (name.contains("mamba") && (edge.v1 in 5..8 || edge.v2 in 5..8)) return true
        return false
    }

    /**
     * Renders authentic flashing Red & Green navigation corridor beacons
     * at the entrance of Coriolis and Dodecahedron space stations.
     */
    private fun renderStationDockingBeacons(
        drawScope: DrawScope,
        blueprint: ShipBlueprint,
        projVertices: List<ProjectedVertex>,
        matrix: Matrix3x3
    ) {
        val now = System.currentTimeMillis()
        val flashInterval = (now / 400L) % 2 == 0L

        val isCoriolis = blueprint.name.contains("Coriolis")
        val slotV1 = if (isCoriolis) 12 else 20
        val slotV3 = if (isCoriolis) 14 else 22

        if (slotV1 in projVertices.indices && slotV3 in projVertices.indices) {
            val pStarboard = projVertices[slotV1].screen2D
            val pPort = projVertices[slotV3].screen2D

            if (pStarboard != null && projVertices[slotV1].cam3D.z > 10f) {
                val beaconColor = if (flashInterval) Color(0xFF00FF44) else Color(0xFF005511)
                drawScope.drawCircle(
                    color = beaconColor,
                    radius = 3.5f,
                    center = Offset(pStarboard.x, pStarboard.y)
                )
            }

            if (pPort != null && projVertices[slotV3].cam3D.z > 10f) {
                val beaconColor = if (!flashInterval) Color(0xFFFF2222) else Color(0xFF550000)
                drawScope.drawCircle(
                    color = beaconColor,
                    radius = 3.5f,
                    center = Offset(pPort.x, pPort.y)
                )
            }
        }
    }
}
