package com.example.radha.bitmapprocessing;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

public final class IOUtilities {
	private static final String TAG = "IOUtilities";
	
	public static final int IO_BUFFER_SIZE = 8 * 1024;
	
	public static File getExternalFile(String file) {
		return new File(Environment.getExternalStorageDirectory(), file);
	}
	
	 /**
     * Copy the content of the input stream into the output stream, using a temporary
     * byte array buffer whose size is defined by {@link #IO_BUFFER_SIZE}.
     *
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     *
     * @throws java.io.IOException If any error occurs during the copy.
     */
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read()) != -1) {
			out.write(b, 0, read);
		}
	}
	
	/**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
	public static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				Log.e(TAG, "Could not close stream", e);
			}
		}
	}
	
	public static String streamToString(InputStream is, Charset cs) {
		BufferedReader br;
		if(cs == null) {
			br = new BufferedReader(new InputStreamReader(is));
		} else {
			br = new BufferedReader(new InputStreamReader(is, cs));
		}

		StringBuffer sb = new StringBuffer();
		try {
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}

		} catch (Exception ex) {
			ex.getMessage();
		} finally {
			try {
				is.close();
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}

	public static String streamToString(InputStream is) {
		return streamToString(is, null);
	}

	public static byte[] streamToByteArray(InputStream is){
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		try {
			while ((nRead = is.read(data, 0, data.length)) != -1) {
			  buffer.write(data, 0, nRead);
			}
			buffer.flush();
			return buffer.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
