package com.crosafan.chfarmer;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class ORBTest {

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
				Mat img2 = Imgcodecs.imread("Orangefish.png", Imgcodecs.IMREAD_COLOR);

                ORB orb = ORB.create(50000, 1.1f, 7, 12, 0, 3, ORB.HARRIS_SCORE, 12, 8);

				// Detect keypoints and compute descriptors
				MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
				Mat descriptors1 = new Mat();
				MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
				Mat descriptors2 = new Mat();

				orb.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
				orb.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);

				// Create a Brute-Force descriptor matcher
				DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

				// Knn match descriptors
				List<MatOfDMatch> knnMatches = new ArrayList<>();
				matcher.knnMatch(descriptors2, descriptors1, knnMatches, 2);

				// Filter good matches using the ratio test
				float ratioThreshold = 0.5f;
				List<DMatch> goodMatches = new ArrayList<>();

				for (MatOfDMatch knnMatch : knnMatches) {
					DMatch[] matchesArray = knnMatch.toArray();
					if (matchesArray.length >= 2) {
						float ratio = matchesArray[0].distance / matchesArray[1].distance;
						if (ratio < ratioThreshold) {
							goodMatches.add(matchesArray[0]);
						}
					}
				}

				// Draw matches on a new image
				MatOfDMatch goodMatchesMat = new MatOfDMatch();
				goodMatchesMat.fromList(goodMatches);
				Mat imgMatches = new Mat();
				Features2d.drawMatches(img2, keypoints2, img1, keypoints1, goodMatchesMat, imgMatches);

				// Draw a rectangle around the detected object on img1
				if (goodMatches.size() >= 10) {
					// Get matching keypoints
					List<KeyPoint> keypointsList1 = keypoints1.toList();
					List<KeyPoint> keypointsList2 = keypoints2.toList();

					// Extract matching points
					LinkedList<Point> objList = new LinkedList<>();
					LinkedList<Point> sceneList = new LinkedList<>();

					for (DMatch match : goodMatches) {
						objList.addLast(keypointsList2.get(match.queryIdx).pt);
						sceneList.addLast(keypointsList1.get(match.trainIdx).pt);
					}

					MatOfPoint2f obj = new MatOfPoint2f(objList.toArray(new Point[0]));
					MatOfPoint2f scene = new MatOfPoint2f(sceneList.toArray(new Point[0]));

					// Find the homography matrix

					Mat H = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 5);

					// Get the bounding box of the object (img2)
					Mat objCorners = new Mat(4, 1, CvType.CV_32FC2);
					Mat sceneCorners = new Mat(4, 1, CvType.CV_32FC2);

					objCorners.put(0, 0, 0, 0);
					objCorners.put(1, 0, img2.cols(), 0);
					objCorners.put(2, 0, img2.cols(), img2.rows());
					objCorners.put(3, 0, 0, img2.rows());

					// Transform object corners to scene coordinates
					Core.perspectiveTransform(objCorners, sceneCorners, H);

					// Draw a rectangle on img1
					if (sceneCorners != null && sceneCorners.cols() == 1 && sceneCorners.rows() == 4) {
						Point topLeft = new Point(sceneCorners.get(0, 0)[0], sceneCorners.get(0, 0)[1]);
						Point bottomRight = new Point(sceneCorners.get(3, 0)[0], sceneCorners.get(3, 0)[1]);

						Imgproc.rectangle(img1, topLeft, bottomRight, new Scalar(0, 255, 0), 2);
						ImageIO.write(matToBufferedImage(img1), "jpg",
								new File("output/img_" + System.currentTimeMillis() + ".jpg"));
					}
				}

				// Display the result
				HighGui.imshow("ORB Knn Matches with Rectangle", imgMatches);

				HighGui.waitKey();

//				 Release resources

				keypoints1.release();
				descriptors1.release();
				keypoints2.release();
				descriptors2.release();
				imgMatches.release();
				img1.release();
				img2.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Window not found.");
		}

		System.out.println("Done");
	}

	private static BufferedImage[] divideImage(BufferedImage originalImage, int blockSize) {
		int rows = originalImage.getHeight() / blockSize;
		int cols = originalImage.getWidth() / blockSize;

		BufferedImage[] blocks = new BufferedImage[rows * cols];

		int blockIndex = 0;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				int x = col * blockSize;
				int y = row * blockSize;

				BufferedImage block = originalImage.getSubimage(x, y, blockSize, blockSize);
				blocks[blockIndex++] = block;
			}
		}

		return blocks;
	}

	public boolean isFish() {
		return false;
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
