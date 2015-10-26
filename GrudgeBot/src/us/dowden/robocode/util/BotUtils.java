package us.dowden.robocode.util;

public final class BotUtils {

	public static int compareDouble(double d1, double d2) {
		return (int) Math.round(Double.compare(d1, d2));
	}

}
