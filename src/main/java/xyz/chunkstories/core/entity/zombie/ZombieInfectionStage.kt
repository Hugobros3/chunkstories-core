package xyz.chunkstories.core.entity.zombie

enum class ZombieInfectionStage constructor(val speed: Double, val aggroRadius: Double, val attackCooldown: Int, val attackDamage: Float, val hp: Float) {
    INFECTION(0.045, 5.0, 1800, 10f, 40f),
    TAKEOVER(0.060, 10.0, 1200, 15f, 80f),
    WHOLESOME(0.075, 15.0, 800, 20f, 160f)
}
