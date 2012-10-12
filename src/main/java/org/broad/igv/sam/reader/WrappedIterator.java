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

package org.broad.igv.sam.reader;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;
import org.broad.igv.sam.Alignment;
import org.broad.igv.sam.SamAlignment;

/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: Sep 22, 2009
 * Time: 2:32:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class WrappedIterator implements CloseableIterator<Alignment> {

    CloseableIterator<SAMRecord> iter;

    WrappedIterator(CloseableIterator<SAMRecord> iter) {
        this.iter = iter;
    }

    public void close() {
        iter.close();
    }

    public boolean hasNext() {
        return iter.hasNext();
    }

    public Alignment next() {
        return new SamAlignment(iter.next());
    }

    public void remove() {
        iter.remove();
    }
}
