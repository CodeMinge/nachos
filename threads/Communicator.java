package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	lock = new Lock();
    	speaker = new Condition(lock);
    	litener = new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    /**
     * 先获得锁，然后进行判断，如果没有听者等待，就要把说者的话e放入队列然后睡眠。
     * 如果有听者等待，就要唤醒一个听者，然后传递消息，最后释放锁。
     * @param word
     */
    public void speak(int word) {
    	boolean intStatus = Machine.interrupt().disable();
    	lock.acquire();
    	
    	if(num_litener == 0) {
    		num_speaker ++;
    		queue.add(word);
    		speaker.sleep();
    		litener.wake();
    		num_speaker --; 
    	}
    	else {
    		queue.add(word);
    		litener.wake();
    	}
    	
    	lock.release();
    	Machine.interrupt().restore(intStatus);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    /**
     * 先获得锁，然后进行判断尝试唤醒speaker，如果没有说者等待，就要把听者放入队列然后睡眠。
     * 如果有说者等待，就要唤醒一个说者，将自己挂起以等待speaker准备好数据再将自己唤醒，然后传递消息，最后释放锁。
     * @return
     */
    public int listen() {
    	boolean intStatus = Machine.interrupt().disable();
    	lock.acquire();
    	
    	if(num_speaker == 0) {
    		num_litener ++;
    		litener.sleep();
    		num_litener --; 
    	}
    	else {
    		litener.sleep();
    		speaker.wake();
    	}
    	
    	lock.release();
    	Machine.interrupt().restore(intStatus);
    	
    	return queue.poll();
    }
    
    private Lock lock; //互斥锁
    Condition speaker;  //说者条件变量
    Condition litener; //听者条件变量
    private static int num_speaker = 0;
    private static int num_litener = 0;
    LinkedList<Integer> queue = new LinkedList<Integer>(); //说话者话语数量
}
