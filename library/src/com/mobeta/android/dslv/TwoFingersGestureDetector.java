package com.mobeta.android.dslv;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class TwoFingersGestureDetector {
	private static final int LONGPRESS_TIMEOUT = ViewConfiguration
			.getLongPressTimeout();
	private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

	private static final int LONG_PRESS = 1;

	private static final int TWO_FINGERS = 2;

	public interface OnTwoFingersGestureListener {
		public boolean onUp(MotionEvent e);

		public boolean onDown(MotionEvent e);

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float scrollX,
				float scrollY);

		public void onLongPress(MotionEvent e);
	}

	public static class SimpleTwoFingerGestureListener implements
			OnTwoFingersGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float scrollX,
				float scrollY) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onUp(MotionEvent e) {
			return false;
		}

	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LONG_PRESS:
				dispatchLongPress();
				break;
			default:
				throw new RuntimeException("Unknown message " + msg); // never
			}
		}
	};

	private OnTwoFingersGestureListener listener;
	private MotionEvent currentDownEvent;
	private boolean longpressEnabled;
	private boolean inLongPress;
	private boolean alwaysInTapRegion;
	private float lastFocusX;
	private float lastFocusY;
	private int touchSlopSquare;
	private int touchSlop;

	public TwoFingersGestureDetector(OnTwoFingersGestureListener listener) {
		this(null, listener);
	}

	public TwoFingersGestureDetector(Context context,
			OnTwoFingersGestureListener listener) {
		this.listener = listener;
		init(context);
	}

	@SuppressWarnings("deprecation")
	private void init(Context context) {
		if (listener == null) {
			throw new NullPointerException("OnGestureListener must not be null");
		}
		longpressEnabled = true;

		if (context == null) {
			touchSlop = ViewConfiguration.getTouchSlop();
		} else {
			final ViewConfiguration configuration = ViewConfiguration
					.get(context);
			touchSlop = configuration.getScaledTouchSlop();
		}
		touchSlopSquare = Math.min(touchSlop * touchSlop, 100);
	}

	public boolean onTouchEvent(MotionEvent ev) {
	//	MotionEvent ev = MotionEvent.obtain(e);
		
		final int action = ev.getAction();
		final boolean pointerUp = (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
		final int skipIndex = pointerUp ? ev.getActionIndex() : -1;

		// Determine focal point
		float sumX = 0, sumY = 0;
		final int count = ev.getPointerCount();
		for (int i = 0; i < count; i++) {
			if (skipIndex == i)
				continue;
			sumX += ev.getX(i);
			sumY += ev.getY(i);
		}
		final int div = pointerUp ? count - 1 : count;
		final float focusX = sumX / div;
		final float focusY = sumY / div;

	//	ev.setLocation(focusX, focusY);

		boolean handled = false;

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			lastFocusX = focusX;
			lastFocusY = focusY;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			lastFocusX = focusX;
			lastFocusY = focusY;

			if (ev.getPointerCount() == TWO_FINGERS) {
				if (currentDownEvent != null) {
					currentDownEvent.recycle();
				}
				currentDownEvent = MotionEvent.obtain(ev);
				alwaysInTapRegion = true;
				inLongPress = false;

				if (longpressEnabled) {
					handler.removeMessages(LONG_PRESS);
					handler.sendEmptyMessageAtTime(LONG_PRESS,
							currentDownEvent.getDownTime() + TAP_TIMEOUT
									+ LONGPRESS_TIMEOUT);
				}

				handled |= listener.onDown(ev);
			}
			break;

		case MotionEvent.ACTION_MOVE:
			Log.e("TWOFINGER", "");
			if (inLongPress) {
				break;
			}

			final float scrollX = lastFocusX - focusX;
			final float scrollY = lastFocusY - focusY;
			if (ev.getPointerCount() < TWO_FINGERS) {
				handler.removeMessages(LONG_PRESS);
			} else if (alwaysInTapRegion) {
				boolean longpress = true;
				for (int i = 0; i < TWO_FINGERS; ++i) {
					if (currentDownEvent.getPointerCount() > i
							&& ev.getPointerCount() > i) {
						final int deltaX = (int) (currentDownEvent.getX(i) - ev
								.getX(i));
						final int deltaY = (int) (currentDownEvent.getY(i) - ev
								.getY(i));
						int distance = (deltaX * deltaX) + (deltaY * deltaY);

						Log.e("test", "TapRegion= " + distance + " deltaX="
								+ deltaX + "deltaY" + deltaY);

						if (distance > touchSlopSquare) {
							longpress = false;
							break;
						}
					} else {
						longpress = false;
						break;
					}

				}
				if (!longpress) {
					handler.removeMessages(LONG_PRESS);
					handled = listener.onScroll(currentDownEvent, ev, scrollX,
							scrollY);
					lastFocusX = focusX;
					lastFocusY = focusY;
					alwaysInTapRegion = false;
				}
			} else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
				handled = listener.onScroll(currentDownEvent, ev, scrollX,
						scrollY);
				lastFocusX = focusX;
				lastFocusY = focusY;
			}
			break;
		case MotionEvent.ACTION_UP:
			cansel();

			break;
		case MotionEvent.ACTION_POINTER_UP:
			if (alwaysInTapRegion) {
				handled = listener.onUp(ev);
			}
			cansel();
			break;
		case MotionEvent.ACTION_CANCEL:
			cansel();
			break;
		}

		//ev.recycle();
		return handled;
	}

	private void cansel() {
		handler.removeMessages(LONG_PRESS);
		inLongPress = false;
	}

	private void dispatchLongPress() {
		inLongPress = true;
		listener.onLongPress(currentDownEvent);
	}

	public boolean isLongpressEnabled() {
		return longpressEnabled;
	}

	public void setLongpressEnabled(boolean longpressEnabled) {
		this.longpressEnabled = longpressEnabled;
	}
}