package de.markusfisch.android.swipeimageview.widget;

import de.markusfisch.android.scalingimageview.widget.ScalingImageView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.v4.widget.EdgeEffectCompat;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;

public class SwipeImageView extends ScalingImageView {
	private final ArrayList<Image> previewImages = new ArrayList<>();
	private final Matrix matrix = new Matrix();
	private final Runnable animationRunnable = new Runnable() {
		@Override
		public void run() {
			long now = System.currentTimeMillis();
			double factor = (now - last) / 16.0;
			last = now;

			deltaX += stepX * factor;

			if (stepX > 0 ? deltaX > finalX : deltaX < finalX) {
				stopSwipe();
			} else {
				post(animationRunnable);
			}

			invalidate();
		}
	};
	private final Runnable loadMaxRunnable = new Runnable() {
		@Override
		public void run() {
			if (swiping) {
				return;
			}
			final int index = currentIndex;
			decodeFileAtAsync(currentIndex, maxSize,
					new OnBitmapLoadedListener() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap) {
					if (!swiping && index == currentIndex) {
						setImageBitmap(bitmap);
					}
				}
			});
		}
	};

	private EdgeEffectCompat edgeEffectLeft;
	private EdgeEffectCompat edgeEffectRight;
	private Cursor cursor;
	private int columnIndex;
	private int imageCount;
	private int currentIndex;
	private int maxSize = 1024;
	private int previewSize = 256;
	private int radius = 3;
	private int pointerId;
	private boolean swiping = false;
	private float swipeThreshold;
	private float initialX;
	private float deltaX;
	private float stepX;
	private float finalX;
	private long initialTime;
	private long last;

	public SwipeImageView(Context context) {
		super(context);
		init(context);
	}

	public SwipeImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SwipeImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void setImages(Cursor cursor, String columnName) {
		setImages(cursor, columnName, 0);
	}

	public void setImages(Cursor cursor, String columnName, int index) {
		if (cursor == null ||
				(columnIndex = cursor.getColumnIndex(columnName)) < 0 ||
				(imageCount = cursor.getCount()) < 1) {
			return;
		}
		this.cursor = cursor;
		currentIndex = Math.abs(index) % imageCount;
		loadPreviews();
		postLoadMax();
	}

	public void setMaxImageSize(int size) {
		maxSize = Math.max(1, size);
	}

	public void setPreviewImageSize(int size) {
		previewSize = Math.max(1, size);
	}

	public int getCurrentIndex() {
		return currentIndex;
	}

	public int getImageCount() {
		return imageCount;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (!swiping) {
			super.onDraw(canvas);
		} else {
			RectF bounds = getBounds();
			float boundsWidth = bounds.width();

			for (int i = Math.max(0, currentIndex - 1),
					l = Math.min(imageCount - 1, currentIndex + 1);
					i <= l; ++i) {
				Image image = getPreviewImage(i);
				if (image != null && image.bitmap != null) {
					matrix.setRectToRect(
							image.rect,
							bounds,
							Matrix.ScaleToFit.CENTER);
					matrix.postTranslate(
							deltaX + (i - currentIndex) * boundsWidth,
							0);
					canvas.drawBitmap(image.bitmap, matrix, null);
				}
			}
		}

		drawEdgeEffects(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// ignore all input while animating
		if (last > 0) {
			return true;
		}

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				initSwipe(event, -1);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				if (swiping) {
					initSwipe(event, event.getActionIndex());
					return true;
				}
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				if (swiping) {
					return true;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				int pointerCount = event.getPointerCount();
				if (swiping) {
					swipe(event);
					return true;
				} else if (initialX > -1 &&
						pointerCount == 1 &&
						inBounds() &&
						Math.abs(getSwipeDistance(event)) > swipeThreshold) {
					startSwipe();
					return true;
				} else if (pointerCount == 1 && inBounds()) {
					// keep ScalingImageView from invoking transform()
					// for an irrelevant event; a single pointer can't
					// scale nor move the image when in bounds
					return true;
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (swiping) {
					completeSwipe(event);
					return true;
				}
				break;
		}

		// forward all other events to ScalingImageView
		return super.onTouchEvent(event);
	}

	protected float getDelta() {
		return deltaX;
	}

	protected Image getPreviewImage(int index) {
		int previewIndex = getPreviewIndex(index);
		if (previewIndex < 0 || previewIndex >= previewImages.size()) {
			return null;
		}
		return previewImages.get(previewIndex);
	}

	protected Bitmap decodeFile(String file, int size) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file, options);
		options.inSampleSize = calculateInSampleSize(
				options.outWidth,
				options.outHeight,
				size,
				size);
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(file, options);
	}

	protected static int calculateInSampleSize(
			int width,
			int height,
			int reqWidth,
			int reqHeight) {
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			int halfHeight = height / 2;
			int halfWidth = width / 2;

			while ((halfHeight / inSampleSize) >= reqHeight &&
					(halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	protected void drawEdgeEffects(Canvas canvas) {
		drawEdgeEffect(canvas, edgeEffectLeft, 90);
		drawEdgeEffect(canvas, edgeEffectRight, 270);
	}

	private void drawEdgeEffect(
			Canvas canvas,
			EdgeEffectCompat edgeEffect,
			int degrees) {
		if (canvas == null || edgeEffect == null ||
				edgeEffect.isFinished()) {
			return;
		}

		int restoreCount = canvas.getSaveCount();
		int width = getWidth();
		int height = getHeight() - getPaddingTop() - getPaddingBottom();

		canvas.rotate(degrees);

		if (degrees == 270) {
			canvas.translate(
					-height + getPaddingTop(),
					0);
		} else {
			canvas.translate(
					-getPaddingTop(),
					-width);
		}

		edgeEffect.setSize(height, width);

		if (edgeEffect.draw(canvas)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				postInvalidateOnAnimation();
			} else {
				postInvalidate();
			}
		}

		canvas.restoreToCount(restoreCount);
	}

	private void init(Context context) {
		float dp = context.getResources().getDisplayMetrics().density;
		swipeThreshold = 16f * dp;

		edgeEffectLeft = new EdgeEffectCompat(context);
		edgeEffectRight = new EdgeEffectCompat(context);

		for (int i = radius * 2 + 1; i-- > 0;) {
			previewImages.add(new Image());
		}
	}

	private void initSwipe(MotionEvent event, int ignore) {
		if (ignore < 0) {
			initialTime = event.getEventTime();
		}
		for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
			if (i != ignore) {
				pointerId = event.getPointerId(i);
				initialX = event.getX(i);
				if (ignore > -1) {
					initialX -= deltaX;
				}
				break;
			}
		}
	}

	private void startSwipe() {
		removeCallbacks(loadMaxRunnable);
		swiping = true;
	}

	private void swipe(MotionEvent event) {
		deltaX = trim(getSwipeDistance(event));

		float width = getBounds().width();
		if (Math.abs(deltaX) >= width) {
			if (deltaX > 0) {
				--currentIndex;
				initialX += width;
				deltaX -= width;
			} else {
				++currentIndex;
				initialX -= width;
				deltaX += width;
			}
			shiftPreviews(deltaX);
			setImageBitmapFromPreview(currentIndex);
		}

		invalidate();
	}

	private void completeSwipe(MotionEvent event) {
		deltaX = trim(getSwipeDistance(event));

		final float swipeDistance = Math.abs(deltaX);
		final float swipeTime = event.getEventTime() - initialTime;
		final float width = getBounds().width();
		final float speed = Math.min(width * .1f, Math.max(
				width * .06f,
				swipeDistance * 16f / swipeTime));

		if (deltaX == 0) {
			swiping = false;
			invalidate();
			return;
		} else if (swipeDistance > width * .5f || swipeTime < 300) {
			if (deltaX > 0) {
				finalX = width;
				stepX = speed;
			} else {
				finalX = -width;
				stepX = -speed;
			}
		} else {
			finalX = 0;
			stepX = deltaX > 0 ? -speed : speed;
		}

		last = System.currentTimeMillis() - 16;
		removeCallbacks(animationRunnable);
		post(animationRunnable);
	}

	private void stopSwipe() {
		if (finalX != 0) {
			currentIndex += finalX < 0 ? 1 : -1;
			shiftPreviews(finalX);
			setImageBitmapFromPreview(currentIndex);
			postLoadMax();
		}
		last = 0;
		swiping = false;
	}

	private float trim(float d) {
		if (d > 0 ?
				currentIndex == 0 :
				currentIndex == imageCount - 1) {
			if (!edgeEffectLeft.isFinished()) {
				edgeEffectLeft.onRelease();
			}
			if (!edgeEffectRight.isFinished()) {
				edgeEffectRight.onRelease();
			}
			float nd = d / getWidth();
			if (nd < 0) {
				// onPull(float) is deprecated from SDK 11 but
				// required because of support for SDK 9
				edgeEffectLeft.onPull(nd);
			} else if (nd > 0) {
				edgeEffectRight.onPull(nd);
			}
			return 0;
		}
		return d;
	}

	private void shiftPreviews(float d) {
		if (d < 0) {
			previewImages.get(0).recycle();
			previewImages.remove(0);
			previewImages.add(new Image());
			loadPreviewAt(currentIndex + radius);
		} else if (d > 0) {
			int lastItem = previewImages.size() - 1;
			previewImages.get(lastItem).recycle();
			previewImages.remove(lastItem);
			previewImages.add(0, new Image());
			loadPreviewAt(currentIndex - radius);
		}
	}

	private void setImageBitmapFromPreview(int index) {
		Image image = getPreviewImage(index);
		setImageBitmap(image != null ? image.bitmap : null);
	}

	private float getSwipeDistance(MotionEvent event) {
		for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
			if (event.getPointerId(i) == pointerId) {
				return event.getX(i) - initialX;
			}
		}
		return 0;
	}

	private void loadPreviews() {
		loadPreviewAt(currentIndex);
		for (int i = Math.max(0, currentIndex - radius); i < currentIndex; ++i) {
			loadPreviewAt(i);
		}
		for (int i = currentIndex + 1, l = Math.min(imageCount, i + radius);
				i < l; ++i) {
			loadPreviewAt(i);
		}
	}

	private void loadPreviewAt(final int index) {
		decodeFileAtAsync(index, previewSize, new OnBitmapLoadedListener() {
			@Override
			public void onBitmapLoaded(Bitmap bitmap) {
				Image image = getPreviewImage(index);
				if (image == null) {
					return;
				}
				image.set(bitmap);
				if (index == currentIndex) {
					setImageBitmap(bitmap);
				}
			}
		});
	}

	private int getPreviewIndex(int index) {
		return index - currentIndex + radius;
	}

	private void postLoadMax() {
		removeCallbacks(loadMaxRunnable);
		postDelayed(loadMaxRunnable, 300);
	}

	private void decodeFileAtAsync(
			final int index,
			final int size,
			final OnBitmapLoadedListener listener) {
		AsyncTask<Void, Void, Bitmap> task =
				new AsyncTask<Void, Void, Bitmap>() {
			@Override
			public Bitmap doInBackground(Void... nothings) {
				return decodeFileAt(index, size);
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				listener.onBitmapLoaded(bitmap);
			}
		};

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			// from DONUT until HONEYCOMB AsyncTask had a pool of threads
			// allowing multiple tasks to operate in parallel
			task.execute();
		} else {
			// starting with HONEYCOMB, tasks are executed on a single
			// thread (what would mean this task would block all other
			// AsyncTask's) unless executeOnExecutor() is used
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private Bitmap decodeFileAt(final int index, int size) {
		if (index < 0 || index >= imageCount) {
			return null;
		}
		String path = getImagePathAt(index);
		for (int tries = 2; tries-- > 0;) {
			try {
				return decodeFile(path, size);
			} catch (OutOfMemoryError e) {
				postDelayed(new Runnable() {
					@Override
					public void run() {
						loadPreviewAt(index);
					}
				}, 300);
			}
		}
		return null;
	}

	private String getImagePathAt(int index) {
		return !cursor.isClosed() && cursor.moveToPosition(index) ?
				cursor.getString(columnIndex) :
				null;
	}

	private interface OnBitmapLoadedListener {
		void onBitmapLoaded(Bitmap bitmap);
	}

	protected static class Image {
		protected final RectF rect = new RectF();
		protected Bitmap bitmap;

		private void set(Bitmap bitmap) {
			recycle();
			if (bitmap != null) {
				this.bitmap = bitmap;
				rect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
			}
		}

		private void recycle() {
			if (bitmap != null) {
				bitmap.recycle();
				bitmap = null;
			}
		}
	}
}
