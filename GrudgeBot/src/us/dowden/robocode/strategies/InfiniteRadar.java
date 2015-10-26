package us.dowden.robocode.strategies;

import static java.lang.Double.POSITIVE_INFINITY;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class InfiniteRadar implements RadarStrategy {
	private AdvancedRobot robot;

	public InfiniteRadar(AdvancedRobot robot) {
		this.robot = robot;
	}

	@Override
	public void scan(ScannedRobotEvent e) {
	}

	@Override
	public void turn(long time) {
		if (robot.getRadarTurnRemaining() == 0.0)
			robot.setTurnRadarRightRadians(POSITIVE_INFINITY);
	}

}
