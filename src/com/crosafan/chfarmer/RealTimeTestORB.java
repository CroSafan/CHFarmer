package com.crosafan.chfarmer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class RealTimeTestORB {

	private static final int WIDTH = 1600;
	private static final int HEIGHT = 800;
	private static final int FPS = 12;
	private static final int BOX_SIZE = 300;
	private static final int SPEED_FACTOR = 99999;
	private static int NUM_FEATURES = 10000;

	public static float scaleFactor = 1.3f;

	public static void main(String[] args) {
		System.loadLibrary("opencv_java480");

		String windowTitle = "Clicker Heroes"; // Replace with the title of the target window

		JFrame frame = new JFrame("Screen Capture Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		JPanel panel1 = new JPanel();
		panel1.setPreferredSize(new Dimension(800, HEIGHT));
		mainPanel.add(panel1);

		

		// Create another panel for additional content
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
		panel2.setPreferredSize(new Dimension(50, HEIGHT));
		mainPanel.add(panel2);

		frame.setContentPane(mainPanel);
		frame.setVisible(true);

		int boxX = 0;
		int boxY = 0;
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

					BufferedImage windowScreenshot = robot.createScreenCapture(
							new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top));

					Mat img1 = img2Mat(windowScreenshot);
					Mat img2 = Imgcodecs.imread("Orangefish.png", Imgcodecs.IMREAD_COLOR);

					// Extract the region of interest (ROI) as a small box
					Rect roi = new Rect(boxX, boxY, BOX_SIZE, BOX_SIZE);
					Mat smallBox = new Mat(img1, roi);

					// Draw the small box on the original frame
					smallBox.copyTo(new Mat(img1, roi));

					// Detect ORB keypoints and descriptors
					ORB orb = ORB.create(NUM_FEATURES, scaleFactor, 7, 12, 0, 3, ORB.HARRIS_SCORE, 12, 2);

					MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
					Mat descriptors1 = new Mat();
					MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
					Mat descriptors2 = new Mat();
					Mat grayImage1 = new Mat();
					Mat grayImage2 = new Mat();

					Imgproc.cvtColor(img1, grayImage1, Imgproc.COLOR_BGR2GRAY);
					Imgproc.cvtColor(img2, grayImage2, Imgproc.COLOR_BGR2GRAY);

//					Mat canny1 = new Mat();
//					Imgproc.GaussianBlur(grayImage1, grayImage1, new Size(5, 5), 0);
//					Imgproc.Canny(grayImage1, canny1, treshold1, treshold2);
//
//					Mat canny2 = new Mat();
////			        Imgproc.GaussianBlur(grayImage2, grayImage2, new Size(5, 5), 0);
//					Imgproc.Canny(grayImage2, canny2, treshold1, treshold2);

					orb.detectAndCompute(grayImage1, new Mat(), keypoints1, descriptors1);
					orb.detectAndCompute(grayImage2, new Mat(), keypoints2, descriptors2);

					DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

					// Knn match descriptors
					List<MatOfDMatch> knnMatches = new ArrayList<>();
					matcher.knnMatch(descriptors2, descriptors1, knnMatches, 2);

					// Filter good matches using the ratio test
					float ratioThreshold = 0.9f;
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
					Features2d.drawMatches(grayImage2, keypoints2, grayImage1, keypoints1, goodMatchesMat, imgMatches);

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

							Imgproc.rectangle(imgMatches, topLeft, bottomRight, new Scalar(0, 255, 0), 2);
//							ImageIO.write(matToBufferedImage(img1), "jpg",
//									new File("output/img_" + System.currentTimeMillis() + ".jpg"));
						}
					}

//					HighGui.imshow("ORB Knn Matches with Rectangle", imgMatches);
//
//					HighGui.waitKey();

					// Convert the frame to BufferedImage for display
					BufferedImage image = matToBufferedImage(imgMatches);

					// Display the image in the panel
					panel1.getGraphics().drawImage(image, 0, 0, WIDTH, HEIGHT, null);

					// Move the box by one pixel on the x-axis and then on the y-axis
					boxX = (boxX + 10) % (windowScreenshot.getWidth() - BOX_SIZE + 1);
					if (boxX >= windowScreenshot.getWidth() - BOX_SIZE) {
						boxY = (boxY + 50) % (windowScreenshot.getHeight() - BOX_SIZE + 51);
					}

					// Wait for a short duration to achieve the desired FPS
					try {
						Thread.sleep(1000 / (FPS * SPEED_FACTOR));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else

			{
				System.out.println("Window not found.");
			}
		}

		// Release the capture object

	}

	private static BufferedImage matToBufferedImage(Mat mat) {
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".png", mat, matOfByte);
		byte[] byteArray = matOfByte.toArray();
		BufferedImage image = null;
		try {
			image = ImageIO.read(new ByteArrayInputStream(byteArray));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image;
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
