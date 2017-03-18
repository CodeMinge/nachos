package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.*;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);

		pid = numOfProcess++;
		numOfRunningProcess++;
		// 线程创建后，键盘输入流和显示输出流在不调用open方法的情况下自动打开
		openfile[0] = UserKernel.console.openForReading();
		openfile[1] = UserKernel.console.openForWriting();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		// 从虚拟内存地址读出到data中
		// 偏移量与长度都要为正数，偏移量+长度<=总的数据长度
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
		// getMemory会返回主程序的数组
		byte[] memory = Machine.processor().getMemory(); // 获得物理数组
		// 计算剩下的页表字节个数
		if (length > (pageSize * numPages - vaddr))
			length = pageSize * numPages - vaddr;
		// 计算能够传输的数据的大小，如果data数组中存不下length，则减小length（传输字节数）
		if (data.length - offset < length)
			length = data.length - offset;
		// 转换成功的字节数
		int transferredbyte = 0;
		do {
			// 计算页号
			int pageNum = Processor.pageFromAddress(vaddr + transferredbyte);
			// 页号大于 页表的长度 或者 为负 是异常情况
			if (pageNum < 0 || pageNum >= pageTable.length)
				return 0;
			// 计算页偏移量
			int pageOffset = Processor.offsetFromAddress(vaddr + transferredbyte);
			// 计算剩余页的容量
			int leftByte = pageSize - pageOffset;
			// 计算下一次传送的数量:剩余页容量和需要转移的字节数中较小者
			int amount = Math.min(leftByte, length - transferredbyte);
			// 计算物理内存的地址
			int realAddress = pageTable[pageNum].ppn * pageSize + pageOffset;
			// 将物理内存的东西传输到虚拟内存
			System.arraycopy(memory, realAddress, data, offset + transferredbyte, amount);
			// 修改传输成功的字节数
			transferredbyte = transferredbyte + amount;
		} while (transferredbyte < length);

		return transferredbyte;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
		// 物理内存
		byte[] memory = Machine.processor().getMemory();
		// 写内存的长度如果超过页剩余量
		if (length > (pageSize * numPages - vaddr))
			length = pageSize * numPages - vaddr;
		// 如果数组中要写的长度比给定的小，则给length减为数组剩余的长度
		if (data.length - offset < length)
			length = data.length - offset;
		int transferredbyte = 0;
		do {
			// 此函数返回给定地址的页号
			int pageNum = Processor.pageFromAddress(vaddr + transferredbyte);
			if (pageNum < 0 || pageNum >= pageTable.length)
				return 0;
			// 此函数返回给定地址的页偏移量
			int pageOffset = Processor.offsetFromAddress(vaddr + transferredbyte);
			// 页剩余的字节数
			int leftByte = pageSize - pageOffset;
			// 设置本次转移的数量
			int amount = Math.min(leftByte, length - transferredbyte);
			int realAddress = pageTable[pageNum].ppn * pageSize + pageOffset;
			// 从虚拟内存写入到物理内存
			System.arraycopy(data, offset + transferredbyte, memory, realAddress, amount);
			// 改变写成功的字节数
			transferredbyte = transferredbyte + amount;
		} while (transferredbyte < length);

		return transferredbyte;
		// for now, just assume that virtual addresses equal physical addresses
		// if (vaddr < 0 || vaddr >= memory.length)
		// return 0;
		//
		// int amount = Math.min(length, memory.length - vaddr);
		// System.arraycopy(data, offset, memory, vaddr, amount);
		//
		// return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 *
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		UserKernel.allocateMemoryLock.acquire();// 获取分配的内存的锁
		// 进程所需要的页面数已知，保存在numPages中
		// 判断能否分配内存
		// numPages中 已经确定了需要页的数量
		// 页数量如果大于实际物理内存的页数量，则不能加载
		if (numPages > Machine.processor().getNumPhysPages()) {
			// 判断能否装载
			coff.close();
			// 缺少物理内存（如果程序的页数大于处理器的空闲物理页数就会失败）
			Lib.debug(dbgProcess, "\t insufficient（缺少物理地址） physical memory");
			UserKernel.allocateMemoryLock.release();
			return false;// 返回装载失败
		}
		pageTable = new TranslationEntry[numPages];// 实例化页表
		for (int i = 0; i < numPages; i++) {
			// 从空闲物理页号链表中拿出一个
			int nextPage = UserKernel.memoryLinkedList.remove();
			pageTable[i] = new TranslationEntry(i, nextPage, true, false, false, false);
		}
		UserKernel.allocateMemoryLock.release();
		// load sections（段），一个段是由很多页组成
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s); // coff获得Section
			Lib.debug(dbgProcess,
					"\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");
			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;// 得到每一段虚拟页号（有页号偏移i），section不同
				pageTable[vpn].readOnly = section.isReadOnly();// 标记为只读
				// for now, just assume virtual addresses=physical addresses
				// 装入物理页
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}
		return true;
	}

	/**
	 * 释放内存 Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		UserKernel.allocateMemoryLock.acquire();

		for (int i = 0; i < numPages; i++) {
			UserKernel.memoryLinkedList.add(pageTable[i].ppn); // 将该用户进程占用的内存加入空闲内存链表中
			pageTable[i] = null;
		}

		UserKernel.allocateMemoryLock.release();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		// 处理halt系统调用，只有根进程可以
		if (pid == 0)
			Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exec() system call. 创建一个子进程，并执行
	 */
	private int handleExec(int fileAddress, int argc, int argvAddress) {
		// 从内存中读出这个进程使用的文件的名字
		String filename = readVirtualMemoryString(fileAddress, 256);
		if (filename == null || argc < 0 || argvAddress < 0 || argvAddress > numPages * pageSize)
			return -1;

		// 得到子进程的参数表
		String[] args = new String[argc];
		for (int i = 0; i < argc; i++) {
			byte[] argsAddress = new byte[4];
			if (readVirtualMemory(argvAddress + i * 4, argsAddress) > 0)
				args[i] = readVirtualMemoryString(Lib.bytesToInt(argsAddress, 0), 256);
		}

		UserProcess process = UserProcess.newUserProcess();
		if (!process.execute(filename, args))
			return -1;

		process.parentProcess = this; // 将父进程指向调用这个方法的进程
		childProcess.add(process); // 在子进程的链表中加入新创建的进程
		return process.pid;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(int status) {
		coff.close(); // 关闭文件
		for (int i = 0; i < 16; i++) {// 关闭所有打开的文件
			if (openfile[i] != null) {
				openfile[i].close();
				openfile[i] = null;
			}
		}
		this.status = status;// 把状态置入
		normalExit = true;// 属于正常退出

		// 如果有父进程，就从父进程的子进程链表中删除，而且如果父进程join子进程，唤醒父进程
		if (parentProcess != null) {
			joinLock.acquire();
			joinCondition.wake();
			joinLock.release();
			parentProcess.childProcess.remove(this);
		}

		unloadSections(); // 释放内存
		KThread.finish(); // 将进程置为完成态，进行下一个进程，如果有join的进程，将join移出等待队列

		if (numOfRunningProcess == 1) // 如果是最后一个进程，则关闭机器
			Machine.halt();

		numOfRunningProcess--;

		return 0;
	}

	/**
	 * Handle the join() system call.
	 */
	private int handleJoin(int pid, int statusAddress) {
		UserProcess process = null;
		for (int i = 0; i < childProcess.size(); i++) {// 找到是否属于自己的子进程
			if (pid == childProcess.get(i).pid) {
				process = childProcess.get(i);
				break;
			}
		}

		if (process == null || process.thread == null) {
			// 如果没有子进程或者子进程还没创建UThread，出错
			return -1;
		}

		process.joinLock.acquire();// 获得join锁
		process.joinCondition.sleep();// 进程休眠等待直到子进程结束将其唤醒
		process.joinLock.release();// 释放join锁，此时子线程已经结束

		byte[] childstat = new byte[4];
		Lib.bytesFromInt(childstat, 0, process.status);// 得到子进程的状态
		int numWriteByte = writeVirtualMemory(statusAddress, childstat);// 将子进程的状态装入自己拥有的内存
		if (process.normalExit && numWriteByte == 4)// 子进程是正常结束，且写入状态成功
			return 1;

		return 0;
	}

	/**
	 * Handle the create() system call. 创建一个文件，返回文件描述符
	 */
	private int handleCreate(int fileAddress) {
		String fileName = readVirtualMemoryString(fileAddress, 256); // 限定文件名长度
		if (fileName == null)// 文件名不存在,创建失败
			return -1;

		int fileDescriptor = findEmpty();
		if (fileDescriptor == -1) // 进程打开文件数已经达到上限，无法创建并打开
			return fileDescriptor;
		else { // 文件不存在直接创建
			openfile[fileDescriptor] = ThreadedKernel.fileSystem.open(fileName, true);
		}
		return fileDescriptor;
	}

	/**
	 * Handle the open() system call. 打开文件
	 */
	private int handleOpen(int fileAddress) {
		String fileName = readVirtualMemoryString(fileAddress, 256); // 限定文件名长度
		if (fileName == null)// 文件名不存在,创建失败
			return -1;

		int fileDescriptor = findEmpty();
		if (fileDescriptor == -1) // 进程打开文件数已经达到上限，无法创建并打开
			return fileDescriptor;
		else { // 文件不存在直接创建
			openfile[fileDescriptor] = ThreadedKernel.fileSystem.open(fileName, false);
		}
		return fileDescriptor;
	}

	/**
	 * Handle the read() system call. 从文件中读出数据写入自己拥有的内存的指定地址
	 */
	private int handleRead(int fileDescriptor, int bufferAddress, int length) {
		if (fileDescriptor > 15 || fileDescriptor < 0 || openfile[fileDescriptor] == null)
			return -1; // 文件未打开，出错

		byte temp[] = new byte[length];
		int readNumber = openfile[fileDescriptor].read(temp, 0, length);
		if (readNumber <= 0)
			return 0; // 没读出数据

		int writeNumber = writeVirtualMemory(bufferAddress, temp);

		return writeNumber;
	}

	/**
	 * Handle the write() system call. 将自己拥有的内存的指定地址的数据写入文件
	 */
	private int handleWrite(int fileDescriptor, int bufferAddress, int length) {
		if (fileDescriptor > 15 || fileDescriptor < 0 || openfile[fileDescriptor] == null)
			return -1; // 文件未打开，出错

		byte temp[] = new byte[length];
		int readNumber = readVirtualMemory(bufferAddress, temp);
		if (readNumber <= 0)
			return 0; // 没读出数据

		int writeNumber = openfile[fileDescriptor].write(temp, 0, length);
		if (writeNumber < length)
			return -1;// 未完全写入，出错

		return writeNumber;
	}

	/**
	 * Handle the close() system call. 用于关闭打开的文件
	 */
	private int handleClose(int fileDescriptor) {
		if (fileDescriptor > 15 || fileDescriptor < 0 || openfile[fileDescriptor] == null)
			return -1;// 文件不存在，关闭出错

		openfile[fileDescriptor].close();
		openfile[fileDescriptor] = null;
		return 0;
	}

	/**
	 * Handle the unlink() system call. 用于删除文件
	 */
	private int handleUnlink(int fileAddress) {
		String filename = readVirtualMemoryString(fileAddress, 256);
		if (filename == null)
			return 0;// 文件不存在,不必删除

		if (ThreadedKernel.fileSystem.remove(filename))// 删除磁盘中实际的文件
			return 0;
		else
			return -1;
	}

	// 找到合适的文件打开表的位置，返回文件描述符
	private int findEmpty() {
		for (int i = 0; i < 16; i++) {
			if (openfile[i] == null)
				return i;
		}
		return -1;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2, syscallJoin = 3, syscallCreate = 4,
			syscallOpen = 5, syscallRead = 6, syscallWrite = 7, syscallClose = 8, syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0), processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1), processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	public int status = 0; // 进程的状态
	private int pid = 0; // 进程号
	OpenFile openfile[] = new OpenFile[16]; // 进程打开的文件表

	public UThread thread = null; // 这个进程所对应的实际线程

	public boolean normalExit = false;// 退出状态，是否为正常退出

	public LinkedList<UserProcess> childProcess = new LinkedList();// 所创建的子进程链表
	public UserProcess parentProcess = null;// 创建这个进程的父进程
	private static int numOfProcess = 0;// 进程计数器
	private static int numOfRunningProcess = 0;// 运行进程计数器

	private Lock joinLock = new Lock();// 进程join方法等待锁
	private Condition joinCondition = new Condition(joinLock);// join方法使用的条件变量

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
}
