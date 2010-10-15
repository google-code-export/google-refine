package com.google.refine.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionNumberComparisonRowFilter;
import com.google.refine.browsing.util.ExpressionBasedRowEvaluable;
import com.google.refine.browsing.util.ExpressionNumericValueBinner;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.browsing.util.NumericBinRecordIndex;
import com.google.refine.browsing.util.NumericBinRowIndex;
import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class RangeFacet implements Facet {
    /*
     * Configuration, from the client side
     */
    protected String     _name;       // name of facet
    protected String     _expression; // expression to compute numeric value(s) per row
    protected String     _columnName; // column to base expression on, if any
    
    protected double    _from; // the numeric selection
    protected double    _to;
    
    protected boolean   _selectNumeric; // whether the numeric selection applies, default true
    protected boolean   _selectNonNumeric;
    protected boolean   _selectBlank;
    protected boolean   _selectError;
    
    /*
     * Derived configuration data
     */
    protected int        _cellIndex;
    protected Evaluable  _eval;
    protected String     _errorMessage;
    protected boolean    _selected; // false if we're certain that all rows will match
                                    // and there isn't any filtering to do
    
    /*
     * Computed data, to return to the client side
     */
    protected double    _min;
    protected double    _max;
    protected double    _step;
    protected int[]     _baseBins;
    protected int[]     _bins;
    
    protected int       _baseNumericCount;
    protected int       _baseNonNumericCount;
    protected int       _baseBlankCount;
    protected int       _baseErrorCount;
    
    protected int       _numericCount;
    protected int       _nonNumericCount;
    protected int       _blankCount;
    protected int       _errorCount;
    
    public RangeFacet() {
    }

    protected static final String MIN = "min";
    protected static final String MAX = "max";
    protected static final String TO = "to";
    protected static final String FROM = "from";
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("expression"); writer.value(_expression);
        writer.key("columnName"); writer.value(_columnName);
        
        if (_errorMessage != null) {
            writer.key("error"); writer.value(_errorMessage);
        } else {
            if (!Double.isInfinite(_min) && !Double.isInfinite(_max)) {
                writer.key(MIN); writer.value(_min);
                writer.key(MAX); writer.value(_max);
                writer.key("step"); writer.value(_step);
                
                writer.key("bins"); writer.array();
                for (int b : _bins) {
                    writer.value(b);
                }
                writer.endArray();
                
                writer.key("baseBins"); writer.array();
                for (int b : _baseBins) {
                    writer.value(b);
                }
                writer.endArray();
                
                writer.key(FROM); writer.value(_from);
                writer.key(TO); writer.value(_to);
            } else {
                writer.key("error"); writer.value("No numeric value present.");
            }
            
            writer.key("baseNumericCount"); writer.value(_baseNumericCount);
            writer.key("baseNonNumericCount"); writer.value(_baseNonNumericCount);
            writer.key("baseBlankCount"); writer.value(_baseBlankCount);
            writer.key("baseErrorCount"); writer.value(_baseErrorCount);
            
            writer.key("numericCount"); writer.value(_numericCount);
            writer.key("nonNumericCount"); writer.value(_nonNumericCount);
            writer.key("blankCount"); writer.value(_blankCount);
            writer.key("errorCount"); writer.value(_errorCount);
        }
        writer.endObject();
    }

    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");
        _expression = o.getString("expression");
        _columnName = o.getString("columnName");
        
        if (_columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        try {
            _eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }
        
        if (o.has(FROM) || o.has(TO)) {
            _from = o.has(FROM) ? o.getDouble(FROM) : _min;
            _to = o.has(TO) ? o.getDouble(TO) : _max;
            _selected = true;
        }
        
        _selectNumeric = JSONUtilities.getBoolean(o, "selectNumeric", true);
        _selectNonNumeric = JSONUtilities.getBoolean(o, "selectNonNumeric", true);
        _selectBlank = JSONUtilities.getBoolean(o, "selectBlank", true);
        _selectError = JSONUtilities.getBoolean(o, "selectError", true);
        
        if (!_selectNumeric || !_selectNonNumeric || !_selectBlank || !_selectError) {
            _selected = true;
        }
    }

    public RowFilter getRowFilter(Project project) {
        if (_eval != null && _errorMessage == null && _selected) {
            return new ExpressionNumberComparisonRowFilter(
        		getRowEvaluable(project), _selectNumeric, _selectNonNumeric, _selectBlank, _selectError) {
                
                protected boolean checkValue(double d) {
                    return d >= _from && d < _to;
                };
            };
        } else {
            return null;
        }
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
    	RowFilter rowFilter = getRowFilter(project);
    	return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (_eval != null && _errorMessage == null) {
            RowEvaluable rowEvaluable = getRowEvaluable(project);
            
            Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
            String key = "numeric-bin:row-based:" + _expression;
            NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
            if (index == null) {
                index = new NumericBinRowIndex(project, rowEvaluable);
                column.setPrecompute(key, index);
            }
            
            retrieveDataFromBaseBinIndex(index);
            
            ExpressionNumericValueBinner binner = 
                new ExpressionNumericValueBinner(rowEvaluable, index);
            
            filteredRows.accept(project, binner);
            retrieveDataFromBinner(binner);
        }
    }
    
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        if (_eval != null && _errorMessage == null) {
            RowEvaluable rowEvaluable = getRowEvaluable(project);
            
            Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
            String key = "numeric-bin:record-based:" + _expression;
            NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
            if (index == null) {
                index = new NumericBinRecordIndex(project, rowEvaluable);
                column.setPrecompute(key, index);
            }
            
            retrieveDataFromBaseBinIndex(index);
            
            ExpressionNumericValueBinner binner = 
                new ExpressionNumericValueBinner(rowEvaluable, index);
            
            filteredRecords.accept(project, binner);
            
            retrieveDataFromBinner(binner);
        }
    }
    
    protected RowEvaluable getRowEvaluable(Project project) {
    	return new ExpressionBasedRowEvaluable(_columnName, _cellIndex, _eval);
    }
    
    protected void retrieveDataFromBaseBinIndex(NumericBinIndex index) {
        _min = index.getMin();
        _max = index.getMax();
        _step = index.getStep();
        _baseBins = index.getBins();
        
        _baseNumericCount = index.getNumericRowCount();
        _baseNonNumericCount = index.getNonNumericRowCount();
        _baseBlankCount = index.getBlankRowCount();
        _baseErrorCount = index.getErrorRowCount();
        
        if (_selected) {
            _from = Math.max(_from, _min);
            _to = Math.min(_to, _max);
        } else {
            _from = _min;
            _to = _max;
        }
    }
    
    protected void retrieveDataFromBinner(ExpressionNumericValueBinner binner) {
        _bins = binner.bins;
        _numericCount = binner.numericCount;
        _nonNumericCount = binner.nonNumericCount;
        _blankCount = binner.blankCount;
        _errorCount = binner.errorCount;
    }
}
