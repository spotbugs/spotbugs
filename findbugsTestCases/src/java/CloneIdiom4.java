/** non-final class with final clone() method.
 * 
 *  This is probably bug, but the detector shoud be careful
 *  that the final clone() implementation doesn't directly
 *  or indirectly invoke some kind of non-final copy() method.
 */
public class CloneIdiom4 implements Cloneable {
	@Override
    final public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
