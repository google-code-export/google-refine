package com.google.gridworks.tests.importers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.gridworks.ProjectMetadata;
import com.google.gridworks.importers.TsvCsvImporter;
import com.google.gridworks.model.Project;
import com.google.gridworks.tests.GridworksTest;

public class TsvCsvImporterTests extends GridworksTest {

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //constants
    String SAMPLE_ROW = "NDB_No,Shrt_Desc,Water";

    //System Under Test
    TsvCsvImporter SUT = null;

    //mock dependencies
    Project project = null;
    Properties properties = null;


    @BeforeMethod
    public void SetUp(){
        SUT = new TsvCsvImporter();
        project = new Project(); //FIXME - should we try and use mock(Project.class); - seems unnecessary complexity
        properties = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        project = null;
        properties = null;
    }

    @Test(dataProvider = "CSV-or-null")
    public void readJustColumns(String sep){
        String input = "col1,col2,col3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));

        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readUnseperatedData(String sep){
        String input = "value1,value2,value3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));

        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 0, false, false, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 1);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Column");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, input);
    }

    @Test(dataProvider = "CSV-or-null")
    public void readSimpleData_CSV_1Header_1Row(String sep){
        String input = "col1,col2,col3\n" +
                       "data1,data2,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test
    public void readSimpleData_TSV_1Header_1Row(){
        String input = "col1\tcol2\tcol3\n" +
                       "data1\tdata2\tdata3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, "\t", -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readSimpleData_CSV_1Header_1Row_GuessValues(String sep){
        String input = "col1,col2,col3\n" +
                       "data1,234,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, true, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertTrue(project.rows.get(0).cells.get(1).value instanceof Long);
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, Long.parseLong("234"));
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readSimpleData_0Header_1Row(String sep){
        String input = "data1,data2,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 0, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "Column");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "Column2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "Column3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(groups = {  }, dataProvider = "CSV-or-null")
    public void readDoesTrimsLeadingTrailingWhitespace(String sep){
        String input = " data1 , data2 , data3 ";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 0, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readTrimsLeadingTrailingWhitespace(String sep){
        String input = " data1, data2, data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 0, true, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readCanAddNull(String sep){
        String input = " data1, , data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 0, true, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertNull(project.rows.get(0).cells.get(1));
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readSimpleData_2Header_1Row(String sep){
        String input = "col1,col2,col3\n" +
                       "sub1,sub2,sub3\n" +
                       "data1,data2,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 2, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readSimpleData_RowLongerThanHeader(String sep){
        String input = "col1,col2,col3\n" +
        "data1,data2,data3,data4,data5,data6";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 6);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "Column");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "Column");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "Column");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 6);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
        Assert.assertEquals(project.rows.get(0).cells.get(3).value, "data4");
        Assert.assertEquals(project.rows.get(0).cells.get(4).value, "data5");
        Assert.assertEquals(project.rows.get(0).cells.get(5).value, "data6");
    }

    @Test(groups = { }, dataProvider = "CSV-or-null")
    public void readQuotedData(String sep){
        String input = "col1,col2,col3\n" +
                       "\"\"\"To Be\"\" is often followed by \"\"or not To Be\"\"\",data2";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "\"To Be\" is often followed by \"or not To Be\"");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readIgnoreFirstLine(String sep){
        String input = "ignore1\n" +
                       "col1,col2,col3\n" +
                       "data1,data2,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 1, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readSkipFirstDataLine(String sep){
        String input = "col1,col2,col3\n" +
                       "skip1\n" +
                       "data1,data2,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 1, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(dataProvider = "CSV-or-null")
    public void readIgnore3_Header2_Skip1(String sep){
        String input = "ignore1\n" +
                       "ignore2\n" +
                       "ignore3\n" +
                       "col1,col2,col3\n" +
                       "sub1,sub2,sub3\n" +
                       "skip1\n" +
                       "data1,data2,data3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 1, 3, 2, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(groups = {  }, dataProvider = "CSV-or-null")
    public void readIgnore3_Header2_Skip2_limit2(String sep){
        String input = "ignore1\n" +
                       "ignore2\n" +
                       "ignore3\n" +
                       "col1,col2,col3\n" +
                       "sub1,sub2,sub3\n" +
                       "skip1\n" +
                       "skip2\n" +
                       "data-row1-cell1,data-row1-cell2,data-row1-cell3\n" +
                       "data-row2-cell1,data-row2-cell2,\n" + //missing last data point of this row on purpose
                       "data-row3-cell1,data-row3-cell2,data-row1-cell3";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, 2, 2, 3, 2, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
        Assert.assertEquals(project.rows.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data-row1-cell1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data-row1-cell2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data-row1-cell3");
        Assert.assertEquals(project.rows.get(1).cells.size(), 3);
        Assert.assertEquals(project.rows.get(1).cells.get(0).value, "data-row2-cell1");
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "data-row2-cell2");
        Assert.assertNull(project.rows.get(1).cells.get(2));
    }

    @Test(dataProvider = "CSV-or-null")
    public void ignoreQuotes(String sep){
        String input = "data1,data2\",data3,data4";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 0, false, true, true);
        } catch (IOException e) {
            Assert.fail();
        }
        //Assert.assertEquals(project.columnModel.columns.size(), 4);
        Assert.assertEquals(project.rows.size(), 1);
        //Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
    }

    @Test(groups = { }, dataProvider = "CSV-or-null")
    public void readWithMultiLinedQuotedData(String sep){
        String input = "col1,col2,col3\n" +
            "\"\"\"To\n Be\"\" is often followed by \"\"or not To\n Be\"\"\",data2";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "\"To\n Be\" is often followed by \"or not To\n Be\"");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
    }

    @Test(groups = {  }, dataProvider = "CSV-or-null")
    public void readWithMultiLinedQuotedDataAndBlankLines(String sep){
        String input = "col1,col2,col3\n" +
            "\"A line with many \n\n\n\n\n empty lines\",data2";
        LineNumberReader lnReader = new LineNumberReader(new StringReader(input));
        try {
            SUT.read(lnReader, project, sep, -1, 0, 0, 1, false, true, false);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "A line with many \n\n\n\n\n empty lines");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
    }

    //---------------------read tests------------------------
    @Test
    public void readCsvWithProperties(){
        StringReader reader = new StringReader(SAMPLE_ROW);

        when(properties.getProperty("separator")).thenReturn(",");
        whenGetIntegerOption("ignore",properties,0);
        whenGetIntegerOption("header-lines",properties,0);
        whenGetIntegerOption("limit",properties,-1);
        whenGetIntegerOption("skip",properties,0);
        whenGetIntegerOption("ignore-quotes",properties,0);

        try {
            SUT.read(reader, project, new ProjectMetadata(), properties);
        } catch (Exception e) {
            Assert.fail();
        }


        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals((String)project.rows.get(0).cells.get(0).value, "NDB_No");
        Assert.assertEquals((String)project.rows.get(0).cells.get(1).value, "Shrt_Desc");
        Assert.assertEquals((String)project.rows.get(0).cells.get(2).value, "Water");

        verify(properties, times(1)).getProperty("separator");
        verifyGetOption("ignore",properties);
        verifyGetOption("header-lines",properties);
        verifyGetOption("limit",properties);
        verifyGetOption("skip",properties);
        verifyGetOption("ignore-quotes",properties);
    }

    @Test
    public void readCsvWithPropertiesIgnoreQuotes(){
        String input = "data1,data2\",data3,data4";
        StringReader reader = new StringReader(input);

        when(properties.getProperty("separator")).thenReturn(",");
        whenGetIntegerOption("ignore",properties,0);
        whenGetIntegerOption("header-lines",properties,0);
        whenGetIntegerOption("limit",properties,-1);
        whenGetIntegerOption("skip",properties,0);
        whenGetBooleanOption("ignore-quotes",properties,true);

        try {
            SUT.read(reader, project, new ProjectMetadata(), properties);
        } catch (Exception e) {
            Assert.fail();
        }


        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 4);
        Assert.assertEquals((String)project.rows.get(0).cells.get(0).value, "data1");
        Assert.assertEquals((String)project.rows.get(0).cells.get(1).value, "data2");
        Assert.assertEquals((String)project.rows.get(0).cells.get(2).value, "data3");
        Assert.assertEquals((String)project.rows.get(0).cells.get(3).value, "data4");

        verify(properties, times(1)).getProperty("separator");
        verifyGetOption("ignore",properties);
        verifyGetOption("header-lines",properties);
        verifyGetOption("limit",properties);
        verifyGetOption("skip",properties);
        verifyGetOption("ignore-quotes",properties);
    }

    //--helpers--
    /**
     * Used for parameterized testing for both SeparatorParser and TsvCsvParser.
     */
    @DataProvider(name = "CSV-or-null")
    public Object[][] CSV_or_null(){
        return new Object[][]{{
                ",",
                null
        }};
    }

    public void whenGetBooleanOption(String name, Properties properties, Boolean def){
        when(properties.containsKey(name)).thenReturn(true);
        when(properties.getProperty(name)).thenReturn(Boolean.toString(def));
    }

    public void whenGetIntegerOption(String name, Properties properties, int def){
        when(properties.containsKey(name)).thenReturn(true);
        when(properties.getProperty(name)).thenReturn(Integer.toString(def));
    }

    public void verifyGetOption(String name, Properties properties){
        verify(properties, times(1)).containsKey(name);
        verify(properties, times(1)).getProperty(name);
    }

}
