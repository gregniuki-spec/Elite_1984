package com.example.elite.math3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D vector and projection math recreating the 6502 assembler algorithms
 * from elite-source.asm (subroutines MVS, PROJ, LL9, DVID3).
 */
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)
    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vector3(x / s, y / s, z / s)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.0001f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 0f)
    }

    fun dot(v: Vector3): Float = x * v.x + y * v.y + z * v.z

    fun cross(v: Vector3) = Vector3(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x
    )
}

data class Point2D(
    val x: Float,
    val y: Float
)

class Matrix3x3(
    val m: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
) {
    fun transform(v: Vector3): Vector3 {
        return Vector3(
            m[0] * v.x + m[1] * v.y + m[2] * v.z,
            m[3] * v.x + m[4] * v.y + m[5] * v.z,
            m[6] * v.x + m[7] * v.y + m[8] * v.z
        )
    }

    operator fun times(other: Matrix3x3): Matrix3x3 {
        val result = FloatArray(9)
        for (i in 0..2) {
            for (j in 0..2) {
                result[i * 3 + j] = m[i * 3] * other.m[j] +
                                    m[i * 3 + 1] * other.m[3 + j] +
                                    m[i * 3 + 2] * other.m[6 + j]
            }
        }
        return Matrix3x3(result)
    }

    companion object {
        val IDENTITY = Matrix3x3()
        fun identity() = Matrix3x3()

        fun rotationXYZ(pitch: Float, yaw: Float, roll: Float): Matrix3x3 {
            val cp = cos(pitch)
            val sp = sin(pitch)
            val cy = cos(yaw)
            val sy = sin(yaw)
            val cr = cos(roll)
            val sr = sin(roll)

            // Combined rotation matrix R = Rz(roll) * Rx(pitch) * Ry(yaw)
            return Matrix3x3(
                floatArrayOf(
                    cy * cr + sy * sp * sr, -cy * sr + sy * sp * cr, sy * cp,
                    cp * sr,                cp * cr,                 -sp,
                    -sy * cr + cy * sp * sr, sy * sr + cy * sp * cr, cy * cp
                )
            )
        }
    }
}
