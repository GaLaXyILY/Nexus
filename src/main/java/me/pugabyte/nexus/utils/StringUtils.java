package me.pugabyte.nexus.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import joptsimple.internal.Strings;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import net.md_5.bungee.api.ChatColor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {
	@Getter
	private static final String colorChar = "§";
	@Getter
	private static final String altColorChar = "&";
	@Getter
	private static final String colorCharsRegex = "[" + colorChar + altColorChar + "]";
	@Getter
	private static final Pattern colorPattern = Pattern.compile(colorCharsRegex + "[0-9a-fA-F]");
	@Getter
	private static final Pattern formatPattern = Pattern.compile(colorCharsRegex + "[k-orK-OR]");
	@Getter
	private static final Pattern hexPattern = Pattern.compile(colorCharsRegex + "#[a-fA-F0-9]{6}");
	@Getter
	private static final Pattern hexColorizedPattern = Pattern.compile(colorCharsRegex + "x(" + colorCharsRegex + "[a-fA-F0-9]){6}");
	@Getter
	private static final Pattern colorGroupPattern = Pattern.compile("(" + colorPattern + "|(" + hexPattern + "|" + hexColorizedPattern + "))((" + formatPattern + ")+)?");
	@Getter
	public static final String CHECK = "&a✔";
	@Getter
	public static final String X = "&c✗";

	public static String getPrefix(Class<?> clazz) {
		return getPrefix(clazz.getSimpleName());
	}

	public static String getPrefix(String prefix) {
		return colorize("&8&l[&e" + prefix + "&8&l]&3 ");
	}

	public static String getDiscordPrefix(String prefix) {
		return "**[" + prefix + "]** ";
	}

	public static String colorize(String input) {
		if (input == null)
			return null;

		while (true) {
			Matcher matcher = hexPattern.matcher(input);
			if (!matcher.find()) break;

			String color = matcher.group();
			input = input.replace(color, ChatColor.of(color.replaceFirst(colorCharsRegex, "")).toString());
		}

		return ChatColor.translateAlternateColorCodes(altColorChar.charAt(0), input);
	}

	@Deprecated
	public static String decolorize(String input) {
		if (input == null)
			return null;

		input = colorize(input);

		while (true) {
			Matcher matcher = hexColorizedPattern.matcher(input);
			if (!matcher.find()) break;

			String color = matcher.group();
			input = input.replace(color, color.replace(colorChar + "x", "&#").replaceAll(colorChar, ""));
		}

		return input.replaceAll(colorChar, altColorChar);
	}

	public static String toHex(ChatColor color) {
		return "#" + Integer.toHexString(color.getColor().getRGB()).substring(2);
	}

	public static String stripColor(String input) {
		return ChatColor.stripColor(colorize(input));
	}

	public static String stripFormat(String input) {
		return formatPattern.matcher(colorize(input)).replaceAll("");
	}

	public static int countUpperCase(String s) {
		return (int) s.codePoints().filter(c-> c >= 'A' && c <= 'Z').count();
	}

	public static int countLowerCase(String s) {
		return (int) s.codePoints().filter(c-> c >= 'a' && c <= 'z').count();
	}

	// TODO This will break with hex
	public static String loreize(String string) {
		if (string == null) return null;

		int i = 0, lineLength = 0;
		boolean watchForNewLine = false, watchForColor = false;
		string = colorize(string);

		for (String character : string.split("")) {
			if (character.contains("\n")) {
				lineLength = 0;
				continue;
			}

			if (watchForNewLine) {
				if ("|".equalsIgnoreCase(character))
					lineLength = 0;
				watchForNewLine = false;
			} else if ("|".equalsIgnoreCase(character))
				watchForNewLine = true;

			if (watchForColor) {
				if (character.matches("[A-Fa-fK-Ok-oRr0-9]"))
					lineLength -= 2;
				watchForColor = false;
			} else if ("&".equalsIgnoreCase(character))
				watchForColor = true;

			++lineLength;

			if (lineLength > 28)
				if (" ".equalsIgnoreCase(character)) {
					String before = left(string, i);
					String excess = right(string, string.length() - i);
					if (excess.length() > 5) {
						excess = excess.trim();
						boolean doSplit = true;
						if (excess.contains("||") && excess.indexOf("||") <= 5)
							doSplit = false;
						if (excess.contains(" ") && excess.indexOf(" ") <= 5)
							doSplit = false;
						if (lineLength >= 38)
							doSplit = true;

						if (doSplit) {
							string = before + "||" + getLastColor(before) + excess.trim();
							lineLength = 0;
							i += 4;
						}
					}
				}

			++i;
		}

		return string;
	}

	public static List<String> splitLore(String lore) {
		return new ArrayList<>(Arrays.asList(lore.split("\\|\\|")));
	}

	public static String getLastColor(String text) {
		Matcher matcher = colorGroupPattern.matcher(text);
		String last = "";
		while (matcher.find())
			last = matcher.group();
		return last.toLowerCase();
	}

	public static String plural(String label, Number number) {
		return label + (number.doubleValue() == 1 ? "" : "s");
	}

	public static String trimFirst(String string) {
		return string.substring(1);
	}

	public static String right(String string, int number) {
		return string.substring(Math.max(string.length() - number, 0));
	}

	public static String left(String string, int number) {
		return string.substring(0, Math.min(number, string.length()));
	}

	public static String camelCase(Enum<?> _enum) {
		return camelCase(_enum.name());
	}

	public static String camelCase(String text) {
		if (Strings.isNullOrEmpty(text)) {
			return text;
		}

		return Arrays.stream(text.replaceAll("_", " ").split(" "))
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	public static String camelCaseWithUnderscores(String text) {
		if (Strings.isNullOrEmpty(text)) {
			return text;
		}

		return Arrays.stream(text.split("_"))
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
				.collect(Collectors.joining("_"));
	}

	public static String asOxfordList(List<String> items, String separator) {
		if (!separator.contains(", "))
			throw new InvalidInputException("Separator must contain ', '");

		String message = String.join(separator, items);
		int commaIndex = message.lastIndexOf(", ");
		message = new StringBuilder(message).replace(commaIndex, commaIndex + 2, (items.size() > 2 ? "," : "") + " and ").toString();
		return message;
	}

	public static String listFirst(String string, String delimiter) {
		return string.split(delimiter)[0];
	}

	public static String listLast(String string, String delimiter) {
		return string.substring(string.lastIndexOf(delimiter) + 1);
	}

	public static String listGetAt(String string, int index, String delimiter) {
		String[] split = string.split(delimiter);
		return split[index - 1];
	}

	public static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	public static String uuidFormat(String uuid) {
		uuid = uuidUnformat(uuid);
		String formatted = "";
		formatted += uuid.substring(0, 8) + "-";
		formatted += uuid.substring(8, 12) + "-";
		formatted += uuid.substring(12, 16) + "-";
		formatted += uuid.substring(16, 20) + "-";
		formatted += uuid.substring(20, 32);
		return formatted;
	}

	private static String uuidUnformat(String uuid) {
		return uuid.replaceAll("-", "");
	}

	public static final String UUID_REGEX = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";

	public static boolean isUuid(String uuid) {
		return uuid.matches(UUID_REGEX);
	}

	public static boolean isV4Uuid(UUID uuid) {
		return isV4Uuid(uuid.toString());
	}

	public static boolean isV4Uuid(String uuid) {
		return uuid.charAt(14) == '4';
	}

	public static boolean isValidJson(String json) {
		try {
			new JSONObject(json);
		} catch (JSONException ex) {
			try {
				new JSONArray(json);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	public static String toPrettyString(Object object) {
		try {
			return getPrettyPrinter().toJson(object);
		} catch (Exception | StackOverflowError ignored) {
			return object.toString();
		}
	}

	public static Gson getPrettyPrinter() {
		return new GsonBuilder().setPrettyPrinting().create();
	}

	public static String pretty(ItemStack item) {
		return item.getAmount() + " " + camelCase(item.getType().name());
	}

	private static final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();

	public static String pretty(Number number) {
		String format = trimFirst(moneyFormat.format(number));
		if (format.endsWith(".00"))
			format = left(format, format.length() - 3);

		return format;
	}

	public static String prettyMoney(Number number) {
		return "$" + pretty(number);
	}

	public static String stripTrailingZeros(String number) {
		return number.contains(".") ? number.replaceAll("0*$", "").replaceAll("\\.$", "") : number;
	}

	// Attempt to strip symbols and support euro formatting
	public static String asParsableDecimal(String value) {
		if (value == null)
			return "0";

		value = value.replace("$", "");
		if (value.contains(",") && value.contains("."))
			if (value.indexOf(",") < value.indexOf("."))
				value = value.replaceAll(",", "");
			else {
				value = value.replaceAll("\\.", "");
				value = value.replaceAll(",", ".");
			}
		else if (value.contains(",") && value.indexOf(",") == value.lastIndexOf(","))
			if (value.indexOf(",") == value.length() - 3)
				value = value.replace(",", ".");
			else
				value = value.replace(",", "");
		return value;
	}

	public static String ellipsis(String text, int length) {
		if (text.length() > length)
			return text.substring(0, length) + "...";
		else
			return text;
	}

	public static String bool(boolean b) {
		if (b)
			return "&atrue";
		else
			return "&cfalse";
	}

	public enum ProgressBarStyle {
		NONE,
		COUNT,
		PERCENT
	}

	public static String progressBar(int progress, int goal) {
		return progressBar(progress, goal, ProgressBarStyle.NONE, 25);
	}

	public static String progressBar(int progress, int goal, ProgressBarStyle style) {
		return progressBar(progress, goal, style, 25);
	}

	public static String progressBar(int progress, int goal, ProgressBarStyle style, int length) {
		double percent = Math.min((double) progress / goal, 1);
		ChatColor color = ChatColor.RED;
		if (percent == 1)
			color = ChatColor.GREEN;
		else if (percent >= 2/3)
			color = ChatColor.YELLOW;
		else if (percent >= 1/3)
			color = ChatColor.GOLD;

		int n = (int) Math.floor(percent * length);

		String bar = String.join("", Collections.nCopies(length, "|"));
		String first = left(bar, n);
		String last = right(bar, length - n);
		String result = color + first + "&8" + last;

		// TODO: Style
		if (style == ProgressBarStyle.COUNT)
			result += " &f" + progress + "/" + goal;
		if (style == ProgressBarStyle.PERCENT)
			result += " &f" + Math.floor(percent * 100);

		return result;
	}

	private static final String[] compassParts = {"[S]","SW","[W]","NW","[N]","NE","[E]","SE"};

	public static String compass(Player player) {
		return compass(player, 8);
	}

	public static String compass(Player player, int extra) {
		return compass(player, extra, 4);
	}

	public static String compass(Player player, int extra, int separators) {
		String compass = "";
		for (String compassPart : compassParts)
			compass += compassPart + " " + String.join("", Collections.nCopies(separators, "-")) + " ";

		float yaw = Location.normalizeYaw(player.getLocation().getYaw());
		if (yaw < 0) yaw = 360 + yaw;

		int center = (int) Math.round(yaw / (360D / compass.length())) + 1;

		String instance;
		if (center - extra < 0) {
			center += compass.length();
			instance = (compass + compass).substring(center - extra, center + extra + 1);
		} else if (center + extra + 1 > compass.length())
			instance = (compass + compass).substring(center - extra, center + extra + 1);
		else
			instance = compass.substring(center - extra, center + extra + 1);

		instance = instance.replaceAll("\\[", "&2[&f");
		instance = instance.replaceAll("]", "&2]&f");
		return colorize(instance);
	}

	public static String timespanDiff(LocalDate from) {
		return timespanDiff(from.atStartOfDay());
	}

	public static String timespanDiff(LocalDateTime from) {
		LocalDateTime now = LocalDateTime.now();
		if (from.isBefore(now))
			return timespanDiff(from, now);
		else
			return timespanDiff(now, from);
	}

	public static String timespanDiff(LocalDateTime from, LocalDateTime to) {
		return Timespan.of(Long.valueOf(from.until(to, ChronoUnit.SECONDS)).intValue()).format();
	}

	public enum TimespanFormatType {
		SHORT("y", "d", "h", "m", "s") {
			@Override
			public String get(String label, int value) {
				return label + " ";
			}
		},
		LONG("year", "day", "hour", "minute", "second") {
			@Override
			public String get(String label, int value) {
				return " " + plural(label, value) + " ";
			}
		};

		@Getter
		private final String year, day, hour, minute, second;

		TimespanFormatType(String year, String day, String hour, String minute, String second) {
			this.year = year;
			this.day = day;
			this.hour = hour;
			this.minute = minute;
			this.second = second;
		}

		abstract String get(String label, int value);
	}

	public static class Timespan {
		private final int original;
		private final boolean noneDisplay;
		private final TimespanFormatType formatType;
		private int years, days, hours, minutes, seconds;

		@lombok.Builder(buildMethodName = "_build")
		public Timespan(int seconds, boolean noneDisplay, TimespanFormatType formatType) {
			this.original = seconds;
			this.seconds = seconds;
			this.noneDisplay = noneDisplay;
			this.formatType = formatType == null ? TimespanFormatType.SHORT : formatType;
			calculate();
		}

		public static TimespanBuilder of(long seconds) {
			return of(Long.valueOf(seconds).intValue());
		}

		public static TimespanBuilder of(int seconds) {
			return Timespan.builder().seconds(seconds);
		}

		public static class TimespanBuilder {

			public String format() {
				return _build().format();
			}

			@Deprecated
			public Timespan build() {
				throw new UnsupportedOperationException("Use format()");
			}

		}

		private void calculate() {
			if (seconds == 0) return;

			years = seconds / 60 / 60 / 24 / 365;
			seconds -= years * 60 * 60 * 24 * 365;
			days = seconds / 60 / 60 / 24;
			seconds -= days * 60 * 60 * 24;
			hours = seconds / 60 / 60;
			seconds -= hours * 60 * 60;
			minutes = seconds / 60;
			seconds -= minutes * 60;
		}

		public String format() {
			if (original == 0 && noneDisplay)
				return "None";

			String result = "";
			if (years > 0)
				result += years + formatType.get(formatType.getYear(), years);
			if (days > 0)
				result += days + formatType.get(formatType.getDay(), days);
			if (hours > 0)
				result += hours + formatType.get(formatType.getHour(), hours);
			if (minutes > 0)
				result += minutes + formatType.get(formatType.getMinute(), minutes);
			if (years == 0 && days == 0 && hours == 0 && minutes > 0 && seconds > 0)
				result += seconds + formatType.get(formatType.getSecond(), seconds);

			if (result.length() == 0)
				result = original + formatType.get(formatType.getSecond(), seconds);

			return result.trim();
		}
	}

	public static String distanceMetricFormat(int cm) {
		int original = cm;
		int km = cm / 1000 / 100;
		cm -= km * 1000 * 100;
		int meters = cm / 100;
		cm -= meters * 100;

		String result = "";
		if (km > 0)
			result += km + "km ";
		if (meters > 0)
			result += meters + "m ";

		if (result.length() > 0)
			return result.trim();
		else
			return original + "cm";
	}

	public static String longDateTimeFormat(LocalDateTime dateTime) {
		return longDateFormat(dateTime.toLocalDate()) + " " + longTimeFormat(dateTime);
	}

	public static String shortDateTimeFormat(LocalDateTime dateTime) {
		return shortDateFormat(dateTime.toLocalDate()) + " " + shortTimeFormat(dateTime);
	}

	public static String longDateFormat(LocalDate date) {
		return camelCase(date.getMonth().name()) + " " + getNumberWithSuffix(date.getDayOfMonth()) + ", " + date.getYear();
	}

	public static String shortDateFormat(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("M/d/yy"));
	}

	public static String dateFormat(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
	}

	public static String longTimeFormat(LocalDateTime time) {
		return time.format(DateTimeFormatter.ofPattern("h:mm:ss a"));
	}

	public static String shortTimeFormat(LocalDateTime time) {
		return time.format(DateTimeFormatter.ofPattern("h:mm a"));
	}

	public static LocalDate parseShortDate(String input) {
		return LocalDate.from(DateTimeFormatter.ofPattern("M/d/yyyy").parse(input));
	}

	public static LocalDate parseDate(String input) {
		return LocalDate.parse(input);
	}
	public static LocalDateTime parseDateTime(String input) {
		return LocalDateTime.parse(input);
	}

	public static String getNumberWithSuffix(int number) {
		String text = String.valueOf(number);
		if (text.endsWith("1"))
			if (text.endsWith("11"))
				return number + "th";
			else
				return number + "st";
		else if (text.endsWith("2"))
			if (text.endsWith("12"))
				return number + "th";
			else
				return number + "nd";
		else if (text.endsWith("3"))
			if (text.endsWith("13"))
				return number + "th";
			else
				return number + "rd";
		else
			return number + "th";
	}

	@Getter
	private static final DecimalFormat df = new DecimalFormat("#.00");

	@Getter
	private static final DecimalFormat nf = new DecimalFormat("#");

	public static DecimalFormat getFormatter(Class<?> type) {
		if (Integer.class == type || Integer.TYPE == type) return nf;
		if (Double.class == type || Double.TYPE == type) return df;
		if (Float.class == type || Float.TYPE == type) return df;
		if (Short.class == type || Short.TYPE == type) return nf;
		if (Long.class == type || Long.TYPE == type) return nf;
		if (Byte.class == type || Byte.TYPE == type) return nf;
		if (BigDecimal.class == type) return df;
		throw new InvalidInputException("No formatter found for class " + type.getSimpleName());
	}

	public static String getLocationString(Location loc) {
		return "&3World: &e" + loc.getWorld().getName() + " &3x: &e" + df.format(loc.getX()) + " &3y: &e" +
				df.format(loc.getY()) + " &3z: &e" +  df.format(loc.getZ());
	}

	public static String getShortLocationString(Location loc) {
		return (int) loc.getX() + " " + (int) loc.getY() + " " +  (int) loc.getZ() + " " + loc.getWorld().getName();
	}

	public static void sendJsonLocation(String message, Location location, Player player) {
		int x = (int) location.getX();
		int y = (int) location.getY();
		int z = (int) location.getZ();
		double yaw = location.getYaw();
		double pitch = location.getPitch();
		String world = location.getWorld().getName();

		new JsonBuilder().next(message).command("/tppos " + x + " " + y + " " + z + " " + yaw + " " + pitch + " " + world).send(player);
	}

	private static final String HASTEBIN = "https://paste.bnn.gg/";

	@Data
	private static class PasteResult {
		private String key;
	}

	@SneakyThrows
	public static String paste(String content) {
		Request request = new Request.Builder().url(HASTEBIN + "documents").post(RequestBody.create(MediaType.get("text/plain"), content)).build();
		try (Response response = new OkHttpClient().newCall(request).execute()) {
			PasteResult result = new Gson().fromJson(response.body().string(), PasteResult.class);
			return HASTEBIN + result.getKey();
		}
	}

	@NonNull
	public static String getPaste(String code) throws InvalidInputException {
		try {
			Request request = new Request.Builder().url(HASTEBIN + "raw/" + code).get().build();
			try (Response response = new OkHttpClient().newCall(request).execute()) {
				return response.body().string();
			}
		} catch (Exception ex) {
			throw new InvalidInputException("An error occurred while retrieving the paste data: " + ex.getMessage());
		}
	}

	@RequiredArgsConstructor
	public static class Gradient {
		@NonNull
		private final ChatColor color1, color2;

		public static Gradient of(ChatColor color1, ChatColor color2) {
			return new Gradient(color1, color2);
		}

		public String apply(String text) {
			int l = text.length();
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < l; i++) {
				builder.append(ChatColor.of(new Color(
						(color1.getColor().getRed() + (i * (1F / l) * (color2.getColor().getRed() - color1.getColor().getRed()))) / 255,
						(color1.getColor().getGreen() + (i * (1F / l) * (color2.getColor().getGreen() - color1.getColor().getGreen()))) / 255,
						(color1.getColor().getBlue() + (i * (1F / l) * (color2.getColor().getBlue() - color1.getColor().getBlue()))) / 255
				)));
				builder.append(text.charAt(i));
			}
			return builder.toString();
		}
	}

	public static class Rainbow {
		public static String apply(String text) {
			StringBuilder builder = new StringBuilder();
			int l = text.length();
			for (int i = 0; i < l; i++) {
				builder.append(ChatColor.of(Color.getHSBColor(((float) i / l) * .75F, .9F, .9F)));
				builder.append(text.charAt(i));
			}
			return builder.toString();
		}
	}

}
