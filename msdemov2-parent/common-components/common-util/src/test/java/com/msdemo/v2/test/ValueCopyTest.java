package com.msdemo.v2.test;
import com.msdemo.v2.common.util.ValueCopy;
import com.msdemo.v2.common.util.ValueCopyUtils;

public class ValueCopyTest {
	
	public static class A{
		private String a;

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}
		
	}
	
	public static class B extends A{
		private String b;

		public String getB() {
			return b;
		}

		public void setB(String b) {
			this.b = b;
		}		
	}
	
	public static class C extends A{
		@ValueCopy(ignored=true)
		private B b;

		public B getB() {
			return b;
		}

		public void setB(B b) {
			this.b = b;
		}		
	}
	
	public static void main(String[] args){
		B from= new B();
		from.setA("1");
		from.setB("2");
		C to = new C();
		ValueCopyUtils.beanToBean(from, to);
		System.out.println(to.getA()+","+to.getB());
	}
	

}
