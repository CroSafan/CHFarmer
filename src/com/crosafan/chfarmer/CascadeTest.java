package com.crosafan.chfarmer;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class CascadeTest {
	public static void main(String[] args) {
		System.loadLibrary("opencv_java480");

		String windowTitle = "Clicker Heroes"; // Replace with the title of the target window
		
		
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

				String xmlFile = "myhaar.xml";
				CascadeClassifier classifier = new CascadeClassifier(xmlFile);
				// Detecting the face in the snap
				MatOfRect fishDetection = new MatOfRect();
				classifier.detectMultiScale(img1, fishDetection);
				System.out.println(String.format("Detected %s faces", fishDetection.toArray().length));
				// Drawing boxes
				for (Rect fishRect : fishDetection.toArray()) {
					Imgproc.rectangle(img1, new Point(fishRect.x, fishRect.y),
							new Point(fishRect.x + fishRect.width, fishRect.y + fishRect.height), new Scalar(0, 0, 255),
							3);
				}

				HighGui.imshow("ORB Knn Matches with Rectangle", img1);
				HighGui.waitKey();
				img1.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Window not found.");
		}

		System.out.println("Done");
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

}
