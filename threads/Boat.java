package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
	
	public Boat(int children, int adult) {
		passengerNum = 0;
		childOnO = children; 
		adultOnO = adult; 
		childOnM = 0; 
		location = 0; 		
		boatlock = new Lock(); 
		OA = new Condition(boatlock);
		OC = new Condition(boatlock);
		MA = new Condition(boatlock);
		MC = new Condition(boatlock);; 
		overed = false;
		
		selfTest();
	}

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with " + childOnO + " children, " + adultOnO + "adult***");
		begin(adultOnO, childOnO, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1
		// adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3
		// adults***");
		// begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		Runnable r = new Runnable() {
			public void run() {
				SampleItinerary();
			}
		};
		KThread t = new KThread(r);
		t.setName("Sample Boat Thread");
		t.fork();

	}

	static void AdultItinerary() {
		bg.initializeAdult(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.

		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
		
		KThread t = new KThread(new BoatAdult());
		t.fork();
	}

	static void ChildItinerary() {
		bg.initializeChild(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.
		
		KThread t = new KThread(new BoatChild());
		t.fork();
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		/*bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();*/
		for(int i = 0; i < childOnO; i ++)
			ChildItinerary();
		for(int i = 0; i < adultOnO; i ++)
			AdultItinerary();
	}
	
	static BoatGrader bg;
	
	static final int boatSize = 2; // ���ĳ�������
	static final int childSize = 1; // С��ռ�����صĳ̶�
	static final int adultSize = 1; // ����ռ�����صĳ̶�
	static int passengerNum; // ���ϳ˿͵�����
	static int childOnO; // ��O���ĺ�����Ŀ
	static int adultOnO; // ��O���Ĵ�����Ŀ
	static int childOnM; // ��M���ĺ�����Ŀ
	static int location; // ����λ�ã���M����O����0����O��
	static Lock boatlock; // ���ƶԴ�ʹ�õ���
	static Condition OA, OC, MA, MC; // ��O����M����˯�ߵĴ��˻�С������������
	static boolean overed; //�����Ƿ��Ѿ����
	
	/**���ں����̣߳�
	1)���ں���Ҫ��O��M��֮�����ر��������Ժ����߳��б�Ҫ�˽��Լ���λ����Ϣ���ж��Լ������񣬹ʺ���������myLocation����ָ�����ӵ�λ����Ϣ��
	2)��������̺߳ʹ�����O�����������û���ˣ����ȡ���������ʱO���ϻ����������ӣ����߳̿�ʼ���С������ڵ���M����˯�ߣ������ʱO����û����������������˴ζԴ���ռ�У����Ի���O���ϵĴ��ˡ�
	3)��������̺߳ʹ�����O�����Ҵ����Ѿ���һ��������Ϊ�˿ͣ�����߳���Ϊ�˿ͻ�ȡ��ʻ��M������������������飬���O�������Լ��뿪��û�����˵Ļ�����������finished��Ϊ�档���û�н�����������ӽ��е�pilot�����ν���ʻ��O����
	4)��������̺߳ʹ�����O�����������Ѿ����أ����������
	5)��������̺߳ʹ�����M������˴�ʻ��O������O���ϵĺ��Ӵ����߳̽��д�����Դ���ᡣ
	 *
	 */
	public static class BoatChild implements Runnable {
		
		BoatChild() {
			mylocation = 0;
		}

		public void run() {
			while(!overed) {
				boatlock.acquire();
				
				if(mylocation == 0 && location == 0) {
					if(passengerNum == 0) {
						passengerNum ++;
						childOnO --;
						mylocation = 1;
						
						if(childOnO != 0) {
							bg.ChildRowToMolokai();
							MC.sleep();
							boatlock.release();
						}
						else {
							System.out.println("------------");
							passengerNum --;
							childOnO ++;
							mylocation = 0;
							OA.wake();
							OC.sleep();
							boatlock.release();
						}
					}
					else if(passengerNum == 1) {
						passengerNum ++;
						childOnO --;
						childOnM += 2;
						bg.ChildRowToMolokai();	
						mylocation = 1;
						location = 1;
						if(adultOnO == 0 && childOnO == 0) {
							overed = true;
						}
						boatlock.release();
					}
					else if(passengerNum == 2) {
						System.out.println("**no seat.child sleep on o.");
						OC.sleep();
						boatlock.release();
					}
				}
				else if(mylocation == 1 && location == 1) {
					passengerNum = 0;
					childOnM --;
					childOnO ++;
					bg.ChildRowToOahu();
					mylocation = 0;
					location = 0;
					OA.wake();
					OC.wake();
					boatlock.release();
				}
				else {
					if(mylocation == 1) {
						System.out.println("**child sleep on m.");
						MC.sleep();
						boatlock.release();
					}
					else if(mylocation == 0) {
						System.out.println("**child sleep on o.");
						OC.sleep();
						boatlock.release();
					}
				}
			}
		}
		
		int mylocation;
	}
	
	/** ���ڴ����̣߳�
	���˵Ĺ����ǳ��򵥣�ֻҪ�Լ���O������M�����ɡ����ȴ��˽��̻���ͼ�õ����������ڵõ���֮�����һϵ�еļ��:
	1)�������λ����O�������M����û�к��ӣ�����ζ������������˴�������M���Ļ��������Լ����Ŵ�������������ò�����������Ǵ��˿���������һ��û����������飬������������´��˷����Դ���ռ�ã���O��˯�ߵȴ������ͷ�����
	2)���M�����к��ӣ�����O������Ҵ���ʣ��������������һ�����ˣ���ô�����̻߳�ȡ����ʻ��M�����޸���������߳��Լ�����λ����Ϣ���ڵ���֮������M�������ߵȴ��ĺ����̲߳����ͷ�����
	3)�������λ����O�������M�����к��ӣ����Ǵ����Ѿ�û���ݵ���һ�����˵�λ���ˣ���ʱ�����̷߳�����ζԴ���ռ����O������˯�ߡ�
	 */
	public static class BoatAdult implements Runnable {

		public void run() {
			while(!overed) {
				boatlock.acquire();
				
				if(location == 0 && childOnM == 0) {
					System.out.println("**Adult sleep on o.");
					OA.sleep();
					boatlock.release();
				}
				else if(location == 0 && childOnM != 0 && passengerNum == 0) {
					passengerNum += 2;
					bg.AdultRowToMolokai();
					location = 1;
					MC.wake();
					
					passengerNum -= 2;
					adultOnO --;
					if(adultOnO == 0 && childOnO == 0) {
						overed = true;
					}
					boatlock.release();
				}
				else if(location == 0 && childOnM != 0 && passengerNum != 0) {
					System.out.println("**no seat.Adult sleep on o.");
					OA.sleep();
					boatlock.release();
				}
			}	
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
