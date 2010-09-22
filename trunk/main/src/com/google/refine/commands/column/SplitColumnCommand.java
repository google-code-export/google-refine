package com.google.refine.commands.column;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.column.ColumnSplitOperation;
import com.google.refine.util.ParsingUtilities;

public class SplitColumnCommand extends EngineDependentCommand {
    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, JSONObject engineConfig) throws Exception {
        
        String columnName = request.getParameter("columnName");
        boolean guessCellType = Boolean.parseBoolean(request.getParameter("guessCellType"));
        boolean removeOriginalColumn = Boolean.parseBoolean(request.getParameter("removeOriginalColumn"));
        String mode = request.getParameter("mode");
        if ("separator".equals(mode)) {
            String maxColumns = request.getParameter("maxColumns");
            
            return new ColumnSplitOperation(
                engineConfig, 
                columnName, 
                guessCellType,
                removeOriginalColumn,
                request.getParameter("separator"),
                Boolean.parseBoolean(request.getParameter("regex")),
                maxColumns != null && maxColumns.length() > 0 ? Integer.parseInt(maxColumns) : 0
            );
        } else {
            String s = request.getParameter("fieldLengths");
            
            JSONArray a = ParsingUtilities.evaluateJsonStringToArray(s);
            int[] fieldLengths = new int[a.length()];
            
            for (int i = 0; i < fieldLengths.length; i++) {
                fieldLengths[i] = a.getInt(i);
            }
            
            return new ColumnSplitOperation(
                engineConfig, 
                columnName, 
                guessCellType,
                removeOriginalColumn,
                fieldLengths
            );
        }
    }
}
