package stateMachine;

import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class LexicalAnalyzer {
	
	// This stores the root attribute
	TextAttr root_head_ptr = null;
	// This stores the tail attribute
	TextAttr root_tail_ptr = null;
	// This stores the size of the delimiter
	int delimiter_size;
	// This stores the number of down [, (
	int down_bracket = 0;
	
	// This checks the credibility of a character as a delimiter
	private boolean IsDelimeter(char pch, char ch, char nch) {
		
		if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '.' || ch == '"' || ch == '\'') {
			return true;
		}
		
		if(ch == '/' && nch == '*') {
			return true;
		}
		
		if(ch == '/' && nch == '/') {
			return true;
		}
		
		/*// i.e +5 or -5, a single constant
		if(pch == ' ' && (ch =='-' || ch =='+') && (nch >='0' && nch <= '9')) {
			return true;
		}*/
		
		return false;
	}
	
	// This returns the attribute type
	private AttrType DelimiterType(char buff[], int offset) {
		
		while(buff[offset] == ' ') {
			offset++;
		}
		
		if(offset >= buff.length) {
			return null;
		}
		
		delimiter_size = 1;
		String str;
		
		if(offset < buff.length - 2) {
			str = (new String(buff)).substring(offset, offset + 2);
			if(str.equals("==")) {
				delimiter_size = 2;
				return AttrType.EQUALITY;
			}
			
			if(str.equals("!=")) {
				delimiter_size = 2;
				return AttrType.NE;
			}
			
			if(str.equals(">=")) {
				delimiter_size = 2;
				return AttrType.GTE;
			}
			
			if(str.equals("++")) {
				delimiter_size = 2;
				return AttrType.INCREMENT;
			}
			
			if(str.equals("--")) {
				delimiter_size = 2;
				return AttrType.DECREMENT;
			}
			
			if(str.equals("<=")) {
				delimiter_size = 2;
				return AttrType.LTE;
			}
			
			if(str.equals("+=")) {
				delimiter_size = 2;
				return AttrType.PLUS_EQUAL;
			}
			
			if(str.equals("-=")) {
				delimiter_size = 2;
				return AttrType.MINUS_EQUAL;
			}
			
			if(str.equals("/=")) {
				delimiter_size = 2;
				return AttrType.DIV_EQUAL;
			}
			
			if(str.equals("*=")) {
				delimiter_size = 2;
				return AttrType.MULT_EQUAL;
			}
			
			if(str.equals("->")) {
				delimiter_size = 2;
				return AttrType.R_ARROW;
			}
			
			if(str.equals("||")) {
				delimiter_size = 2;
				return AttrType.OR;
			}
			
			if(str.equals("&&")) {
				delimiter_size = 2;
				return AttrType.AND;
			}
		}
		
		str = (new String(buff)).substring(offset, offset + 1);
		
		if(str.equals("[")) {
			return AttrType.L_SQUARE_BR;
		}
		
		if(str.equals("]")) {
			return AttrType.R_SQUARE_BR;
		}
		
		if(str.equals("{")) {
			return AttrType.L_CURL_BRACE;
		}
		
		if(str.equals("}")) {
			return AttrType.R_CURL_BRACE;
		}
		
		if(str.equals("(")) {
			return AttrType.L_CURL_BR;
		}
		
		if(str.equals(")")) {
			return AttrType.R_CURL_BR;
		}
		
		if(str.equals(",")) {
			return AttrType.COMMA;
		}
		
		if(str.equals(";")) {
			return AttrType.SEMI_COLON;
		}
		
		AttrType delim = null;
		if(str.equals(">")) {
			delim = AttrType.GT;
		}
		
		if(str.equals("<")) {
			delim = AttrType.LT;
		}
		
		if(str.equals("-")) {
			delim = AttrType.SUBTRACT;
		}
		
		if(str.equals("+")) {
			delim = AttrType.ADD;
		}
		
		if(str.equals("/")) {
			delim = AttrType.DIVIDE;
		}
		
		if(str.equals("*")) {
			delim = AttrType.MULTIPLY;
		}
		
		if(str.equals("~")) {
			delim = AttrType.INVERT;
		}
		
		if(str.equals("!")) {
			delim = AttrType.INVERT;
		}
		
		if(str.equals("^")) {
			delim = AttrType.EQOR;
		}
		
		if(str.equals("=")) {
			delim = AttrType.EQ;
		}
		
		if(str.equals(":")) {
			delim = AttrType.COLON;
		}
		
		if(str.equals("@")) {
			return AttrType.UNKNOWN;
		}
		
		if(str.equals("#")) {
			return AttrType.UNKNOWN;
		}
		
		if(str.equals("$")) {
			return AttrType.UNKNOWN;
		}
		
		if(str.equals("%")) {
			return AttrType.UNKNOWN;
		}
		
		if(offset < buff.length - 2 && delim != null) {
			if(buff[offset + 1] != ' ' && buff[offset + 1] != '\n' &&
					IsDelimeter(offset > 0 ? buff[offset - 1] : '&', buff[offset + 1], ' ') == false) {
				return AttrType.UNKNOWN;
			}
		}
		
		return delim;
	}
	
	// This is the entry function that builds the document structure
	public TextAttr BuildDocument(char buffer[]) throws IOException, CodeException {

		if(buffer == null) {
			File file = new File("program.txt");
		    int offset = 0;
		     
		    try {
		        @SuppressWarnings("resource")
				BufferedReader bufferedReader = new BufferedReader(
		                new FileReader(file));
		 
		        buffer = new char[(int)file.length()];
		 
		        int c = bufferedReader.read();
		 
		        while (c != -1) {
		        	buffer[offset++] = (char)c;
		            c = bufferedReader.read();
		        }
		    } catch (FileNotFoundException e) {
		    } catch (IOException e) {
		    }
		}
	  
	    int start = 0;
	    int line_num = 0;
	    boolean is_down = false;
	    boolean is_up = false;
	    ArrayList<TextAttr> stack = new ArrayList<TextAttr>();
	    stack.add(null);
	    
	    int line_char_num = 0;
	    boolean is_line_comment = false;
	    boolean is_block_comment = false;
	    for(int i=0; i<buffer.length; i++) {
	    	
	    	char nch = ' '; 
	    	if(i + 1 < buffer.length) {
	    		nch = buffer[i + 1];
	    	}
	    	
	    	if(is_block_comment == true) {
	    		if(buffer[i] == '*' && nch == '/') {
		    		is_block_comment = false;
		    		i++;
		    	}
	    		continue;
	    	}

	    	if(buffer[i] == '\n') {
	    		start = i + 1;
	    		line_num ++;
	    		if(down_bracket != 0) {
	    			throw new CodeException("Expecting ) or ]", line_num);
	    		}
	    		
	    		is_line_comment = false;
	    		if(line_char_num > 0 && root_tail_ptr.delim_type != AttrType.L_CURL_BRACE && 
	    				root_tail_ptr.delim_type != AttrType.R_CURL_BRACE &&
	    				root_tail_ptr.delim_type != AttrType.SEMI_COLON) {
	    			throw new CodeException("Expect ; ", line_num);
	    		}
	    		
	    		line_char_num = 0;
	    		continue;
	    	}
	    	
	    	if(is_line_comment == true) {
	    		continue;
	    	}

	    	if(buffer[i] == '/' && nch == '/') {
	    		is_line_comment = true;
	    		continue;
	    	}
	    	
	    	if(buffer[i] == '/' && nch == '*') {
	    		is_block_comment = true;
	    		continue;
	    	}
	    	
	    	if(IsDelimeter(i > 0 ? buffer[i - 1] : '&', buffer[i], nch) == true) {
	    		if(buffer[i] == '"') {
	    			while(buffer[++i] != '"');
	    		}
	    		
	    		if(buffer[i] == '\'') {
	    			while(buffer[++i] != '\'');
	    		}
	    		
	    		line_char_num++;
	    		continue;
	    	}
	    	
	    	AttrType att_type = DelimiterType(buffer, i);

	    	if(att_type != null || i - start > 0) {
		    	if(root_tail_ptr == null) {
		    		root_tail_ptr = new TextAttr();
		    	} else if(is_up == true) {
		    		stack.remove(stack.size() - 1);
		    		root_tail_ptr = stack.get(stack.size() - 1);
		    		root_tail_ptr.next_ptr = new TextAttr();
		    		root_tail_ptr.next_ptr.parent_ptr = root_tail_ptr.parent_ptr;
		    		root_tail_ptr = root_tail_ptr.next_ptr;
		    		is_up = false;
		    	} else if(is_down == true) {
		    		root_tail_ptr.child_ptr = new TextAttr();
		    		root_tail_ptr.child_ptr.parent_ptr = root_tail_ptr;
		    		root_tail_ptr = root_tail_ptr.child_ptr;
		    		stack.add(null);
		    		is_down = false;
		    	} else {
		    		root_tail_ptr.next_ptr = new TextAttr();
		    		root_tail_ptr.next_ptr.parent_ptr = root_tail_ptr.parent_ptr;
		    		root_tail_ptr = root_tail_ptr.next_ptr;
		    	}
		    	
		    	stack.set(stack.size() - 1, root_tail_ptr);
		    	if(root_head_ptr == null) {
		    		root_head_ptr = root_tail_ptr;
		    	}
	
		    	root_tail_ptr.delim_type = att_type;
		    	root_tail_ptr.attr = (new String(buffer)).substring(start, i);
		    	root_tail_ptr.line_number = line_num;
	    	}
	    	
	    	if(att_type == AttrType.L_CURL_BR || att_type == AttrType.L_CURL_BRACE 
	    			|| att_type == AttrType.L_SQUARE_BR) {
	    		
	    		if(att_type != AttrType.L_CURL_BRACE) {
	    			down_bracket++;
	    		}
	    		
	    		is_down = true;
	    	}
	    	
	    	if(att_type == AttrType.R_CURL_BR || att_type == AttrType.R_CURL_BRACE 
	    			|| att_type == AttrType.R_SQUARE_BR) {
	    		
	    		if(stack.size() < 2) {
	    			throw new CodeException("Not Expecting ), ] or }", line_num);
	    		}
	    		
	    		AttrType delim = stack.get(stack.size() - 2).delim_type;
	    		if(att_type == AttrType.R_CURL_BR == true && delim != AttrType.L_CURL_BR) {
	    			throw new CodeException("Expecting )", line_num);
	    		}
	    		
	    		if(att_type == AttrType.R_CURL_BRACE == true && delim != AttrType.L_CURL_BRACE) {
	    			throw new CodeException("Expecting }", line_num);
	    		}
	    		
	    		if(att_type == AttrType.R_SQUARE_BR == true && delim != AttrType.L_SQUARE_BR) {
	    			throw new CodeException("Expecting ]", line_num);
	    		}
	    		
	    		if(att_type != AttrType.R_CURL_BRACE) {
	    			down_bracket--;
	    		}
	    	
	    		is_up = true;
	    	}
	    	
	    	while(buffer[i] == ' ') {
	    		if(IsDelimeter(i > 0 ? buffer[i - 1] : '&', buffer[i + 1], buffer[i + 2]) == true) {
	    			break;
	    		}
	    		i++;
    		}
	    	
	    	start = i + delimiter_size;
	    	i += delimiter_size - 1;
	    	
	    	while(start < buffer.length - 1 && buffer[start] == ' ') {
	    		start++;
	    		i++;
	    	}
	    }
	    
	    return root_head_ptr;
	}
	
	// This prints the document
	public void PrintDocument(TextAttr ptr) throws IOException {
		
		while(ptr != null) {
			if(ptr.attr.length() > 0) {
				System.out.print(ptr.attr+" ");
			}
			
			if(ptr.child_ptr != null) {
				System.out.print("down");
				PrintDocument(ptr.child_ptr);
			}

			ptr = ptr.next_ptr;
		}
		
		
		System.out.println("up");
		
		
	}
	
	// This returns the root attribute 
	public TextAttr RootAttr() {
		return root_head_ptr;
	}
	

}
