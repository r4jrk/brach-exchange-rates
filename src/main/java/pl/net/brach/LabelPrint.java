package pl.net.brach;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;

public class LabelPrint implements Printable {
    //Set the label dimensions in printer's properties to: 50 mm x 30 mm. Margins: 0 mm

    private static final String FONT_NAME = "Courier New";
    private static final int FONT_SIZE = 8;
    static final float PRINT_PAGE_HEIGHT = 60;
    static final float PRINT_PAGE_WIDTH = 80;

    private final ArrayList<String> labelText;

    public LabelPrint(ArrayList<String> labelText) {
        this.labelText = labelText;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        //One page only
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));

        int loopCount = 0;

        String line = labelText.get(loopCount++);

        while (loopCount != labelText.size()) {
            g2d.drawString(line, 0, loopCount * FONT_SIZE);

            line = labelText.get(loopCount);

            loopCount++;
        }
        return PAGE_EXISTS;
    }
}
