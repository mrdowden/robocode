package us.dowden.robocode.bots;

import static java.lang.String.format;
import static robocode.Rules.ROBOT_HIT_DAMAGE;
import static robocode.Rules.getBulletDamage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import us.dowden.robocode.RobotStatManager;
import us.dowden.robocode.RobotStats;
import us.dowden.robocode.strategies.ChaseMovement;
import us.dowden.robocode.strategies.MovementStrategy;
import us.dowden.robocode.strategies.NoniterativeLinearTargeting;
import us.dowden.robocode.strategies.RadarStrategy;
import us.dowden.robocode.strategies.TargetingStrategy;
import us.dowden.robocode.strategies.WidthLockRadar;

public class GrudgeBot extends AdvancedRobot {

	private String target;
	private List<String> dead = new ArrayList<>();
	private RadarStrategy radarStrategy = new WidthLockRadar(this);
	private TargetingStrategy gunStrategy = new NoniterativeLinearTargeting(this);
	private MovementStrategy moveStrategy = new ChaseMovement(this);
	private RobotStatManager statManager = new RobotStatManager();

	@Override
	public void run() {
		setBodyColor(new Color(0, 0, 0));
		setGunColor(new Color(237, 237, 237));
		setRadarColor(new Color(180, 180, 180));
		setBulletColor(new Color(16, 255, 0));
		setScanColor(new Color(102, 211, 255));
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Main processing loop
		do {
			chooseTarget(null);
			radarStrategy.turn();
			gunStrategy.fire();
			moveStrategy.move();
			execute();
		} while (true);
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		// Acquire target
		if (target == null)
			chooseTarget(e.getName());

		// Ignore anything that's not our current target
		if (target == null || target.equals(e.getName())) {
			radarStrategy.scan(e);
			gunStrategy.scan(e);
			moveStrategy.scan(e);
		}
	}

	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		// Track dead robots
		dead.add(event.getName());
		// If our target dies, clear our target
		if (target != null && target.equals(event.getName())) {
			target = null;
		}
	}

	@Override
	public void onBulletHit(BulletHitEvent event) {
		// Record Stats
		statManager.addDamageDealt(event.getName(), getBulletDamage(event.getBullet().getPower()));
		if (event.getEnergy() <= 0.0)
			statManager.addKill(event.getName());
	}

	@Override
	public void onHitByBullet(HitByBulletEvent event) {
		// Record Stats
		statManager.addDamageTaken(event.getName(), getBulletDamage(event.getPower()));
	}

	@Override
	public void onHitRobot(HitRobotEvent event) {
		// Record Stats
		statManager.addDamageDealt(event.getName(), ROBOT_HIT_DAMAGE);
		statManager.addDamageTaken(event.getName(), ROBOT_HIT_DAMAGE);
		if (event.getEnergy() <= 0.0)
			statManager.addKill(event.getName());
	}

	@Override
	public void onDeath(DeathEvent event) {
		// Record Stats
		statManager.addDeath();
	}

	private void chooseTarget(String defaultTarget) {
		// Select the living bot that has killed me the most times
		Optional<RobotStats> deathCandidate = statManager.stream()
				.filter(r -> !dead.contains(r.name))
				.max((RobotStats r1, RobotStats r2) -> (int) (r1.deaths - r2.deaths));

		// Select the living bot that has dealt the most damage this round
		Optional<RobotStats> dmgCandidate = statManager.stream()
				.filter(r -> !dead.contains(r.name))
				.max((RobotStats r1, RobotStats r2) -> (int) (r1.damageTaken - r2.damageTaken));

		// Start by targeting the bot that's killed me the most (initial setting for each round)
		if (target == null && deathCandidate.isPresent()) {
			target = deathCandidate.get().name;
			out.println(format("Targeting %s for killing me %d times", target,
					deathCandidate.get().deaths));

			// Next choice is the bot that's done the most damage
		} else if (target == null && dmgCandidate.isPresent()) {
			target = dmgCandidate.get().name;
			out.println(format("Targeting %s for dealing %.2f damage", target,
					dmgCandidate.get().damageTaken));

			// Opportunity to change target to the one actively damaging me
		} else if (target != null && dmgCandidate.isPresent()) {
			RobotStats targetStats = statManager.getStats(target);

			if (dmgCandidate.get().damageTaken >= targetStats.damageTaken * 1.5) {
				target = dmgCandidate.get().name;
				out.println(format("Targeting %s for dealing %.2f damage", target,
						deathCandidate.get().damageTaken));
			}

			// If I still don't have a target, use the one provided
		} else if (target == null && defaultTarget != null) {
			target = defaultTarget;
			out.println(format("Targeting %s for existing", target));
		}
	}
}
