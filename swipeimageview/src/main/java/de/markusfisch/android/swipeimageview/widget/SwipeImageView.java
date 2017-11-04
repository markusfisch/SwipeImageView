package de.markusfisch.android.swipeimageview.widget;

import de.markusfisch.android.scalingimageview.widget.ScalingImageView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.support.v4.widget.EdgeEffectCompat;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.io.IOException;
import java.util.ArrayList;

public class SwipeImageView extends ScalingImageView {
	private final ArrayList<PreviewImage> previewImages = new ArrayList<>();
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
				public void onBitmapLoaded(Bitmap bitmap, int orientation) {
					if (!swiping && index == currentIndex) {
						setImageBitmap(bitmap, orientation);
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
		initView(context);
	}

	public SwipeImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public SwipeImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView(context);
	}

	/**
	 * Close cursor and free resources.
	 * Make sure to call this as soon as this view isn't required anymore.
	 */
	public void closeCursor() {
		if (cursor == null) {
			return;
		}
		synchronized (cursor) {
			cursor.close();
		}
		cursor = null;
	}

	/**
	 * Set image collection
	 *
	 * @param cursor cursor to image collection
	 * @param columnName name of column holding image path
	 */
	public void setImages(Cursor cursor, String columnName) {
		setImages(cursor, columnName, 0);
	}

	/**
	 * Set image collection
	 *
	 * @param cursor cursor to image collection
	 * @param columnName name of column holding image path
	 * @param index selected image
	 */
	public void setImages(Cursor cursor, String columnName, int index) {
		closeCursor();
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

	/**
	 * Set maximum image size for zoom/pan image.
	 * Default is 1024.
	 *
	 * @param size maximum number of pixels in larger dimension
	 */
	public void setMaxImageSize(int size) {
		maxSize = Math.max(1, size);
	}

	/**
	 * Set maximum image size of preview images.
	 * Default is 256.
	 *
	 * @param size maximum number of pixels in larger dimension
	 */
	public void setPreviewImageSize(int size) {
		previewSize = Math.max(1, size);
	}

	/** Return index of currently selected image */
	public int getCurrentIndex() {
		return currentIndex;
	}

	/** Return number of images in collection */
	public int getImageCount() {
		return imageCount;
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

	@Override
	protected void onDraw(Canvas canvas) {
		if (!swiping) {
			super.onDraw(canvas);
		} else {
			RectF bounds = getBounds();
			float boundsWidth = bounds.width();

			for (int i = Math.max(0, currentIndex - 1),
					l = Math.min(imageCount - 1, currentIndex + 1);
					i <= l; ++i) {
				PreviewImage image = getPreviewImage(i);
				if (image != null && image.bitmap != null) {
					matrix.setRectToRect(
							image.rect,
							bounds,
							Matrix.ScaleToFit.CENTER);
					matrix.preRotate(
							image.orientation,
							image.centerX,
							image.centerY);
					matrix.postTranslate(
							deltaX + (i - currentIndex) * boundsWidth,
							0);
					canvas.drawBitmap(image.bitmap, matrix, null);
				}
			}
		}

		drawEdgeEffects(canvas);
	}

	/** Return current swipe displacement */
	protected float getDelta() {
		return deltaX;
	}

	/**
	 * Return preview image
	 *
	 * @param index index in image collection
	 */
	protected PreviewImage getPreviewImage(int index) {
		int previewIndex = getPreviewIndex(index);
		if (previewIndex < 0 || previewIndex >= previewImages.size()) {
			return null;
		}
		return previewImages.get(previewIndex);
	}

	/**
	 * Decode given image file
	 *
	 * @param file image path
	 * @param size maximum image size
	 */
	protected OrientedBitmap decodeFile(String file, int size) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file, options);
		options.inSampleSize = calculateInSampleSize(
				options.outWidth,
				options.outHeight,
				size,
				size);
		options.inJustDecodeBounds = false;
		return new OrientedBitmap(
				BitmapFactory.decodeFile(file, options),
				getExifOrientation(file));
	}

	/**
	 * Calculate sampling factor
	 *
	 * @param width image width in pixels
	 * @param height image height in pixels
	 * @param reqWidth required width in pixels
	 * @param reqHeight required height in pixels
	 */
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

	/**
	 * Return image orientation in degrees
	 *
	 * @param file path of image file
	 */
	protected static int getExifOrientation(String file) {
		try {
			ExifInterface exif = new ExifInterface(file);
			switch (exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_UNDEFINED)) {
				default:
					return 0;
				case ExifInterface.ORIENTATION_ROTATE_90:
					return 90;
				case ExifInterface.ORIENTATION_ROTATE_180:
					return 180;
				case ExifInterface.ORIENTATION_ROTATE_270:
					return 270;
			}
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * Draw edge effects when the user scrolls beyond content bounds
	 *
	 * @param canvas target canvas
	 */
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
					(float) -height + getPaddingTop(),
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

	private void initView(Context context) {
		float dp = context.getResources().getDisplayMetrics().density;
		swipeThreshold = 16f * dp;

		edgeEffectLeft = new EdgeEffectCompat(context);
		edgeEffectRight = new EdgeEffectCompat(context);

		for (int i = radius * 2 + 1; i-- > 0;) {
			previewImages.add(new PreviewImage());
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
		deltaX = 0;
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
		final float swipeTime = (float) event.getEventTime() - initialTime;
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
		deltaX = 0;
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
			previewImages.add(new PreviewImage());
			loadPreviewAt(currentIndex + radius);
		} else if (d > 0) {
			int lastItem = previewImages.size() - 1;
			previewImages.get(lastItem).recycle();
			previewImages.remove(lastItem);
			previewImages.add(0, new PreviewImage());
			loadPreviewAt(currentIndex - radius);
		}
	}

	private void setImageBitmapFromPreview(int index) {
		PreviewImage image = getPreviewImage(index);
		Bitmap bitmap = null;
		int orientation = 0;
		if (image != null) {
			bitmap = image.bitmap;
			orientation = image.orientation;
		}
		setImageBitmap(bitmap, orientation);
	}

	private void setImageBitmap(Bitmap bitmap, int orientation) {
		setImageRotation(orientation);
		setImageBitmap(bitmap);
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
			public void onBitmapLoaded(Bitmap bitmap, int orientation) {
				PreviewImage image = getPreviewImage(index);
				if (image == null) {
					return;
				}
				image.set(bitmap, orientation);
				if (index == currentIndex) {
					setImageBitmap(bitmap, orientation);
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
		AsyncTask<Void, Void, OrientedBitmap> task =
				new AsyncTask<Void, Void, OrientedBitmap>() {
			@Override
			public OrientedBitmap doInBackground(Void... nothings) {
				return decodeFileAt(index, size);
			}

			@Override
			protected void onPostExecute(OrientedBitmap orientedBitmap) {
				if (orientedBitmap != null) {
					listener.onBitmapLoaded(
							orientedBitmap.bitmap,
							orientedBitmap.orientation);
				} else {
					listener.onBitmapLoaded(null, 0);
				}
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

	private OrientedBitmap decodeFileAt(final int index, int size) {
		if (index < 0 || index >= imageCount) {
			return null;
		}
		String path = getImagePathAt(index);
		if (path == null) {
			return null;
		}
		try {
			return decodeFile(path, size);
		} catch (OutOfMemoryError e) {
			postDelayed(new Runnable() {
				@Override
				public void run() {
					loadPreviewAt(index);
				}
			}, 300);
			return null;
		}
	}

	private String getImagePathAt(int index) {
		if (cursor == null) {
			return null;
		}
		// because Cursor is not thread-safe!
		synchronized (cursor) {
			return !cursor.isClosed() && cursor.moveToPosition(index) ?
					cursor.getString(columnIndex) :
					null;
		}
	}

	/** A preview image */
	protected class PreviewImage {
		protected final RectF rect = new RectF();
		protected Bitmap bitmap;
		protected int orientation;
		protected float centerX;
		protected float centerY;

		private void set(Bitmap bitmap, int orientation) {
			recycle();
			if (bitmap == null) {
				return;
			}

			this.orientation = orientation;
			this.bitmap = bitmap;

			float w = bitmap.getWidth();
			float h = bitmap.getHeight();
			rect.set(0, 0, w, h);
			centerX = w * .5f;
			centerY = h * .5f;
			matrix.setRotate(orientation, centerX, centerY);
			matrix.mapRect(rect, rect);
		}

		private void recycle() {
			if (bitmap != null) {
				bitmap.recycle();
				bitmap = null;
			}
		}
	}

	/** A bitmap with an orientation */
	protected static class OrientedBitmap {
		protected final Bitmap bitmap;
		protected final int orientation;

		protected OrientedBitmap(Bitmap bitmap, int orientation) {
			this.bitmap = bitmap;
			this.orientation = orientation;
		}
	}

	private interface OnBitmapLoadedListener {
		void onBitmapLoaded(Bitmap bitmap, int orientation);
	}
}
