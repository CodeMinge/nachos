package nachos.vm;

import java.util.HashSet;
import java.util.LinkedList;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class ClockPagingManager extends MemoryManager {

	public ClockPagingManager() {
		numPhysPages = Machine.processor().getNumPhysPages();
		for (int i = 0; i < numPhysPages; i++)
			freePages.add(i);
	}

	/**
	 * 回收页面
	 */
	@Override
	protected void removePage(int ppn) {
		usedQueue.remove(new Integer(ppn));
		freePages.add(ppn);
	}

	/**
	 * 申请页面
	 */
	@Override
	protected int nextPage() {
		if (!freePages.isEmpty())
			return freePages.removeFirst();

		for (ptr %= numPhysPages;; ptr = (ptr + 1) % numPhysPages)
			if (usedQueue.contains(ptr))
				if (VMKernel.coreMap[ptr].entry.used)
					VMKernel.coreMap[ptr].entry.used = false;
				else {
					usedQueue.remove(new Integer(ptr));
					return ptr++;
				}
	}

	/**
	 * 页面的换入操作
	 */
	@Override
	public TranslationEntry swapIn(PageItem item, LazyLoader lazyLoader) {
		VMKernel.tlbManager.flush();
		int ppn = nextPage(); // 找到下一可用页
		swapOut(ppn);

		TranslationEntry entry = lazyLoader.load(item, ppn);
		usedQueue.add(ppn);
		VMKernel.invertedPageTable.put(item, ppn);
		VMKernel.coreMap[ppn] = new Page(item, entry);

		return entry;
	}

	//TLB搜索
	@Override
	protected int seekInTLB(int vpn) {
		for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.valid && entry.vpn == vpn)
				return i;
		}
		return -1;
	}

	private int ptr = 0;
	private int numPhysPages;
	private HashSet<Integer> usedQueue = new HashSet<Integer>();         // 已用队列
	private LinkedList<Integer> freePages = new LinkedList<Integer>();   // 空闲队列
}