package us.dowden.robocode.strategies;

import static java.lang.Math.max;
import static java.lang.Math.sin;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class ChaseMovement implements MovementStrategy {
	private AdvancedRobot robot;

	private ScannedRobotEvent scanEvent;

	public ChaseMovement(AdvancedRobot robot) {
		this.robot = robot;
	}

	@Override
	public void scan(ScannedRobotEvent e) {
		scanEvent = e;
	}

	@Override
	public void move() {
		if (scanEvent != null) {
			double absoluteBearing = robot.getHeadingRadians() + scanEvent.getBearingRadians();
			double robotVelocity = max(1.0, robot.getVelocity());
			double turnRightRadians = absoluteBearing
					- robot.getHeadingRadians()
					+ (scanEvent.getVelocity()
							* sin(scanEvent.getHeadingRadians() - absoluteBearing) / robotVelocity);
			double aheadDistance = scanEvent.getEnergy() <= 0.6 ? scanEvent.getDistance() * 4.0
					: scanEvent.getDistance() / 4.0;

			robot.setTurnRightRadians(turnRightRadians);
			robot.ahead(aheadDistance);
		}
	}
}
