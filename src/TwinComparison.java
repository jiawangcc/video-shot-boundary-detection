import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TwinComparison {
	
	private int[][] intensityMatrix = new int[4000][26]; //stores 25 features of intensity for 4000 frames
	private int[] SD = new int[3999]; //stores 3999 frame-to-frame difference of 4000 frames
	private double Tb;    //Tb threshold
	private double Ts;    //Ts threshold
	private final int Tor = 2;  //Tor threshold
	
	public ArrayList<Integer> Ce = new ArrayList<>(); //stores all Ce results
	public ArrayList<Integer> Fs = new ArrayList<>(); //stores all Fs results
	
	/* This method works as the TwinComparison algorithm.
	 * Step1: call readIntensityFile() to read intensity.txt file to get the intensityMatrix
	 * Step2: call getSD() to compute SD[] array (frame to frame difference)
	 * Step3: call setThresholds() to compute Tb and Ts thresholds
	 * Step4: call detectShots() to loop through SD[] to detect which frames are shot boundary
	 */
	public TwinComparison(){
		
		readIntensityFile();
		getSD();
		for(int i = 0; i < SD.length; i++){
			System.out.println("SD["+ i+ "] = "+ SD[i]);
		}
		setThresholds();
		System.out.println("Tb = " + Tb);
		System.out.println("Ts = " + Ts);
		detectGradualShots();
		detectCutShots();		
		
		for( int i = 0; i < Ce.size(); i++){
			System.out.println("Ce - "+ Ce.get(i));
		}
		
		for( int i = 0; i < Fs.size(); i++){
			System.out.println("Fs + 1 - "+ (Fs.get(i) + 1));
		}
	}
	
	/*
	 * This method opens the intensity.txt file containing the intensity matrix
	 * with the histogram bin values for each frame. The contents of the matrix
	 * are stored in intensityMatrix.
	 */
	public void readIntensityFile() {
		
		Scanner read;
		String line = "";
		int lineNumber = 0;
		try {
			read = new Scanner(new File("intensity.txt"));
			while (read.hasNextLine()) {
	            line = read.nextLine();
	            String[] s = line.split(",");
	            for (int i = 0; i < s.length; i++){
	            	intensityMatrix[lineNumber][i] = Integer.parseInt(s[i]);
	            }            	
	            lineNumber++;
			}
			
		} catch (FileNotFoundException EE) {
			System.err.println("The file intensity.txt does not exist");
		}

	}
	
	/*
	 * This method computes the frame to frame difference in terms of intensity.
	 * Intensity feature are from intensityMatrix[][1] to intensityMatrix[][25].
	 * The 3999 differences of 4000 frames are stored in SD[].
	 */
	public void getSD(){
		
		for( int i = 0; i < SD.length; i++){
			for(int j = 1; j < 26; j++){
				SD[i] += Math.abs(intensityMatrix[i][j] - intensityMatrix[i+1][j]);
			}		
		}
	}
	
	/*
	 * This method computes the Tb threshold for cut and Ts threshold for gradual transition. 
	 * Tb = mean(SD) + std(SD)*11
	 * Ts = mean(SD)*2
	 */
	public void setThresholds(){
		
		//compute mean
		double sum = 0.0d;
		for( int i = 0; i < SD.length; i++){
			sum += SD[i];
		}
		double mean = sum / (double)SD.length;		
		
		//compute standard deviation
		sum = 0.0d;
		for( int i = 0; i < SD.length; i++){
			sum += Math.pow((double)SD[i] - mean, 2);
		}
		double std = Math.sqrt(sum / (SD.length - 1.0d));
		
		Tb = mean + std * 11.0d;
		Ts = mean * 2.0d;		
	}
	
	
	public void detectShots(){
		
		int Fs_candi = 0;
		int Fe_candi = 0;
		for( int i = 0; i < SD.length; i++){
			if( SD[i] >= Tb){
				Ce.add(i + 1 + 1000);
			}
			else if( SD[i] < Tb && SD[i] >= Ts){
				Fs_candi = i;
				while( SD[i] < Tb && SD[i] >= Ts){
					i++;
					if( i == SD.length ){
						Fe_candi = i - 1;
						break;
					}
					else if( SD[i] < Ts && SD[i+1] < Ts ){
						Fe_candi = i - 1;
						break;
					}
					else if( SD[i] < Ts && SD[i+1] >= Ts ){
						i++;
					}
				}
				if( i < SD.length && SD[i] >= Tb){
					Fe_candi = i - 1;
					Ce.add(i + 1 + 1000);
				}
				int sum = 0;
				for( int j = Fs_candi; j <= Fe_candi; j++){
					sum += SD[j];
				}
				if(sum >= Tb){
					Fs.add(Fs_candi + 1000);
				}			
			}
		}
	}
	
	private void detectGradualShots() {
		
		int Fs_candi = 0;
		int Fe_candi = 0;
		int i = 0;
		boolean newSearch = true;
		while (i < SD.length) {
			if (newSearch && Ts <= SD[i] && SD[i] < Tb) {
				Fs_candi = i;
				newSearch = false;
			}
			
			if (!newSearch) {
				int lookahead = 1;
				boolean foundEnd = true;
				while ((i+lookahead < SD.length) && (lookahead <= Tor)) {
					if (SD[i+lookahead] >= Tb) {
						break;
					}
					if (!(SD[i+lookahead] < Ts)) {
						foundEnd = false;
						break;
					}
					lookahead++;
				}
				if (foundEnd) {
					Fe_candi = (lookahead > Tor) ? i : i+lookahead-1;
					
					int sum = 0;
					for (int j = Fs_candi; j <= Fe_candi; j++) {
						sum += SD[j];
					}
					if (sum >= Tb) {
						Fs.add(Fs_candi + 1000);
					}
					newSearch = true;
				}
			}
			i++;
		}
		
	}

	/*
	 * This method is for detect cut boundary. Loop through SD[], if SD[i] is greater than Tb,
	 * it will be considered as a cut and i + 1 will be sorted as Ce.
	 */
	private void detectCutShots() {
		for (int i = 0; i < SD.length; i++) {
			if (SD[i] >= Tb) {
				Ce.add(i + 1 + 1000);
			}
		}
		
	}
	//main method 
	public static void main(String[] args) {
		new TwinComparison();
	}
}
