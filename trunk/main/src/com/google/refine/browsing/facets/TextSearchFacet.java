package com.google.refine.browsing.facets;

import java.util.Properties;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionStringComparisonRowFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.ast.VariableExpr;
import com.google.refine.model.Project;

public class TextSearchFacet implements Facet {
    /*
     *  Configuration
     */
    protected String     _name;
    protected String     _columnName;
    protected String     _query;
    protected String     _mode;
    protected boolean    _caseSensitive;
    
    /*
     *  Derived configuration
     */
    protected int        _cellIndex;
    protected Pattern    _pattern;
    
    public TextSearchFacet() {
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("columnName"); writer.value(_columnName);
        writer.key("query"); writer.value(_query);
        writer.key("mode"); writer.value(_mode);
        writer.key("caseSensitive"); writer.value(_caseSensitive);
        writer.endObject();
    }

    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");
        _columnName = o.getString("columnName");
        
        _cellIndex = project.columnModel.getColumnByName(_columnName).getCellIndex();
        
        if (!o.isNull("query")) {
            _query = o.getString("query"); 
        }
        
        _mode = o.getString("mode");
        _caseSensitive = o.getBoolean("caseSensitive");
        if (_query != null) {
            if ("regex".equals(_mode)) {
                try {
                    _pattern = Pattern.compile(
                    		_query, 
                    		_caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                } catch (java.util.regex.PatternSyntaxException e) {
                    e.printStackTrace();
                }
            } else if (!_caseSensitive) {
        		_query = _query.toLowerCase();
            }
        }
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        if (_query == null || _query.length() == 0) {
            return null;
        } else if ("regex".equals(_mode) && _pattern == null) {
            return null;
        }
        
        Evaluable eval = new VariableExpr("value");
        
        if ("regex".equals(_mode)) {
            return new ExpressionStringComparisonRowFilter(eval, _columnName, _cellIndex) {
                protected boolean checkValue(String s) {
                    return _pattern.matcher(s).find();
                };
            };
        } else {
            return new ExpressionStringComparisonRowFilter(eval, _columnName, _cellIndex) {
                protected boolean checkValue(String s) {
                    return (_caseSensitive ? s : s.toLowerCase()).contains(_query);
                };
            };
        }        
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
    	RowFilter rowFilter = getRowFilter(project);
    	return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        // nothing to do
    }
    
    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
    	// nothing to do
    }
}
