package com.crosafan.chfarmer;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

public class ORBTest2 {

    private static final String OPENCV_LIBRARY_NAME = "opencv_java480";
    private static final String SMALL_IMAGE_PATH = "fish_test2.png";
    private static final float RATIO_THRESHOLD = 0.6f;
    private static final int MIN_GOOD_MATCHES = 4;

    public static void main(String[] args) {
        System.loadLibrary(OPENCV_LIBRARY_NAME);

        String windowTitle = "Clicker Heroes"; // Replace with the title of the target window

        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);

        if (hwnd != null) {
            try {
                Robot robot = new Robot();
                User32.INSTANCE.SetForegroundWindow(hwnd);
                BufferedImage windowScreenshot = robot.createScreenCapture(getWindowRectangle(hwnd));

                Mat img1 = img2Mat(windowScreenshot);
                Mat img2 = Imgcodecs.imread(SMALL_IMAGE_PATH, Imgcodecs.IMREAD_COLOR);

                ORB orb = ORB.create(50000, 1.1f, 7, 12, 0, 3, ORB.HARRIS_SCORE, 12, 8);

                performORBMatching(img1, img2, orb);

                HighGui.waitKey();

//                releaseResources(img1, img2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Window not found.");
        }

        System.out.println("Done");
    }

    private static void performORBMatching(Mat img1, Mat img2, ORB orb) {
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors2 = new Mat();

        orb.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
        orb.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors2, descriptors1, knnMatches, 2);

        List<DMatch> goodMatches = filterGoodMatches(knnMatches);

        Mat imgMatches = drawMatches(img1, img2, keypoints1, keypoints2, goodMatches);

        if (goodMatches.size() >= 1) {
            drawRectangleAroundObject(img1, keypoints1, keypoints2, goodMatches);
        }

        HighGui.imshow("ORB Knn Matches with Rectangle", imgMatches);
    }

    private static List<DMatch> filterGoodMatches(List<MatOfDMatch> knnMatches) {
        List<DMatch> goodMatches = new ArrayList<>();

        for (MatOfDMatch knnMatch : knnMatches) {
            DMatch[] matchesArray = knnMatch.toArray();
            if (matchesArray.length >= 2) {
                float ratio = matchesArray[0].distance / matchesArray[1].distance;
                if (ratio < RATIO_THRESHOLD) {
                    goodMatches.add(matchesArray[0]);
                }
            }
        }

        return goodMatches;
    }

    private static Mat drawMatches(Mat img1, Mat img2, MatOfKeyPoint keypoints1, MatOfKeyPoint keypoints2, List<DMatch> goodMatches) {
        MatOfDMatch goodMatchesMat = new MatOfDMatch();
        goodMatchesMat.fromList(goodMatches);
        Mat imgMatches = new Mat();
        Features2d.drawMatches(img2, keypoints2, img1, keypoints1, goodMatchesMat, imgMatches);

        return imgMatches;
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
    private static void drawRectangleAroundObject(Mat img1, MatOfKeyPoint keypoints1, MatOfKeyPoint keypoints2, List<DMatch> goodMatches) {
        List<KeyPoint> keypointsList1 = keypoints1.toList();
        List<KeyPoint> keypointsList2 = keypoints2.toList();

        LinkedList<Point> objList = new LinkedList<>();
        LinkedList<Point> sceneList = new LinkedList<>();

        for (DMatch match : goodMatches) {
            objList.addLast(keypointsList2.get(match.queryIdx).pt);
            sceneList.addLast(keypointsList1.get(match.trainIdx).pt);
        }

        MatOfPoint2f obj = new MatOfPoint2f(objList.toArray(new Point[0]));
        MatOfPoint2f scene = new MatOfPoint2f(sceneList.toArray(new Point[0]));

        Mat H = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 5);

        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2);
        Mat sceneCorners = new Mat(4, 1, CvType.CV_32FC2);

        objCorners.put(0, 0, 0, 0);
        objCorners.put(1, 0, img1.cols(), 0);
        objCorners.put(2, 0, img1.cols(), img1.rows());
        objCorners.put(3, 0, 0, img1.rows());

        Core.perspectiveTransform(objCorners, sceneCorners, H);

        if (sceneCorners != null && sceneCorners.cols() == 1 && sceneCorners.rows() == 4) {
            Point topLeft = new Point(sceneCorners.get(0, 0)[0], sceneCorners.get(0, 0)[1]);
            Point bottomRight = new Point(sceneCorners.get(3, 0)[0], sceneCorners.get(3, 0)[1]);

            Imgproc.rectangle(img1, topLeft, bottomRight, new Scalar(0, 0, 255), 5);
        }
    }
    
    private static Rectangle getWindowRectangle(WinDef.HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        int left = rect.left;
        int top = rect.top;
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        return new Rectangle(left, top, width, height);
    }
}