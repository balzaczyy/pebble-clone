package cn.zhouyiyan.pebble;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class Formatter {
	private final ResourceBundle rb;
	private final Locale loc;
	private final TimeZone tz;

	public Formatter(Locale loc, TimeZone tz) {
		this.rb = ResourceBundle.getBundle("resources", loc);
		this.loc = loc;
		this.tz = tz;
	}

	public String message(String key) {
		return rb.getString(key);
	}

	public String message(String key, String var) {
		return rb.getString(key).replace("{0}", var);
	}

	public String message(String key, String var1, String var2) {
		return rb.getString(key).replace("{0}", var1).replace("{1}", var2);
	}

	public String formatDate(Date date, String pattern) {
		DateFormat df = new SimpleDateFormat(pattern, loc);
		df.setTimeZone(tz);
		return df.format(date);
	}

	public String longDate(Date date) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, loc);
		df.setTimeZone(tz);
		return df.format(date);
	}

	public String longDate(Date date, TimeZone overridedTz) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, loc);
		df.setTimeZone(overridedTz);
		return df.format(date);
	}

	public String mediumDateShortTime(Date date) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, loc);
		df.setTimeZone(tz);
		return df.format(date);
	}

	public String formatDayOfMonth(Calendar cal) {
		String ans = NumberFormat.getIntegerInstance(loc).format(cal.get(Calendar.DAY_OF_MONTH));
		if (ans.length() == 1) ans = "&nbsp;" + ans;
		return ans;
	}

	public String validOrEmpty(String value) {
		return value != null ? value : "";
	}
}