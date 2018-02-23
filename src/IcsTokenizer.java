
public class IcsTokenizer {
	String key;
	String value;
	IcsTokenizer (String s) {
		if (s==null || s.length()==0) {s=":";} //ignore empty lines
		int i = s.indexOf(":");
		key = s.substring(0, i);
		value = s.substring(i+1, s.length());
	}
}
