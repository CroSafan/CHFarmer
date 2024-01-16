package com.crosafan.chfarmer;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class RealTimeTestTemplateMatch {

	private static final int WIDTH = 1600;
	private static final int HEIGHT = 800;
	private static final int FPS = 12;
	private static final int BOX_SIZE = 300;
	private static final int SPEED_FACTOR = 1;
	static int match_method = Imgproc.TM_CCOEFF_NORMED;
	static double threshold = 1d; // Set your desired threshold value

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
					Mat img2 = Imgcodecs.imread("Orangefish_sml.png", Imgcodecs.IMREAD_COLOR);

					// Extract the region of interest (ROI) as a small box
					Rect roi = new Rect(boxX, boxY, BOX_SIZE, BOX_SIZE);
					Mat smallBox = new Mat(img1, roi);

					// Draw the small box on the original frame
					smallBox.copyTo(new Mat(img1, roi));

					ArrayList<Object[]> validResults = new ArrayList<Object[]>();
					for (double scale = 0.5; scale <= 1.3; scale += 0.1d) {
						for (double angle = 0.0; angle <= 360.0; angle += 45.0d) {
							Mat rotatedImg2 = rotateAndScale(img2, angle, scale);

							// Create the result matrix
							int result_cols = smallBox.cols() - rotatedImg2.cols() + 1;
							int result_rows = smallBox.rows() - rotatedImg2.rows() + 1;
							Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

							// Do the Matching and Normalize
							Imgproc.matchTemplate(smallBox, rotatedImg2, result, 3);
							Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

							MinMaxLocResult mmr = Core.minMaxLoc(result);
							Point matchLoc;
							if (mmr.maxVal >= threshold) {
								matchLoc = mmr.maxLoc;

			                    // Thresholding
			                    Mat thresholdResult = new Mat();
			                    Core.compare(result, new Scalar(threshold), thresholdResult, Core.CMP_GT);

			                    // Non-maximum Suppression
			                    Mat nonMaxResult = new Mat();
			                    Imgproc.dilate(thresholdResult, nonMaxResult, new Mat());
			                    Core.compare(thresholdResult, nonMaxResult, thresholdResult, Core.CMP_EQ);

			                    // Bounding Box Filtering
			                    List<MatOfPoint> contours = new ArrayList<>();
			                    Imgproc.findContours(thresholdResult, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

			                    for (MatOfPoint contour : contours) {
			                        Rect boundingBox = Imgproc.boundingRect(contour);

			                        // Add additional filtering based on bounding box size or aspect ratio
			                        if (boundingBox.width > rotatedImg2.width() && boundingBox.height > rotatedImg2.height()) {
			                            // Show the match or perform other actions
			                            // You can also add the matchLoc and rotatedImg2 to your validResults list
			                            // based on additional filtering criteria if needed.
			                            Object[] values = { matchLoc, rotatedImg2 };
			                            if (validResults.size() < 25) {
			                                validResults.add(values);
			                            }
			                        }
			                    }

							}
						}
					}

					for (Object[] point : validResults) {
						Imgproc.rectangle(smallBox, (Point) point[0],
								new Point(((Point) point[0]).x + ((Mat) point[1]).cols(),
										((Point) point[0]).y + ((Mat) point[1]).rows()),
								new Scalar(0, 255, 0), 2);
					}

					// Convert the frame to BufferedImage for display
					BufferedImage image = matToBufferedImage(img1);

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

	protected static Mat rotateAndScale(Mat src, double angle, double scale) {
		Point center = new Point(src.cols() / 2.0, src.rows() / 2.0);
		Mat rotMat = Imgproc.getRotationMatrix2D(center, angle, scale);
		Mat dst = new Mat();
		Imgproc.warpAffine(src, dst, rotMat, src.size());
		return dst;
	}

}
