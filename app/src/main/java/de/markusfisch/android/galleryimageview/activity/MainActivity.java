package de.markusfisch.android.galleryimageview.activity;

import de.markusfisch.android.galleryimageview.widget.GalleryImageView;
import de.markusfisch.android.galleryimageview.R;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		GalleryImageView galleryView = (GalleryImageView) findViewById(
				R.id.gallery);
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
		for (int i = Math.min(10, cursor.getCount()); i-- > 0; ) {
			files.add(cursor.getString(
					cursor.getColumnIndex(Images.Media.DATA)));
			cursor.moveToNext();
		}
		return files;
	}
}
