
public class HashTest {

	public static long hash(String s, int start, int end) {
		if (start < 0) {
			start = 0;
		}

		if (end > s.length()) {
			end = s.length();
		}

		long h = 0L;

		for (int i = start; i < end; ++i) {
			h = (h << 5) - h + (long) s.charAt(i);
		}

		return h;
	}
	
	public static void main(String[] args) {
		System.out.println(hash("a1",0,2) & 1024L);
		System.out.println(hash("45",0,2) +","+ (hash("45",0,2) & 1024L));
	}

}
