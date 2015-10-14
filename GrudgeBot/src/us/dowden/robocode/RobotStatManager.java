package us.dowden.robocode;

import static java.lang.String.format;
import static java.lang.System.out;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RobotStatManager {

	private Map<String, RobotStats> data = new HashMap<>();
	private String lastDamageFrom;

	public void reset() {
		for (RobotStats rs : data.values()) {
			out.println(format("Starting Stats for %s: %d kills, %.2f damage", rs.name, rs.deaths,
					rs.damageTaken));
			rs.damageDealt = 0;
			rs.damageTaken = 0;
		}
	}

	public RobotStats getStats(String name) {
		if (!data.containsKey(name)) {
			data.put(name, new RobotStats(name));
		}
		return data.get(name);
	}

	public Stream<RobotStats> stream() {
		return data.values().stream();
	}

	/**
	 * How many times I've killed this bot.
	 */
	public void addKill(String name) {
		getStats(name).kills++;
		out.println(format("Killed %s", name));
	}

	/**
	 * How many times this bot has killed me.
	 */
	public void addDeath() {
		if (lastDamageFrom != null) {
			getStats(lastDamageFrom).deaths++;
			out.println(format("Killed by %s", lastDamageFrom));
		}
	}

	/**
	 * Damage I've dealt to this bot.
	 */
	public void addDamageDealt(String name, double energy) {
		getStats(name).damageDealt += energy;
		out.println(format("Dealt %.2f damage to %s", energy, name));
	}

	/**
	 * Damage I've taken from this bot.
	 */
	public void addDamageTaken(String name, double energy) {
		lastDamageFrom = name;
		getStats(name).damageTaken += energy;
		out.println(format("Took %.2f damage from %s", energy, name));
	}

}
