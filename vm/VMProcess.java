package nachos.vm;

import nachos.machine.*;
import nachos.threads.Lock;
import nachos.userprog.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		VMKernel.tlbManager.clear();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		// super.restoreState();
	}

	/**
	 * 将这个方法重写后，就完全推翻了执行之前就得将所有内容加载到内存中这种做法 Initializes page tables for this
	 * process so that the executable can be demand-paged.
	 *
	 * @return <tt>	·true</tt> if successful.
	 */
	protected boolean loadSections() {
		lazyLoader = new LazyLoader(coff);

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	@Override
	protected void unloadSections() {
		coff.close();

		VMKernel.tlbManager.clear();

		// 这里是将用户进程的物理页回收，物理页有可能还未被申请
		for (int i = 0; i < numPages; i++) {
			PageItem item = new PageItem(PID, i);
			Integer ppn = VMKernel.invertedPageTable.remove(item);
			if (ppn != null) {
				VMKernel.memoryManager.removePage(ppn);
				VMKernel.coreMap[ppn].entry.valid = false;
			}

			VMKernel.getSwapper().deleteSwapPage(item);
		}
	}

	/**
	 * Handle a user exception. Called by
	 * <tt>UserKernel.exceptionHandler()</tt>. The <i>cause</i> argument
	 * identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionPageFault:
			handleTLBMissException(Processor.pageFromAddress(       // MIPS会将发生异常的虚拟地址写到寄存器中
					processor.readRegister(Processor.regBadVAddr)));
			break;
		case Processor.exceptionTLBMiss:
			handleTLBMissException(Processor.pageFromAddress(       // MIPS会将发生异常的虚拟地址写到寄存器中
					processor.readRegister(Processor.regBadVAddr)));
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private void handleTLBMissException(int vpn) {
		TranslationEntry entry = VMKernel.getPageEntry(new PageItem(PID, vpn));
		
		if(entry == null) {
			entry = handlePageFault(vpn);
			if (entry == null)
				handleExit(-1);
		}

		VMKernel.tlbManager.addEntry(entry);
	}

	/**
	 * 页缺少处理
	 */
	private TranslationEntry handlePageFault(int vpn) {
		lock.acquire();
		TranslationEntry result = VMKernel.memoryManager.swapIn(new PageItem(
				PID, vpn), lazyLoader);
		lock.release();
		return result;
	}

	@Override
	protected TranslationEntry getTranslationEntry(int vpn, boolean isWrite) {
		TranslationEntry result = VMKernel.tlbManager.find(vpn, isWrite);
		if (result == null) {
			handleTLBMissException(vpn);
			result = VMKernel.tlbManager.find(vpn, isWrite);
		}
		return result;
	}

	private static final char dbgVM = 'v';
	
	public static int numPageFaults = 0;

	private LazyLoader lazyLoader;
	private static Lock lock = new Lock();

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
}