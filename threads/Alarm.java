package nachos.threads;

import java.util.*;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	AlarmWait alarmWait = null;
    	for(int i = 0; i < alarmWaitQueue.size(); i ++) {
    		if(Machine.timer().getTime() >= alarmWaitQueue.get(i).wakeTime) {
    			alarmWait = alarmWaitQueue.remove(i);
    			System.out.println("唤醒线程："+alarmWait.thread.getName()+
    					"，时间为："+Machine.timer().getTime());
    			alarmWait.thread.ready();
    		}
    	}
    	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    	long wakeTime = Machine.timer().getTime() + x; //等待时间
    	
    	boolean intStatus = Machine.interrupt().disable();
    	AlarmWait alarmWait = new AlarmWait(KThread.currentThread(), wakeTime);
    	alarmWaitQueue.add(alarmWait);
    	System.out.println(KThread.currentThread().getName() +
    			"线程休眠，时间为："+Machine.timer().getTime()+",应在"+wakeTime+"醒来.");
    	KThread.sleep();
    	Machine.interrupt().restore(intStatus);
	}
    
    private LinkedList<AlarmWait> alarmWaitQueue = new LinkedList<AlarmWait>(); //闹钟等待队列
    
    private class AlarmWait {
    	AlarmWait(KThread thread, long wakeTime) {
    		this.thread = thread;
    		this.wakeTime = wakeTime;
    	}
    	
    	/* 这些方法暂时用不上
    	public KThread getThread() {
    		return thread;
    	}
    	
    	public long getWakeTime() {
    		return wakeTime;
    	}
    	
    	public void setThread(KThread thread) {
    		this.thread = thread;
    	}
    	
    	public void setWakeTime(long wakeTime) {
    		this.wakeTime = wakeTime;
    	}
    	*/
    	
    	private KThread thread;
    	private long wakeTime;
    }
}
