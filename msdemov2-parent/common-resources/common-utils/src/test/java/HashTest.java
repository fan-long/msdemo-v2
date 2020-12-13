import java.util.HashSet;

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
//		System.out.println(hash("a1",0,2) & 1024L);
//		System.out.println(hash("45",0,2) +","+ (hash("45",0,2) & 1024L));
		
		A a1= new A(1);
		A a2 = new A(1);
		HashSet<A> set = new HashSet<>();
		set.add(a1);
		set.add(a2);
		System.out.println(set.size());
	}

	
	public static class A {
		int aa;
		
		public A(int param){
			aa=param;
		}
		@Override
		public int hashCode(){
			return aa;
		}
		@Override
		public String toString(){
			return aa+"";
		}
		@Override
		public boolean equals(Object obj){
			return this.toString().equals(obj.toString());
		}
	}
}
