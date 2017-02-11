package de.markusfisch.android.galleryimageview.activity;

import de.markusfisch.android.galleryimageview.widget.GalleryImageView;
import de.markusfisch.android.galleryimageview.R;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
	private static final int REQUEST_PERMISSIONS = 1;

	private GalleryImageView imageView;
	private Cursor cursor;

	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			@NonNull String permissions[],
			@NonNull int grantResults[]) {
		if (requestCode != REQUEST_PERMISSIONS || grantResults.length < 1) {
			return;
		}

		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			setGallery();
		} else {
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		imageView = (GalleryImageView) findViewById(R.id.gallery);

		if (checkPermissions()) {
			setGallery();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		closeCursor();
	}

	private boolean checkPermissions() {
		String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

		if (ContextCompat.checkSelfPermission(this, permission) ==
				PackageManager.PERMISSION_GRANTED) {
			return true;
		}

		ActivityCompat.requestPermissions(
				this,
				new String[]{permission},
				REQUEST_PERMISSIONS);

		return false;
	}

	private void setGallery() {
		cursor = getContentResolver().query(
			Images.Media.EXTERNAL_CONTENT_URI,
			null,
			null,
			null,
			null);
		if (cursor == null) {
			return;
		} else if (!cursor.moveToFirst()) {
			closeCursor();
			return;
		}
		imageView.setImages(cursor, Images.Media.DATA);
	}

	private void closeCursor() {
		if (cursor != null) {
			cursor.close();
			cursor = null;
		}
	}
}
