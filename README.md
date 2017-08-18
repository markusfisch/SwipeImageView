# SwipeImageView

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
		compile 'com.github.markusfisch:SwipeImageView:1.1.2'
	}

### Manually

If you prefer your repositories to be self-reliant, just copy the
`swipeimageview` folder into your project root and add it as a subproject
to `settings.gradle`:

	include ':app', ':swipeimageview'

And to the dependencies block of your `app/build.gradle`:

	dependencies {
		compile project(':swipeimageview')
	}

How to use
----------

Add it to a layout:

	<de.markusfisch.android.swipeimageview.widget.SwipeImageView
		xmlns:android="http://schemas.android.com/apk/res/android"
		android:id="@+id/swipe_view"
		android:layout_width="match_parent"
		android:layout_height="match_parent"/>

Or create it in java:

	import de.markusfisch.android.swipeimageview.widget.SwipeImageView;

	SwipeImageView swipeImageView = new SwipeImageView(context);

Then, set up the images to display from a [Cursor][cursor]:

	swipeImageView.setImages(cursor, columnName, startIndex);

Make sure to always invoke `SwipeImageView.closeCursor()` to close the
transferred cursor:

	@Override
	public void onDestroy() {
		super.onDestroy();
		swipeImageView.closeCursor();
	}

If you don't have a Cursor for your image collection, you may simply use
a [MatrixCursor][matrixcursor]:

	String columnName = "path";
	cursor = new MatrixCursor(new String[]{columnName});
	for (String path : paths) {
		cursor.addRow(new Object[]{path});
	}
	swipeImageView.setImages(cursor, columnName, index);

Make sure to call `SwipeImageView.closeCursor()` here too.

Demo
----

This is a demo app you may use to see and try if this widget is what
you're searching for. Either import it into Android Studio or, if you're
not on that thing from Redmond, just type make to build, install and run.

License
-------

This widget is so basic, it should be Public Domain. And it is.

[scalingimageview]: https://github.com/markusfisch/ScalingImageView
[cursor]: https://developer.android.com/reference/android/database/Cursor.html
[matrixcursor]: https://developer.android.com/reference/android/database/MatrixCursor.html
