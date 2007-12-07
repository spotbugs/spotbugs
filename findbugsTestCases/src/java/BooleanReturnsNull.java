public class BooleanReturnsNull {

	
	public Boolean always_null(){
		return null;
	}
	
	public Boolean sometimes_null(int n){
		if (n > 3){
			return new Boolean(true);
		}
		else if(n < 1){
			return new Boolean(false);
		}
		else {
			return null;
		}
			
	}
	
	public Boolean never_null(int n){
		if (n>2){
			return new Boolean(true);
		}
		else{
			return new Boolean(false);
		}
	}
	
	public static void main(String[] args){
		//nothing!!
	}
}
