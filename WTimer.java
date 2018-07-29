package timerInAction;

public class WTimer {
	
	double initialTime;
	double stoppedTime;
	boolean running = false;
	
	public WTimer(){
		initialTime = 0;
		stoppedTime = 0;
	}//WTimer
	
	public double getTime(){
		if(running){
			return System.currentTimeMillis() - initialTime;
		}//ifRunning
		else{
			return stoppedTime;
		}//ifNotRunning
	}//getTime
	
	public void start(){
		if(!running){
			running = true;
			initialTime = System.currentTimeMillis() - stoppedTime;
		}//ifNotRunning
	}//start
	
	public void stop(){
		if(running){
			stoppedTime = System.currentTimeMillis() - initialTime;
			running = false;
		}//ifRunning
	}//stop
	
	public void reset(){
		initialTime = 0;
		stoppedTime = 0;
		running = false;
	}//reset
}//WTimer
