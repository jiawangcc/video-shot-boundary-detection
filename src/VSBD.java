import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;


public class VSBD extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private JLabel selectedLabel = new JLabel(); // container to hold the selected video
	private JButton[] button; // creates an array of JButtons
	private int[] buttonOrder = new int[101]; // creates an array to keep up with the image order
	private JPanel mainPanel; //panel for entire interface
	private JPanel topPanel;  //panel to hold query image on the left and the buttons on the right
	private JPanel bottomPanel; //panel to hold picRow panel and boxRow panel
	private JPanel buttonPanel;
	
	private int[][] intensityMatrix = new int[4000][26];//store image size and 25 intensity features of 4000 images.
	private int[] sd = new int[3999];
	int videoNo = 0;      //keeps up with the query image
	int videoCount = 1; // keeps up with the number of images displayed

	//main method
	public static void main(String args[]) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				VSBD app = new VSBD();
				app.setVisible(true);
			}
		});
	}

	//constructor to set up UI
	public VSBD() {		
		JFrame frame = new JFrame("VDBD: Video Shot Boundary Detection");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.setMaximumSize(new Dimension(700, 250));

		selectedLabel = new JLabel();
		selectedLabel.setLayout(new GridLayout(1, 1));
		selectedLabel.setMaximumSize(new Dimension(340, 260));
		selectedLabel.setHorizontalAlignment(JLabel.CENTER);
		selectedLabel.setBorder(BorderFactory.createTitledBorder("Video Preview"));
				
		JPanel middlePanel = new JPanel();
		middlePanel.setLayout(new GridLayout(1, 1));
		middlePanel.setMaximumSize(new Dimension(700, 30));
		
		bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridLayout(5,6));
		bottomPanel.setMaximumSize(new Dimension(700, 540));
		
		topPanel.add(Box.createRigidArea(new Dimension(180, 0)));
		topPanel.add(selectedLabel);
		mainPanel.add(topPanel);
		middlePanel.add(new JLabel("Detection results for video 20020924_juve_dk_02a.mpg (1000 - 4999)"));
		mainPanel.add(middlePanel);
		mainPanel.add(bottomPanel);
			
		frame.setContentPane(mainPanel);
		frame.setSize(720, 800);
		frame.setMinimumSize(new Dimension(720, 800));
		frame.setVisible(true);
		
		displayFirstPage();
	}


	
	/*
	 * This class implements an ActionListener for each iconButton. When an icon
	 * button is clicked, the video on the the button is added to the
	 * selectedLabel and the picNo is set to the image number selected and
	 * being displayed.
	 */
	private class IconButtonHandler implements ActionListener {
		private int firstFrame;
		private int endFrame;

		IconButtonHandler(int firstFrame, int endFrame) {
			this.firstFrame = firstFrame;
			this.endFrame = endFrame;
		}

		public void actionPerformed(ActionEvent e) {
//			selectedLabel.setIcon(iconUsed);	
		}

	}

	/*
	 * This method displays the first twenty images in the bottomPanel. The for
	 * loop starts at number one and gets the image number stored in the
	 * buttonOrder array and assigns the value to imageButNo. The picRow
	 * associated with five image and boxRow associated with five checkboxes 
	 * are then added to bottomPanel. The for loop continues this process 
	 * until twenty images  and twenty checkboxes are displayed in the bottomPanel
	 */
	private void displayFirstPage() {
		
		TwinComparison tc = new TwinComparison();
		
		List<Integer> Ce = tc.Ce;
		List<Integer> Fs = tc.Fs;
		List<DetectionResult> results = new ArrayList<>(Ce.size()+Fs.size());
		
		for(Integer i: Ce){
			results.add(new DetectionResult(i, FrameType.CUT));
		}
		for(Integer i: Fs){
			//for each Fs, plus 1 to get the first frame
			results.add(new DetectionResult(i + 1, FrameType.GRADUAL_TRANSITION)); 
		}
		Collections.sort(results);
		
		BufferedImage[] imgRes = new BufferedImage[results.size()];
		
		FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber("./video/20020924_juve_dk_02a.mpg");
		int frameNum = 0;
		int delay = 22;
		int n = 0;
		try {			
			frameGrabber.start();
			int totalFrameNum = frameGrabber.getLengthInFrames();
			int width = frameGrabber.getImageWidth();
			int height = frameGrabber.getImageHeight();
			
			while (frameNum < totalFrameNum - delay) {
				Frame f = frameGrabber.grabImage();
				frameNum = frameGrabber.getFrameNumber() - delay;
				
				for( DetectionResult r : results){
					if (frameNum == r.getStartFrame()) {	
						imgRes[n] = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
						Java2DFrameConverter.copy(f, imgRes[n]);
						n++;
					}
				}			
			}
			frameGrabber.stop();
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		button = new JButton[imgRes.length];
		for( int i = 0; i < imgRes.length; i++){
			ImageIcon icon = new ImageIcon(imgRes[i].getScaledInstance(-1, 80, Image.SCALE_SMOOTH));
			button[i] = new JButton(results.get(i).toString(), icon);
			button[i].setVerticalTextPosition(SwingConstants.BOTTOM);
			button[i].setHorizontalTextPosition(SwingConstants.CENTER);
			if( i == imgRes.length - 1 ){
				button[i].addActionListener(new IconButtonHandler(results.get(i).getStartFrame(), 4999));
			}
			else{
				button[i].addActionListener(new IconButtonHandler(results.get(i).getStartFrame(), results.get(i+1).getStartFrame() -1 ));
			}
		}
		
		for (int i = 0; i < imgRes.length; i++){
			bottomPanel.add(button[i]);
			
		}
		
		bottomPanel.revalidate();
		bottomPanel.repaint();
	}
	
	private class DetectionResult implements Comparable<DetectionResult>{
		private final int startFrame;
		private final FrameType type;
		
		public DetectionResult(int startFrame, FrameType type) {
			this.startFrame = startFrame;
			this.type = type;
		}

		public int getStartFrame() {
			return startFrame;
		}

		public FrameType getType() {
			return type;
		}
		
		@Override
		public String toString(){
			if(this.getType() == FrameType.CUT){
				return "Ce = " + this.getStartFrame();
			}
			if(this.getType() == FrameType.GRADUAL_TRANSITION){
				return "Fs+1 = " + this.getStartFrame();
			}
			throw new IllegalStateException("Unexpected FrameType");
		}

		@Override
		public int compareTo(DetectionResult o) {
			return this.getStartFrame() - o.getStartFrame();
		}
		
	}
	
	private enum FrameType{
		CUT, GRADUAL_TRANSITION;
	}

	/*
	 * This class implements an ActionListener when the user selects the
	 * intensityHandler button. The image number that the user would like to
	 * find similar images for is stored in the variable picNo. The size of
	 * the image is retrieved from the intensityMatrix[*][0].In the computeDistance
	 * method selected image's intensity bin values are compared to all the other
	 * image's intensity bin values and a score is determined for how well the
	 * images compare. The images are then arranged from most similar to the least.
	 */
	private class intensityHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
			if( videoNo == 0){
				return;
			}
			List<Integer> result = computeDistance(videoNo);
			
			// Shift to right by one position
			result.add(0, -1);
			
			for (int i = 1; i < 101; i++) {
				buttonOrder[i] = result.get(i);		
			}
			videoCount = 1;
//			pageDisplay.setText("Page 1 of 5");
//			displayFirstPage();			
		}
		
		public List<Integer> computeDistance( int selectPicNum){
			
			int selectPicSize = intensityMatrix[selectPicNum - 1][0];
			List<PicDistance> arr = new ArrayList<>();
			
			for (int i = 0; i < 100; i++) {
				int currentPicSize = intensityMatrix[i][0];
				double d = 0;
				for (int j = 1; j < 26; j++){
					d = d + Math.abs(((double)intensityMatrix[selectPicNum-1][j] / (double)selectPicSize) - ((double)intensityMatrix[i][j] / (double)currentPicSize));
				}
				arr.add(new PicDistance(i,d));
			}
			
			Collections.sort(arr);
			
			return arr.stream().map(pd -> pd.imgNo + 1).collect(Collectors.toList());
		}
	}

	/*
	 * This class implements the Comparable interface.This class facilitates Collections.sort()
	 * to achieve the sorting function.Each PicDistance instance has a imgNo variable for image 
	 * number and distance variable for the corresponding distance from query image. The 
	 * compareTo method compares distance and return the smaller distance and associated with 
	 * the image number.
	 */
	
	private class PicDistance implements Comparable<PicDistance> {
		
		private final int imgNo;
		private final double distance;
		
		public PicDistance(int imgNo, double distance) {
			this.imgNo = imgNo;
			this.distance = distance;
		}

		@Override
		public int compareTo(PicDistance o) {
			if (this.distance == o.distance) {
				return 0;
			} else {
				return (this.distance < o.distance) ? -1 : 1;
			}
		}
	}
}
	
	