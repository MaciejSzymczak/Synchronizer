
public class IcsTokenizer {
	String key;
	String value;
	IcsTokenizer (String s) {
		int i = s.indexOf(":");
		key = s.substring(0, i);
		value = s.substring(i+1, s.length());
	}
}
