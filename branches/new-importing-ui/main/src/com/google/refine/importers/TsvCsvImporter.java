/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import au.com.bytecode.opencsv.CSVParser;

import com.google.refine.ProjectMetadata;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingParser;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;

public class TsvCsvImporter implements ReaderImporter,StreamImporter, ImportingParser {
    
    @Override
    public void read(Reader reader, Project project, ProjectMetadata metadata, Properties options) throws ImportException {
        boolean splitIntoColumns = ImporterUtilities.getBooleanOption("split-into-columns", options, true);
        
        String sep = options.getProperty("separator"); // auto-detect if not present
        int ignoreLines = ImporterUtilities.getIntegerOption("ignore", options, -1);
        int headerLines = ImporterUtilities.getIntegerOption("header-lines", options, 1);

        int limit = ImporterUtilities.getIntegerOption("limit",options,-1);
        int skip = ImporterUtilities.getIntegerOption("skip",options,0);
        boolean guessValueType = ImporterUtilities.getBooleanOption("guess-value-type", options, true);
        boolean ignoreQuotes = ImporterUtilities.getBooleanOption("ignore-quotes", options, false);

        LineNumberReader lnReader = new LineNumberReader(reader);
        
        try {
            read(lnReader, project, sep,
                limit, skip, ignoreLines, headerLines,
                guessValueType, splitIntoColumns, ignoreQuotes
            );
        } catch (IOException e) {
            throw new ImportException("Import failed",e);
        }
    }

    /**
     *
     * @param lnReader
     *           LineNumberReader used to read file or string contents
     * @param project
     *           The project into which the parsed data will be added
     * @param sep
     *           The character used to denote different the break between data points
     * @param limit
     *           The maximum number of rows of data to import
     * @param skip
     *           The number of initial data rows to skip
     * @param ignoreLines
     *           The number of initial lines within the data source which should be ignored entirely
     * @param headerLines
     *           The number of lines in the data source which describe each column
     * @param guessValueType
     *           Whether the parser should try and guess the type of the value being parsed
     * @param splitIntoColumns
     *           Whether the parser should try and split the data source into columns
     * @param ignoreQuotes
     *           Quotation marks are ignored, and all separators and newlines treated as such regardless of whether they are within quoted values
     * @throws IOException
     */
    public void read(LineNumberReader lnReader, Project project, String sep, int limit, int skip, int ignoreLines, int headerLines, boolean guessValueType, boolean splitIntoColumns, boolean ignoreQuotes ) throws IOException{
        CSVParser parser = (sep != null && sep.length() > 0 && splitIntoColumns) ?
                        new CSVParser(sep.toCharArray()[0],//HACK changing string to char - won't work for multi-char separators.
                                CSVParser.DEFAULT_QUOTE_CHARACTER,
                                (char) 0, // escape character
                                CSVParser.DEFAULT_STRICT_QUOTES,
                                CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                                ignoreQuotes) : null;
        List<String> columnNames = new ArrayList<String>();
        String line = null;
        int rowsWithData = 0;

        while ((line = lnReader.readLine()) != null) {
            if (ignoreLines > 0) {
                ignoreLines--;
                continue;
            } else if (StringUtils.isBlank(line)) {
                continue;
            }

            //guess separator
            if (parser == null) {
                int tab = line.indexOf('\t');
                if (tab >= 0) {
                    parser = new CSVParser('\t',
                            CSVParser.DEFAULT_QUOTE_CHARACTER,
                            (char) 0, // escape character
                            CSVParser.DEFAULT_STRICT_QUOTES,
                            CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                            ignoreQuotes);
                } else {
                    parser = new CSVParser(',',
                            CSVParser.DEFAULT_QUOTE_CHARACTER,
                            (char) 0, // escape character
                            CSVParser.DEFAULT_STRICT_QUOTES,
                            CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                            ignoreQuotes);
                }
            }


            if (headerLines > 0) {
                //column headers
                headerLines--;

                ArrayList<String> cells = getCells(line, parser, lnReader, splitIntoColumns);

                for (int c = 0; c < cells.size(); c++) {
                    String cell = cells.get(c).trim();
                    //add column even if cell is blank
                    ImporterUtilities.appendColumnName(columnNames, c, cell);
                }
            } else {
                //data
                Row row = new Row(columnNames.size());

                ArrayList<String> cells = getCells(line, parser, lnReader, splitIntoColumns);

                if( cells != null && cells.size() > 0 )
                    rowsWithData++;

                if (skip <=0  || rowsWithData > skip){
                    //add parsed data to row
                    for(String s : cells){
                        if (ExpressionUtils.isNonBlankData(s)) {
                            Serializable value = guessValueType ? ImporterUtilities.parseCellValue(s) : s;
                            row.cells.add(new Cell(value, null));
                        }else{
                            row.cells.add(null);
                        }
                    }
                    project.rows.add(row);
                    project.columnModel.setMaxCellIndex(row.cells.size());

                    ImporterUtilities.ensureColumnsInRowExist(columnNames, row);

                    if (limit > 0 && project.rows.size() >= limit) {
                        break;
                    }
                }
            }
        }

        ImporterUtilities.setupColumns(project, columnNames);
    }

    protected ArrayList<String> getCells(String line, CSVParser parser, LineNumberReader lnReader, boolean splitIntoColumns) throws IOException{
        ArrayList<String> cells = new ArrayList<String>();
        if(splitIntoColumns){
            String[] tokens = parser.parseLineMulti(line);
            for(String s : tokens){
                cells.add(s);
            }
            while(parser.isPending()){
                tokens = parser.parseLineMulti(lnReader.readLine());
                for(String s : tokens){
                    cells.add(s);
                }
            }
        }else{
            cells.add(line);
        }
        return cells;
    }

    @Override
    public void read(InputStream inputStream, Project project,
            ProjectMetadata metadata, Properties options) throws ImportException {
        read(new InputStreamReader(inputStream), project, metadata, options);
    }

    @Override
    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
            return
                "text/plain".equals(contentType) ||
                "text/csv".equals(contentType) ||
                "text/x-csv".equals(contentType) ||
                "text/tab-separated-value".equals(contentType);
            
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".tsv")) {
                return true;
            }else if (fileName.endsWith(".csv")){
                return true;
            }
        }
        return false;
    }

    @Override
    public JSONObject createParserUIInitializationData(ImportingJob job,
            List<JSONObject> fileRecords, String format) {
        JSONObject options = new JSONObject();
        
        JSONUtilities.safePut(options, "lineSeparator", "\n");
        if ("text/line-based/*sv/csv".equals(format)) {
            JSONUtilities.safePut(options, "separator", ",");
            JSONUtilities.safePut(options, "splitIntoColumn", true);
        } else if ("text/line-based/*sv/tsv".equals(format)) {
            JSONUtilities.safePut(options, "separator", "\t");
            JSONUtilities.safePut(options, "splitIntoColumn", true);
        } else {
            JSONUtilities.safePut(options, "splitIntoColumn", false);
        }
        JSONUtilities.safePut(options, "headerLines", 1);
        JSONUtilities.safePut(options, "guessCellValue", true);
        JSONUtilities.safePut(options, "includeFileName", fileRecords.size() > 1);
        
        return options;
    }

    @Override
    public void parse(Project project, ProjectMetadata metadata,
            ImportingJob job, List<JSONObject> fileRecords, String format,
            int limit, JSONObject options, List<Exception> exceptions) {
        // TODO Auto-generated method stub
        
    }
}
