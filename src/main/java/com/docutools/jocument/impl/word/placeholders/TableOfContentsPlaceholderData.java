package com.docutools.jocument.impl.word.placeholders;

import com.docutools.jocument.impl.word.CustomWordPlaceholderData;
import com.docutools.jocument.impl.word.WordUtilities;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STOnOff;

public class TableOfContentsPlaceholderData extends CustomWordPlaceholderData {
    @Override
    protected void transform(IBodyElement placeholder, XWPFDocument document) {
        var paragraph = document.insertNewParagraph(WordUtilities.openCursor(placeholder).orElseThrow());
        var toc = paragraph.getCTP().addNewFldSimple();
        toc.setInstr("TOC \\h");
        toc.addNewR().addNewT().setStringValue("Table of contents (Please refresh)"); // https://stackoverflow.com/questions/61816416/apache-poi-table-of-contents-field-is-not-processed-correctly
        toc.setDirty(STOnOff.TRUE);
        document.enforceUpdateFields();
        WordUtilities.removeIfExists(placeholder);

    }
}
