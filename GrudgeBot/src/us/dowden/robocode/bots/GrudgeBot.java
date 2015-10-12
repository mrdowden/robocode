package us.dowden.robocode.bots;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import us.dowden.robocode.strategies.ChaseMovement;
import us.dowden.robocode.strategies.MovementStrategy;
import us.dowden.robocode.strategies.NoniterativeLinearTargeting;
import us.dowden.robocode.strategies.RadarStrategy;
import us.dowden.robocode.strategies.TargetingStrategy;
import us.dowden.robocode.strategies.WidthLockRadar;

public class GrudgeBot extends AdvancedRobot {

	private ScannedRobotEvent target;
	private RadarStrategy radarStrategy = new WidthLockRadar(this);
	private TargetingStrategy gunStrategy = new NoniterativeLinearTargeting(this);
	private MovementStrategy moveStrategy = new ChaseMovement(this);

	@Override
	public void run() {
		setBodyColor(new Color(0, 0, 0));
		setGunColor(new Color(237, 237, 237));
		setRadarColor(new Color(180, 180, 180));
		setBulletColor(new Color(16, 255, 0));
		setScanColor(new Color(102, 211, 255));
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		do {
			radarStrategy.turn();
			gunStrategy.fire();
			moveStrategy.move();
			execute();
		} while (true);
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		// Ignore anything that's not our current target
		if (target == null || target.getName().equals(e.getName())) {
			// Acquire target
			target = e;

			radarStrategy.scan(e);
			gunStrategy.scan(e);
			moveStrategy.scan(e);
		}
	}

}
