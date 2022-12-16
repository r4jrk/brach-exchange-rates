package kursy;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Drukowanie implements Printable {
    
    private static final String FONT_NAME = "Courier New";
    private static final int FONT_SIZE = 8;
    
    private final String printData;
    
    public Drukowanie(String printDataIn){
        this.printData = printDataIn;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        //Tylko jedna strona
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setFont(new Font(FONT_NAME, 0, FONT_SIZE));
        
        int lineHeight = FONT_SIZE;

        BufferedReader br = new BufferedReader(new StringReader(printData));

        //Stwórz stronę do druku
        try {
            String line = br.readLine();
            int y = 0;
            while (line != null) {
                y += lineHeight;
                g2d.drawString(line, 0, y);
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        
        return PAGE_EXISTS;
    }

}
