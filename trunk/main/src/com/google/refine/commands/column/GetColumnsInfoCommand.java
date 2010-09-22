package com.google.refine.commands.column;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.browsing.util.ExpressionBasedRowEvaluable;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.browsing.util.NumericBinRowIndex;
import com.google.refine.commands.Command;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public class GetColumnsInfoCommand extends Command {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            //long start = System.currentTimeMillis();

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            Project project = getProject(request);
            //Engine engine = getEngine(request, project);
            
            JSONWriter writer = new JSONWriter(response.getWriter());

            writer.array();
            for (Column column : project.columnModel.columns) {
                writer.object();
                    write(project, column, writer);
                writer.endObject();
            }
            writer.endArray();
            
            //Gridworks.log("Obtained columns info in " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
            respondException(response, e);
        }
    }
    
    private NumericBinIndex getBinIndex(Project project, Column column) {
        String expression = "value";
        String key = "numeric-bin:" + expression;
        Evaluable eval = null;
        try {
            eval = MetaParser.parse(expression);
        } catch (ParsingException e) {
            // this should never happen
        }
        NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
        if (index == null) {
            index = new NumericBinRowIndex(project, new ExpressionBasedRowEvaluable(column.getName(), column.getCellIndex(), eval));
            column.setPrecompute(key, index);
        }
        return index;
    }
    
    private void write(Project project, Column column, JSONWriter writer) throws JSONException {
        NumericBinIndex columnIndex = getBinIndex(project, column);
        if (columnIndex != null) {
            writer.key("name");
            writer.value(column.getName());
            boolean is_numeric = columnIndex.isNumeric();
            writer.key("is_numeric");
            writer.value(is_numeric);
            writer.key("numeric_row_count");
            writer.value(columnIndex.getNumericRowCount());
            writer.key("non_numeric_row_count");
            writer.value(columnIndex.getNonNumericRowCount());
            writer.key("error_row_count");
            writer.value(columnIndex.getErrorRowCount());
            writer.key("blank_row_count");
            writer.value(columnIndex.getBlankRowCount());
            if (is_numeric) {
                writer.key("min");
                writer.value(columnIndex.getMin());
                writer.key("max");
                writer.value(columnIndex.getMax());
                writer.key("step");
                writer.value(columnIndex.getStep());
            }
        } else {
            writer.key("error");
            writer.value("error finding numeric information on the '" + column.getName() + "' column");
        }
    }
}
