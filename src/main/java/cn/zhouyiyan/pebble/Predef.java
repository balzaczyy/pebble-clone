package cn.zhouyiyan.pebble;

import java.util.HashSet;
import java.util.Set;

public final class Predef {
	public static Set<String> set(String... values) {
		Set<String> ans = new HashSet<String>();
		for (String value : values) {
			ans.add(value);
		}
		return ans;
	}
}
