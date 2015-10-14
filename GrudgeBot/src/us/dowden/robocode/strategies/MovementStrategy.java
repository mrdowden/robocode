package us.dowden.robocode.strategies;

import java.awt.Graphics2D;

public interface MovementStrategy extends Strategy {

	public void move();

	public void paint(Graphics2D g);
}
