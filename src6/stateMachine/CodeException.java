package stateMachine;

public class CodeException extends Throwable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String error;
	int line;

	public CodeException(String error, int line) {
		this.error = error;
		this.line = line;
	}
	
	public String getError() {
		return error;
	}
	
	public int getLine() {
		return line;
	}

}
