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
     * �Ȼ������Ȼ������жϣ����û�����ߵȴ�����Ҫ��˵�ߵĻ�e�������Ȼ��˯�ߡ�
     * ��������ߵȴ�����Ҫ����һ�����ߣ�Ȼ�󴫵���Ϣ������ͷ�����
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
     * �Ȼ������Ȼ������жϳ��Ի���speaker�����û��˵�ߵȴ�����Ҫ�����߷������Ȼ��˯�ߡ�
     * �����˵�ߵȴ�����Ҫ����һ��˵�ߣ����Լ������Եȴ�speaker׼���������ٽ��Լ����ѣ�Ȼ�󴫵���Ϣ������ͷ�����
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
    
    private Lock lock; //������
    Condition speaker;  //˵����������
    Condition litener; //������������
    private static int num_speaker = 0;
    private static int num_litener = 0;
    LinkedList<Integer> queue = new LinkedList<Integer>(); //˵���߻�������
}
