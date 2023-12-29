package com.crosafan.chfarmer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.FlipImageTransform;
import org.datavec.image.transform.ImageTransform;
import org.datavec.image.transform.WarpImageTransform;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

//https://mgubaidullin.github.io/deeplearning4j-docs/programmingguide/04_convnet
public class GoldFishClassifier {
	protected static int numLabels = 2;// Binary classification (fish or non-fish)

	protected static int batchSize = 50;
	protected static int numExamples = 100;

	protected static int seed = 12312;
	protected static int iterations = 5;

	protected static Random rng = new Random(seed);

	protected static double splitTrainTest = 0.8;

	protected static int height = 100;
	protected static int width = 100;
	protected static int channels = 3;

	protected static int epochs = 50;

	public static void main(String[] args) throws IOException {

		ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();

		File mainPath = new File(System.getProperty("user.dir"), "bin/data");
		FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
		BalancedPathFilter pathFilter = new BalancedPathFilter(rng, labelMaker, numExamples, numLabels, batchSize);

		InputSplit[] inputSplit = fileSplit.sample(pathFilter, splitTrainTest, 1 - splitTrainTest);
		InputSplit trainData = inputSplit[0];
		InputSplit testData = inputSplit[1];

		ImageTransform flipTransform1 = new FlipImageTransform(rng);
		ImageTransform flipTransform2 = new FlipImageTransform(new Random(123));
		ImageTransform warpTransform = new WarpImageTransform(rng, 42);
		List<ImageTransform> transforms = Arrays
				.asList(new ImageTransform[] { flipTransform1, warpTransform, flipTransform2 });

		ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
		DataSetIterator dataIter;

		recordReader.initialize(trainData);
		dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);

		DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
		scaler.fit(dataIter);
		dataIter.setPreProcessor(scaler);

		MultipleEpochsIterator trainIter = new MultipleEpochsIterator(epochs, dataIter);

		for (ImageTransform transform : transforms) {
			recordReader.initialize(trainData, transform);
			// above code with DataSetIterator and etc.
		}

		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).iterations(iterations)
				.regularization(false).l2(0.005).activation(Activation.RELU).learningRate(0.0001)
				.weightInit(WeightInit.XAVIER).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.updater(new Nesterovs(0.9)).list()
				.layer(0, convInit("cnn1", channels, 50, new int[] { 5, 5 }, new int[] { 1, 1 }, new int[] { 0, 0 }, 0))
				.layer(1, maxPool("maxpool1", new int[] { 2, 2 }))
				.layer(2, conv5x5("cnn2", 100, new int[] { 5, 5 }, new int[] { 1, 1 }, 0))
				.layer(3, maxPool("maxool2", new int[] { 2, 2 })).layer(4, new DenseLayer.Builder().nOut(500).build())
				.layer(5,
						new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nOut(numLabels)
								.activation(Activation.SOFTMAX).build())
				.backprop(true).pretrain(false).setInputType(InputType.convolutional(height, width, channels)).build();

		MultiLayerNetwork network = new MultiLayerNetwork(conf);
		network.fit(trainIter);

		File locationToSave = new File("FishClassifierModel.zip");
		ModelSerializer.writeModel(network, locationToSave, false);

		Evaluation eval = network.evaluate(dataIter);
		System.out.println(eval.stats(true));

	}

	private static ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad,
			double bias) {
		return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
	}

	private static ConvolutionLayer conv5x5(String name, int out, int[] stride, int[] pad, double bias) {
		return new ConvolutionLayer.Builder(new int[] { 5, 5 }, stride, pad).name(name).nOut(out).biasInit(bias)
				.build();
	}

	private static SubsamplingLayer maxPool(String name, int[] kernel) {
		return new SubsamplingLayer.Builder(kernel, new int[] { 2, 2 }).name(name).build();
	}

	public static MultiLayerConfiguration buildModel(int height, int width, int channels, int numClasses) {
		return new NeuralNetConfiguration.Builder().seed(123)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(new Adam())
				.weightInit(WeightInit.XAVIER).list()
				.layer(0,
						new ConvolutionLayer.Builder().kernelSize(3, 3).stride(1, 1).nIn(channels).nOut(32)
								.activation(Activation.RELU).build())
				.layer(1, new DenseLayer.Builder().nOut(128).activation(Activation.RELU).build())
				.layer(2,
						new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nOut(numClasses)
								.activation(Activation.SOFTMAX).build())
				.setInputType(org.deeplearning4j.nn.conf.inputs.InputType.convolutionalFlat(height, width, channels))
				.build();

	}

}
