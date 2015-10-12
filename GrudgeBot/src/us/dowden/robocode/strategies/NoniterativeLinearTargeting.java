package us.dowden.robocode.strategies;

import static java.lang.Math.sin;
import static robocode.util.Utils.normalRelativeAngle;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class NoniterativeLinearTargeting implements TargetingStrategy {

	private static final double ESTIMATED_BULLET_SPEED = 13.0;
	private static final double BULLET_SIZE = 3.0;
	private AdvancedRobot robot;
	private ScannedRobotEvent scanEvent;

	public NoniterativeLinearTargeting(AdvancedRobot robot) {
		this.robot = robot;
	}

	@Override
	public void scan(ScannedRobotEvent e) {
		scanEvent = e;
	}

	@Override
	public void fire() {
		if (scanEvent != null) {
			double absoluteBearing = robot.getHeadingRadians() + scanEvent.getBearingRadians();
			robot.setTurnGunRightRadians(normalRelativeAngle(absoluteBearing
					- robot.getGunHeadingRadians()
					+ (scanEvent.getVelocity()
							* sin(scanEvent.getHeadingRadians() - absoluteBearing) / ESTIMATED_BULLET_SPEED)));
			robot.setFire(BULLET_SIZE);
		}
	}

}
