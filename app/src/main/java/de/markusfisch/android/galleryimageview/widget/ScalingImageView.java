package de.markusfisch.android.galleryimageview.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

public class ScalingImageView extends ImageView {
	private final SparseArray<PointF> initialPoint = new SparseArray<>();
	private final Matrix initialMatrix = new Matrix();
	private final Matrix transformMatrix = new Matrix();
	private final Gesture initialGesture = new Gesture();
	private final Gesture transformGesture = new Gesture();
	private final RectF initialRect = new RectF();
	private final RectF bounds = new RectF();

	private long doubleTapTimeout;
	private float doubleTapSensivity;
	private long lastUp;
	private float minWidth = 0f;
	private float rotation = 0f;
	private ImageView.ScaleType scaleType =
			ImageView.ScaleType.CENTER_INSIDE;

	public ScalingImageView(Context context) {
		super(context);
		init(context);
	}

	public ScalingImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ScalingImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ScalingImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}

	@Override
	public void setImageMatrix(Matrix matrix) {
		transformMatrix.set(matrix);
		setMinWidth(bounds, new Matrix());
		fitMatrix(transformMatrix, getDrawableRect(), bounds);
		super.setImageMatrix(transformMatrix);
	}

	@Override
	public void setScaleType(ImageView.ScaleType scaleType) {
		if (scaleType != ImageView.ScaleType.CENTER &&
				scaleType != ImageView.ScaleType.CENTER_CROP &&
				scaleType != ImageView.ScaleType.CENTER_INSIDE) {
			throw new UnsupportedOperationException();
		}

		this.scaleType = scaleType;
		center(bounds);
		invalidate();
	}

	@Override
	public ScaleType getScaleType() {
		return scaleType;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int pointerCount = event.getPointerCount();

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_POINTER_UP:
				// number of pointers has changed so
				// (re)initialize the transformation
				initTransform(event, pointerCount,
						// ignore the pointer that has gone up
						event.getActionIndex());
				return true;
			case MotionEvent.ACTION_DOWN:
				if (event.getEventTime() - lastUp <= doubleTapTimeout &&
						distanceFromInitial(event) < doubleTapSensivity) {
					magnify(event.getX(), event.getY());
				}
				initTransform(event, pointerCount, -1);
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				initTransform(event, pointerCount, -1);
				return true;
			case MotionEvent.ACTION_MOVE:
				// position of the pointer(s) has changed
				// so transform accordingly
				transform(event, pointerCount);
				return true;
			case MotionEvent.ACTION_CANCEL:
				return true;
			case MotionEvent.ACTION_UP:
				lastUp = event.getEventTime();
				setInitialPoint(event, event.getActionIndex());
				return true;
		}

		return super.onTouchEvent(event);
	}

	public void setImageRotation(float degrees) {
		if (degrees == rotation) {
			return;
		}

		rotation = degrees;
		requestLayout();
	}

	public float getImageRotation() {
		return rotation;
	}

	public Rect getRectInBounds() {
		RectF srcRect = getDrawableRect();
		RectF dstRect = new RectF();
		transformMatrix.mapRect(dstRect, srcRect);

		float scale = dstRect.width() / srcRect.width();
		return new Rect(
				Math.round((bounds.left - dstRect.left) / scale),
				Math.round((bounds.top - dstRect.top) / scale),
				Math.round((bounds.right - dstRect.left) / scale),
				Math.round((bounds.bottom - dstRect.top) / scale));
	}

	public RectF getNormalizedRectInBounds() {
		RectF dstRect = getMappedRect();
		float w = dstRect.width();
		float h = dstRect.height();
		return new RectF(
				(bounds.left - dstRect.left) / w,
				(bounds.top - dstRect.top) / h,
				(bounds.right - dstRect.left) / w,
				(bounds.bottom - dstRect.top) / h);
	}

	@Override
	protected void onLayout(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		// use a separate method to layout the image so it's possible
		// to override this behaviour without skipping super.onLayout()
		layoutImage(changed, left, top, right, bottom);
	}

	protected void layoutImage(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		if (changed) {
			setBounds(left, top, right, bottom);
		}

		center(bounds);
	}

	protected void setBounds(
			float left,
			float top,
			float right,
			float bottom) {
		bounds.set(left, top, right, bottom);
	}

	protected void setBounds(RectF rect) {
		bounds.set(rect);
	}

	protected RectF getBounds() {
		return bounds;
	}

	protected void center(RectF rect) {
		setMinWidth(rect, transformMatrix);
		super.setImageMatrix(transformMatrix);
	}

	protected boolean inBounds() {
		return getMappedRect().width() <= minWidth;
	}

	protected void setMinWidth(RectF rect, Matrix matrix) {
		// don't try to store the drawable dimensions by overriding
		// setImageDrawable() since it is called in the ImageView's
		// constructor and no referenced member of this object will
		// have been initialized yet. So it's best to simply request
		// the dimensions when they are required only.
		RectF srcRect = new RectF(getDrawableRect());

		if (rect == null || matrix == null) {
			return;
		}

		float dw = srcRect.width();
		float dh = srcRect.height();
		float rw = rect.width();
		float rh = rect.height();

		if (dw < 1 || dh < 1 || rw < 1 || rh < 1) {
			return;
		}

		RectF dstRect = new RectF();
		matrix.setTranslate(dw * -.5f, dh * -.5f);
		matrix.postRotate(rotation);
		matrix.mapRect(dstRect, srcRect);

		float xr = rw / dstRect.width();
		float yr = rh / dstRect.height();
		float scale;

		if (scaleType == ImageView.ScaleType.CENTER) {
			scale = 1f;
		} else if (scaleType == ImageView.ScaleType.CENTER_INSIDE) {
			scale = Math.min(xr, yr);
		} else if (scaleType == ImageView.ScaleType.CENTER_CROP) {
			scale = Math.max(xr, yr);
		} else {
			throw new UnsupportedOperationException();
		}

		matrix.postScale(scale, scale);
		matrix.postTranslate(
				Math.round(rect.left + rw * .5f),
				Math.round(rect.top + rh * .5f));

		matrix.mapRect(dstRect, srcRect);

		minWidth = dstRect.width();
	}

	private void init(Context context) {
		super.setScaleType(ImageView.ScaleType.MATRIX);
		float dp = context.getResources().getDisplayMetrics().density;
		doubleTapSensivity = 16f * dp;
		doubleTapTimeout = ViewConfiguration.get(context)
				.getDoubleTapTimeout();
	}

	private RectF getDrawableRect() {
		Drawable drawable;
		int w = 0;
		int h = 0;

		if ((drawable = getDrawable()) != null) {
			w = drawable.getIntrinsicWidth();
			h = drawable.getIntrinsicHeight();
		}

		return new RectF(0, 0, w, h);
	}

	private RectF getMappedRect() {
		RectF dstRect = new RectF();
		transformMatrix.mapRect(dstRect, getDrawableRect());
		return dstRect;
	}

	private void initTransform(
			MotionEvent event,
			int pointerCount,
			int ignoreIndex) {
		initialMatrix.set(transformMatrix);
		initialRect.set(getDrawableRect());

		// try to find two pointers that are down;
		// pointerCount may include a pointer that
		// has gone up (ignoreIndex)
		int p1 = 0xffff;
		int p2 = 0xffff;

		for (int i = 0; i < pointerCount; ++i) {
			setInitialPoint(event, i);

			if (i == ignoreIndex || p2 != 0xffff) {
				continue;
			}

			if (p1 == 0xffff) {
				p1 = i;
			} else {
				p2 = i;
			}
		}

		if (p2 != 0xffff) {
			initialGesture.set(event, p1, p2);
		}
	}

	private void setInitialPoint(MotionEvent event, int pointerIndex) {
		int id = event.getPointerId(pointerIndex);
		initialPoint.put(id, new PointF(
				event.getX(pointerIndex),
				event.getY(pointerIndex)));
	}

	private void transform(MotionEvent event, int pointerCount) {
		transformMatrix.set(initialMatrix);

		if (pointerCount == 1) {
			/*int pointerIndex = event.getActionIndex();
			int id = event.getPointerId(pointerIndex);
			PointF point = initialPoint.get(id);
			if (point != null) {
				transformMatrix.postTranslate(
						event.getX(pointerIndex) - point.x,
						event.getY(pointerIndex) - point.y);
			}*/
			PointF point = getPointerDelta(event);
			if (point != null) {
				transformMatrix.postTranslate(point.x, point.y);
			}
		} else if (pointerCount > 1) {
			transformGesture.set(event, 0, 1);

			float scale = fitScale(
					initialMatrix,
					initialRect,
					transformGesture.length / initialGesture.length);

			transformMatrix.postScale(
					scale,
					scale,
					initialGesture.pivotX,
					initialGesture.pivotY);

			transformMatrix.postTranslate(
					transformGesture.pivotX - initialGesture.pivotX,
					transformGesture.pivotY - initialGesture.pivotY);
		}

		if (fitMatrix(transformMatrix, initialRect, bounds)) {
			initTransform(event, pointerCount, -1);
		}

		super.setImageMatrix(transformMatrix);
	}

	private float fitScale(
			Matrix matrix,
			RectF rect,
			float scale) {
		RectF dstRect = new RectF();
		matrix.mapRect(dstRect, rect);

		float w = dstRect.width();
		return w * scale < minWidth ?
				minWidth / w :
				scale;
	}

	private static boolean fitMatrix(
			Matrix matrix,
			RectF rect,
			RectF frame) {
		RectF dstRect = new RectF();
		matrix.mapRect(dstRect, rect);

		float x = dstRect.left;
		float y = dstRect.top;
		float w = dstRect.width();
		float h = dstRect.height();
		float bw = frame.width();
		float bh = frame.height();
		float minX = frame.right - w;
		float minY = frame.bottom - h;
		float dx = w > bw ?
				Math.max(minX - x, Math.min(frame.left - x, 0)) :
				(frame.left + Math.round((bw - w) * .5f)) - x;
		float dy = h > bh ?
				Math.max(minY - y, Math.min(frame.top - y, 0)) :
				(frame.top + Math.round((bh - h) * .5f)) - y;

		if (dx != 0 || dy != 0) {
			matrix.postTranslate(dx, dy);
			return true;
		}

		return false;
	}

	/*private double distanceFromInitial(MotionEvent event) {
		int pointerIndex = event.getActionIndex();
		int id = event.getPointerId(pointerIndex);
		PointF point = initialPoint.get(id);
		if (point == null) {
			return 0;
		}
		float dx = event.getX(pointerIndex) - point.x;
		float dy = event.getY(pointerIndex) - point.y;
		return Math.sqrt(dx * dx + dy * dy);
	}*/

	private double distanceFromInitial(MotionEvent event) {
		PointF point = getPointerDelta(event);
		return point == null ? 0 :
				Math.sqrt(point.x * point.x + point.y * point.y);
	}

	private PointF getPointerDelta(MotionEvent event) {
		int pointerIndex = event.getActionIndex();
		int id = event.getPointerId(pointerIndex);
		PointF point = initialPoint.get(id);
		return point == null ? null : new PointF(
				event.getX(pointerIndex) - point.x,
				event.getY(pointerIndex) - point.y);
	}

	private void magnify(float x, float y) {
		if (inBounds()) {
			float scale = 3f;
			transformMatrix.postScale(scale, scale, x, y);
		} else {
			setMinWidth(bounds, transformMatrix);
		}
		super.setImageMatrix(transformMatrix);
	}

	private static class Gesture {
		private float length;
		private float pivotX;
		private float pivotY;

		private void set(MotionEvent event, int p1, int p2) {
			float x1 = event.getX(p1);
			float y1 = event.getY(p1);
			float x2 = event.getX(p2);
			float y2 = event.getY(p2);
			float dx = x2 - x1;
			float dy = y2 - y1;

			length = (float) Math.sqrt(dx * dx + dy * dy);
			pivotX = (x1 + x2) * .5f;
			pivotY = (y1 + y2) * .5f;
		}
	}
}
