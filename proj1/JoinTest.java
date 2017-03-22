package nachos.proj1;

import nachos.threads.*;

public class JoinTest {
	public JoinTest() {

	}

	public static void simpleJoinTest() {
		KThread A_thread = new KThread(new JoinTest.A_thread(5));
		KThread B_thread = new KThread(new JoinTest.B_thread(A_thread));
		B_thread.fork();
		B_thread.join();
	}

	public static class B_thread implements Runnable {
		B_thread(KThread joinee) {
			this.joinee = joinee;
		}

		public void run() {
			System.out.println("B is ready");
			System.out.println("forking and joining A...");
			this.joinee.fork();
			this.joinee.join();
			System.out.println("B is end");
		}

		private KThread joinee;
	}

	public static class A_thread implements Runnable {
		A_thread(int num) {
			this.num = num;
		}

		public void run() {
			System.out.println("A is ready");
			System.out.println("A is going on");

			for (int i = 0; i < this.num; i++) {
				System.out.println("A loops" + i + "times");
				KThread.currentThread().yield();
			}
			System.out.println("A is end");
		}

		private int num;
	}
}
