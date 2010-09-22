package com.google.refine.importers.parsers;

import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.importers.ImporterUtilities;
import com.google.refine.model.Cell;
import com.google.refine.model.Row;

public class SeparatorRowParser extends RowParser {

    String sep;
    
    public SeparatorRowParser(String sep) {
        this.sep = sep;
    }
    
    public List<String> split(String line, LineNumberReader lineReader) {
        String[] cells = StringUtils.splitPreserveAllTokens(line, sep);
        
        List<String> results = new ArrayList<String>();
        for (int c = 0; c < cells.length; c++) {
            results.add(cells[c]);
        }
        
        return results;
    }
    
    public boolean parseRow(Row row, String line, boolean guessValueType, LineNumberReader lineReader) {
        boolean hasData = false;
        
        String[] cells = StringUtils.splitPreserveAllTokens(line, sep);
        for (int c = 0; c < cells.length; c++) {
            String text = cells[c];
            
            Serializable value = guessValueType ? ImporterUtilities.parseCellValue(text) : text;
            if (ExpressionUtils.isNonBlankData(value)) {
                row.cells.add(new Cell(value, null));
                hasData = true;
            } else {
                row.cells.add(null);
            }
        }
        return hasData;
    }    
    
}
