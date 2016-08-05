package org.gillius.jagnet;

public class TestCopy {
	public static void main(String[] args) {
		KryoCopier copier = new KryoCopier().register(A.class, B.class);

		A a = new A();
		a.x = 1;
		a.y = 2;

		A a2 = new A();

		System.out.println(a);
		System.out.println(a2);

		copier.copy(a, a2);

		System.out.println(a);
		System.out.println(a2);

		B b = new B();
		b.x = 1; b.y = 2; b.z = 3;
		B b2 = new B();
		System.out.println(b);

		copier.copy(b, b2);
		System.out.println(b2);
	}

	public static class A {
		public int x, y;

		@Override
		public String toString() {
			return "A{" +
			       "x=" + x +
			       ", y=" + y +
			       '}';
		}
	}

	public static class B {
		private int x, y, z;

		@Override
		public String toString() {
			return "B{" +
			       "x=" + x +
			       ", y=" + y +
			       ", z=" + z +
			       '}';
		}
	}
}
