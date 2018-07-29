package timerInAction;

import java.util.*;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

public class Main extends Application{
	
	int canvasSize = 600;
	
	int gridSceneWidth = canvasSize + 500;
	int gridSceneHeight = canvasSize + 50;
	
	GridPane grid;
	Scene gridScene;
	Canvas canvas;
	GraphicsContext gc;
	AnimationTimer animator;
	TextArea collisionTextArea;
	TextArea particle0TextArea;
	
	Random rand = new Random();
	
	double k = 1.38064852 * Math.pow(10, -23); // Boltzmans constant in Joules per Kelvin
	double m = 15.9994 * 2 / (1000 * 6.0221409 * Math.pow(10, 23)); // Mass of a single diatomic oxygen molecule in kilograms
	double rad = 152 * Math.pow(10, -12); // Radius of diatomic oxygen in meters
	
	double lengthOfBox = 35*rad; // Length of each dimension of box in meters
	int DIMENSION = 4; // Dimensionality of the box
	double T = 395; // Temperature of the box in Kelvin
	int N = 1200; // Number of diatomic oxygen molecules in the box
	
	double[][] p = new double[N][DIMENSION]; // Momentum matrix,  N x DIM. Gaussian distribution (mean = 0, var = mkT)
	double[][] x = new double[N][DIMENSION]; // Position matrix, N x DIM. Uniform distribution (x, y, z: (0, canvasSize))
	
	double secondsOfAnimationPerSecond = 1 * Math.pow(10, -12); // Simulated seconds per second
	
	double pixelsPerAtomAtMinZ = (canvasSize/lengthOfBox)*2*rad;
	double pixelsPerAtomAtMaxZ = pixelsPerAtomAtMinZ / 2;
	WTimer timer = new WTimer();
	double t = 0;
	double dt = 0;
	
	ArrayList<Double> timeOfParticle0Collide = new ArrayList<Double>();
	ArrayList<Integer> partnerOfParticle0Collide = new ArrayList<Integer>();
	double avgCollisionTime = Integer.MAX_VALUE;
	double numCollisions = 0;
	
	public static void main(String[] args){
		Application.launch(args);
	}//main
	
	public void start(Stage mainStage){
		initializeParticles();
		grid();
		scene();
		canvas();
		screenText();
		stage(mainStage);
	}//start
	
	private void screenText() {
		collisionTextArea = new TextArea();
		collisionTextArea.setFocusTraversable(false);
		collisionTextArea.setOnMouseClicked(null);
		collisionTextArea.setWrapText(true);
		collisionTextArea.setMaxHeight(12);
		grid.add(collisionTextArea, 1, 0, 1, 1);
		
		particle0TextArea = new TextArea();
		particle0TextArea.setFocusTraversable(false);
		particle0TextArea.setOnMouseClicked(null);
		particle0TextArea.setWrapText(true);
		grid.add(particle0TextArea, 1, 1, 1, 1);
	}
	
	private void initializeParticles() {
		// Px, Py, Pz are normally distributed about 0 with variance of m*k*T.
		// The Maxwell-Boltzman distribution will be the distribution on p which is the pythagorean sum of Px, Py, and Pz.
		
		// I verified that my generation led to the same result as:
		//double avgSquareVelocity = 3 * k * T / m; // verified this was the case.
		
		// generate momenta:
		for (int i = 0; i < N; i++) {
			for (int dir = 0; dir < DIMENSION; dir++) {
				p[i][dir] = rand.nextGaussian() * Math.sqrt(m * k * T);
			}
		}
		// generate positions:
		for (int i = 0; i < N; i++) {
			for (int dir = 0; dir < DIMENSION; dir++) {
				x[i][dir] = rad + rand.nextDouble() * (lengthOfBox - 2*rad);
			}
		}
	}
	
	private void grid(){
		grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));
		//grid.setGridLinesVisible(true);
		//^^ This would show grid lines, useful for understanding and keeping track of gaps and insets.
	}//grid
	
	private void scene(){
		gridScene = new Scene(grid, gridSceneWidth, gridSceneHeight);
	}//scene

	private void canvas(){
		
		canvas = new Canvas(canvasSize, canvasSize);
		gc = canvas.getGraphicsContext2D();
		
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, canvasSize, canvasSize);
        
        animator = new AnimationTimer() {

        	// GRAPHICS TRANSFORMATION FUNCTIONS -
			private double metersToPixelsAtMinZ(double meters) {
				return meters * pixelsPerAtomAtMinZ/(2*rad);
			}
			private double pixelsToMetersAtMinZ(double pixels) {
				return pixels * 2*rad/pixelsPerAtomAtMinZ;
			}
			private double circleRadiusFromZ(double z) { // MOLECULES IN THE DISTANCE ARE SMALLER (in 3rd dimension)
				double ratioToMaxZ = z / lengthOfBox;
				double pixelDiameter = -(pixelsPerAtomAtMinZ - pixelsPerAtomAtMaxZ)*ratioToMaxZ + pixelsPerAtomAtMinZ; 
				return pixelDiameter / 2;
			}
        	
			// GRAPHICS LOOP -
        	public void handle(long now) { 
    			timer.start(); // this does nothing if it has already been started
        		dt = (timer.getTime() * secondsOfAnimationPerSecond) / 1000 - t;
        		t = (timer.getTime() * secondsOfAnimationPerSecond) / 1000; // current time elapsed in seconds
        		
        		// UPDATE MOMENTA -
        		for (int i = 0; i < N; i++) {
        			// COLLISIONS WITH WALLS OF BOX -
        			for (int dir = 0; dir < DIMENSION; dir++) {
        				if (x[i][dir] <= rad) {
        					p[i][dir] = Math.abs(p[i][dir]);
        				} else if (x[i][dir] >= lengthOfBox - rad) {
        					p[i][dir] = -Math.abs(p[i][dir]);
        				}
        			}
        			// COLLISIONS BETWEEN GAS MOLECULES -
        			for (int j = i + 1; j < N; j++) {
        				double d = 0;
        				double[] dx = new double[DIMENSION];
        				for (int dir = 0; dir < DIMENSION; dir++) {
        					dx[dir] = x[j][dir] - x[i][dir];
        					d += Math.pow(dx[dir], 2);
        				}
        				d = Math.sqrt(d);
        				if (d <= 2*rad) {
        					numCollisions++;
        					avgCollisionTime = t / numCollisions;
        					//prints collision information - 
        					String collisionInfo = "";
        					collisionInfo += "Particle " + i + " collided with Particle " + j;
//        					for (int dir = 0; dir < DIMENSION; dir++) {
//        						collisionInfo += "r" + (dir+1) + ": " + (x[i][dir] + dx[dir]/2) + " m,\n";
//        					}
        					collisionInfo += " at time t = " + String.format("%.4g seconds", t);
        					collisionTextArea.setText(collisionInfo);
        					
        					if (i == 0) {
        						timeOfParticle0Collide.add(t);
        						partnerOfParticle0Collide.add(j);
        					}
        					
        					double[] temp = new double[DIMENSION];
        					for (int dir = 0; dir < DIMENSION; dir++) { // ELASTIC COLLISIONS -
        						temp[dir] = p[i][dir];
        						p[i][dir] = p[j][dir];
        						p[j][dir] = temp[dir];
        						
        						x[j][dir] = x[i][dir] + dx[dir]*((2*rad)/d + .01); // Prevents problems due to small dt and fast dx/dt
        					}
        				}
        			}
        		}
        		
        		// UPDATE POSITIONS -
    			for (int i = 0; i < N; i++) {
					for (int dir = 0; dir < DIMENSION; dir++) {
						x[i][dir] = x[i][dir] + (p[i][dir]/m) * dt;
					}
				}
    			
    			// UPDATE SCREEN TEXT
    			String particle0Info = "";
    			particle0Info += "Temperature: " + T + " Kelvin\n";
    			particle0Info += "Number of diatomix oxygen molecules: " + N + "\n";
    			particle0Info += "Mass (1 diatomic oxygen molecule): " + String.format("%.4g kg\n", m);
    			particle0Info += "Radius (1 diatomic oxygen molecule): " + rad + " meters\n";
    			particle0Info += "Length of box: " + lengthOfBox  + " meters\n";
    			particle0Info += "Dimensionality of box: " + DIMENSION + " dimensions\n";
    			particle0Info += "Time elapsed: " + String.format("%.4g seconds\n", t);
    			particle0Info += "Simulation rate: " + secondsOfAnimationPerSecond + " seconds of simulation per second\n\n";
    			particle0Info += "Avgerage Collision Time: " + String.format("%.4g seconds between collisions on average\n", avgCollisionTime);
    			particle0Info += "Number of Total Collisions: " + ((int) numCollisions) + " total collisions";
    			
    			double avgCollisionTimeGapParticle0 = 0;
    			for (int i = 1; i < timeOfParticle0Collide.size(); i++) {
    				avgCollisionTimeGapParticle0 += timeOfParticle0Collide.get(i) - timeOfParticle0Collide.get(i - 1);
    			}
    			avgCollisionTimeGapParticle0 /= (timeOfParticle0Collide.size() - 1);
    			particle0Info += "\n\nOn the average, Particle 0 has collided with another gas molecule after every " + String.format("delta t = %.4g seconds\n\n", avgCollisionTimeGapParticle0);
    			
    			particle0Info += "Particle 0 is at:\n";
    			for (int dir = 0; dir < DIMENSION; dir++) {
    				particle0Info += "r" + (dir+1) + ": " + String.format("%.4g meters,\n", x[0][dir]);
    			}
    			
    			for (int i = timeOfParticle0Collide.size() - 1; i >= 0; i--) {
    				particle0Info += "\nParticle 0 collided with Particle " + partnerOfParticle0Collide.get(i) + " after t = " + String.format("%.4g seconds", timeOfParticle0Collide.get(i));
    			}
    			
    			particle0TextArea.setText(particle0Info);
    			
    			// DRAW BACKGROUND -
        		drawBackground(gc);
    			gc.setStroke(Color.BLACK);
    		    gc.setLineWidth(2);
    		    gc.strokeRect(0, 0, canvasSize, canvasSize);

        		// DRAW THE GAS MOLECULES -
        		int numToDrawRed = 1;
        		if (DIMENSION == 1) { // 1-D Graphics Function (x along x-axis)
        			for (double[] r : x) {
        				gc.setFill(Color.BLACK);
        				gc.fillOval(r[0], canvasSize/2, 5, 5); //location on screen from x, y; size on screen from z
        			}
        			gc.setFill(Color.RED);
        			for (int i = 0; i < numToDrawRed; i++) {
        				gc.fillOval(x[i][0] - rad, canvasSize/2, 5, 5);
        			}
        		}else if (DIMENSION == 2) { // 2-D Graphics Function (y along y-axis)
        			for (double[] r : x) {
        				gc.setFill(Color.BLACK);
        				gc.fillOval(metersToPixelsAtMinZ(r[0]) - pixelsPerAtomAtMinZ/2, metersToPixelsAtMinZ(r[1]) - pixelsPerAtomAtMinZ/2, pixelsPerAtomAtMinZ, pixelsPerAtomAtMinZ); //location on screen from x, y; size on screen from z
        			}
        			gc.setFill(Color.RED);
        			for (int i = 0; i < numToDrawRed; i++) {
        				gc.fillOval(metersToPixelsAtMinZ(x[i][0] - rad), metersToPixelsAtMinZ(x[i][1] - rad), pixelsPerAtomAtMinZ, pixelsPerAtomAtMinZ);
        			}
        		}else if (DIMENSION == 3) { // 3-D Graphics Function (z given by small z is bigger, big z is smaller)
        			for (double[] r : x) {
        				gc.setFill(new Color(0, 0, 0, .3));
        				gc.fillOval(metersToPixelsAtMinZ(r[0]) - circleRadiusFromZ(r[2]), metersToPixelsAtMinZ(r[1]) - circleRadiusFromZ(r[2]), 2*circleRadiusFromZ(r[2]), 2*circleRadiusFromZ(r[2])); //location on screen from x, y; size on screen from z
        			}
        			gc.setFill(new Color(1, 0, 0, .6));
        			for (int i = 0; i < numToDrawRed; i++) {
        				gc.fillOval(metersToPixelsAtMinZ(x[i][0]) - circleRadiusFromZ(x[i][2]), metersToPixelsAtMinZ(x[i][1]) - circleRadiusFromZ(x[i][2]), 2*circleRadiusFromZ(x[i][2]), 2*circleRadiusFromZ(x[i][2]));
        			}
        		}else if (DIMENSION >= 4) { // 4-D Graphics Function (4th dimension by greyscale. blacker <=> closer, whiter <=> farther)
        			for (double[] r : x) {
        				double greyness = 1 - ((r[3] - rad) / pixelsToMetersAtMinZ(canvasSize - 2*pixelsPerAtomAtMinZ));
        				if (greyness > 1) greyness = 1;
        				if (greyness < 0) greyness = 0;
        				gc.setFill(new Color(greyness, greyness, greyness, .6));
        				gc.fillOval(metersToPixelsAtMinZ(r[0]) - circleRadiusFromZ(r[2]), metersToPixelsAtMinZ(r[1]) - circleRadiusFromZ(r[2]), 2*circleRadiusFromZ(r[2]), 2*circleRadiusFromZ(r[2]));
        			}
        			// 4-D PROJECTION OF RANDOM WALK OF A SINGLE N DIMENSIONAL GAS MOLECULE -
        			double greyness = 1 - ((x[0][3] - rad) / pixelsToMetersAtMinZ(canvasSize - 2*pixelsPerAtomAtMinZ));
    				if (greyness > 1) greyness = 1;
    				if (greyness < 0) greyness = 0;
    				gc.setFill(new Color(greyness, greyness, greyness, 1));
    				gc.fillOval(metersToPixelsAtMinZ(x[0][0]) - circleRadiusFromZ(x[0][2]), metersToPixelsAtMinZ(x[0][1]) - circleRadiusFromZ(x[0][2]), 2*circleRadiusFromZ(x[0][2]), 2*circleRadiusFromZ(x[0][2]));
        		}
            }//handle
        };//AnimationTimer
        animator.start();
        
        grid.add(canvas, 0, 0, 1, 2);
	}//canvas

	private void stage(Stage stage) {
		stage.setTitle("Brownian Motion of Diatomic Oxygen Gas in a box in N dimensions");
		stage.setScene(gridScene);
		stage.show();
	}//stage

	private void drawBackground(GraphicsContext gc){
		Paint temp = gc.getFill();
		gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasSize, canvasSize);
        gc.setFill(temp);
	}//drawBackground
	
}//Main