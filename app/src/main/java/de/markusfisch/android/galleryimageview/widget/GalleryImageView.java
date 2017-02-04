package de.markusfisch.android.galleryimageview.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;

public class GalleryImageView extends ScalingImageView {
	private final Matrix matrix = new Matrix();
	private final RectF previousBitmapRectF = new RectF();
	private final RectF currentBitmapRectF = new RectF();
	private final RectF nextBitmapRectF = new RectF();
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

	private ArrayList<String> imageList;
	private int currentIndex;
	private int previewSize = 32;//320;
	private int maxSize = 1024;
	private Bitmap previousBitmap;
	private Bitmap currentBitmap;
	private Bitmap nextBitmap;
	private boolean swiping = false;
	private float swipeThreshold;
	private float initialX;
	private float deltaX;
	private float stepX;
	private float finalX;
	private long initialTime;
	private long last;

	public GalleryImageView(Context context) {
		super(context);
		init(context);
	}

	public GalleryImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public GalleryImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public GalleryImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}

	public void setImages(ArrayList<String> list, int index) {
		imageList = list;
		setCurrentImage(index);
	}

	public void setPreviewImageSize(int size) {
		previewSize = Math.max(2, Math.min(1024, size));
	}

	public void setMaxImageSize(int size) {
		maxSize = Math.max(2, Math.min(1024, size));
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (!swiping) {
			super.onDraw(canvas);
			return;
		}

		RectF bounds = getBounds();
		if (previousBitmap != null) {
			matrix.setRectToRect(
					previousBitmapRectF,
					bounds,
					Matrix.ScaleToFit.CENTER);
			matrix.postTranslate(-bounds.width() + deltaX, 0);
			canvas.drawBitmap(previousBitmap, matrix, null);
		}
		if (currentBitmap != null) {
			matrix.setRectToRect(
					currentBitmapRectF,
					bounds,
					Matrix.ScaleToFit.CENTER);
			matrix.postTranslate(deltaX, 0);
			canvas.drawBitmap(currentBitmap, matrix, null);
		}
		if (nextBitmap != null) {
			matrix.setRectToRect(
					nextBitmapRectF,
					bounds,
					Matrix.ScaleToFit.CENTER);
			matrix.postTranslate(bounds.width() + deltaX, 0);
			canvas.drawBitmap(nextBitmap, matrix, null);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// ignore and swallow everything while animating
		if (last > 0) {
			return true;
		}

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				initSwipe(event, -1);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				initSwipe(event, event.getActionIndex());
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				// ignore additional fingers while swiping
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

		return super.onTouchEvent(event);
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

	private static int calculateInSampleSize(
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

	private void init(Context context) {
		float dp = context.getResources().getDisplayMetrics().density;
		swipeThreshold = 16f * dp;
	}

	private void setCurrentImage(int index) {
		int size;
		if (imageList == null || (size = imageList.size()) < 1) {
			return;
		}
		index = Math.abs(index) % size;

		previousBitmap = decodeFileAt(index - 1, previewSize);
		currentBitmap = decodeFileAt(index, previewSize);
		nextBitmap = decodeFileAt(index + 1, previewSize);
		updateRects();

		currentIndex = index;
		setPreviewAndLoadAsync();
	}

	private Bitmap decodeFileAt(int index, int size) {
		return index > -1 && index < imageList.size() ?
				decodeFile(imageList.get(index), size) :
				null;
	}

	private void setPreviewAndLoadAsync() {
		setImageBitmap(currentBitmap);
		if (currentBitmap != null &&
				(currentBitmap.getWidth() >= maxSize ||
						currentBitmap.getHeight() >= maxSize)) {
			return;
		}
		final int index = currentIndex;
		decodeFileAtAsync(currentIndex, maxSize, new OnBitmapLoadedListener() {
			@Override
			public void onBitmapLoaded(Bitmap bitmap) {
				if (currentIndex == index) {
					currentBitmap = bitmap;
					updateRects();
					setImageBitmap(bitmap);
				} else if (currentBitmap == null) {
// how can currentBitmap be null?
					setPreviewAndLoadAsync();
				}
			}
		});
	}

	private void updateRects() {
		if (previousBitmap != null) {
			previousBitmapRectF.set(getBitmapRect(previousBitmap));
		}
		if (currentBitmap != null) {
			currentBitmapRectF.set(getBitmapRect(currentBitmap));
		}
		if (nextBitmap != null) {
			nextBitmapRectF.set(getBitmapRect(nextBitmap));
		}
	}

	private static Rect getBitmapRect(Bitmap bitmap) {
		return new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	}

	private void initSwipe(MotionEvent event, int ignore) {
		if (ignore < 0) {
			initialTime = event.getEventTime();
		}
		for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
			if (i != ignore) {
				initialX = event.getX(i);
				break;
			}
		}
	}

	private float getSwipeDistance(MotionEvent event) {
		return event.getX() - initialX;
	}

	private void startSwipe() {
		swiping = true;
	}

	private void swipe(MotionEvent event) {
		deltaX = getSwipeDistance(event);
		if (deltaX > 0 ? currentIndex == 0 :
				currentIndex == imageList.size() - 1) {
			deltaX = 0;
		}
		invalidate();
	}

	private void completeSwipe(MotionEvent event) {
		final float swipeDistance = Math.abs(event.getX() - initialX);
		final float swipeTime = event.getEventTime() - initialTime;
		final float width = getBounds().width();
		final float speed = Math.max(
				width * .06f,
				swipeDistance * 16f / swipeTime);

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
		deltaX = finalX;
		if (finalX != 0) {
			shiftBitmaps(finalX > 0);
		}
		last = 0;
		swiping = false;
	}

	private void shiftBitmaps(boolean backwards) {
		if (backwards) {
			--currentIndex;
			nextBitmap = currentBitmap;
			currentBitmap = previousBitmap;
			previousBitmap = null;
			final int previousIndex = currentIndex - 1;
			decodeFileAtAsync(previousIndex, previewSize,
					new OnBitmapLoadedListener() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap) {
					if (previousIndex == currentIndex - 1) {
						previousBitmap = bitmap;
						updateRects();
					}
				}
			});
		} else {
			++currentIndex;
			previousBitmap = currentBitmap;
			currentBitmap = nextBitmap;
			nextBitmap = null;
			final int nextIndex = currentIndex + 1;
			decodeFileAtAsync(nextIndex, previewSize,
					new OnBitmapLoadedListener() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap) {
					if (nextIndex == currentIndex + 1) {
						nextBitmap = bitmap;
						updateRects();
					}
				}
			});
		}
		updateRects();
		setPreviewAndLoadAsync();
	}

	private void decodeFileAtAsync(
			final int index,
			//final int deviation,
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
				//if (currentIndex + deviation == index) {
				if (bitmap != null) {
					listener.onBitmapLoaded(bitmap);
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

	private interface OnBitmapLoadedListener {
		void onBitmapLoaded(Bitmap bitmap);
	}
}
