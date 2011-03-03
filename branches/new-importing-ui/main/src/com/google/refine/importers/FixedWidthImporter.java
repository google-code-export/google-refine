package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class FixedWidthImporter extends TabularImportingParserBase {
    public FixedWidthImporter() {
        super(false);
    }
    
    @Override
    public JSONObject createParserUIInitializationData(
            ImportingJob job, List<JSONObject> fileRecords, String format) {
        JSONObject options = super.createParserUIInitializationData(job, fileRecords, format);
        
        JSONUtilities.safePut(options, "lineSeparator", "\n");
        JSONUtilities.safePut(options, "columnWidths", new JSONArray());
        JSONUtilities.safePut(options, "guessCellValueTypes", true);
        
        return options;
    }

    @Override
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        Reader reader,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) {
        // String lineSeparator = JSONUtilities.getString(options, "lineSeparator", "\n");
        final int[] columnWidths = JSONUtilities.getIntArray(options, "columnWidths");
        
        final LineNumberReader lnReader = new LineNumberReader(reader);
        
        DataReader dataReader = new DataReader() {
            @Override
            public List<Object> getNextRowOfCells() throws IOException {
                String line = lnReader.readLine();
                if (line == null) {
                    return null;
                } else {
                    return getCells(line, columnWidths);
                }
            }
        };
        
        parseOneFile(project, metadata, job, dataReader, fileSource, limit, options, exceptions);
    }
    
    /**
     * Splits the line into columns
     * @param line
     * @param lnReader
     * @param splitIntoColumns
     * @return
     */
    static private ArrayList<Object> getCells(String line, int[] widths) {
        ArrayList<Object> cells = new ArrayList<Object>();
        
        int columnStartCursor = 0;
        int columnEndCursor = 0;
        for (int width : widths) {
            if (columnStartCursor >= line.length()) {
                cells.add(null); //FIXME is adding a null cell (to represent no data) OK?
                continue;
            }
            
            columnEndCursor = columnStartCursor + width;
            
            if (columnEndCursor > line.length()) {
                columnEndCursor = line.length();
            }
            if (columnEndCursor <= columnStartCursor) {
                cells.add(null); //FIXME is adding a null cell (to represent no data, or a zero width column) OK? 
                continue;
            }
            
            cells.add(line.substring(columnStartCursor, columnEndCursor));
            
            columnStartCursor = columnEndCursor;
        }
        return cells;
    }
    
    static public int[] guessColumnWidths(File file, String encoding) {
        try {
            InputStream is = new FileInputStream(file);
            try {
                Reader reader = encoding != null ? new InputStreamReader(is, encoding) : new InputStreamReader(is);
                LineNumberReader lineNumberReader = new LineNumberReader(reader);
                
                int[] counts = null;
                int totalBytes = 0;
                int lineCount = 0;
                String s;
                while (totalBytes < 64 * 1024 &&
                       lineCount < 100 &&
                       (s = lineNumberReader.readLine()) != null) {
                    
                    totalBytes += s.length() + 1; // count the new line character
                    if (s.length() == 0) {
                        continue;
                    }
                    lineCount++;
                    
                    if (counts == null) {
                        counts = new int[s.length()];
                        for (int c = 0; c < counts.length; c++) {
                            counts[c] = 0;
                        }
                    }
                    
                    for (int c = 0; c < counts.length && c < s.length(); c++) {
                        char ch = s.charAt(c);
                        if (ch == ' ') {
                            counts[c]++;
                        }
                    }
                }
                
                if (counts != null) {
                    List<Integer> widths = new ArrayList<Integer>();
                    
                    int startIndex = 0;
                    for (int c = 0; c < counts.length; c++) {
                        int count = counts[c];
                        if (count == lineCount && c > startIndex) {
                            widths.add(c - startIndex + 1);
                            startIndex = c + 1;
                        }
                    }
                    
                    for (int i = widths.size() - 1; i > 0; i--) {
                        if (widths.get(i) == 1) {
                            widths.remove(i);
                            widths.set(i - 1, widths.get(i - 1) + 1);
                        }
                    }
                    
                    int[] widthA = new int[widths.size()];
                    for (int i = 0; i < widthA.length; i++) {
                        widthA[i] = widths.get(i);
                    }
                    return widthA;
                }
            } finally {
                is.close();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
