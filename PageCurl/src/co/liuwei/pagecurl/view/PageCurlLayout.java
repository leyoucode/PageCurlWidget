package co.liuwei.pagecurl.view;

import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 描述：翻页控件
 * 
 * 作者: Liu wei
 * 
 * 邮箱：i@liuwei.co
 * 
 * 创建时间: 2013-9-9
 */
public class PageCurlLayout extends FrameLayout {

	public static final String LOG_TAG = "PageCurlLayout";

	private Context mContext;
	private boolean hasInit = false;
	private final int defaultWidth = 600, defaultHeight = 400;
	private int contentWidth = 0;
	private int contentHeight = 0;

	private LinearLayout invisibleLayout;
	private MianLinearLayout mainLayout;
	private BookView mBookView;
	private Handler aniEndHandle;

	private Corner mSelectCorner;
	private float scrollX = 0, scrollY = 0;

	private PageState mPageState = PageState.CLOSED;

	private boolean isPageCurlEnabled = true;

	private static final long ANIMATION_TIME = 1000;
	private static final long TIME_OFFSET = 2;

	private BookState mState;
	private Point aniStartPos;
	private Point aniStopPos;
	private Date aniStartTime;
	private long aniTime = ANIMATION_TIME;

	private GestureDetector mGestureDetector;
	private BookOnGestureListener mGestureListener;

	private View closedPage, opendedPage;
	private View backgroundPage;

	private Handler onPageChangeHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				onPageCurlListener.onPageClosed();
			} else if (msg.what == 1) {
				onPageCurlListener.onPageOpened();
			}
		};
	};

	private static float SCROLL_MIN_X = 320f;
	private static float SCROLL_MIN_Y = 0.01f;

	private static float SCROLL_MAX_X = 320f;
	private static float SCROLL_MAX_Y = 520f;

	private OnPageCurChangelListener onPageCurlListener;

	public void setOnPageCurlListener(
			OnPageCurChangelListener onPageCurlListener) {
		this.onPageCurlListener = onPageCurlListener;
	}

	public PageCurlLayout(Context context) {
		super(context);
		Init(context);
	}

	public PageCurlLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		Init(context);
	}

	public PageCurlLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Init(context);
	}

	public void set2Pages(View closedPage, View OpendedPage) {
		this.closedPage = closedPage;
		this.opendedPage = OpendedPage;
	}

	public void Init(Context context) {
		Log.d(LOG_TAG, "Init");
		mContext = context;
		mSelectCorner = Corner.None;

		mGestureListener = new BookOnGestureListener();
		mGestureDetector = new GestureDetector(mGestureListener);
		mGestureDetector.setIsLongpressEnabled(false);
		aniEndHandle = new Handler();

		this.setOnTouchListener(touchListener);
		this.setLongClickable(true);

		if (mPageState == PageState.OPENED) {
			onPageChangeHandler.sendEmptyMessage(1);
		} else if (mPageState == PageState.CLOSED) {
			onPageChangeHandler.sendEmptyMessage(0);
		}
	}

	public void setPageStateIsOpened(boolean pageOpen) {
		if (pageOpen) {
			mPageState = PageState.OPENED;
			onPageChangeHandler.sendEmptyMessage(1);
		} else {
			mPageState = PageState.CLOSED;
			onPageChangeHandler.sendEmptyMessage(0);
		}
	}

	public void setPageCurlEnabled(boolean isPageCurlEnabled) {
		this.isPageCurlEnabled = isPageCurlEnabled;
	}

	protected void dispatchDraw(Canvas canvas) {
		Log.d(LOG_TAG, "dispatchDraw");
		super.dispatchDraw(canvas);
		if (!hasInit) {
			hasInit = true;
			if (closedPage == null || opendedPage == null) {
				throw new RuntimeException("please set the 2 Pages on init");
			}
			mainLayout = new MianLinearLayout(mContext);
			mainLayout.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			mainLayout.setBackgroundColor(0xffffffff);
			mState = BookState.READY;

			invisibleLayout = new LinearLayout(mContext);
			invisibleLayout.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));

			this.addView(invisibleLayout);
			this.addView(mainLayout);

			mBookView = new BookView(mContext);
			mBookView.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			this.addView(mBookView);

			updatePageView();

			invalidate();

		} else if (mState == BookState.READY) {
			mBookView.update();
		}
	}

	public void updatePageView() {
		Log.d(LOG_TAG, "updatePageView");

		invisibleLayout.removeAllViews();
		mainLayout.removeAllViews();

		if (mPageState == PageState.CLOSED) {
			closedPage.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			mainLayout.addView(closedPage);

			opendedPage.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			invisibleLayout.addView(opendedPage);
		} else if (mPageState == PageState.OPENED) {

			opendedPage.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			mainLayout.addView(opendedPage);

			closedPage.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			invisibleLayout.addView(closedPage);
		}

		if (backgroundPage == null) {
			backgroundPage = new WhiteView(mContext);
		}
		backgroundPage.setLayoutParams(new LayoutParams(contentWidth,
				contentHeight));
		invisibleLayout.addView(backgroundPage);

		Log.d(LOG_TAG, "updatePageView finish");
	}

	float touchStartX, touchEndX, touchStartY, touchEndY;

	OnTouchListener touchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			Log.d(LOG_TAG,
					"onTouch " + " x: " + event.getX() + " y: " + event.getY()
							+ " mState:" + mState);
			mGestureDetector.onTouchEvent(event);

			int action = event.getAction();

			switch (action) {
			case MotionEvent.ACTION_DOWN:
				touchStartX = event.getX();
				touchStartY = event.getY();
				break;
			case MotionEvent.ACTION_UP:
				touchEndX = event.getX();
				touchEndY = event.getY();
				if (mPageState == PageState.CLOSED) {
					if (isPageCurlEnabled) {

						if (mSelectCorner != Corner.None
								&& mState == BookState.TRACKING) {
							if (mState == BookState.ANIMATING)
								return false;
							if (mSelectCorner == Corner.RightBottom
									&& mPageState == PageState.CLOSED) {
								aniStartPos = new Point((int) scrollX,
										(int) scrollY);
								if (scrollY < contentHeight / 2) {
									aniStopPos = new Point((int) SCROLL_MIN_X,
											(int) SCROLL_MIN_Y);
								} else {
									aniStopPos = new Point((int) SCROLL_MAX_X,
											(int) SCROLL_MAX_Y);
								}
								aniTime = ANIMATION_TIME;
								mState = BookState.ABOUT_TO_ANIMATE;
								aniStartTime = new Date();
								mBookView.startAnimation();
							}
						}

					}
				} else if (mPageState == PageState.OPENED) {
					aniStopPos = new Point((int) SCROLL_MAX_X,
							(int) SCROLL_MAX_Y);
					aniStartPos = new Point((int) SCROLL_MIN_X,
							(int) SCROLL_MIN_Y);
					aniTime = ANIMATION_TIME;
					mState = BookState.ABOUT_TO_ANIMATE;
					aniStartTime = new Date();
					mBookView.startAnimation();
				}
				break;
			case MotionEvent.ACTION_CANCEL:

				break;

			default:
				break;
			}
			return false;
		}
	};

	class BookOnGestureListener implements OnGestureListener {
		public boolean onDown(MotionEvent event) {
			Log.d(LOG_TAG, "onDown");

			if (mState == BookState.ANIMATING)
				return false;
			float x = event.getX(), y = event.getY();
			int w = contentWidth, h = contentHeight;
			if (x >= w / 2 && y >= h * 4 / 5) {
				if (mPageState == PageState.CLOSED) {
					mSelectCorner = Corner.RightBottom;
				}
			}

			return false;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			Log.d(LOG_TAG, "onFling velocityX:" + velocityX + " velocityY:"
					+ velocityY);

			if (mSelectCorner != Corner.None) {
				if (mSelectCorner == Corner.RightBottom) {
					if (velocityY < 0) {
						aniStopPos = new Point(contentWidth * 4 / 5, 0);
					} else {
						aniStopPos = new Point(contentWidth, contentHeight);
					}
				}
				Log.d(LOG_TAG, "onFling animate");
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = ANIMATION_TIME;
				mState = BookState.ABOUT_TO_ANIMATE;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}

			return false;
		}

		public void onLongPress(MotionEvent e) {
			Log.d(LOG_TAG, "onLongPress");
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {

			if (mSelectCorner != Corner.None) {

				mState = BookState.TRACKING;
				scrollY = e2.getY();
				scrollX = e2.getX();

				mBookView.startAnimation();
			}
			return false;
		}

		public void onShowPress(MotionEvent e) {
			Log.d(LOG_TAG, "onShowPress");
		}

		public boolean onSingleTapUp(MotionEvent e) {
			Log.d(LOG_TAG, "onSingleTapUp");

			if (mSelectCorner != Corner.None) {
				if (mSelectCorner == Corner.RightBottom) {
					if (scrollY < contentHeight / 2) {
						aniStopPos = new Point((int) SCROLL_MIN_X,
								(int) SCROLL_MIN_Y);
					} else {
						aniStopPos = new Point((int) SCROLL_MAX_X,
								(int) SCROLL_MAX_Y);
					}
				}
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = ANIMATION_TIME;
				mState = BookState.ABOUT_TO_ANIMATE;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}
	}

	protected void onFinishInflate() {
		Log.d(LOG_TAG, "onFinishInflate");
		super.onFinishInflate();
	}

	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		contentWidth = this.getWidth();
		contentHeight = this.getHeight();
		if (contentWidth == 0)
			contentWidth = defaultWidth;
		if (contentHeight == 0)
			contentHeight = defaultHeight;

		initCurlPoint(contentWidth, contentHeight);

		Log.d(LOG_TAG, "onLayout, width:" + contentWidth + " height:"
				+ contentHeight);
	}

	private void initCurlPoint(int contentWidth, int contentHeight) {

		SCROLL_MAX_X = contentWidth * 9 / 10;
		SCROLL_MAX_Y = contentHeight * 9 / 10;

		SCROLL_MIN_X = contentWidth * 8 / 10;
		SCROLL_MIN_Y = contentHeight * 1 / 15;

		Log.d(LOG_TAG, "initCurlPoint, SCROLL_MIN_X:" + SCROLL_MIN_X
				+ "    SCROLL_MIN_Y:" + SCROLL_MIN_Y + "    SCROLL_MAX_X:"
				+ SCROLL_MAX_X + "    SCROLL_MAX_Y:" + SCROLL_MAX_Y);
	}

	class BookView extends SurfaceView implements SurfaceHolder.Callback {

		private static final int COLOR_0X55 = 0x55000000;
		private static final int COLOR_0X00 = 0x00000000;
		private static final int COLOR_0X99 = 0x99000000;

		DrawThread dt;
		SurfaceHolder surfaceHolder;
		Paint mDarkPaint = new Paint();
		Paint mPaint = new Paint();
		Bitmap tmpBmp = Bitmap.createBitmap(contentWidth, contentHeight,
				Bitmap.Config.ARGB_8888);
		Canvas mCanvas = new Canvas(tmpBmp);

		Paint bmpPaint = new Paint();
		Paint ivisiblePaint = new Paint();

		public BookView(Context context) {
			super(context);
			surfaceHolder = getHolder();
			surfaceHolder.addCallback(this);

			mDarkPaint.setColor(0x88000000);
			Shader mLinearGradient = new LinearGradient(0, 0, contentWidth, 0,
					new int[] { COLOR_0X00, COLOR_0X55, COLOR_0X00 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.MIRROR);
			mPaint.setAntiAlias(true);
			mPaint.setShader(mLinearGradient);

			bmpPaint.setFilterBitmap(true);
			bmpPaint.setAntiAlias(true);

			ivisiblePaint.setAlpha(0);
			ivisiblePaint.setFilterBitmap(true);
			ivisiblePaint.setAntiAlias(true);
			ivisiblePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		}

		public void startAnimation() {
			touchStartX = 0;
			touchEndX = 0;
			touchStartY = 0;
			touchEndY = 0;

			if (dt == null) {
				Log.d(LOG_TAG, "startAnimation");
				dt = new DrawThread(this, getHolder());
				dt.start();
			}
		}

		public void stopAnimation(Canvas canvas) {
			Log.d(LOG_TAG, "stopAnimation");
			if (dt != null) {
				dt.flag = false;
				Thread t = dt;
				dt = null;
				t.interrupt();
			}
		}

		public synchronized void drawCurlOpenning(Canvas canvas) {

			if (scrollY < SCROLL_MIN_Y) {
				scrollY = SCROLL_MIN_Y;
			} else if (scrollY > SCROLL_MAX_Y) {
				scrollY = SCROLL_MAX_Y;
				if (scrollX > SCROLL_MAX_X) {
					scrollX = SCROLL_MAX_X;
				}
			}

			double px = contentWidth - scrollX;
			double py = contentHeight - scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX, scrollY - contentHeight);
			m.postRotate((float) (arc), scrollX, scrollY);

			backgroundPage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(0, contentHeight, (float) px,
					contentHeight - (float) py, new int[] { COLOR_0X00,
							COLOR_0X55 }, new float[] { 0.4f, 0.5f },
					Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = contentWidth - r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));

			opendedPage.draw(mCanvas);

			double x = p2 * p2 / (contentWidth - p1) + contentWidth;
			Shader lg2 = new LinearGradient(contentWidth, contentHeight
					- (float) p2, (float) x, contentHeight, new int[] {
					COLOR_0X99, COLOR_0X00 }, new float[] { 0.0f, 0.1f },
					Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			Log.d(LOG_TAG, "drawCurlOpenning p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = contentWidth - (p2 - contentHeight) * Math.tan(arc);
				path.moveTo(0, 0);
				path.lineTo((float) p3, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight - (float) p2);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public synchronized void drawCurlClosing(Canvas canvas) {

			if (scrollY < SCROLL_MIN_Y) {
				scrollY = SCROLL_MIN_Y;
			} else if (scrollY > SCROLL_MAX_Y) {
				scrollY = SCROLL_MAX_Y;
				if (scrollX > SCROLL_MAX_X) {
					scrollX = SCROLL_MAX_X;
				}
			}

			double px = contentWidth - scrollX;
			double py = contentHeight - scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX, scrollY - contentHeight);
			m.postRotate((float) (arc), scrollX, scrollY);

			backgroundPage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(0, contentHeight, (float) px,
					contentHeight - (float) py, new int[] { COLOR_0X00,
							COLOR_0X55 }, new float[] { 0.4f, 0.5f },
					Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = contentWidth - r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));

			opendedPage.draw(mCanvas);

			double x = p2 * p2 / (contentWidth - p1) + contentWidth;
			Shader lg2 = new LinearGradient(contentWidth, contentHeight
					- (float) p2, (float) x, contentHeight, new int[] {
					COLOR_0X99, COLOR_0X00 }, new float[] { 0.0f, 0.1f },
					Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			Log.d(LOG_TAG, "drawCurlClosing p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = contentWidth - (p2 - contentHeight) * Math.tan(arc);
				path.moveTo(0, 0);
				path.lineTo((float) p3, 0);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight - (float) p2);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public synchronized void drawNormalCorner(Canvas canvas) {

			if (mState == BookState.ANIMATE_END || mState == BookState.READY) {
				double px = contentWidth - SCROLL_MAX_X;
				double py = contentHeight - SCROLL_MAX_Y;
				double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

				Matrix m = new Matrix();
				m.postTranslate(SCROLL_MAX_X, SCROLL_MAX_Y - contentHeight);
				m.postRotate((float) (arc), SCROLL_MAX_X, SCROLL_MAX_Y);

				backgroundPage.draw(mCanvas);

				Paint ps = new Paint();
				Shader lg1 = new LinearGradient(0, contentHeight, (float) px,
						contentHeight - (float) py, new int[] { COLOR_0X00,
								COLOR_0X55 }, new float[] { 0.4f, 0.5f },
						Shader.TileMode.CLAMP);
				ps.setShader(lg1);
				mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
				canvas.drawBitmap(tmpBmp, m, bmpPaint);

				arc = arc * Math.PI / 360;
				Path path = new Path();
				double r = Math.sqrt(px * px + py * py);
				double p1 = contentWidth - r / (2 * Math.cos(arc));
				double p2 = r / (2 * Math.sin(arc));

				opendedPage.draw(mCanvas);

				double x = p2 * p2 / (contentWidth - p1) + contentWidth;
				Shader lg2 = new LinearGradient(contentWidth, contentHeight
						- (float) p2, (float) x, contentHeight, new int[] {
						COLOR_0X99, COLOR_0X00 }, new float[] { 0.0f, 0.1f },
						Shader.TileMode.CLAMP);
				ps.setShader(lg2);
				mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

				Log.d(LOG_TAG, "drawNormalCorner p1: " + p1 + " p2:" + p2);
				if (arc == 0) {
					path.moveTo(0, 0);
					path.lineTo((float) p1, 0);
					path.lineTo((float) p1, contentHeight);
					path.lineTo(0, contentHeight);
					path.close();
				} else if (p2 > contentHeight || p2 < 0) {
					double p3 = contentWidth - (p2 - contentHeight)
							* Math.tan(arc);
					path.moveTo(0, 0);
					path.lineTo((float) p3, 0);
					path.lineTo((float) p1, contentHeight);
					path.lineTo(0, contentHeight);
					path.close();
				} else {
					path.moveTo(0, 0);
					path.lineTo(contentWidth, 0);
					path.lineTo(contentWidth, contentHeight - (float) p2);
					path.lineTo((float) p1, contentHeight);
					path.lineTo(0, contentHeight);
					path.close();
				}
				mCanvas.drawPath(path, ivisiblePaint);
				canvas.drawBitmap(tmpBmp, 0, 0, null);

				Log.d(LOG_TAG, "drawNormalCorner");
			}

		}

		public synchronized void drawOpenedCorner(Canvas canvas) {

			if (mState == BookState.ANIMATE_END || mState == BookState.READY) {

				double px = contentWidth - SCROLL_MIN_X;
				double py = contentHeight - SCROLL_MIN_Y;
				double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

				Matrix m = new Matrix();
				m.postTranslate(SCROLL_MIN_X, SCROLL_MIN_Y - contentHeight);
				m.postRotate((float) (arc), SCROLL_MIN_X, SCROLL_MIN_Y);

				backgroundPage.draw(mCanvas);

				Paint ps = new Paint();
				Shader lg1 = new LinearGradient(0, contentHeight, (float) px,
						contentHeight - (float) py, new int[] { COLOR_0X00,
								COLOR_0X55 }, new float[] { 0.4f, 0.5f },
						Shader.TileMode.CLAMP);
				ps.setShader(lg1);
				mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
				canvas.drawBitmap(tmpBmp, m, bmpPaint);

				arc = arc * Math.PI / 360;
				Path path = new Path();
				double r = Math.sqrt(px * px + py * py);
				double p1 = contentWidth - r / (2 * Math.cos(arc));
				double p2 = r / (2 * Math.sin(arc));

				canvas.drawBitmap(tmpBmp, m, bmpPaint);

				opendedPage.draw(mCanvas);

				double x = p2 * p2 / (contentWidth - p1) + contentWidth;
				Shader lg2 = new LinearGradient(contentWidth, contentHeight
						- (float) p2, (float) x, contentHeight, new int[] {
						COLOR_0X99, COLOR_0X00 }, new float[] { 0.0f, 0.1f },
						Shader.TileMode.CLAMP);
				ps.setShader(lg2);
				mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

				Log.d(LOG_TAG, "drawOpenedCorner p1: " + p1 + " p2:" + p2);
				if (arc == 0) {
					path.moveTo(0, 0);
					path.lineTo((float) p1, 0);
					// path.lineTo((float) p1, contentHeight);
					// path.lineTo(0, contentHeight);
					path.close();
				} else if (p2 > contentHeight || p2 < 0) {
					double p3 = contentWidth - (p2 - contentHeight)
							* Math.tan(arc);
					path.moveTo(0, 0);
					path.lineTo((float) p3, 0);
					path.lineTo((float) p1, contentHeight);
					path.lineTo(0, contentHeight);
					path.close();
				} else {
					path.moveTo(0, 0);
					path.lineTo(contentWidth, 0);
					path.lineTo(contentWidth, contentHeight - (float) p2);
					path.lineTo((float) p1, contentHeight);
					path.lineTo(0, contentHeight);
					path.close();
				}
				mCanvas.drawPath(path, ivisiblePaint);
				canvas.drawBitmap(tmpBmp, 0, 0, null);
				Log.d(LOG_TAG, "drawOpenedCorner");

			}
		}

		public void drawPrevPageEnd(Canvas canvas) {
			closedPage.draw(mCanvas);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawNextPageEnd(Canvas canvas) {
			opendedPage.draw(mCanvas);
			canvas.drawBitmap(tmpBmp, contentWidth, 0, null);
		}

		public void drawPage(Canvas canvas) {
			if (mPageState == PageState.CLOSED) {
				drawCurlOpenning(canvas);
			} else if (mPageState == PageState.OPENED) {
				drawCurlClosing(canvas);
			}
		}

		public void update() {
			Canvas canvas = surfaceHolder.lockCanvas(null);
			try {
				synchronized (surfaceHolder) {
					doDraw(canvas);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}

		protected void doDraw(Canvas canvas) {
			Log.d(LOG_TAG, "bookView doDraw");
			mainLayout.draw(canvas);
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {

		}

		public void surfaceCreated(SurfaceHolder holder) {
			update();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			if (dt != null) {
				dt.flag = false;
				dt = null;
			}
		}
	}

	/**
	 * 计算动画数据
	 */
	public void getAnimateData() {
		Log.d(LOG_TAG, "getAnimateData");
		long time = aniTime;
		Date date = new Date();
		long t = date.getTime() - aniStartTime.getTime();
		t += TIME_OFFSET;
		if (t < 0 || t > time) {
			mState = BookState.ANIMATE_END;
		} else {
			mState = BookState.ANIMATING;
			double sx = aniStopPos.x - aniStartPos.x;
			scrollX = (float) (sx * t / time + aniStartPos.x);
			double sy = aniStopPos.y - aniStartPos.y;
			scrollY = (float) (sy * t / time + aniStartPos.y);
		}
	}

	public void handleAniEnd(Canvas canvas) {
		Log.d(LOG_TAG, "handleAniEnd");
		if (mPageState == PageState.CLOSED
				&& mSelectCorner == Corner.RightBottom
				&& Math.abs(scrollY - SCROLL_MAX_Y) > 50) {
			mPageState = PageState.OPENED;
			mBookView.drawNextPageEnd(canvas);
			if (onPageCurlListener != null) {
				onPageChangeHandler.sendEmptyMessage(1);
			}
		} else if (mPageState == PageState.OPENED
				&& Math.abs(scrollY - SCROLL_MIN_Y) > 50) {
			mPageState = PageState.CLOSED;
			mBookView.drawPrevPageEnd(canvas);
			if (onPageCurlListener != null) {
				onPageChangeHandler.sendEmptyMessage(0);
			}
		}
		mSelectCorner = Corner.None;
		mState = BookState.READY;

		mBookView.stopAnimation(canvas);

		Log.i(LOG_TAG, "scrollX:" + scrollX + "====scrollY" + scrollY);

		aniEndHandle.post(new Runnable() {
			public void run() {
				PageCurlLayout.this.invalidate();
			}
		});

		if (mPageState == PageState.CLOSED) {
			mBookView.drawNormalCorner(canvas);
		} else if (mPageState == PageState.OPENED) {
			mBookView.drawOpenedCorner(canvas);
		}
	}

	class WhiteView extends View {
		public WhiteView(Context context) {
			super(context);
		}

		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			canvas.drawColor(Color.WHITE);
		}
	}

	public class DrawThread extends Thread {
		BookView bv;
		SurfaceHolder surfaceHolder;
		boolean flag = false;
		int sleepSpan = 30;

		public DrawThread(BookView bv, SurfaceHolder surfaceHolder) {
			this.bv = bv;
			this.surfaceHolder = surfaceHolder;
			this.flag = true;
		}

		public void run() {
			Canvas canvas = null;
			while (flag) {
				try {
					canvas = surfaceHolder.lockCanvas(null);
					if (canvas == null)
						continue;
					synchronized (surfaceHolder) {
						if (mState == BookState.ABOUT_TO_ANIMATE
								|| mState == BookState.ANIMATING) {
							bv.doDraw(canvas);
							getAnimateData();
							bv.drawPage(canvas);
						} else if (mState == BookState.TRACKING) {
							bv.doDraw(canvas);
							bv.drawPage(canvas);
						} else if (mState == BookState.ANIMATE_END) {
							handleAniEnd(canvas);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (canvas != null) {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
				try {
					Thread.sleep(sleepSpan);
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}
		}
	}

	class MianLinearLayout extends LinearLayout {

		public MianLinearLayout(Context context) {
			super(context);
		}

		@Override
		public void draw(Canvas canvas) {
			super.draw(canvas);
			if (mPageState == PageState.CLOSED) {
				mBookView.drawNormalCorner(canvas);
			} else if (mPageState == PageState.OPENED) {
				mBookView.drawOpenedCorner(canvas);
			}
		}

	}

}
