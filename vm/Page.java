package nachos.vm;

import nachos.machine.TranslationEntry;

/**
 * 页面，内存在内核中以数组形式存在
 * 将PageItem和TranslationEntry联合，实质是将用户进程、虚拟内存、物理内存进行的联合
 * @author door
 *
 */
public class Page {
	PageItem item;
	TranslationEntry entry;

	public Page(PageItem item, TranslationEntry entry) {
		this.item = item;
		this.entry = entry;
	}
}
