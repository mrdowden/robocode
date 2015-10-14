package us.dowden.robocode.bots;

import static java.lang.String.format;
import static java.lang.System.err;
import static java.util.Arrays.asList;
import static robocode.Rules.ROBOT_HIT_DAMAGE;
import static robocode.Rules.getBulletDamage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import robocode.AdvancedRobot;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import us.dowden.robocode.RobotStatManager;
import us.dowden.robocode.RobotStats;
import us.dowden.robocode.strategies.ChaseMovement;
import us.dowden.robocode.strategies.MovementStrategy;
import us.dowden.robocode.strategies.NoniterativeLinearTargeting;
import us.dowden.robocode.strategies.RadarStrategy;
import us.dowden.robocode.strategies.TargetingStrategy;
import us.dowden.robocode.strategies.WidthLockRadar;

public class GrudgeBot extends AdvancedRobot {

	private static List<String> grudges = asList("DWTaggart", "Karolos", "DrunkenBoxer",
			"Shandroid", "BotInBlack");
	private static RobotStatManager statManager = new RobotStatManager();

	private String target;
	private List<String> dead = new ArrayList<>();
	private RadarStrategy radarStrategy;
	private TargetingStrategy gunStrategy;
	private MovementStrategy moveStrategy;
	private Map<String, ScannedRobotEvent> scans = new HashMap<>();

	@Override
	public void run() {
		radarStrategy = new WidthLockRadar(this);
		gunStrategy = new NoniterativeLinearTargeting(this);
		moveStrategy = new ChaseMovement(this);

		setBodyColor(new Color(0, 0, 0));
		setGunColor(new Color(237, 237, 237));
		setRadarColor(new Color(180, 180, 180));
		setBulletColor(new Color(16, 255, 0));
		setScanColor(new Color(102, 211, 255));
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Scan the battlefield once
		while (scans.size() < getOthers()) {
			setTurnRadarRight(360);
			execute();
		}
		// Reset the stat manager between rounds (should have target by now)
		statManager.reset();
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
		// Ensure battlefield scan happens before selecting a target
		if (scans.size() < getOthers()) {
			out.println(format("Scanned %d robots of %d", scans.size() + 1, getOthers()));
		} else if (target == null) {
			// Acquire target
			chooseTarget(e.getName());
		}

		// Capture latest scan for each bot
		scans.put(e.getName(), e);

		// Ignore anything that's not our current target
		if (target != null && target.equals(e.getName())) {
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

	@Override
	public void onHitWall(HitWallEvent event) {
		err.println(format("Oops! Hit wall x:%.1f y:%.1f heading:%.2f", getX(), getY(),
				getHeading()));
	}

	@Override
	public void onWin(WinEvent event) {
		// Stop moving
		setAhead(0.0);
		// Fly our flag
		setBodyColor(Color.RED);
		setGunColor(Color.WHITE);
		setRadarColor(Color.BLUE);
		// Victory Dance
		turnRightRadians(1.0);
		turnRightRadians(-1.0);
		turnRightRadians(1.0);
		turnRightRadians(-1.0);
	}

	@Override
	public void onPaint(Graphics2D g) {
		moveStrategy.paint(g);
	}

	private void chooseTarget(String defaultTarget) {
		// Ensure we don't have a dead target
		if (dead.contains(target))
			target = null;

		String oldTarget = target;

		// Select the living bot that has killed me the most times
		Optional<RobotStats> deathCandidate = statManager
				.stream()
				.filter(r -> r.deaths > 0)
				.filter(r -> !dead.contains(r.name))
				.max((RobotStats r1, RobotStats r2) -> r1.deaths != r2.deaths ? r1.deaths
						- r2.deaths : (int) (r1.damageTaken - r2.damageTaken));

		// Select the living bot that has dealt the most damage this round
		Optional<RobotStats> dmgCandidate = statManager.stream().filter(r -> r.damageTaken > 10)
				.filter(r -> !dead.contains(r.name))
				.max((RobotStats r1, RobotStats r2) -> (int) (r1.damageTaken - r2.damageTaken));

		// Select living grudge with weakest energy
		Optional<ScannedRobotEvent> grudge = scans
				.values()
				.stream()
				.filter(r -> !dead.contains(r.getName()))
				.filter(r -> grudges.stream().anyMatch(g -> r.getName().contains(g)))
				.min((ScannedRobotEvent e1, ScannedRobotEvent e2) -> (int) (e1.getEnergy() - e2
						.getEnergy()));

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

			// Next lets look at long-term grudges
		} else if (target == null && grudge.isPresent()) {
			target = grudge.get().getName();
			out.println(format("Targeting %s with energy %.2f for a grudge", target, grudge.get()
					.getEnergy()));

			// Opportunity to change target to the one actively damaging me
		} else if (target != null && dmgCandidate.isPresent()) {
			RobotStats targetStats = statManager.getStats(target);

			if ((dmgCandidate.get().damageTaken >= targetStats.damageTaken * 1.2)
					&& (dmgCandidate.get().damageTaken > targetStats.damageTaken + 18)) {
				target = dmgCandidate.get().name;
				out.println(format("Targeting %s for dealing %.2f damage", target,
						dmgCandidate.get().damageTaken));
			}

			// If I still don't have a target, use the one provided
		} else if (target == null && defaultTarget != null) {
			target = defaultTarget;
			out.println(format("Targeting %s for existing", target));
		}

		// If we have a new target, populate the latest scan
		if (target != null && !target.equals(oldTarget) && scans.containsKey(target)) {
			ScannedRobotEvent scanTarget = scans.get(target);
			radarStrategy.scan(scanTarget);
			gunStrategy.scan(scanTarget);
			moveStrategy.scan(scanTarget);
		}
	}
}
