package de.markusfisch.android.galleryimageview.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
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
			dx += step * factor;

			if (Math.abs(targetX - dx) < Math.abs(step)) {
				dx = targetX;
				if (targetX != 0) {
					shiftBitmaps(targetX > 0);
				}
				last = 0;
				swiping = false;
			} else {
				post(animationRunnable);
			}

			invalidate();
		}
	};

	private interface OnBitmapLoadedListener {
		void onBitmapLoaded(Bitmap bitmap);
	}

	private ArrayList<String> imageList;
	private int currentIndex;
	private Bitmap previousBitmap;
	private Bitmap currentBitmap;
	private Bitmap nextBitmap;
	private boolean swiping = false;
	private float threshold;
	private float originX;
	private float dx;
	private float speed;
	private float step;
	private float targetX;
	private long last = 0;
	private int maxImageSize = 1024;

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

	public void setImages(ArrayList<String> list, int index) {
		imageList = list;
		setCurrentImage(index);
	}

	public void setMaxImageSize(int size) {
		maxImageSize = Math.max(16, Math.min(1280, size));
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
			matrix.postTranslate(-bounds.width() + dx, 0);
			canvas.drawBitmap(previousBitmap, matrix, null);
		}
		if (currentBitmap != null) {
			matrix.setRectToRect(
					currentBitmapRectF,
					bounds,
					Matrix.ScaleToFit.CENTER);
			matrix.postTranslate(dx, 0);
			canvas.drawBitmap(currentBitmap, matrix, null);
		}
		if (nextBitmap != null) {
			matrix.setRectToRect(
					nextBitmapRectF,
					bounds,
					Matrix.ScaleToFit.CENTER);
			matrix.postTranslate(bounds.width() + dx, 0);
			canvas.drawBitmap(nextBitmap, matrix, null);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// ignore all input while animating
		if (last > 0) {
			return true;
		}

		int pointerCount = event.getPointerCount();

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				initSwipe(event);
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_DOWN:
				// ignore additional fingers while swiping
				if (swiping) {
					return true;
				}
				// ignore all events if there are additional pointers
				originX = -1;
				break;
			case MotionEvent.ACTION_MOVE:
				if (swiping) {
					return swipe(event);
				} else if (originX > -1 &&
						pointerCount == 1 &&
						!isScaled() &&
						Math.abs(getSwipeDistance(event)) > threshold) {
					return startSwipe(event);
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (swiping) {
					return stopSwipe();
				}
				break;
		}

		return super.onTouchEvent(event);
	}

	protected Bitmap decodeFile(String file) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file, options);
		options.inSampleSize = calculateInSampleSize(
				options.outWidth,
				options.outHeight,
				maxImageSize,
				maxImageSize);
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
		threshold = 16f * dp;
		speed = 10f * dp;
	}

	private void setCurrentImage(int index) {
		if (imageList == null || imageList.size() < 1) {
			return;
		}

		previousBitmap = decodeFileAt(index - 1);
		currentBitmap = decodeFileAt(index);
		nextBitmap = decodeFileAt(index + 1);
		updateRects();

		currentIndex = index;
		setImageBitmap(currentBitmap);
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

	private Bitmap decodeFileAt(int index) {
		return index > -1 && index < imageList.size() ?
				decodeFile(imageList.get(index)) :
				null;
	}

	private static Rect getBitmapRect(Bitmap bitmap) {
		return new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	}

	private void initSwipe(MotionEvent event) {
		originX = event.getX();
	}

	private float getSwipeDistance(MotionEvent event) {
		return event.getX() - originX;
	}

	private boolean startSwipe(MotionEvent event) {
		swiping = true;
		return true;
	}

	private boolean swipe(MotionEvent event) {
		dx = getSwipeDistance(event);
		if ((dx > 0 && previousBitmap == null) ||
				(dx < 0 && nextBitmap == null)) {
			dx = 0;
		}
		invalidate();
		return true;
	}

	private boolean stopSwipe() {
		float width = getBounds().width();
		if (dx == 0) {
			swiping = false;
			invalidate();
			return true;
		} else if (Math.abs(dx) > width * .5f) {
			if (dx > 0) {
				targetX = width;
				step = speed;
			} else {
				targetX = -width;
				step = -speed;
			}
		} else {
			targetX = 0;
			step = dx > 0 ? -speed : speed;
		}

		last = System.currentTimeMillis() - 16;
		post(animationRunnable);
		return true;
	}

	private void shiftBitmaps(boolean backwards) {
		if (backwards) {
			--currentIndex;
			nextBitmap = currentBitmap;
			currentBitmap = previousBitmap;
			previousBitmap = null;
			decodeFileAtAsync(currentIndex - 1, new OnBitmapLoadedListener() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap) {
					previousBitmap = bitmap;
					updateRects();
				}
			});
		} else {
			++currentIndex;
			previousBitmap = currentBitmap;
			currentBitmap = nextBitmap;
			nextBitmap = null;
			decodeFileAtAsync(currentIndex + 1, new OnBitmapLoadedListener() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap) {
					nextBitmap = bitmap;
					updateRects();
				}
			});
		}
		updateRects();
		setImageBitmap(currentBitmap);
	}

	private void decodeFileAtAsync(
			final int index,
			final OnBitmapLoadedListener listener) {
		new AsyncTask<Void, Void, Bitmap>() {
			@Override
			public Bitmap doInBackground(Void... nothings) {
				return decodeFileAt(index);
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				listener.onBitmapLoaded(bitmap);
			}
		}.execute();
	}
}
