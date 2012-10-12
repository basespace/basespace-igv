/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.ui.panel;


import org.broad.igv.lists.GeneList;
import org.broad.igv.ui.IGV;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.util.*;

/**
 * The drag & drop code was modified from the excellent example of Bryan E. Smith.
 *
 * @author jrobinso
 * @date Sep 10, 2010
 */
public class HeaderPanelContainer extends JPanel implements Paintable {

    private Collection<ReferenceFrame> frames = new ArrayList();

    JPanel contentPanel;

    public HeaderPanelContainer() {

        this.setLayout(new BorderLayout()); //new DataPanelLayout());
        init();
        createHeaderPanels();

    }

    public void init() {
        this.setTransferHandler(new DragAndDropTransferHandler());

        // Create the listener to do the work when dropping on this object!
        this.setDropTarget(new DropTarget(this, new HeaderDropTargetListener(this)));

    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (contentPanel != null) {
            for (Component c : contentPanel.getComponents()) {
                if (c instanceof HeaderPanel) {
                    c.setBackground(color);
                }
            }
        }
    }

    public void createHeaderPanels() {

        removeAll();
        frames = FrameManager.getFrames();

        contentPanel = new JPanel();
        contentPanel.setLayout(new DataPanelLayout());
        for (ReferenceFrame f : frames) {
            HeaderPanel dp = new HeaderPanel(f);
            dp.setBackground(getBackground());
            contentPanel.add(dp);
        }
        add(contentPanel, BorderLayout.CENTER);

        if (FrameManager.isGeneListMode()) {
            GeneList gl = IGV.getInstance().getSession().getCurrentGeneList();
            String name = gl.getDisplayName();
            JLabel label = new JLabel(name, JLabel.CENTER);
            Border border = BorderFactory.createLineBorder(Color.lightGray);
            label.setBorder(border);
            add(label, BorderLayout.NORTH);
        }

        invalidate();
    }

    public void paintOffscreen(Graphics2D g, Rectangle rect) {
       paint(g);
    }
}


/**
 * <p>Listens for drops and performs the updates.</p>
 * <p>The real magic behind the drop!</p>
 */
class HeaderDropTargetListener implements DropTargetListener {

    private final HeaderPanelContainer rootPanel;

    /**
     * <p>Two cursors with which we are primarily interested while dragging:</p>
     * <ul>
     * <li>Cursor for droppable condition</li>
     * <li>Cursor for non-droppable consition</li>
     * </ul>
     * <p>After drop, we manually change the cursor back to default, though does this anyhow -- just to be complete.</p>
     */
    //private static final Cursor droppableCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    //private static final Cursor notDroppableCursor = Cursor.getDefaultCursor();
    public HeaderDropTargetListener(HeaderPanelContainer sheet) {
        this.rootPanel = sheet;
    }

    // Could easily find uses for these, like cursor changes, etc.

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
        //  if (!this.rootPanel.getCursor().equals(droppableCursor)) {
        //      this.rootPanel.setCursor(droppableCursor);
        //  }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
        //  this.rootPanel.setCursor(notDroppableCursor);
    }

    /**
     * <p>The user drops the item. Performs the drag and drop calculations and layout.</p>
     *
     * @param dtde
     */
    public void drop(DropTargetDropEvent dtde) {

        // Done with cursors, dropping
        //this.rootPanel.setCursor(Cursor.getDefaultCursor());

        // Just going to grab the expected DataFlavor to make sure
        // we know what is being dropped
        DataFlavor dragAndDropPanelFlavor = null;
        Object transferableObj = null;

        try {
            // Grab expected flavor
            dragAndDropPanelFlavor = HeaderPanel.getDragAndDropPanelDataFlavor();

            Transferable transferable = dtde.getTransferable();

            // What does the Transferable support
            if (transferable.isDataFlavorSupported(dragAndDropPanelFlavor)) {
                transferableObj = dtde.getTransferable().getTransferData(dragAndDropPanelFlavor);
            }

        } catch (Exception ex) { /* nope, not the place */ }

        // If didn't find an item, bail
        if (transferableObj == null) {
            return;
        }

        // Cast it to the panel. By this point, we have verified it is a HeaderPanel
        HeaderPanel droppedPanel = (HeaderPanel) transferableObj;
        ReferenceFrame droppedFrame = droppedPanel.frame;

        // Get the y offset from the top of the WorkFlowSheetPanel
        // for the drop option (the cursor on the drop)
        final int dropXLoc = dtde.getLocation().x;


        // Find the index for the drop
        Collection<ReferenceFrame> panels = FrameManager.getFrames();
        java.util.List<ReferenceFrame> orderedPanels = new ArrayList(panels.size());
        panels.remove(droppedFrame);

        boolean dropAdded = false;


        for (ReferenceFrame frame : panels) {
            if (frame.getMidpoint() > dropXLoc && !dropAdded) {
                orderedPanels.add(droppedFrame);
                dropAdded = true;
            }
            orderedPanels.add(frame);
        }
        if (!dropAdded) {
            orderedPanels.add(droppedFrame);
        }


        // Request relayout contents, or else won't update GUI following drop.
        // Will add back in the order to which we just sorted
        FrameManager.setFrames(orderedPanels);
        IGV.getInstance().resetFrames();
    }
} // HeaderDropTargetListener



