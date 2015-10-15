package us.dowden.robocode.strategies;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.System.err;
import static robocode.Rules.MAX_BULLET_POWER;
import static robocode.Rules.getBulletSpeed;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class IterativeLinearTargeting implements TargetingStrategy {

	private ScannedRobotEvent scanEvent;

	private final AdvancedRobot robot;
	private final double battleFieldHeight;
	private final double battleFieldWidth;
	private final double halfHeight;
	private final double halfWidth;

	public IterativeLinearTargeting(AdvancedRobot robot) {
		this.robot = robot;
		halfHeight = robot.getHeight() / 2.0;
		halfWidth = robot.getWidth() / 2.0;
		battleFieldHeight = robot.getBattleFieldHeight();
		battleFieldWidth = robot.getBattleFieldWidth();
	}

	@Override
	public void scan(ScannedRobotEvent e) {
		scanEvent = e;
	}

	@Override
	public void fire() {
		if (scanEvent != null) {
			// Setup inputs
			double bulletPower = min(MAX_BULLET_POWER, robot.getEnergy());
			double myX = robot.getX();
			double myY = robot.getY();
			double absoluteBearing = robot.getHeadingRadians() + scanEvent.getBearingRadians();
			double enemyX = myX + scanEvent.getDistance() * sin(absoluteBearing);
			double enemyY = myY + scanEvent.getDistance() * cos(absoluteBearing);
			double enemyHeading = scanEvent.getHeadingRadians();
			double enemyVelocity = scanEvent.getVelocity();

			// Iterative targeting accounting for walls
			double deltaTime = 0;
			double predictedX = enemyX, predictedY = enemyY;
			while ((++deltaTime) * getBulletSpeed(bulletPower) < Point2D.Double.distance(myX, myY,
					predictedX, predictedY)) {
				predictedX += sin(enemyHeading) * enemyVelocity;
				predictedY += cos(enemyHeading) * enemyVelocity;
				if (predictedX < halfWidth || predictedY < halfHeight
						|| predictedX > battleFieldWidth - halfWidth
						|| predictedY > battleFieldHeight - halfHeight) {
					predictedX = min(max(halfWidth, predictedX), battleFieldWidth - halfWidth);
					predictedY = min(max(halfHeight, predictedY), battleFieldHeight - halfHeight);
					break;
				}
			}
			double theta = normalAbsoluteAngle(atan2(predictedX - myX, predictedY - myY));

			robot.setTurnGunRightRadians(normalRelativeAngle(theta - robot.getGunHeadingRadians()));

			robot.setFire(bulletPower);
		} else {
			err.println("Targeting: Scan Event NULL!!");
		}
	}

}
