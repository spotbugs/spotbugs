import javax.annotation.Resource;


public class Ejb3Fields {
	@Resource String s;
	
	@Override
    public int hashCode() {
		return s.hashCode();
	}

}
