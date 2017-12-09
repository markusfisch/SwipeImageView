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

### As subproject

If you prefer your project to be self-reliant and completely modifiable,
just copy the `swipeimageview` folder (and `scalingimageview` from
[here][scalingimageview]) into your project root and add them as
subprojects to `settings.gradle`:

	include ':app', ':swipeimageview', ':scalingimageview'

Add `swipeimageview` to the dependencies block of your `app/build.gradle`:

	dependencies {
		compile project(':swipeimageview')
	}

And replace the `com.github.markusfisch:ScalingImageView...` entry in
`swipeimageview/build.gradle` with the local subproject:

	dependencies {
		compile project(':scalingimageview')
	}

Because `swipeimageview/build.gradle` and `scalingimageview/build.gradle`
use variables to manage version numbers of Android's dependencies, you
need to define them in you root `build.gradle` (or replace the variables
with literals):

	buildscript {
		ext.tools_version = '2.3.3'
		ext.support_version = '25.3.1'
		...
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
