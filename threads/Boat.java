package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
	
	public Boat(int children, int adult) {
		
	}

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		 System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		 begin(1, 2, b);

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

//		parentThread = 
		childOnO = children;
		adultOnO = adults;
		for(int i = 0; i < adults; i ++)
			AdultItinerary();
		for(int i = 0; i < children; i ++)
			ChildItinerary();

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
		//System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		/*bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();*/
		
	}
	
	static BoatGrader bg;
		
	static final int boatSize = 2; // 船的承载能力
	static final int childSize = 1; // 小孩占船承载的程度
	static final int adultSize = 1; // 大人占船承载的程度
	static int passengerNum = 0; // 船上乘客的数量
	static int childOnO = 0; // 在O岛的孩子数目
	static int adultOnO = 0; // 在O岛的大人数目
	static int childOnM = 0; // 在M岛的孩子数目
	static int location = 0; // 船的位置，在M岛或O岛，0是在O岛
	static Lock boatlock = new Lock(); // 控制对船使用的锁
	// 在O岛，M岛上睡眠的大人或小孩的条件变量
	static Condition OA = new Condition(boatlock);
	static Condition OC = new Condition(boatlock);
	static Condition MA = new Condition(boatlock);
	static Condition MC = new Condition(boatlock);  
	static boolean overed = false;  //问题是否已经解决
	
	/**对于孩子线程：
	1)由于孩子要在O岛M岛之间来回奔波，所以孩子线程有必要了解自己的位置信息来判断自己的任务，故孩子类中有myLocation变量指明孩子的位置信息。
	2)如果孩子线程和船都在O岛，如果船上没有人，则获取船。如果此时O岛上还有其他孩子，则线程开始旅行。并且在到达M岛后睡眠；如果此时O岛上没有其他孩子则放弃此次对船的占有，尝试唤醒O岛上的大人。
	3)如果孩子线程和船都在O岛，且船上已经有一个孩子作为乘客，则该线程作为乘客获取船驶向M岛，并且做问题解决检查，如果O岛上在自己离开后没有人了的话则问题解决，finished置为真。如果没有结束，这个孩子将承担pilot的责任将船驶回O岛。
	4)如果孩子线程和船都在O岛，但船上已经满载，则放弃船。
	5)如果孩子线程和船都在M岛，则乘船驶回O岛唤醒O岛上的孩子大人线程进行船的资源争夺。
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
	
	/** 对于大人线程：
	大人的工作非常简单，只要自己从O岛到达M岛即可。首先大人进程会试图得到船的锁，在得到锁之后进行一系列的检查:
	1)如果船的位置在O岛这边且M岛上没有孩子，这意味着如果大人上了船到达了M岛的话，除非自己开着船回来否则问题得不到解决，但是大人开船回来是一件没有意义的事情，故在这种情况下大人放弃对船的占用，在O岛睡眠等待并且释放锁。
	2)如果M岛上有孩子，船在O岛这边且船上剩余容量可以容下一个大人，那么大人线程获取船并驶向M岛，修改这个大人线程以及船的位置信息，在到达之后唤醒在M岛上休眠等待的孩子线程并且释放锁。
	3)如果船的位置在O岛这边且M岛上有孩子，但是船上已经没有容得下一个大人的位置了，此时大人线程放弃这次对船的占有在O岛继续睡眠。
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
