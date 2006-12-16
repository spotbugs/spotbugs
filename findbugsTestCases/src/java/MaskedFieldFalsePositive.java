import java.io.FilterInputStream;
import java.io.InputStream;


public class MaskedFieldFalsePositive extends FilterInputStream {

	InputStream in;
	protected MaskedFieldFalsePositive(InputStream in) {
		super(in);
		this.in = in;
	}
	

}
