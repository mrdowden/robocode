package mrd;

import static java.lang.String.format;
import static java.lang.System.err;
import static robocode.Rules.ROBOT_HIT_DAMAGE;
import static robocode.Rules.getBulletDamage;
import static us.dowden.robocode.util.BotUtils.compareDouble;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
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
import us.dowden.robocode.Paintable;
import us.dowden.robocode.RobotStatManager;
import us.dowden.robocode.RobotStats;
import us.dowden.robocode.strategies.ChaseMovement;
import us.dowden.robocode.strategies.IterativeLinearTargeting;
import us.dowden.robocode.strategies.MovementStrategy;
import us.dowden.robocode.strategies.RadarStrategy;
import us.dowden.robocode.strategies.TargetingStrategy;
import us.dowden.robocode.strategies.WidthLockRadar;
import us.dowden.robocode.util.DrawObject;

public class Opportunist extends AdvancedRobot {

	private static RobotStatManager statManager = new RobotStatManager();

	private String target;
	private List<String> dead = new ArrayList<>();
	private RadarStrategy radarStrategy;
	private TargetingStrategy gunStrategy;
	private MovementStrategy moveStrategy;
	private Map<String, ScannedRobotEvent> scans = new HashMap<>();
	private Deque<DrawObject> drawStack = new LinkedList<>();

	@Override
	public void run() {
		radarStrategy = new WidthLockRadar(this);
		gunStrategy = new IterativeLinearTargeting(this);
		moveStrategy = new ChaseMovement(this);

		setBodyColor(Color.ORANGE);
		setGunColor(Color.GREEN);
		setRadarColor(Color.YELLOW);
		setBulletColor(Color.RED);
		setScanColor(Color.CYAN);
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
			if (target == null)
				setTurnRadarRightRadians(0.0);
			radarStrategy.turn(getTime());
			gunStrategy.fire(getTime());
			moveStrategy.move(getTime());
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
		while (!drawStack.isEmpty()) {
			DrawObject obj = drawStack.pop();
			if (obj.color != null)
				g.setColor(obj.color);
			if (obj.draw != null)
				g.draw(obj.draw);
			if (obj.fill != null)
				g.fill(obj.fill);
		}
		((Paintable) moveStrategy).paint(g);
	}

	private void chooseTarget(String defaultTarget) {
		// Ensure we don't have a dead target
		if (dead.contains(target))
			target = null;

		String oldTarget = target;

		// Select living bot with the lowest energy
		Optional<ScannedRobotEvent> lowEnergy = scans.values().stream()
				.filter(r -> !dead.contains(r.getName()))
				.min((r1, r2) -> compareDouble(r1.getEnergy(), r2.getEnergy()));

		// Select living bot that has killed/hurt me the least
		int fewestDeaths = statManager.stream().filter(r -> !dead.contains(r.name))
				.mapToInt(r -> r.deaths).min().orElse(0);
		Optional<RobotStats> lowThreat = statManager.stream().filter(r -> !dead.contains(r.name))
				.filter(r -> r.deaths == fewestDeaths)
				.min((r1, r2) -> compareDouble(r1.damageTaken, r2.damageTaken));

		// Start by targeting weakest bot, switching to the weakest bot when appropriate
		if (lowEnergy.isPresent()
				&& (target == null || (scans.get(target).getEnergy() > 20 && lowEnergy.get()
						.getEnergy() + 20 < scans.get(target).getEnergy()))) {
			target = lowEnergy.get().getName();
			out.println(format("Targeting %s for low energy %.2f", target, lowEnergy.get()
					.getEnergy()));

			// Next choice is the bot with the least threat (fewest kills, least damage)
		} else if (target == null && lowThreat.isPresent()) {
			target = lowThreat.get().name;
			out.println(format("Targeting %s for low threat (%d kills, %.2f damage)", target,
					lowThreat.get().deaths, lowThreat.get().damageTaken));

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
