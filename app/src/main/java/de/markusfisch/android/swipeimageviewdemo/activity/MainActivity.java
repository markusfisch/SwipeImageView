package de.markusfisch.android.swipeimageviewdemo.activity;

import de.markusfisch.android.swipeimageview.widget.SwipeImageView;

import de.markusfisch.android.swipeimageviewdemo.R;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
	private static final int REQUEST_READ_PERMISSION = 1;
	private static final String CURRENT_INDEX = "current_index";

	private SwipeImageView imageView;
	private int currentIndex;

	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			@NonNull String permissions[],
			@NonNull int grantResults[]) {
		if (requestCode != REQUEST_READ_PERMISSION ||
				grantResults.length < 1) {
			return;
		}

		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			setImages(currentIndex);
		} else {
			finish();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_INDEX, imageView.getCurrentIndex());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		imageView = (SwipeImageView) findViewById(R.id.swipe_view);
		currentIndex = state != null ? state.getInt(CURRENT_INDEX) : 0;

		if (requestReadPermission()) {
			setImages(currentIndex);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		imageView.closeCursor();
	}

	private boolean requestReadPermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}

		String permission = android.Manifest.permission.READ_EXTERNAL_STORAGE;

		if (ContextCompat.checkSelfPermission(this, permission) ==
				PackageManager.PERMISSION_GRANTED) {
			return true;
		}

		ActivityCompat.requestPermissions(
				this,
				new String[]{permission},
				REQUEST_READ_PERMISSION);

		return false;
	}

	private void setImages(int index) {
		Cursor cursor = getContentResolver().query(
				Images.Media.EXTERNAL_CONTENT_URI,
				null,
				null,
				null,
				null);
		if (cursor == null) {
			return;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return;
		}
		imageView.setImages(cursor, Images.Media.DATA, index);
	}
}
