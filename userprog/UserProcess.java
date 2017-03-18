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
		// �̴߳����󣬼�������������ʾ������ڲ�����open������������Զ���
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
		// �������ڴ��ַ������data��
		// ƫ�����볤�ȶ�ҪΪ������ƫ����+����<=�ܵ����ݳ���
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
		// getMemory�᷵�������������
		byte[] memory = Machine.processor().getMemory(); // �����������
		// ����ʣ�µ�ҳ���ֽڸ���
		if (length > (pageSize * numPages - vaddr))
			length = pageSize * numPages - vaddr;
		// �����ܹ���������ݵĴ�С�����data�����д治��length�����Сlength�������ֽ�����
		if (data.length - offset < length)
			length = data.length - offset;
		// ת���ɹ����ֽ���
		int transferredbyte = 0;
		do {
			// ����ҳ��
			int pageNum = Processor.pageFromAddress(vaddr + transferredbyte);
			// ҳ�Ŵ��� ҳ��ĳ��� ���� Ϊ�� ���쳣���
			if (pageNum < 0 || pageNum >= pageTable.length)
				return 0;
			// ����ҳƫ����
			int pageOffset = Processor.offsetFromAddress(vaddr + transferredbyte);
			// ����ʣ��ҳ������
			int leftByte = pageSize - pageOffset;
			// ������һ�δ��͵�����:ʣ��ҳ��������Ҫת�Ƶ��ֽ����н�С��
			int amount = Math.min(leftByte, length - transferredbyte);
			// ���������ڴ�ĵ�ַ
			int realAddress = pageTable[pageNum].ppn * pageSize + pageOffset;
			// �������ڴ�Ķ������䵽�����ڴ�
			System.arraycopy(memory, realAddress, data, offset + transferredbyte, amount);
			// �޸Ĵ���ɹ����ֽ���
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
		// �����ڴ�
		byte[] memory = Machine.processor().getMemory();
		// д�ڴ�ĳ����������ҳʣ����
		if (length > (pageSize * numPages - vaddr))
			length = pageSize * numPages - vaddr;
		// ���������Ҫд�ĳ��ȱȸ�����С�����length��Ϊ����ʣ��ĳ���
		if (data.length - offset < length)
			length = data.length - offset;
		int transferredbyte = 0;
		do {
			// �˺������ظ�����ַ��ҳ��
			int pageNum = Processor.pageFromAddress(vaddr + transferredbyte);
			if (pageNum < 0 || pageNum >= pageTable.length)
				return 0;
			// �˺������ظ�����ַ��ҳƫ����
			int pageOffset = Processor.offsetFromAddress(vaddr + transferredbyte);
			// ҳʣ����ֽ���
			int leftByte = pageSize - pageOffset;
			// ���ñ���ת�Ƶ�����
			int amount = Math.min(leftByte, length - transferredbyte);
			int realAddress = pageTable[pageNum].ppn * pageSize + pageOffset;
			// �������ڴ�д�뵽�����ڴ�
			System.arraycopy(data, offset + transferredbyte, memory, realAddress, amount);
			// �ı�д�ɹ����ֽ���
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
		UserKernel.allocateMemoryLock.acquire();// ��ȡ������ڴ����
		// ��������Ҫ��ҳ������֪��������numPages��
		// �ж��ܷ�����ڴ�
		// numPages�� �Ѿ�ȷ������Ҫҳ������
		// ҳ�����������ʵ�������ڴ��ҳ���������ܼ���
		if (numPages > Machine.processor().getNumPhysPages()) {
			// �ж��ܷ�װ��
			coff.close();
			// ȱ�������ڴ棨��������ҳ�����ڴ������Ŀ�������ҳ���ͻ�ʧ�ܣ�
			Lib.debug(dbgProcess, "\t insufficient��ȱ�������ַ�� physical memory");
			UserKernel.allocateMemoryLock.release();
			return false;// ����װ��ʧ��
		}
		pageTable = new TranslationEntry[numPages];// ʵ����ҳ��
		for (int i = 0; i < numPages; i++) {
			// �ӿ�������ҳ���������ó�һ��
			int nextPage = UserKernel.memoryLinkedList.remove();
			pageTable[i] = new TranslationEntry(i, nextPage, true, false, false, false);
		}
		UserKernel.allocateMemoryLock.release();
		// load sections���Σ���һ�������ɺܶ�ҳ���
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s); // coff���Section
			Lib.debug(dbgProcess,
					"\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");
			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;// �õ�ÿһ������ҳ�ţ���ҳ��ƫ��i����section��ͬ
				pageTable[vpn].readOnly = section.isReadOnly();// ���Ϊֻ��
				// for now, just assume virtual addresses=physical addresses
				// װ������ҳ
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}
		return true;
	}

	/**
	 * �ͷ��ڴ� Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		UserKernel.allocateMemoryLock.acquire();

		for (int i = 0; i < numPages; i++) {
			UserKernel.memoryLinkedList.add(pageTable[i].ppn); // �����û�����ռ�õ��ڴ��������ڴ�������
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
		// ����haltϵͳ���ã�ֻ�и����̿���
		if (pid == 0)
			Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exec() system call. ����һ���ӽ��̣���ִ��
	 */
	private int handleExec(int fileAddress, int argc, int argvAddress) {
		// ���ڴ��ж����������ʹ�õ��ļ�������
		String filename = readVirtualMemoryString(fileAddress, 256);
		if (filename == null || argc < 0 || argvAddress < 0 || argvAddress > numPages * pageSize)
			return -1;

		// �õ��ӽ��̵Ĳ�����
		String[] args = new String[argc];
		for (int i = 0; i < argc; i++) {
			byte[] argsAddress = new byte[4];
			if (readVirtualMemory(argvAddress + i * 4, argsAddress) > 0)
				args[i] = readVirtualMemoryString(Lib.bytesToInt(argsAddress, 0), 256);
		}

		UserProcess process = UserProcess.newUserProcess();
		if (!process.execute(filename, args))
			return -1;

		process.parentProcess = this; // ��������ָ�������������Ľ���
		childProcess.add(process); // ���ӽ��̵������м����´����Ľ���
		return process.pid;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(int status) {
		coff.close(); // �ر��ļ�
		for (int i = 0; i < 16; i++) {// �ر����д򿪵��ļ�
			if (openfile[i] != null) {
				openfile[i].close();
				openfile[i] = null;
			}
		}
		this.status = status;// ��״̬����
		normalExit = true;// ���������˳�

		// ����и����̣��ʹӸ����̵��ӽ���������ɾ�����������������join�ӽ��̣����Ѹ�����
		if (parentProcess != null) {
			joinLock.acquire();
			joinCondition.wake();
			joinLock.release();
			parentProcess.childProcess.remove(this);
		}

		unloadSections(); // �ͷ��ڴ�
		KThread.finish(); // ��������Ϊ���̬��������һ�����̣������join�Ľ��̣���join�Ƴ��ȴ�����

		if (numOfRunningProcess == 1) // ��������һ�����̣���رջ���
			Machine.halt();

		numOfRunningProcess--;

		return 0;
	}

	/**
	 * Handle the join() system call.
	 */
	private int handleJoin(int pid, int statusAddress) {
		UserProcess process = null;
		for (int i = 0; i < childProcess.size(); i++) {// �ҵ��Ƿ������Լ����ӽ���
			if (pid == childProcess.get(i).pid) {
				process = childProcess.get(i);
				break;
			}
		}

		if (process == null || process.thread == null) {
			// ���û���ӽ��̻����ӽ��̻�û����UThread������
			return -1;
		}

		process.joinLock.acquire();// ���join��
		process.joinCondition.sleep();// �������ߵȴ�ֱ���ӽ��̽������份��
		process.joinLock.release();// �ͷ�join������ʱ���߳��Ѿ�����

		byte[] childstat = new byte[4];
		Lib.bytesFromInt(childstat, 0, process.status);// �õ��ӽ��̵�״̬
		int numWriteByte = writeVirtualMemory(statusAddress, childstat);// ���ӽ��̵�״̬װ���Լ�ӵ�е��ڴ�
		if (process.normalExit && numWriteByte == 4)// �ӽ�����������������д��״̬�ɹ�
			return 1;

		return 0;
	}

	/**
	 * Handle the create() system call. ����һ���ļ��������ļ�������
	 */
	private int handleCreate(int fileAddress) {
		String fileName = readVirtualMemoryString(fileAddress, 256); // �޶��ļ�������
		if (fileName == null)// �ļ���������,����ʧ��
			return -1;

		int fileDescriptor = findEmpty();
		if (fileDescriptor == -1) // ���̴��ļ����Ѿ��ﵽ���ޣ��޷���������
			return fileDescriptor;
		else { // �ļ�������ֱ�Ӵ���
			openfile[fileDescriptor] = ThreadedKernel.fileSystem.open(fileName, true);
		}
		return fileDescriptor;
	}

	/**
	 * Handle the open() system call. ���ļ�
	 */
	private int handleOpen(int fileAddress) {
		String fileName = readVirtualMemoryString(fileAddress, 256); // �޶��ļ�������
		if (fileName == null)// �ļ���������,����ʧ��
			return -1;

		int fileDescriptor = findEmpty();
		if (fileDescriptor == -1) // ���̴��ļ����Ѿ��ﵽ���ޣ��޷���������
			return fileDescriptor;
		else { // �ļ�������ֱ�Ӵ���
			openfile[fileDescriptor] = ThreadedKernel.fileSystem.open(fileName, false);
		}
		return fileDescriptor;
	}

	/**
	 * Handle the read() system call. ���ļ��ж�������д���Լ�ӵ�е��ڴ��ָ����ַ
	 */
	private int handleRead(int fileDescriptor, int bufferAddress, int length) {
		if (fileDescriptor > 15 || fileDescriptor < 0 || openfile[fileDescriptor] == null)
			return -1; // �ļ�δ�򿪣�����

		byte temp[] = new byte[length];
		int readNumber = openfile[fileDescriptor].read(temp, 0, length);
		if (readNumber <= 0)
			return 0; // û��������

		int writeNumber = writeVirtualMemory(bufferAddress, temp);

		return writeNumber;
	}

	/**
	 * Handle the write() system call. ���Լ�ӵ�е��ڴ��ָ����ַ������д���ļ�
	 */
	private int handleWrite(int fileDescriptor, int bufferAddress, int length) {
		if (fileDescriptor > 15 || fileDescriptor < 0 || openfile[fileDescriptor] == null)
			return -1; // �ļ�δ�򿪣�����

		byte temp[] = new byte[length];
		int readNumber = readVirtualMemory(bufferAddress, temp);
		if (readNumber <= 0)
			return 0; // û��������

		int writeNumber = openfile[fileDescriptor].write(temp, 0, length);
		if (writeNumber < length)
			return -1;// δ��ȫд�룬����

		return writeNumber;
	}

	/**
	 * Handle the close() system call. ���ڹرմ򿪵��ļ�
	 */
	private int handleClose(int fileDescriptor) {
		if (fileDescriptor > 15 || fileDescriptor < 0 || openfile[fileDescriptor] == null)
			return -1;// �ļ������ڣ��رճ���

		openfile[fileDescriptor].close();
		openfile[fileDescriptor] = null;
		return 0;
	}

	/**
	 * Handle the unlink() system call. ����ɾ���ļ�
	 */
	private int handleUnlink(int fileAddress) {
		String filename = readVirtualMemoryString(fileAddress, 256);
		if (filename == null)
			return 0;// �ļ�������,����ɾ��

		if (ThreadedKernel.fileSystem.remove(filename))// ɾ��������ʵ�ʵ��ļ�
			return 0;
		else
			return -1;
	}

	// �ҵ����ʵ��ļ��򿪱��λ�ã������ļ�������
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

	public int status = 0; // ���̵�״̬
	private int pid = 0; // ���̺�
	OpenFile openfile[] = new OpenFile[16]; // ���̴򿪵��ļ���

	public UThread thread = null; // �����������Ӧ��ʵ���߳�

	public boolean normalExit = false;// �˳�״̬���Ƿ�Ϊ�����˳�

	public LinkedList<UserProcess> childProcess = new LinkedList();// ���������ӽ�������
	public UserProcess parentProcess = null;// ����������̵ĸ�����
	private static int numOfProcess = 0;// ���̼�����
	private static int numOfRunningProcess = 0;// ���н��̼�����

	private Lock joinLock = new Lock();// ����join�����ȴ���
	private Condition joinCondition = new Condition(joinLock);// join����ʹ�õ���������

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
