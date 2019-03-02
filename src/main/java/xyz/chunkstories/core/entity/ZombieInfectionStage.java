package xyz.chunkstories.core.entity;

public enum ZombieInfectionStage {
	INFECTION(0.045, 5, 1800, 10f, 40f), TAKEOVER(0.060, 10, 1200, 15f, 80f), WHOLESOME(0.075, 15, 800, 20f, 160f),
	;

	ZombieInfectionStage(double speed, double aggroDistance, int attackCooldown, float attackDamage, float hp) {
		this.speed = speed;
		this.aggroRadius = aggroDistance;
		this.attackCooldown = attackCooldown;
		this.attackDamage = attackDamage;
		this.hp = hp;
	}

	public final double speed;
	public final double aggroRadius;
	public final int attackCooldown;
	public final float attackDamage;
	public final float hp;
}
