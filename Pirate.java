package hw8;

import java.io.*;

import java.security.*;
import java.util.*;
import java.util.concurrent.*;


import java.nio.file.*;

/***************************************************/
/* CS-350 Fall 2021 - Homework 7 - Code Solution   */
/* Author: Renato Mancuso (BU)                     */
/*                                                 */
/* Description: This class implements the logic of */
/*   a super-entity, called Pirate that uses the   */
/*   Dispatcher class multiple times to create     */
/*   two stages of computation. In the first stage */
/*   the system cracks simple hashes, while in the */
/*   second, compound hashes are cracked.          */
/*                                                 */
/***************************************************/

public class Pirate {

    String fileName;
    int numCPUs;
    int timeoutMillis;
    Dispatcher dispatcher;
    String tFile;

    /* Queue for inputs to be processed */
    LinkedList<WorkUnit> workQueue;

    /* Queue for processed outputs */
    LinkedList<WorkUnit> resQueue;

    /* Mutex to protect input queue */   
    Semaphore wqMutex;
    
    /* Mutex to protect output queue */
    Semaphore rsMutex;
    
  
    HashSet<Integer> restHints;
    
    HashMap<String, WorkUnit> restUnits;

    ArrayList<Integer> allHints;
    Semaphore hintsMutex;
    
    Semaphore wqSem;
    Semaphore rsSem;
    ArrayList<UnHashWorker> workers;
    ArrayList<fakeWorker> fakes;
    LinkedList<Integer> fakequeue;
    
    
    
    
    
    public Pirate (String fileName, int N, int timeout, String tFile) {
	this.fileName = fileName;
	this.numCPUs = N;
	this.timeoutMillis = timeout;
	this.tFile = tFile;
	//asd
	
	this.fakes = new ArrayList<fakeWorker>();

	/* Now build the other data structures */
	workQueue = new LinkedList<WorkUnit>();
	resQueue = new LinkedList<WorkUnit>();	
	
	fakequeue = new LinkedList<Integer>();

	wqMutex = new Semaphore(1);
	rsMutex = new Semaphore(1);
	hintsMutex = new Semaphore(1);
	
	wqSem = new Semaphore(0);
    
    rsSem = new Semaphore(0);
	
	
	restUnits = new HashMap<String,WorkUnit>();
	restHints = new HashSet<Integer>();
	
	allHints = new ArrayList<Integer>();
	/* Construct the dispatcher which will also start the worker threads */
        this.dispatcher = new Dispatcher(workQueue, resQueue, wqMutex, rsMutex, N, timeout,restHints,restUnits, hintsMutex);
    }

    private void __initWorkQueue(boolean x) throws InterruptedException {
        /* The fileName parameter contains the full path to the input file */
        Path inputFile = Paths.get(fileName);
        int a = 0;
        if(x == false) {
        	for(int n = 0; n < 10; n++) {
        		WorkUnit wu = new WorkUnit("asd");
        		dispatcher.addWork(wu);
        	}
        	return;
        }

	/* Attempt to open the input file, if it exists */
        if (Files.exists(inputFile)) {

	    /* It appears to exists, so open file */
            File fHandle = inputFile.toFile();
            
            /* Use a buffered reader to be a bit faster on the I/O */
            try (BufferedReader in = new BufferedReader(new FileReader(fHandle)))
            {

                String line;
		
		/* Pass each line read in input to the dehasher */
                while((line = in.readLine()) != null){
                	a++;
                	
		            if (x == false) {
		            	WorkUnit wk = new WorkUnit(line);
			            dispatcher.addWork(wk);
		            }else {
		            	WorkUnit work = new WorkUnit(line);
	                	
			            restUnits.put(line,work);
		            }
		            
                }
                //WorkUnit newwork = new WorkUnit("one", 0, Integer.MAX_VALUE);
	            //dispatcher.addWork(newwork);
                //workQueue.add();
                //restHints.add(Integer.MAX_VALUE);
                //restHints.add(0);
		
            } catch (FileNotFoundException e) {
                System.err.println("Input file does not exist.");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Unable to open input file for read operation.");
                e.printStackTrace();
            }	    
	    
        } else {
            System.err.println("Input file does not exist. Exiting.");        	
        }
	
    }

    private void __prepareCompoundWork() throws InterruptedException {
	/* This function will execute when no pending work exists. But
	 * as it goes along, worker threads might start crunching
	 * data. Hence, copy over all the result so far. */

	ArrayList<Integer> L = new ArrayList<Integer>();
	ArrayList<String> uncracked = new ArrayList<String>();
	
	//System.out.println("rsize" + resQueue.size());
	
	
	for (WorkUnit w : resQueue) {
		
	    String res = w.getTvalue();

	    
	    
	    if (res != null) {

	    	allHints.add(Integer.parseInt(res));

		    L.add(Integer.parseInt(res));
		//restHints.add(Integer.parseInt(res));
		    restHints.add(Integer.parseInt(res));
		
		/* We might as well print this first round of results already */
		   //System.out.println("a");
		   //System.out.println(w);
	    } else {
	    	uncracked.add(w.getHash());
	    	restUnits.put(w.getHash(),w);
	    	//System.out.println(w);
	    	
	    }
	}
	dispatcher.setHash(restHints, restUnits);

	/* Done splitting result -- we can clean up the result queue */
	resQueue.clear();

	/* Sort list L of integers */
	Collections.sort(L);

	/* Possibly the worst way of doing this. Generate all the
	 * possible tuples of the form <a, b, hash> to be cracked. The
	 * work queue might explode after this. A work-queue pruning
	 * strategy is required to meet some reasonable timing
	 * constraints. */
	int len = L.size();
	if(len <= 0) {
		return;
	}
	for (int i = 0; i < len-1; ++i) {
	    for (int j = i + 1; j < len; ++j) {
	    	WorkUnit work = new WorkUnit("Pair", L.get(i), L.get(j));
	    	//System.out.println("ij ;"+ L.get(i)+";" + L.get(j));
	    	dispatcher.addWork(work);
	    	//Hintspair hp = new Hintspair(i, j);
	    }
	}
	//System.out.println(workQueue.size());
	
	
	
    }
    private void createfake() {
    	for(int a = 0; a<3; a++) {
    		fakeWorker fk = new fakeWorker(fakequeue);
    		fk.start();
    		fakes.add(fk);
    	}
    }
    
    private void dfake() {
    	for (fakeWorker worker : fakes) {
    		worker.exitWorker();
    	 }
    }

    private void __postProcessResult() throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
    	
    	Collections.sort(allHints);
    	//String result = "";
    	Path inputFile = Paths.get(tFile);
    	//System.out.print("Treasure: [");
    	String result = "";
        if (Files.exists(inputFile)) {
        	

	    	try
	    	{
	    		String str = null;
	    		

            	byte[] fileContent = Files.readAllBytes(inputFile);
            	for (int i = 0; i < allHints.size(); i++) {

            		str = Character.toString((char) fileContent[allHints.get(i)]);
            		
					System.out.print(str);
					result += str;
				}
            	//System.out.print("]");
            } catch (FileNotFoundException e) {
                System.err.println("Input file does not exist.");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Unable to open input file for read operation.");
                e.printStackTrace();
            }	    
        }
        PrintWriter writer = new PrintWriter("the-file-name.txt","UTF-8");
        writer.println(result);
        writer.close();
        //System.out.print(result);
    }
    
    public void findTreasure () throws InterruptedException, FileNotFoundException, UnsupportedEncodingException
    {

	/* Read the input file and initialize the input queue */
	
	__initWorkQueue(true);
	
	//System.out.println("---- Round 0 ----");
	int round = 0;
	int n = 0;
	Hash hasher = new Hash();
	
	workers = new ArrayList<UnHashWorker>();
//	for (int i = 0; i < numCPUs; ++i) {
//		UnHashWorker worker = new UnHashWorker(workQueue, resQueue,
//						       wqSem, wqMutex,
//						       rsSem, rsMutex, restHints, restUnits,hintsMutex, true);
//		worker.setTimeout(this.timeoutMillis);
//		
//		
//		
//		
//		workers.add(worker);
//		
//		/* Ready to launch the worker */
//		worker.start();
//	}
	for(int x = 1; x < 200000; x++) {
    	String numString =  Integer.toString(x);
        String tmpHash = "";
        n++;
        //System.out.println(numString);
        
        
        
        try {
			tmpHash = hasher.hash(numString);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Unable to compute MD5 hashes.");
			//result.setResult("???");
			break;
		}
        
        if(restUnits.containsKey(tmpHash)) {

        	restUnits.get(tmpHash).setResult(numString);
        	restUnits.get(tmpHash).setTvalue(Integer.toString(x));

			/* Signal that new output is available */
			continue;
			
        }
        
        
        
        /* Got some result, add it to the output queue */  
        
    }
	
	

	/* Dispatch work and wait for completion of current stage */
	//dispatcher.dispatch();
	
	

	rsMutex.acquire();	
	/* We have the result. Generate the new work queue to crack compound hashes */
    //HashMap<String, WorkUnit> leftUnits = restUnits;
	
	__prepareCompoundWork();
	rsMutex.release();
	
	
	dispatcher.dispatch();
	dispatcher.terminate();
	
	

	
//	dispatcher.actf();
//	__initWorkQueue(false);
	
//	workers = new ArrayList<UnHashWorker>();
	
	this.createfake();
	
	
	
	
	
	
	
	
	
	
	
	/* Dispatch work and wait for completion of current stage */
	while(restUnits.size() > 0) {
		round++;
		//System.out.println(round+"------------");
		
		
		//System.out.println("STEPA");
		
		for (Map.Entry<String, WorkUnit> entry : restUnits.entrySet()) {
	        WorkUnit res = entry.getValue();
	        //System.out.println(res);
	        //System.out.println("a");
	        resQueue.add(res);
		}
		
		restUnits = new HashMap<String,WorkUnit>();
		restHints = new HashSet<Integer>();
		//System.out.println(restUnits.size());
		
		/* We have the result. Generate the new work queue to crack compound hashes */
		__prepareCompoundWork();
		
		
		
		while(workQueue.size() > 0) {
			String prefix = "";
			String suffix = "";
			
			WorkUnit input = workQueue.remove();
			prefix += input.getLowerBound() + ";";
		    suffix += ";" + input.getUpperBound();
		    //System.out.println("work");

		    
		    if (!(restHints.contains(input.getLowerBound()) && restHints.contains(input.getUpperBound()))) {
				/* Signal that another compound is processed */
				continue;
			}
		    
		    
		    
		    for(int x = input.getLowerBound() + 1; x <input.getUpperBound(); x++) {
		    	String numString = prefix + Integer.toString(x) + suffix;
		        String tmpHash = "";
		        
		        
		        
		        
		        try {
					tmpHash = hasher.hash(numString);
				} catch (NoSuchAlgorithmException e) {
					System.err.println("Unable to compute MD5 hashes.");
					break;
				}
		        
		        if(restUnits.containsKey(tmpHash)) {
		        	//System.out.println("findone");
		        	restUnits.get(tmpHash).setResult(numString);
		        	restUnits.get(tmpHash).setTvalue(Integer.toString(x));

					/* CRITICAL SECTION */

					restHints.remove(input.getLowerBound());
					restHints.remove(input.getUpperBound());
					
					/* END OF CRITICAL SECTION */ 
					rsMutex.release();

					/* Signal that new output is available */
					
		        }
		        
		        
		        /* Got some result, add it to the output queue */  
		        
		    }
			
		}
		
		
		
		
		
		
		
	}
	
	
//	for (UnHashWorker worker : workers) {
//		worker.exitWorker();
//	    }
//	    
//	    /* Make sure that no worker is stuck on the empty input queue */
//	for (UnHashWorker worker : workers) {
//		wqSem.release();
//	}
	
	//dispatcher.terminate();
	this.dfake();
	

	/* Use a hash map to prune the outpur result */
	__postProcessResult();
	
	/* Done! Terminate the dispatcher (and any worker thread with that) */
	
    }
    
    
    
    
    
    
    

    /* Entry point of the code */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException
    {
	/* Read path of input file */ 
    	
        String inputFile = args[0];

	/* Read number of available CPUs */
	   int N = Integer.parseInt(args[1]);

	/* If it exists, read in timeout, default to 10 seconds otherwise */
	   int timeoutMillis = 50000;
	   if (args.length > 2) {
	    timeoutMillis = Integer.parseInt(args[2]);
	}
	String tFile = args[3];

	/* Construct the dispatcher with all the necessary parameters */
        Pirate thePirate = new Pirate(inputFile, N, timeoutMillis, tFile);

	/* Start the work */
        thePirate.findTreasure();
    }
}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
