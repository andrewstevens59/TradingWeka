package GUI;

import java.awt.* 
;  
import java.awt.event.* ;  

import javax.swing.* ;  
import javax.swing.event.DocumentListener;

import stateMachine.CodeException;
import stateMachine.ProcessStateMachine;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;

import optimizeTree.ConstructTree;

import java.io.* ;  
import java.util.ArrayList;
public class TextEditor extends JFrame implements ActionListener  
{  
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JMenu file, edit, exit ;  
    JMenuItem new1, open, save, edit1, font, exit1 ;  
    JTextArea textArea ;  
    JFileChooser chooser ;  
    FileInputStream fis ;  
    BufferedReader br ;  
    FileOutputStream fos ;  
    BufferedWriter bwriter ;  
    static TextEditor app ;  
    JTextArea code_error;
    static JTextArea lines;
    
    
    DrawGraph mainPanel;
    TextHighlight highlight = new TextHighlight();
 
    
    // This adds the line numbers to the document
    private void AddLineNumbers(JScrollPane scrollPane) {
    	
    	lines = new JTextArea(String.format("%05d", 1));
    	lines.setFont( new Font( "Arial" , Font.BOLD , 14 ) ) ; 
    	 
    	lines.setBackground(Color.LIGHT_GRAY);
		lines.setEditable(false);
 
		textArea.getDocument().addDocumentListener(new DocumentListener(){
			public String getText(){

				int caretPosition = textArea.getDocument().getLength();
				Element root = textArea.getDocument().getDefaultRootElement();
				String text = String.format("%05d", 1) + System.getProperty("line.separator");

				for(int i = 2; i < root.getElementIndex( caretPosition ) + 2; i++){
					text += String.format("%05d", i) + System.getProperty("line.separator");
				}
				return text;
			}
			@Override
			public void changedUpdate(DocumentEvent de) {
				lines.setText(getText());
			}
 
			@Override
			public void insertUpdate(DocumentEvent de) {
				lines.setText(getText());
			}
 
			@Override
			public void removeUpdate(DocumentEvent de) {
				lines.setText(getText());
			}
 
		});
 
		scrollPane.getViewport().add(textArea);
		scrollPane.setRowHeaderView(lines);
    }
    
    // This is used to create and optimize the state space
    private void GenerateStateSpace() throws IOException, CodeException {
    	
    	JFrame frame = new JFrame("DrawGraph");
    	mainPanel = new DrawGraph(textArea.getText());
       
    	Container c = frame.getContentPane() ;  
        c.setLayout( new BorderLayout() ) ;  

        JButton button = new JButton("Done");
        button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            	
            	mainPanel.TerminateTimer();
            	
            	try {
					mainPanel.ConstructOptimalPath();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (CodeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            	
            	mainPanel.repaint();
            }
        });
        
        JMenuBar bar = new JMenuBar();  
        bar.add(button);
        c.add(bar , BorderLayout.NORTH) ; 
        c.add(mainPanel , BorderLayout.SOUTH) ;
        
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
            	mainPanel.TerminateTimer();
            }
          });
        
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
      
        
    }
    
    public TextEditor() throws IOException, CodeException  
    {  
        super( "SimOp" ) ;  
    
        Container c = getContentPane() ;  
        c.setLayout( new BorderLayout() ) ;  
        
        JToolBar toolBar = new JToolBar();
        
        JButton button = new JButton("Run");
        button.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
           
            	code_error.setText("");
            	
            	try {
       
					try {
						GenerateStateSpace();
					} catch (CodeException e1) {
						int line = e1.getLine() + 1;
						code_error.setText(e1.getError()+", Line: "+line);
						
						DefaultHighlighter.DefaultHighlightPainter highlightPainter = 
						        new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
						 try {
							lines.getHighlighter().addHighlight(7 * (line - 1), 7 * line, 
								            highlightPainter);
						} catch (BadLocationException e11) {
							// TODO Auto-generated catch block
							e11.printStackTrace();
						}
					 
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            	
            }
        });
        
        toolBar.add(button);
        
        
        JMenuBar bar = new JMenuBar() ;  
        bar.setFont( new Font( "Arial" , Font.BOLD , 14 ) ) ;  
        file = new JMenu(" File ") ;  
        new1 = new JMenuItem( " New     ") ;  
        new1.addActionListener( this ) ;  
        open = new JMenuItem( " Open... ") ;  
        open.addActionListener( this ) ;  
        save = new JMenuItem( " Save... ") ;  
        save.addActionListener( this ) ;  
        file.add( new1 ) ;  
        file.add( open ) ;  
        file.add( save ) ;       
        bar.add(  file ) ;  
        edit  = new JMenu(" Edit ") ;  
        edit1 = new JMenuItem( " Edit... ") ;  
        font  = new JMenuItem( " Font... ") ;  
        edit.add( edit1 ) ;  
        edit.add( font ) ;  
        bar.add( edit ) ;  
        exit = new JMenu(" Exit ") ;  
        exit1 = new JMenuItem( "Exit Your App" ) ;  
         
        exit.add( exit1 ) ;  
        bar.add( exit ) ;  
        
        bar.add(button);
        c.add( bar , BorderLayout.NORTH) ;   
        
        UIManager.put("TextArea.margin", new Insets(10,10,10,10));
        textArea = new JTextArea( 20 , 18 ) ;   
        textArea.setFont( new Font( "Arial" , Font.BOLD , 14 ) ) ;  
  
        

        JScrollPane scrollPane = new JScrollPane( textArea ) ;  
        AddLineNumbers(scrollPane);
        c.add( scrollPane , BorderLayout.CENTER ) ;  
        
        code_error = new JTextArea( 6 , 18 ) ;   
        code_error.setFont( new Font( "Arial" , Font.BOLD , 14 ) ) ;  
        scrollPane = new JScrollPane( code_error ) ; 
        c.add( scrollPane , BorderLayout.PAGE_END ) ; 
        scrollPane.revalidate() ;  
        setVisible( true ) ;  
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height) ;  

    }  
    
    public void actionPerformed( ActionEvent event )    
    {  
        Object obj = event.getSource() ;  
        chooser = new JFileChooser("./") ;  
        if ( chooser.showOpenDialog( app ) ==   
                 JFileChooser.APPROVE_OPTION )  
        if ( obj == open )   
        {  
            try  
            {  
                fis = new FileInputStream(   
                      chooser.getSelectedFile() ) ;  
                br  = new BufferedReader(   
                      new InputStreamReader( fis ) ) ;  
                String read ;  
                StringBuffer text = new StringBuffer() ;  
                while( ( read = br.readLine() ) != null )   
                {  
                   text.append( read ).append( "\n" ) ;  
                }  
                textArea.setText( text.toString().replace("	", "        ") ) ;  
            }  
            catch( IOException e )   
            {  
                JOptionPane.showMessageDialog( this ,   
                    "Error in File Operation" ,  
                    "Error in File Operation" ,   
                    JOptionPane.INFORMATION_MESSAGE) ;  
            }  
        }  
    }  
    public static void main( String[] args ) throws IOException, CodeException   
    {  
        app = new TextEditor() ;  
          
        app.setDefaultCloseOperation( EXIT_ON_CLOSE ) ;  

    }  
} 
