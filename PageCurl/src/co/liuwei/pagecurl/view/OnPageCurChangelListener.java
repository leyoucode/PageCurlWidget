package co.liuwei.pagecurl.view;

/** 
 * 描述：翻页状态监听 在翻页关闭或打开后触发
 *
 * 作者: Liu wei
 * 
 * 邮箱：i@liuwei.co
 * 
 * 创建时间: 2013-9-10
 */
public interface OnPageCurChangelListener {

	/**
	 * 页脚被关闭
	 */
	public void onPageClosed();

	/**
	 * 页脚被打开
	 */
	public void onPageOpened();

}
