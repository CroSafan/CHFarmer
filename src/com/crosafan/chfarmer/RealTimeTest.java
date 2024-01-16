package com.crosafan.chfarmer;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class RealTimeTest {

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int FPS = 120;
	private static final int BOX_SIZE = 500;
	private static final int SPEED_FACTOR = 99999;

	public static void main(String[] args) {
		System.loadLibrary("opencv_java480");

		String windowTitle = "Clicker Heroes"; // Replace with the title of the target window

		JFrame frame = new JFrame("Screen Capture Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);

		// Create a panel for displaying images
		JPanel panel = new JPanel();
		frame.setContentPane(panel);
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

					// Extract the region of interest (ROI) as a small box
					Rect roi = new Rect(boxX, boxY, BOX_SIZE, BOX_SIZE);
					Mat smallBox = new Mat(img1, roi);

//			Imgproc.resize(smallBox, smallBox, new Size(BOX_SIZE, BOX_SIZE));

					// Draw the small box on the original frame
					smallBox.copyTo(new Mat(img1, roi));
					// Convert the frame to BufferedImage for display
					BufferedImage image = matToBufferedImage(smallBox);

					// Display the image in the panel
					panel.getGraphics().drawImage(image, 0, 0, WIDTH, HEIGHT, null);

					// Move the box by one pixel on the x-axis and then on the y-axis
					boxX = (boxX + 10) % (windowScreenshot.getWidth() - BOX_SIZE + 1);
					if (boxX >= windowScreenshot.getWidth()-BOX_SIZE) {
						boxY = (boxY + 50) % (windowScreenshot.getHeight()- BOX_SIZE +51);
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
