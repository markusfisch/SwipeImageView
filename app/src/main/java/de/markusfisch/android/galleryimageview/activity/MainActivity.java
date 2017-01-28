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

	private GalleryImageView galleryView;

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

		galleryView = (GalleryImageView) findViewById(R.id.gallery);

		if (checkPermissions()) {
			setGallery();
		}
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
		galleryView.setImages(getImagesFromGallery(), 0);
	}

	private ArrayList<String> getImagesFromGallery() {
		ArrayList<String> files = new ArrayList<>();
		Cursor cursor = getContentResolver().query(
			Images.Media.EXTERNAL_CONTENT_URI,
			null,
			null,
			null,
			null);
		if (cursor == null) {
			return files;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return files;
		}
		for (int i = Math.min(24, cursor.getCount()); i-- > 0; ) {
			files.add(cursor.getString(
					cursor.getColumnIndex(Images.Media.DATA)));
			cursor.moveToNext();
		}
		return files;
	}
}
