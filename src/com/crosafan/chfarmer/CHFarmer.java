package com.crosafan.chfarmer;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class CHFarmer {

	public static void main(String[] args) {
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

				Graphics graphics = windowScreenshot.getGraphics();

//				graphics.setColor(Color.RED);
//				graphics.setFont(Font.getFont("BOLD"));
//				graphics.fillRect(windowScreenshot.getWidth() / 2, windowScreenshot.getHeight() / 2, 50, 50); // Adjust
				// the

				// Set the block size (e.g., 100x100 pixels)
				int blockSize = 100;

				// Divide the image into blocks
				BufferedImage[] blocks = divideImage(windowScreenshot, blockSize);

				int height = 100;
				int width = 100;
				int channels = 3; // Assuming RGB images

				// Load the trained model
				File locationToLoad = new File("FishClassifierModel.zip");
				MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(locationToLoad);
				for (int i = 0; i < blocks.length; i++) {

					ImageIO.write(blocks[i], "jpg",
							new File("output/block_" + i + "_" + System.currentTimeMillis() + ".jpg"));
					
					// Load a new image for classification
					BufferedImage image = blocks[i];

					// Preprocess the image
					NativeImageLoader loader = new NativeImageLoader(height, width, channels);
					INDArray input = loader.asMatrix(image);

					// Normalize pixel values
					input.divi(255.0);

					// Reshape the input array to match the model's input shape
					input = input.reshape(1, channels, height, width);

					// Perform inference
					INDArray output = model.output(input);

					// Get the predicted class (0 or 1 for binary classification)
					int predictedClass = Nd4j.argMax(output, 1).getInt(0);

					System.out.println("Predicted Class: " + predictedClass);

					if (predictedClass == 1) {
						ImageIO.write(blocks[i], "jpg",
								new File("output/block_" + i + "_" + System.currentTimeMillis() + ".jpg"));

					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Window not found.");
		}
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

}
