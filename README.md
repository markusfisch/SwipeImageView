GalleryImageView
================

A swipe/zoom/pinch gallery view.

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
		compile 'com.github.markusfisch:GalleryImageView:1.0.0'
	}

### Manually

Drop GalleryImageView.java and [ScalingImageView.java][scalingimageview]
into your project.

How to use
----------

Add it to a layout:

	<de.markusfisch.android.scalingimageview.widget.GalleryImageView
		xmlns:android="http://schemas.android.com/apk/res/android"
		android:id="@+id/gallery"
		android:layout_width="match_parent"
		android:layout_height="match_parent"/>

Or create it in java:

	GalleryImageView galleryImageView = new GalleryImageView(context);

Then, set up the images to display:

	ArrayList<String> files = new ArrayList<>;
	files.add("path/to/first/image");
	...
	galleryImageView.setImages(files, indexOfFileToSelect);

Demo
----

This is a demo app you may use to see and try if this widget is what
you're searching for. Either import it into Android Studio or, if you're
not on that thing from Redmond, just type make to build, install and run.

License
-------

This widget is so basic, it should be Public Domain. And it is.

[scalingimageview]: https://github.com/markusfisch/ScalingImageView
