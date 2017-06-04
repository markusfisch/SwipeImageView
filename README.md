SwipeImageView
================

Swipe/zoom/pinch ImageView for Android.

How to include
--------------

### Gradle

Add the JitPack repository in your root build.gradle at the end of
repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Then add the dependency in your app/build.gradle:

	dependencies {
		compile 'com.github.markusfisch:SwipeImageView:1.0.0'
	}

### Manually

Drop [SwipeImageView.java][src] and
[ScalingImageView.java][scalingimageview] into your project.

How to use
----------

Add it to a layout:

	<de.markusfisch.android.swipeimageview.widget.SwipeImageView
		xmlns:android="http://schemas.android.com/apk/res/android"
		android:id="@+id/swipe_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"/>

Or create it in java:

	SwipeImageView swipeImageView = new SwipeImageView(context);

Then, set up the images to display from a [Cursor][cursor]:

	swipeImageView.setImages(cursor, columnName, startIndex);

If you don't have a Cursor for your image collection, you may simply use
a [MatrixCursor][matrixcursor]:

	String columnName = "path";
	cursor = new MatrixCursor(new String[]{columnName});
	for (String path : paths) {
		cursor.addRow(new Object[]{path});
	}
	swipeImageView.setImages(cursor, columnName, index);

Just remember to always close Cursor's when they're no longer required:

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (cursor != null) {
			cursor.close();
			cursor = null;
		}
	}

Demo
----

This is a demo app you may use to see and try if this widget is what
you're searching for. Either import it into Android Studio or, if you're
not on that thing from Redmond, just type make to build, install and run.

License
-------

This widget is so basic, it should be Public Domain. And it is.

[src]: https://github.com/markusfisch/SwipeImageView/blob/master/swipeimageview/src/main/java/de/markusfisch/android/swipeimageview/widget/SwipeImageView.java
[scalingimageview]: https://github.com/markusfisch/ScalingImageView
[cursor]: https://developer.android.com/reference/android/database/Cursor.html
[matrixcursor]: https://developer.android.com/reference/android/database/MatrixCursor.html
