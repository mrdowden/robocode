package us.dowden.robocode.strategies;

import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.atan;
import static java.lang.Math.min;
import static robocode.Rules.RADAR_TURN_RATE_RADIANS;
import static robocode.util.Utils.normalRelativeAngle;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class WidthLockRadar implements RadarStrategy {
	private AdvancedRobot robot;

	public WidthLockRadar(AdvancedRobot robot) {
		this.robot = robot;
	}

	@Override
	public void scan(ScannedRobotEvent e) {
		// Absolute angle towards target
		double angleToEnemy = robot.getHeadingRadians() + e.getBearingRadians();

		// Subtract current radar heading to get the turn required to face the
		// enemy, be sure it is normalized
		double radarTurn = normalRelativeAngle(angleToEnemy - robot.getRadarHeadingRadians());

		// Distance we want to scan from middle of enemy to either side
		// The 36.0 is how many units from the center of the enemy robot it
		// scans.
		double extraTurn = min(atan(36.0 / e.getDistance()), RADAR_TURN_RATE_RADIANS);

		// Adjust the radar turn so it goes that much further in the direction
		// it is going to turn
		// Basically if we were going to turn it left, turn it even more left,
		// if right, turn more right.
		// This allows us to overshoot our enemy so that we get a good sweep
		// that will not slip.
		radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);

		// Turn the radar
		robot.setTurnRadarRightRadians(radarTurn);
	}

	@Override
	public void turn() {
		if (robot.getRadarTurnRemaining() == 0.0)
			robot.setTurnRadarRightRadians(POSITIVE_INFINITY);
	}

}
