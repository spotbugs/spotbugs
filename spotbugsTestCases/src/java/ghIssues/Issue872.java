package ghIssues;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;

import org.mockito.BDDMockito;
import org.mockito.Mockito;

public class Issue872 {

	// non compliant
	void checkReturnValue() {
		Value checked = new Value();
		checked.checkMe();
	}
	
	// non compliant
	void nse() {
		Value checked = new Value();
		checked.noSideEffect();
	}
	

	// compliant
	char checkedReturnValue() {
		Value checked = new Value();
		return checked.checkMe().charAt(0);
	}
	
	// compliant
	char nseUsed() {
		Value checked = new Value();
		return checked.noSideEffect().charAt(0);
	}

	// compliant
	void checkReturnValueMockito() {
		Value checked = new Value();
		Mockito.verify(checked).checkMe();
		Mockito.verify(checked, Mockito.times(4)).checkMe();
	}
	
	// compliant
	void nseMockito() {
		Value checked = new Value();
		Mockito.verify(checked).noSideEffect();
		Mockito.verify(checked, Mockito.times(4)).noSideEffect();
	}
	
	// compliant
	void doAnswerMockito() {
		Value spy = Mockito.spy(Value.class);
		Mockito.doAnswer(invocation -> {
				return "foo";
			}
		).when(spy).checkMe();
	}
	
	// compliant
	void doReturnMockito() {
		Value receiver = Mockito.mock(Value.class);
		Mockito.doReturn("foo").when(receiver).checkMe();
	}
	
	// compliant
	void doCallRealMethodMockito() {
		Value receiver = Mockito.mock(Value.class);
		Mockito.doCallRealMethod().when(receiver).checkMe();
	}
	
	// compliant
	void doNothingMockito() {
		Value receiver = Mockito.mock(Value.class);
		Mockito.doNothing().when(receiver).checkMe();
	}
	
	// compliant
	void doThrowMockito() {
		Value receiver = Mockito.mock(Value.class);
		Mockito.doThrow(Exception.class).when(receiver).checkMe();
	}
	
	// compliant
	void bddMockitoThen() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.then(receiver).should(Mockito.times(1)).checkMe();
	}
	
	// compliant
	void bddMockitoGiven() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.given(receiver.checkMe()).willCallRealMethod();
	}
	
	// compliant
	void bddMockitoWillThrow() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.willThrow(new RuntimeException("boo")).given(receiver).checkMe();
	}
	
	// compliant
	void bddMockitoWillReturn() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.willReturn("whatever").given(receiver).checkMe();
	}
	
	// compliant
	void bddMockitoWillDoNothing() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.willDoNothing().given(receiver).checkMe();
	}
	
	// compliant
	void bddMockitoWillCallRealMethod() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.willCallRealMethod().given(receiver).checkMe();
	}
	
	// compliant
	void bddMockitoWillAnswer() {
		Value receiver = Mockito.mock(Value.class);
		BDDMockito.willAnswer(i -> "test").given(receiver).checkMe();
	}
	
	public static class Value {
		@CheckReturnValue
		public String checkMe() {
			return "";
		}
		
		public String noSideEffect() {
			return "nse";
		}
	}
}
