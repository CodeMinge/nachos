package nachos.vm;

import nachos.machine.TranslationEntry;

/**
 * ҳ�棬�ڴ����ں�����������ʽ����
 * ��PageItem��TranslationEntry���ϣ�ʵ���ǽ��û����̡������ڴ桢�����ڴ���е�����
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
