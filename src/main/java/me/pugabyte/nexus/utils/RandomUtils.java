package me.pugabyte.nexus.utils;

import lombok.Getter;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RandomUtils {
	@Getter
	private static final Random random = new Random();

	public static boolean chanceOf(int chance) {
		return chanceOf((double) chance);
	}

	public static boolean chanceOf(double chance) {
		return randomInt(0, 100) <= chance;
	}

	public static int randomInt(int max) {
		return randomInt(0, max);
	}

	public static int randomInt(int min, int max) {
		if (min == max) return min;
		if (min > max) throw new InvalidInputException("Min cannot be greater than max!");
		return (int) ((random.nextDouble() * ((max - min) + 1)) + min);
	}

	public static double randomDouble(double max) {
		return randomDouble(0, max);
	}

	public static double randomDouble(double min, double max) {
		if (min == max) return min;
		if (min > max) throw new InvalidInputException("Min cannot be greater than max!");
		return min + (max - min) * random.nextDouble();
	}

	public static String randomAlphanumeric() {
		return randomElement(Utils.ALPHANUMERICS.split(""));
	}

	public static <T> T randomElement(Object... list) {
		return (T) randomElement(Arrays.asList(list));
	}

	public static <T> T randomElement(Set<T> list) {
		return randomElement(new ArrayList<>(list));
	}

	public static <T> T randomElement(List<T> list) {
		if (Utils.isNullOrEmpty(list)) return null;
		return list.get(random.nextInt(list.size()));
	}

	public static Vector randomVector() {
		double x = random.nextDouble() * 2 - 1;
		double y = random.nextDouble() * 2 - 1;
		double z = random.nextDouble() * 2 - 1;

		return new Vector(x, y, z).normalize();
	}

	public static Vector randomCircleVector() {
		double rnd = random.nextDouble() * 2 * Math.PI;
		double x = Math.cos(rnd);
		double z = Math.sin(rnd);

		return new Vector(x, 0, z);
	}

	public static Material randomMaterial() {
		return randomMaterial(Material.values());
	}

	public static Material randomMaterial(MaterialTag tag) {
		return randomMaterial(tag.getValues().toArray(new Material[0]));
	}

	public static Material randomMaterial(Material[] materials) {
		return materials[random.nextInt(materials.length)];
	}

	public static double randomAngle() {
		return random.nextDouble() * 2 * Math.PI;
	}

	public static <E> E getWeightedRandom(Map<E, Double> weights) {
		E result = null;
		double bestValue = Double.MAX_VALUE;

		for (E element : weights.keySet()) {
			double value = -Math.log(RandomUtils.getRandom().nextDouble()) / weights.get(element);

			if (value < bestValue) {
				bestValue = value;
				result = element;
			}
		}

		return result;
	}

}
