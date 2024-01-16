package com.crosafan.chfarmer;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import javax.imageio.ImageIO;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class TakeScreenshots {

	public static void main(String[] args) throws InterruptedException {
		System.loadLibrary("opencv_java480");

		String windowTitle = "Clicker Heroes"; // Replace with the title of the target window

		while (true) {

			// Find the window by title
			WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);

			if (hwnd != null) {
				try {
					// Get the bounding box of the window
					WinDef.RECT rect = new WinDef.RECT();
					User32.INSTANCE.GetWindowRect(hwnd, rect);

					// Create a Robot object
					Robot robot = new Robot();
					User32.INSTANCE.SetForegroundWindow(hwnd);
					// Capture the specified window's content
					BufferedImage windowScreenshot = robot.createScreenCapture(
							new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top));

					Mat img1 = img2Mat(windowScreenshot);

					ImageIO.write(matToBufferedImage(img1), "bmp",
							new File("output/img_" + System.currentTimeMillis() + ".bmp"));

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Window not found.");
			}
			Thread.sleep(30000);
		}

	}

	protected static Mat img2Mat(BufferedImage in) {
		Mat out;
		byte[] data;
		int r, g, b;

		if (in.getType() == BufferedImage.TYPE_INT_RGB) {
			out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
			data = new byte[in.getWidth() * in.getHeight() * (int) out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
			for (int i = 0; i < dataBuff.length; i++) {
				data[i * 3] = (byte) ((dataBuff[i] >> 0) & 0xFF);
				data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
				data[i * 3 + 2] = (byte) ((dataBuff[i] >> 16) & 0xFF);
			}
		} else {
			out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC1);
			data = new byte[in.getWidth() * in.getHeight() * (int) out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
			for (int i = 0; i < dataBuff.length; i++) {
				r = (byte) ((dataBuff[i] >> 0) & 0xFF);
				g = (byte) ((dataBuff[i] >> 8) & 0xFF);
				b = (byte) ((dataBuff[i] >> 16) & 0xFF);
				data[i] = (byte) ((0.21 * r) + (0.71 * g) + (0.07 * b));
			}
		}
		out.put(0, 0, data);
		return out;
	}

	public static BufferedImage matToBufferedImage(Mat matrix) {
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;

		matrix.get(0, 0, data);

		switch (matrix.channels()) {
		case 1:
			type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case 3:
			type = BufferedImage.TYPE_3BYTE_BGR;
			// bgr to rgb
			byte b;
			for (int i = 0; i < data.length; i = i + 3) {
				b = data[i];
				data[i] = data[i + 2];
				data[i + 2] = b;
			}
			break;
		default:
			return null;
		}

		BufferedImage image = new BufferedImage(cols, rows, type);
		WritableRaster raster = image.getRaster();
		raster.setDataElements(0, 0, cols, rows, data);

		return image;
	}

}
