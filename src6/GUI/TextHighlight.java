package GUI;

import java.awt.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.Highlighter.HighlightPainter;

public class TextHighlight
{
    private Highlighter.HighlightPainter redPainter;
    private Highlighter.HighlightPainter orangePainter;
    private Highlighter.HighlightPainter cyanPainter;
    private Highlighter.HighlightPainter bluePainter;
    // This stores the mapping between keywords and color codes
    HashMap<String, HighlightPainter> keyword_map = new HashMap<String, HighlightPainter>();

    public TextHighlight()
    {
    	
    	
        redPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.RED);
        orangePainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
        cyanPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);
        
    }

    // This is used to apply syntax highlighting to the text
    public void highlightText(JTextArea tarea) {
    	

    	int start = 0;
        String text = tarea.getText();
        Highlighter highlighter = tarea.getHighlighter();
        
        for(int i=0; i<text.length(); i++) {
        	
        	if(text.charAt(i) >= 'a' && text.charAt(i) <= 'z') {
        		continue;
        	}
        	
        	if(text.substring(start, i).equals("if")) {
        		
        		try {
    				highlighter.addHighlight(start, i, bluePainter);
    			} catch (BadLocationException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        	}
        	
        	start = i + 1;
        }

    }

}
