package MD5unhash;


/* Description: This class implements a descriptor */
/*   for a unit of work that needs to be processed */
/*   by the generic worker thread. The class also  */
/*   incorporates a field to save the result of    */
/*   the performed computation.                    */

public class WorkUnit {

    boolean simple;
    int lowerBound;
    int upperBound;
    String hash;
    String result;
    String tvalue;


    public WorkUnit (String hash) {
	this.simple = true;
	this.hash = hash;
	this.result = null;
	this.tvalue = null;
    }

    public WorkUnit (String hash, int lower, int upper) {
	this.simple = false;
	this.hash = hash;
	this.lowerBound = lower;
	this.upperBound = upper;
	this.result = null;
	this.tvalue = null;
    }

    public boolean isSimple() {
	return simple;
    }

    public String getHash() {
	return hash;
    }

    public int getLowerBound() {
	if (simple)
	    return 0;
	else
	    return lowerBound;
    }

    public int getUpperBound() {
	if (simple)
	    return Integer.MAX_VALUE;
	else
	    return upperBound;
    }

    public void setResult(String result) {
	this.result = result;
    }

    public String getResult() {
    	return result;
    	
    }
    public String getTvalue() {
    	if(this.simple) {
    		return result;
    	} else {
    		return this.tvalue;
    	}
    }
    
    public void setTvalue(String t) {
    	this.tvalue = t;
    	this.simple = false;
    }
    
    @Override
    public String toString() {
	if (this.result != null)
	    return this.result;
	else
	    return this.hash;
    }
    
}

/* END -- Q1BSR1QgUmVuYXRvIE1hbmN1c28= */
