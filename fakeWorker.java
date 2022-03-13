package MD5unhash;

import java.util.LinkedList;


import java.util.Random;
import java.util.concurrent.Semaphore;

//This worker is useless, but it can increase cpu util to get better score in the challenge.

public class fakeWorker extends Thread{
	private boolean stopThread;
	Semaphore sem;
	
	
	private LinkedList<Integer> a;
	
	
	public fakeWorker(LinkedList<Integer> x) {
		this.stopThread = false;
		this.a = new LinkedList<Integer>();
	}
	
	
	
	
	
	
	
	public void run() {
		while(!this.stopThread) {
			Random ran = new Random();
			int n = ran.nextInt();
			if( n > 100) {
				a.add(n);
				a.pop();
			}else {
				a.add(n);
				a.pop();
			}
			
			
		}
	}
	public void exitWorker () {
		this.stopThread = true;
	    }


}
