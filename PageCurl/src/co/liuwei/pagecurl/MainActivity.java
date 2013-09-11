package co.liuwei.pagecurl;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import co.liuwei.pagecurl.view.OnPageCurChangelListener;
import co.liuwei.pagecurl.view.PageCurlLayout;

/**
 * 描述：测试
 * 
 * 作者: Liu wei
 * 
 * 邮箱：i@liuwei.co
 * 
 * 创建时间: 2013-9-9
 */
public class MainActivity extends Activity implements OnPageCurChangelListener {

	private PageCurlLayout pageCurlLayout;

	View page1, page2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		page1 = getLayoutInflater().inflate(R.layout.main_menu_hidden, null);

		page2 = getLayoutInflater().inflate(R.layout.main_menu_show, null);

		pageCurlLayout = new PageCurlLayout(this);
		pageCurlLayout.set2Pages(page1, page2);
		// pcl.setPageStateIsOpened(false);
		// pcl.setPageCurlEnabled(false);
		pageCurlLayout.setOnPageCurlListener(this);
		setContentView(pageCurlLayout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onPageClosed() {
		Toast.makeText(this, "Page Closed", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPageOpened() {
		Toast.makeText(this, "Page Opened", Toast.LENGTH_SHORT).show();
	}

}
