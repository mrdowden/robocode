package us.dowden.robocode;

public class RobotStats {

	/**
	 * Robot name.
	 */
	public final String name;

	/**
	 * How many times I've killed this bot.
	 */
	public int kills;

	/**
	 * How many times this bot has killed me.
	 */
	public int deaths;

	/**
	 * Damage I've dealt to this bot.
	 */
	public double damageDealt;

	/**
	 * Damage I've taken from this bot.
	 */
	public double damageTaken;

	public RobotStats(String name) {
		this.name = name;
	}

}
