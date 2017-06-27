package nachos.vm;

import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

/**
 * �ӳټ����㷨 ����ÿ��Coff��ʵ���ǿ�ִ���ļ�����˵���ɷ�Ϊ�����½ڣ�ÿ���½ڵĶγ���ҳ��������һ��----�ڶ�ȡ�ļ���ʱ���֪��װ���ļ����ڴ��С
 * ���Ե�����ҳ���ڴ棩��ʱ��Ӧ������Ҫ����ĳ���½��е�ĳ���� ��ô��Ӧ��ȥ��¼�½�ÿ���ε���ʼ�ڴ�λ��
 * @author door
 *
 */
public class LazyLoader {

	public LazyLoader(Coff coff) {
		this.coff = coff;

		int numCodePages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			numCodePages += section.getLength();
		}

		pages = new CodePage[numCodePages];
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				pages[vpn] = new CodePage(s, i);
			}
		}
	}

	public boolean isCodePage(int vpn) {
		return vpn >= 0 && vpn < pages.length;
	}

	public TranslationEntry loadCodePage(int vpn, int ppn) {
		CoffSection section = coff.getSection(pages[vpn].section);
		TranslationEntry entry = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
		section.loadPage(pages[vpn].offset, ppn);
		return entry;
	}

	public TranslationEntry loadStackPage(int vpn, int ppn) {
		fillMemory(ppn);
		return new TranslationEntry(vpn, ppn, true, false, false, false);
	}

	public TranslationEntry load(PageItem item, int ppn) {
		TranslationEntry entry;
		SwapPage swapPage = VMKernel.getSwapper().getSwapPage(item);
		if (swapPage != null) { // in swap file
			entry = swapPage.entry;
			entry.ppn = ppn;
			entry.valid = true;
			entry.used = false;
			entry.dirty = false;
			Lib.assertTrue(VMKernel.getSwapper().read(swapPage.frameNo, Machine.processor().getMemory(),
					Processor.makeAddress(ppn, 0)), "swap file read error");
		} else {
			if (isCodePage(item.vpn)) // load a code page
				entry = loadCodePage(item.vpn, ppn);
			else
				// new page for stack
				entry = loadStackPage(item.vpn, ppn);
		}
		return entry;
	}

	// ��������о�����Ч��
	private void fillMemory(int ppn) {
		byte[] data = Machine.processor().getMemory();
		int start = Processor.makeAddress(ppn, 0);
		for (int i = start; i < start + Processor.pageSize; i++)
			data[i] = 0;
	}

	class CodePage {
		public CodePage(int section, int offset) {
			this.section = section;
			this.offset = offset;
		}

		public int section;
		public int offset;
	}

	private Coff coff;
	private CodePage[] pages;
}