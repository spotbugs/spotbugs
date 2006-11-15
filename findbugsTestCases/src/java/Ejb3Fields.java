import javax.annotation.Resource;


public class Ejb3Fields {
	@Resource String s;
	
	public int hashCode() {
		return s.hashCode();
	}

}
