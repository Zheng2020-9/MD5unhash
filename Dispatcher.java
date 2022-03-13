package hw8;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;


import java.nio.file.*;


import java.io.File;
import java.io.IOException;


/***************************************************/
/* CS-350 Fall 2021 - Homework 7 - Code Solution   */
/* Author: Renato Mancuso (BU)                     */
/*                                                 */
/* Description: This class implements the logic of */
/*   a linear dispatcher that also spans and       */
/*   manages a set of worker threads to crack a    */
/*   a set of MD5 hashes provied in an input file. */
/*   The dispatcher also implements a work pruning */
/*   routine.                                      */
/*                                                 */
/***************************************************/

public class Dispatcher {

    int numCPUs;
    int timeoutMillis;
    ArrayList<UnHashWorker> workers;
    int workCount;
    int pruned;
    
    /* Queue for inputs to be processed */
    LinkedList<WorkUnit> workQueue;

    /* Semaphore to synch up on the number of input items */
    Semaphore wqSem;

    /* Mutex to protect input queue */   
    Semaphore wqMutex;

    /* Queue for processed outputs */
    LinkedList<WorkUnit> resQueue;
    
    /* Semaphore to synch up on the number of output items */
    Semaphore rsSem;

    /* Mutex to protect output queue */
    Semaphore rsMutex;
    Semaphore hintsMutex;
    HashSet<Integer> restHints;
    
    HashMap<String, WorkUnit> restUnits;
    
    public Dispatcher (LinkedList<WorkUnit> workQueue, LinkedList<WorkUnit> resQueue,
		       Semaphore wqMutex, Semaphore rsMutex,
		       int N, int timeout, HashSet<Integer> restHints, HashMap<String, WorkUnit> restUnits,Semaphore hintsmutex)
	{
	    this.numCPUs = N;
	    this.timeoutMillis = timeout;
	    this.workCount = 0;       
	    this.workQueue = workQueue;
	    this.resQueue = resQueue;
	    this.wqMutex = wqMutex;
	    this.rsMutex = rsMutex;
	    this.restHints = restHints;
	    this.restUnits = restUnits;
	    
	    this.hintsMutex = hintsmutex;

	    
	    workers = new ArrayList<UnHashWorker>();
	    
	    /* Initialize the semaphores necessary to synchronize over the
	     * input and output queues */
	    wqSem = new Semaphore(0);
	    
	    rsSem = new Semaphore(0);
	    
	    /* Start by spawning the worker threads */
	    for (int i = 0; i < N+3; ++i) {
		UnHashWorker worker = new UnHashWorker(workQueue, resQueue,
						       wqSem, wqMutex,
						       rsSem, rsMutex, restHints, restUnits,hintsmutex, false);
		worker.setTimeout(timeout);
		workers.add(worker);
		
		/* Ready to launch the worker */
		worker.start();
	    }
	}
    
    public void addWork(WorkUnit work) throws InterruptedException
	{
	    wqMutex.acquire();
	    /* CRITICAL SECTION */
	    
	    workQueue.add(work);
	    ++workCount;
	    
	    /* Signal the presence of new work to be done */
	    wqSem.release();
	    
	    /* END OF CRITICAL SECTION */
	    wqMutex.release();
	}
    
    public void terminate ()
	{
	    /* All done, terminate all the worker threads */
	    for (UnHashWorker worker : workers) {
		worker.exitWorker();
	    }
	    
	    /* Make sure that no worker is stuck on the empty input queue */
	    for (UnHashWorker worker : workers) {
		wqSem.release();
	    }
	    this.workers = new ArrayList<UnHashWorker>();

	}
    
    public boolean isFinish() {
    	return !(restUnits.size() > 0);
    }
    
    public void addSets(int a) {
    	this.restHints.add(a);
    }
    public void addUnits(String w, WorkUnit unit) {
    	this.restUnits.put(w, unit);
    }
    public void resetHash() {
    	this.restUnits = new HashMap<String,WorkUnit>();
    	this.restHints = new HashSet<Integer>();
    }
    
    public void setHash(HashSet<Integer> hs, HashMap<String, WorkUnit> hm) {
    	this.restHints = hs;
    	this.restUnits = hm;
    }
    public HashMap<String, WorkUnit> getUnits(){
    	return this.restUnits;
    }

    /* This function removes useless work from the shared qork
     * queue. NOTE: what implemented is a very inefficient way of
     * achieving this. It would be better to have the work in a shared
     * hash-map and then removing the unneeded elements in O(1)
     * instead of linearly scanning the entire list every time. */
    private void pruneWork() throws InterruptedException
	{
	    /* Acquire mutexes for both input and output queue */
	    wqMutex.acquire();
	    rsMutex.acquire();

	    for (int i = pruned; i < resQueue.size(); ++i) {

		/* Find items in the work queue with lower or upper
		 * bound that matched with this element */
		WorkUnit w = resQueue.get(i);
		if (!w.isSimple() && w.getResult() != null) {
		    int upper = w.getUpperBound();
		    int lower = w.getLowerBound();

		    /* Now scan through the work queue and remove any
		     * matching element */
		    for (int j = 0; j < workQueue.size(); ++j) {
			WorkUnit u = workQueue.get(j);
			if (u.getUpperBound() == upper || u.getLowerBound() == lower ||
			    u.getLowerBound() == upper || u.getUpperBound() == lower) {
			    workQueue.remove(j--);
			    workCount--;
			}
		    }
		}
		
		pruned++;
	    }
	    
	    rsMutex.release();
	    wqMutex.release();	    
	}
    
    public void act() {
    	for (int i = 0; i < numCPUs; ++i) {
			UnHashWorker worker = new UnHashWorker(workQueue, resQueue,
							       wqSem, wqMutex,
							       rsSem, rsMutex, restHints, restUnits,hintsMutex, false);
			worker.setTimeout(timeoutMillis);
			workers.add(worker);
			
			/* Ready to launch the worker */
			worker.start();
		}
    }
    
    
    public void actf() {
    	int x = 0;
    	if(numCPUs > 1) {
    		x = numCPUs - 1;
    	}else {
    		x = 1;
    	}
    	for (int i = 0; i < x; ++i) {
			UnHashWorker worker = new UnHashWorker(workQueue, resQueue,
							       wqSem, wqMutex,
							       rsSem, rsMutex, restHints, restUnits,hintsMutex, true);
			worker.setTimeout(timeoutMillis);
			workers.add(worker);
			
			/* Ready to launch the worker */
			worker.start();
		}
    }
    
    public void dispatch () throws InterruptedException
	{
	    pruned = 0;
	    
	    /* At this point, we just wait for all the input to be consumed */	    
	    while(workCount-- > 0) {
		/* Wait for result to be produced. */
	    	//System.out.println("0");
	    	rsSem.acquire();
	    	//System.out.println("1");
		

		/* Attempt to perform pruning of the work queue only
		 * when a new result is recorded. */
	    }
	    
	    /* return when done for caller to move to next stage */
	}
    
}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
