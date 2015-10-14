package us.dowden.robocode.strategies;

import static java.lang.Math.max;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.out;
import static robocode.Rules.DECELERATION;
import static robocode.Rules.ROBOT_HIT_DAMAGE;

import java.awt.Color;
import java.awt.Graphics2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class ChaseMovement implements MovementStrategy {
	private AdvancedRobot robot;

	private ScannedRobotEvent scanEvent;
	private final double width;
	private final double height;
	private final double fieldWidth;
	private final double fieldHeight;

	public ChaseMovement(AdvancedRobot robot) {
		this.robot = robot;
		width = robot.getWidth();
		height = robot.getHeight();
		fieldWidth = robot.getBattleFieldWidth();
		fieldHeight = robot.getBattleFieldHeight();
	}

	@Override
	public void scan(ScannedRobotEvent e) {
		scanEvent = e;
	}

	private boolean wallSafe() {
		double x = robot.getX();
		double y = robot.getY();
		double heading = robot.getHeading();
		// Set margins accounting for deceleration rate, include 10px safety buffer
		double marginX = width / 2.0 + robot.getVelocity() * DECELERATION + 10.0;
		double marginY = height / 2.0 + robot.getVelocity() * DECELERATION + 10.0;

		// If pointed at a wall and inside the safe margin, stop
		if (heading > 180 && x < marginX) {
			out.println("Too close to the west wall");
			return false;
		} else if (heading < 180 && x > (fieldWidth - marginX)) {
			out.println("Too close to the east wall");
			return false;
		} else if ((heading < 90 || heading > 270) && y > (fieldHeight - marginY)) {
			out.println("Too close to the north wall");
			return false;
		} else if (heading > 90 && heading < 270 && y < marginY) {
			out.println("Too close to the south wall");
			return false;
		}

		// good to go
		return true;
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
			double aheadDistance = scanEvent.getEnergy() <= ROBOT_HIT_DAMAGE ? scanEvent
					.getDistance() * 4.0 : (scanEvent.getDistance() < 2.0 * width ? -width
					: scanEvent.getDistance() / 2.0);

			/*out.println(format("Turning right %.1f degrees and advancing %.1f pixels",
					toDegrees(turnRightRadians), aheadDistance));*/

			robot.setTurnRightRadians(turnRightRadians);
			if (wallSafe()) {
				robot.setAhead(aheadDistance);
			} else {
				robot.setAhead(0);
			}
		} else {
			err.println("Movement: Scan Event NULL!!");
		}
	}

	public void paint(Graphics2D g) {
		if (scanEvent != null) {
			double absoluteBearing = robot.getHeadingRadians() + scanEvent.getBearingRadians();
			double robotVelocity = max(1.0, robot.getVelocity());
			double turnRightRadians = absoluteBearing
					- robot.getHeadingRadians()
					+ (scanEvent.getVelocity()
							* sin(scanEvent.getHeadingRadians() - absoluteBearing) / robotVelocity);
			double aheadDistance = scanEvent.getEnergy() <= ROBOT_HIT_DAMAGE ? scanEvent
					.getDistance() * 4.0 : (scanEvent.getDistance() < 2.0 * width ? -width
					: scanEvent.getDistance() / 2.0);

			g.setColor(Color.CYAN);
			g.drawArc((int) robot.getX(), (int) robot.getY(), (int) aheadDistance,
					(int) aheadDistance, (int) robot.getHeading(),
					(int) toDegrees(turnRightRadians));
		}
	}
}
