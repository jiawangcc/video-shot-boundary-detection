import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class Preprocess {
		
	int intensityBins[] = new int[26]; //stores 25 features of intensity for each frame
	int intensityMatrix[][] = new int[4000][26]; //stores 25 features of intensity for 4000 frames

	/*
	 * Preprocess the video to get the intensity feature matrix for future detection.
	 * Using FFmpegFrameGrabber from javacv library to grab frame 1000 - 4999 
	 * from video file 20020924_juve_dk_02a.mpg. After each frame is grabbed from the video, 
	 * compute the intensity values for that frame and store the values into intensityMatrix. 
	 */
	public Preprocess() {
		
		FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber("20020924_juve_dk_02a.mpg");
		int delay = 22;
		
		try {		
			frameGrabber.start();
			int totalFrameNum = frameGrabber.getLengthInFrames() - delay;
			int frameNum = 0;
			while (frameNum < totalFrameNum) {
				Frame f = frameGrabber.grabImage();
				frameNum = frameGrabber.getFrameNumber() - delay;
				
				if (frameNum >= 1000 && frameNum < 5000) {
					BufferedImage img = new BufferedImage(f.imageWidth, f.imageHeight, BufferedImage.TYPE_3BYTE_BGR);
					Java2DFrameConverter.copy(f, img);	
					getIntensity(img);
					for (int i = 0; i < intensityBins.length; i++) {
						intensityMatrix[frameNum-1000][i] = intensityBins[i];
					}
				}
				
				if (frameNum % 1000 == 0) {
					System.out.print(" #" + frameNum);
				}			
			}
			frameGrabber.stop();
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			
		writeIntensity();		
		System.out.println(" Intensity matrix is done.");
	}

	/*
	* compute 25 intensity bin values for each image and store the result
	* in intensityBins. 
	*/
	private void getIntensity(BufferedImage image) {
		
		intensityBins = new int[26];
        int height = image.getHeight();
		int width = image.getWidth();
		
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int pixel = image.getRGB(j, i);
				Color col = new Color(pixel);
				int r = col.getRed();
				int g = col.getGreen();
				int b = col.getBlue();
				
				//compute intensity of each pixel
				double intensity = 0.299 * r + 0.587 * g + 0.114 * b; 
				int binNum = (int) intensity / 10;

				if (binNum == 25) { // the last bin range is 240 - 255;
					binNum = 24;
				}

				intensityBins[binNum + 1]++;
			}
		}

	}


	// This method writes the contents of the intensity matrix to a file called
	// intensity.txt
	public void writeIntensity() {

		PrintWriter writer;
		try {
			writer = new PrintWriter("intensity.txt", "UTF-8");

			for (int i = 0; i < intensityMatrix.length; i++) { // 4000
				for (int j = 0; j < intensityBins.length; j++) { // 26
					writer.print(intensityMatrix[i][j] + ",");
				}
				writer.println();
			}
			writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//main method 
	public static void main(String[] args) {
		new Preprocess();
	}

}
