package com.example.radha.bitmapprocessing;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class BitmapUtil {
	/**
	 * This method resolves original file path from given uri.
	 * 
	 * @param context
	 * @param contentUri
	 * @return file path
	 */
	public static String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] proj = { MediaStore.Images.ImageColumns.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null,
					null, null);
			if (cursor == null) {
				return contentUri.getPath();
			}
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Scales the large images by factor of 2 (Actual image will be 4 times
	 * smaller) when images exceed 1000px either on width or height. Also
	 * Rotates images for Samsung phones that set Rotate angles to photos.
	 * 
	 * @param path
	 *            Image Path
	 * @param ctx
	 *            Context in which images are loaded.
	 * @return Scaled image suitable for handling in java code.
	 */
	public static Bitmap getBitmap(Uri pathUri, Context ctx) {
		Uri uri = pathUri;
		InputStream in = null;
		Bitmap bitmap = null;

		try {

			in = ctx.getContentResolver().openInputStream(uri);
			File file = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "tempfile.tmp");
			// write the inputStream to a FileOutputStream
			OutputStream outputStream = new FileOutputStream(file);

			int read = 0;
			byte[] bytes = new byte[1024];

			int angle = 0;
			try {

				while ((read = in.read(bytes)) != -1) {
					outputStream.write(bytes, 0, read);
				}

				ExifInterface exif = new ExifInterface(file.getAbsolutePath());
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_NORMAL);

				if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
					angle = 90;
				else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
					angle = 180;
				else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
					angle = 270;

			} catch (IOException e) {
				Log.e("path", e.toString());
				e.printStackTrace();

			} catch (IllegalArgumentException e) {
				// catches error while loading synced images metadata
				e.printStackTrace();
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			bitmap = BitmapFactory.decodeStream(in, null, options);

			BitmapFactory.Options newImageOptions = new BitmapFactory.Options();
			if (options.outHeight > 1000 || options.outWidth > 1000) {
				newImageOptions.inSampleSize = 4;
			}
			in = ctx.getContentResolver().openInputStream(uri);

			bitmap = BitmapFactory.decodeStream(in, null, newImageOptions);

			return bitmap;
		} catch (FileNotFoundException e) {

		} catch (OutOfMemoryError e) {

			if (bitmap != null)
				bitmap.recycle();
			bitmap = null;
			System.gc();
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	/**
	 * Method fits the given image bitmap into the center of the given view size
	 * by scaling bitmap size using canvas draw method.
	 * 
	 * @param bmp
	 * @param screenWidth
	 * @param screenHeight
	 * @return Bitmap the result bitmap is size of view with given bitmap at the
	 *         center
	 */
	public static Bitmap fitToViewByScale(Bitmap bmp, int screenWidth,
			int screenHeight) {
		Bitmap background = Bitmap.createBitmap(screenWidth, screenHeight,
				Config.ARGB_8888);
		float originalWidth = bmp.getWidth(), originalHeight = bmp.getHeight();
		Canvas canvas = new Canvas(background);
		float scale = screenWidth / (float) originalWidth;
		float xTranslation = 0.0f;
		float yTranslation = (screenHeight - originalHeight * scale) / 2.0f;
		Matrix transformation = new Matrix();
		transformation.postTranslate(xTranslation, yTranslation);
		transformation.preScale(scale, scale);
		Paint paint = new Paint();
		paint.setFilterBitmap(true);
		canvas.drawBitmap(bmp, transformation, paint);
		/*
		 * if (bmp != null) { bmp.recycle(); bmp = null; }
		 */
		return background;
	}

	/**
	 * Method fits the given image bitmap into the center of the given view size
	 * by scaling bitmap size using Rect fit method.
	 * 
	 * @param bmp
	 * @param screenWidth
	 * @param screenHeight
	 * @return Bitmap the result bitmap is size of view with given bitmap at the
	 *         center
	 */
	public static Bitmap fitToViewByRect(Bitmap bmp, int screenWidth,
			int screenHeight) {
		RectF defaultRect = new RectF(0, 0, bmp.getWidth(), bmp.getHeight());
		RectF screenRect = new RectF(0, 0, screenWidth, screenHeight);
		Matrix defToScreenMatrix = new Matrix();
		defToScreenMatrix.setRectToRect(defaultRect, screenRect,
				Matrix.ScaleToFit.CENTER);
		Bitmap newbmp = Bitmap.createBitmap(screenWidth, screenHeight,
				Config.ARGB_8888);
		Canvas c = new Canvas(newbmp);
		c.drawColor(Color.TRANSPARENT,Mode.CLEAR);
		c.drawBitmap(bmp, defToScreenMatrix, null);
		if (bmp != null) {
			bmp.recycle();
			bmp = null;
		}
		return newbmp;
	}

	/**
	 * Scale and keep aspect ratio
	 * 
	 * @param b
	 * @param width
	 * @return Bitmap scaled to fit width-wise and height scaled in scale ratio.
	 */
	static public Bitmap scaleToFitWidth(Bitmap b, int width) {
		float factor = width / (float) b.getWidth();
		return Bitmap.createScaledBitmap(b, width,
				(int) (b.getHeight() * factor), false);
	}

	/**
	 * Scale and keep aspect ratio
	 * 
	 * @param b
	 * @param height
	 * @return Bitmap scaled to fit height-wise and width scaled in scale ratio.
	 */
	static public Bitmap scaleToFitHeight(Bitmap b, int height) {
		float factor = height / (float) b.getHeight();
		return Bitmap.createScaledBitmap(b, (int) (b.getWidth() * factor),
				height, false);
	}

	/**
	 * Scale and keep aspect ratio
	 * 
	 * @param b
	 * @param width
	 * @param height
	 * @return Bitmap both width and height scaled according to scale factor
	 */
	static public Bitmap scaleToFill(Bitmap b, int width, int height) {
		float factorH = height / (float) b.getWidth();
		float factorW = width / (float) b.getWidth();
		float factorToUse = (factorH > factorW) ? factorW : factorH;
		return Bitmap.createScaledBitmap(b, (int) (b.getWidth() * factorToUse),
				(int) (b.getHeight() * factorToUse), false);
	}

	/**
	 * Converts a bitmap into an editable bitmap by temperory file writing
	 * 
	 * @param imgIn
	 * @return Bitmap editable bitmap decoded in ARGB_888 format
	 */
	public static Bitmap convertToMutable(Bitmap imgIn) {
		try {
			// this is the file going to use temporally to save the bytes.
			// This file will not be a image, it will store the raw image data.
			File file = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "temp.tmp");

			// Open an RandomAccessFile
			// Make sure you have added uses-permission
			// android:name=android.permission.WRITE_EXTERNAL_STORAGE
			// into AndroidManifest.xml file
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			// get the width and height of the source bitmap.
			int width = imgIn.getWidth();
			int height = imgIn.getHeight();
			Config type = imgIn.getConfig();

			// Copy the byte to the file
			// Assume source bitmap loaded using options.inPreferredConfig =
			// Config.ARGB_8888;
			FileChannel channel = randomAccessFile.getChannel();
			MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0,
					imgIn.getRowBytes() * height);
			imgIn.copyPixelsToBuffer(map);
			// recycle the source bitmap, this will be no longer used.
			imgIn.recycle();
			System.gc();// try to force the bytes from the imgIn to be released

			// Create a new bitmap to load the bitmap again. Probably the memory
			// will be available.
			imgIn = Bitmap.createBitmap(width, height, type);
			map.position(0);
			// load it back from temporary
			imgIn.copyPixelsFromBuffer(map);
			// close the temporary file and channel , then delete that also
			channel.close();
			randomAccessFile.close();

			// delete the temp file
			file.delete();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return imgIn;
	}

	/**
	 * This Method will return gallery content uri
	 */
	public static String getPath(Context context, Uri uri) {
		String[] stringprojection = { MediaStore.Images.Media.DATA };
		Cursor cursor = context.getContentResolver().query(uri,
				stringprojection, null, null, null);
		if (cursor == null) {
			return null;
		}
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String stringcolumnindex = cursor.getString(column_index);
		return stringcolumnindex;
	}

	public static Bitmap getImageFromSDCard(String resultPath) {
		String path = resultPath;
		InputStream is = null;
		Bitmap bitmap = null;
		try {
			is = new FileInputStream(path);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			bitmap = BitmapFactory.decodeStream(is, null, options);
			BitmapFactory.Options newImageOptions = new BitmapFactory.Options();
			is = new FileInputStream(path);
			bitmap = BitmapFactory.decodeStream(is, null, newImageOptions);
			is.close();
			return bitmap;
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Bitmap getBitmapFromPath(String resultpath,
			DisplayMetrics metrics) {
		InputStream is = null;
		Bitmap bitmap = null;
		try {
			is = new FileInputStream(resultpath);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, options);

			int attributeInt = 0;
			try {
				attributeInt = new ExifInterface(resultpath).getAttributeInt(
						"Orientation", 1);
			} catch (IOException ex) {
			}

			options.inSampleSize = calculateInSampleSize(options,
					metrics.widthPixels, metrics.heightPixels, attributeInt);
			options.inJustDecodeBounds = false;
			/*
			 * BitmapFactory.Options newImageOptions = new
			 * BitmapFactory.Options(); if (options.outHeight >
			 * metrics.heightPixels || options.outWidth > metrics.widthPixels) {
			 * newImageOptions.inSampleSize = 2; }
			 */

			// IOUtilities.closeStream(is);
			is = new FileInputStream(resultpath);

			/* bitmap = BitmapFactory.decodeStream(is, null, options); */
			bitmap = rotateImage(BitmapFactory.decodeStream(is, null, options),
					resultpath);
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return bitmap;
		} catch (FileNotFoundException e) {
		} finally {
			// IOUtilities.closeStream(is);
		}
		return null;
	}
	
	public static Bitmap getBitmapFromPath(String resultpath,
			Point point) {
		InputStream is = null;
		Bitmap bitmap = null;
		try {
			is = new FileInputStream(resultpath);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, options);

			int attributeInt = 0;
			try {
				attributeInt = new ExifInterface(resultpath).getAttributeInt(
						"Orientation", 1);
			} catch (IOException ex) {
			}

			options.inSampleSize = calculateInSampleSize(options,
					point.x, point.y, attributeInt);
			options.inJustDecodeBounds = false;
			/*
			 * BitmapFactory.Options newImageOptions = new
			 * BitmapFactory.Options(); if (options.outHeight >
			 * metrics.heightPixels || options.outWidth > metrics.widthPixels) {
			 * newImageOptions.inSampleSize = 2; }
			 */

			// IOUtilities.closeStream(is);
			is = new FileInputStream(resultpath);

			/* bitmap = BitmapFactory.decodeStream(is, null, options); */
			bitmap = rotateImage(BitmapFactory.decodeStream(is, null, options),
					resultpath);
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return bitmap;
		} catch (FileNotFoundException e) {
		} finally {
			// IOUtilities.closeStream(is);
		}
		return null;
	}
 
	public static  Bitmap getBitmap(String path,Context context) {
		ContentResolver mContentResolver=context.getContentResolver();
		Uri uri = Uri.fromFile(new File(path));
		InputStream in = null;
		Bitmap bitmap = null;
		Matrix matrix = new Matrix();

		int angle = 0;
		try {
			ExifInterface exif = new ExifInterface(path);
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
				angle = 90;
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
				angle = 180;
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
				angle = 270;
			}
		} catch (Exception e) {

		}
		matrix.postRotate(angle);
		try {
			in = mContentResolver.openInputStream(uri);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			bitmap = BitmapFactory.decodeStream(in, null, options);

			BitmapFactory.Options newImageOptions = new BitmapFactory.Options();
			if (options.outHeight > 1000 || options.outWidth > 1000) {
				newImageOptions.inSampleSize = 2;
			}
			in = mContentResolver.openInputStream(uri);

			bitmap = BitmapFactory.decodeStream(in, null, newImageOptions);

			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
					bitmap.getHeight(), matrix, true);
			return bitmap;
		} catch (FileNotFoundException e) {
			//Log.e(TAG, "file " + path + " not found");
		} catch (OutOfMemoryError e) {

			if (bitmap != null)
				bitmap.recycle();
			bitmap = null;
			System.gc();

		}

		finally {
			IOUtilities.closeStream(in);
		}
		return null;
	}
	/**
	 * 
	 * This method is used to get bitmap from gallery,fb,instagram and Camera.
	 * If it fails to return the bitmap (may be case of cached images),then use
	 * the getBitmap() method
	 * 
	 * @param context
	 * @param screenWidth
	 * @param screenHeight
	 * @param isGalleryON
	 * @param galleryUri
	 * @return
	 */
	public static Bitmap getBitmapFromUri(final Context context,
			int screenWidth, int screenHeight, boolean isGalleryON,
			Uri galleryUri, String imagePath) {
		String tempImagepath = imagePath;
		if (tempImagepath == null) {
			if (isGalleryON) {
				final String[] array = { "_data" };
				final Cursor query = context.getContentResolver().query(
						galleryUri, array, null, null, null);
				final boolean moveToFirst = query.moveToFirst();
				if (moveToFirst) {
					tempImagepath = query.getString(query
							.getColumnIndexOrThrow("_data"));
				}
				query.close();
			} else {
				tempImagepath = galleryUri.getPath();
			}
		}
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(tempImagepath, options);
		int attributeInt = 0;
		try {
			attributeInt = new ExifInterface(tempImagepath).getAttributeInt(
					"Orientation", 1);
		} catch (IOException ex) {
		}

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, screenWidth,
				screenHeight, attributeInt);
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return rotateImage(BitmapFactory.decodeFile(tempImagepath, options),
				tempImagepath);
	}

	public static Bitmap getBitmapFromUriWithoutScale(final Context context,
			boolean isGalleryON, Uri galleryUri, String imagePath) {
		String tempImagepath = imagePath;
		if (tempImagepath == null) {
			if (isGalleryON) {
				final String[] array = { "_data" };
				final Cursor query = context.getContentResolver().query(
						galleryUri, array, null, null, null);
				final boolean moveToFirst = query.moveToFirst();
				if (moveToFirst) {
					tempImagepath = query.getString(query
							.getColumnIndexOrThrow("_data"));
				}
				query.close();
			} else {
				tempImagepath = galleryUri.getPath();
			}
		}

		return rotateImage(BitmapFactory.decodeFile(tempImagepath),
				tempImagepath);
	}

	public static Bitmap rotateImage(Bitmap bitmap, String filePath) {
		Bitmap resultBitmap = null;

		try {
			ExifInterface exifInterface = new ExifInterface(filePath);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, 1);
			Matrix matrix = new Matrix();
			float angle = 0;
			if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
				angle = 90;
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
				angle = 180;
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
				angle = 270;
			}
			if (angle != 0) {
				matrix.postRotate(angle);
				// Rotate the bitmap

				resultBitmap = Bitmap.createBitmap(bitmap, 0, 0,
						bitmap.getWidth(), bitmap.getHeight(), matrix, true);

				bitmap.recycle();
				bitmap = null;
			} else {
				return bitmap;
			}
		} catch (Exception exception) {
			return bitmap;
		}
		return resultBitmap;
	}

	public static int calculateInSampleSize(
			final BitmapFactory.Options bitmapFactoryOptions, final int width,
			final int height, final int attributeInt) {
		final int outHeight = bitmapFactoryOptions.outHeight;
		final int outWidth = bitmapFactoryOptions.outWidth;
		int sampleSize = 1;
		if ((outHeight > width || outWidth > height)
				&& (attributeInt == 8 || attributeInt == 6)) {
			final int round = Math.round((float) outHeight / (float) width);
			final int round2 = Math.round((float) outWidth / (float) height);
			if (round < round2) {
				sampleSize = round;
			} else {
				sampleSize = round2;
			}
		} else if (outHeight > height || outWidth > width) {
			final int round3 = Math.round((float) outHeight / (float) height);
			final int round4 = Math.round((float) outWidth / (float) width);
			if (round3 < round4) {
				sampleSize = round3;
			} else {
				sampleSize = round4;
			}
		}
		if (sampleSize > 16) {
			sampleSize = 16;
		} else {
			if (sampleSize > 8) {
				return 8;
			}
			if (sampleSize > 4) {
				return 4;
			}
			if (sampleSize > 2) {
				return 2;
			}
		}
		return sampleSize;
	}
	public static Bitmap decodeUri(String selectedImage, int width, int height)
			throws FileNotFoundException {

		File f = new File(selectedImage);
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(new FileInputStream(f), null, o);

		int angle = 0;
		try {
			ExifInterface exif = new ExifInterface(selectedImage);
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);

			if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
				angle = 90;
			else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
				angle = 180;
			else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
				angle = 270;

			Log.v("KM", "Angle =" + angle);
		} catch (IOException e) {
			// no-op
		}

		int REQUIRED_SIZE = (width < height) ? width : height;

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = o.outWidth, height_tmp = o.outHeight;
		int scale = 1;
		while (true) {
			if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
				break;
			width_tmp /= 2;
			height_tmp /= 2;
			scale *= 2;
		}
		// Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f),
				null, o2);

		if (angle != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(angle);
			Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
					bitmap.getHeight(), matrix, true);
			bitmap.recycle();
			bitmap = bmp;
		}

		return bitmap;
	}
	
	public static Bitmap getBitmapFromPath(String path) {
		InputStream is = null;
		Bitmap bitmap = null;
		try {
			is = new FileInputStream(path);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			bitmap = BitmapFactory.decodeStream(is, null, options);

			BitmapFactory.Options newImageOptions = new BitmapFactory.Options();
			if (options.outHeight > 1000 || options.outWidth > 1000) {
				newImageOptions.inSampleSize = 2;
			}

			IOUtilities.closeStream(is);
			is = new FileInputStream(path);

			bitmap = BitmapFactory.decodeStream(is, null, newImageOptions);

			return bitmap;
		} catch (FileNotFoundException e) {
			// Logger.log("StillsActivity", "msg: " + e.getMessage());
		} finally {
			IOUtilities.closeStream(is);
		}
		return null;
	}
}
