package sysu.mobile.limk.pchelper;

import java.util.List;

/**
 * 保证了一个HiThread对象同一时刻只会有一个Thread对象在执行run()方法， 并且为线程的启动可以添加参数
 * 
 * @author Tiga
 * @author 11331173 李明宽 <sysu_limingkuan@163.com>
 * 
 */
public abstract class HiThread implements Runnable {
	private volatile Thread mThread;
	private final Object M_LOCK = new Object();
	private List<Object> mParams;

	/**
	 * 启动线程
	 * 
	 * @return
	 */
	public boolean start() {
		return start(null);
	}

	/**
	 * 启动线程
	 * 
	 * @param params
	 *            参数列表
	 * @return 如果创建了新的线程执行，则返回true；如果原来的线程没执行完， 则返回false且不启动新线程
	 */
	public boolean start(List<Object> params) {
		if (mThread == null) {
			synchronized (M_LOCK) {
				if (mThread == null) {
					mThread = new Thread() {
						@Override
						public void run() {
							try {
								HiThread.this.run();
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								HiThread.this.stop();
							}
						}
					};
					if (mParams != null) {
						mParams.clear();
						mParams = null;
					}
					mParams = params;
					mThread.setDaemon(true);
					mThread.start();
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 停止线程
	 */
	public void stop() {
		if (mThread != null) {
			synchronized (M_LOCK) {
				if (mThread != null) {
					mThread.interrupt();
					mThread = null;
				}
			}
		}
	}

	public List<Object> getParams() {
		return mParams;
	}
}
