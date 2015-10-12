package us.dowden.robocode.strategies;

import robocode.ScannedRobotEvent;

public interface Strategy {

	public abstract void scan(ScannedRobotEvent e);

}