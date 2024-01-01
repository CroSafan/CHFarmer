package com.crosafan.chfarmer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class CHFarmer {

	public static void main(String[] args) {
		System.loadLibrary("opencv_java480");

		String windowTitle = "Clicker Heroes"; // Replace with the title of the target window

		int match_method = Imgproc.TM_CCOEFF_NORMED;
		double threshold = 0.9;

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
				Mat img2 = Imgcodecs.imread("Orangefish.png", Imgcodecs.IMREAD_COLOR);

				 // Create the result matrix
                int result_cols = img1.cols() - img2.cols() + 1;
                int result_rows = img1.rows() - img2.rows() + 1;
                Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

                // Iterate over different scales
                for (double scale = 0.5; scale <= 2.0; scale += 0.1) {
                    Mat scaledImg2 = new Mat();
                    Imgproc.resize(img2, scaledImg2, new Size(), scale, scale);

                    // Iterate over different rotations
                    for (double angle = 0; angle < 360; angle += 10) {
                        Mat rotatedScaledImg2 = new Mat();
                        Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(scaledImg2.cols() / 2, scaledImg2.rows() / 2), angle, 1);
                        Imgproc.warpAffine(scaledImg2, rotatedScaledImg2, rotationMatrix, scaledImg2.size());

                        // Do the Matching and Normalize
                        Imgproc.matchTemplate(img1, rotatedScaledImg2, result, match_method);
                        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

                        // Localizing the best match with minMaxLoc
                        MinMaxLocResult mmr = Core.minMaxLoc(result);

                        Point matchLoc;
                        if (match_method == Imgproc.TM_SQDIFF
                                || match_method == Imgproc.TM_SQDIFF_NORMED) {
                            matchLoc = mmr.minLoc;
                        } else {
                            matchLoc = mmr.maxLoc;
                        }

                        // Show matches above the threshold
                        if (mmr.maxVal > threshold) {
                            Imgproc.rectangle(img1, matchLoc,
                                    new Point(matchLoc.x + rotatedScaledImg2.cols(), matchLoc.y + rotatedScaledImg2.rows()),
                                    new Scalar(255, 0, 0));
                        }
                    }
                }

                // Display the result
                HighGui.imshow("Template matching result", img1);

                HighGui.waitKey();

//				 Release resources

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
