package nachos.proj1;

import nachos.threads.*;

public class CommunicatorTest {
	public CommunicatorTest() {
		Message = 0;
		numOfSpeakers = 5;
		numOfListeners = 5;
		communicator = new Communicator();
	}

	public void commTest(int num) {
		System.out.println("\n CommunicatorTest begin");
		for (int i = 0; i < num; i++) {
			createSpeakers(numOfSpeakers);
			createListeners(numOfListeners);
			System.out.println("\n speaker:" + numOfSpeakers);
			System.out.println("\n listener:" + numOfListeners);
			sleep(numOfSpeakers + numOfListeners);
			System.out.println("\n speaker and listener has created.");
		}
		System.out.println("CommunicatorTest end");
	}

	public void sleep(int numThreadsCreated) {
		ThreadedKernel.alarm.waitUntil(numThreadsCreated * 100);
	}

	public class Listener implements Runnable {
		public void run() {
			int messageToReceive = communicator.listen();
			System.out.print(Message);
			System.out.println(" " + KThread.currentThread().getName() + "receive" + messageToReceive);
		}
	}

	public class Speaker implements Runnable {
		public void run() {
			communicator.speak(Message++);
			System.out.print(Message);
			System.out.println(" " + KThread.currentThread().getName() + "send" + Message);

		}
	}

	public void createSpeakers(int speakers) {
		int j;
		for (j = 0; j <= speakers; j++) {
			KThread speakerThread = new KThread(new Speaker());
			speakerThread.setName("Speaker" + j);
			speakerThread.fork();
		}
		;
	}

	public void createListeners(int listeners) {
		int k;
		for (k = 0; k < listeners; k++) {
			KThread listenerThread = new KThread(new Listener());
			listenerThread.setName("Listener" + k);
			listenerThread.fork();
		}
	}

	public static int MAX_THREADS = 245;
	private int Message;
	private Communicator communicator;
	private int numOfSpeakers;
	private int numOfListeners;
}