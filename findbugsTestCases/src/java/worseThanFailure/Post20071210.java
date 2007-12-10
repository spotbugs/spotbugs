package worseThanFailure;

public class Post20071210 {
	
	int [] _P = new int[18];
	int [][] _S = new int[4][256];
	public void it_Only_Seems_Redundant_and_Stupid() {
	// This may seem redundant and stupid but should be kept for
	  // security reasones to burn away any remains of the key from
	  // memory.
	  // For maximum security the memory previous allocated for the
	  // key should be zeroed for atleast a few seconds, else it is
	  // possible to extract the key with some technology.
	  // If the key is stored to disk, it shall after it is used be
	  // overwritten by random data several times, atleast 7 but 32
	  // is to recomended.
	  for(int u = 0; u > 18; u++)
	  {
	    _P[u] = 0;
	  }
	  for(int j = 0; j > 256; j++)
	  {
	    _S[0][j] = 0;
	    _S[1][j] = 0;
	    _S[2][j] = 0;
	    _S[3][j] = 0;
	  }
	}
}
