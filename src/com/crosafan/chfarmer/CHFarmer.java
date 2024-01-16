package com.crosafan.chfarmer;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class CHFarmer {

	static double threshold = 1d; // Set your desired threshold value

	public static void main(String[] args) {
		System.loadLibrary("opencv_java480");
		Mat img1 = null;

		String windowTitle = "Clicker Heroes"; // Replace with the title of the target window

		// Find the window by title
		WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
		ArrayList<ValueHold> values = new ArrayList<CHFarmer.ValueHold>();
		if (hwnd != null) {
			try {
				for (int i = 0; i < 5; i++) {
					// Get the bounding box of the window
					WinDef.RECT rect = new WinDef.RECT();
					User32.INSTANCE.GetWindowRect(hwnd, rect);

					// Create a Robot object
					Robot robot = new Robot();
					User32.INSTANCE.SetForegroundWindow(hwnd);

					// Capture the specified window's content
					BufferedImage windowScreenshot = robot.createScreenCapture(
							new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top));
//					Mat orig = img2Mat(windowScreenshot);

					img1 = img2Mat(windowScreenshot);
					Mat img2 = Imgcodecs.imread("Orangefish_sml.png");

//				Imgproc.Canny(img1, img1, 300, 600, 5, true); 
//				Imgproc.Canny(img2, img2, 300, 600, 5, true); 

					// Loop over different scales and rotations
					for (double scale = 0.1; scale <= 2.0; scale += 0.1d) {
						for (double angle = 0.0; angle <= 360.0; angle += 45.0d) {
							Mat rotatedImg2 = rotateAndScale(img2, angle, scale);

							// Create the result matrix
							int result_cols = img1.cols() - rotatedImg2.cols() + 1;
							int result_rows = img1.rows() - rotatedImg2.rows() + 1;
							Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

							// Do the Matching and Normalize
							Imgproc.matchTemplate(img1, rotatedImg2, result, 3);
							Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

							MinMaxLocResult mmr = Core.minMaxLoc(result);
							Point matchLoc;
							if (mmr.maxVal >= threshold) {
								matchLoc = mmr.maxLoc;

								ValueHold hold = new ValueHold();
								hold.orig = img1;
								hold.matchLoc = matchLoc;
								hold.drawLoc = new Point(matchLoc.x + rotatedImg2.cols(),
										matchLoc.y + rotatedImg2.rows());

								values.add(hold);

								// Show the match
//								Imgproc.rectangle(orig, matchLoc,
//										new Point(matchLoc.x + rotatedImg2.cols(), matchLoc.y + rotatedImg2.rows()),
//										new Scalar(0, 255, 0), 2);

							}
						}
					}

				}

				for (int i = 0; i < values.size(); i++) {
					if (i % 2 == 0) {
						Imgproc.rectangle(img1, values.get(i).matchLoc, values.get(i).drawLoc,
								new Scalar(255, 255, 0), 2);
					} else if (i % 3 == 0) {
						Imgproc.rectangle(img1, values.get(i).matchLoc, values.get(i).drawLoc,
								new Scalar(0, 255, 0), 2);
					} else if (i % 5 == 0) {
						Imgproc.rectangle(img1, values.get(i).matchLoc, values.get(i).drawLoc,
								new Scalar(0, 0,255), 2);
					}
				}

				HighGui.imshow("Match", img1);
				HighGui.waitKey();
				HighGui.destroyAllWindows();

				Thread.sleep(1000);

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else

		{
			System.out.println("Window not found.");
		}

		System.out.println("Done");
		System.exit(0);
	}

	public static class ValueHold {
		public Mat orig;
		public Point matchLoc;
		public Point drawLoc;

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

	protected static Mat rotateAndScale(Mat src, double angle, double scale) {
		Point center = new Point(src.cols() / 2.0, src.rows() / 2.0);
		Mat rotMat = Imgproc.getRotationMatrix2D(center, angle, scale);
		Mat dst = new Mat();
		Imgproc.warpAffine(src, dst, rotMat, src.size());
		return dst;
	}

	private static Mat convertToGrayScale(Mat colorImage) {
		// Create a Mat object to store the grayscale image
		Mat grayImage = new Mat();

		// Convert the color image to grayscale using Imgproc.cvtColor
		Imgproc.cvtColor(colorImage, grayImage, Imgproc.COLOR_BGR2GRAY);

		return grayImage;
	}

}
