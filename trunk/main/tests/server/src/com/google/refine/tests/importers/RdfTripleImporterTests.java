package com.google.refine.tests.importers;

import java.io.StringReader;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.RdfTripleImporter;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;


public class RdfTripleImporterTests extends RefineTest {
    
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    
    //System Under Test
    RdfTripleImporter SUT = null;
    Project project = null;
    Properties options = null;

    @BeforeMethod
    public void SetUp(){
        SUT = new RdfTripleImporter();
        project = new Project();
        options = new Properties();
        options.put("base-url", "http://rdf.freebase.com");
    }

    @Test(enabled=false)
    public void CanParseSingleLineTriple(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, new ProjectMetadata(), options);
            project.update();
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
    }

    @Test
    public void CanParseMultiLineTriple(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.under_the_red_sky>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.bringing_it_all_back_home>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, new ProjectMetadata(), options);
            project.update();
        } catch (Exception e) {
            Assert.fail();
        }

        //columns
        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");

        //rows
        Assert.assertEquals(project.rows.size(), 3);
        
        //row0
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");

        //row1
        Assert.assertEquals(project.rows.get(1).cells.size(), 2);
        Assert.assertNull(project.rows.get(1).cells.get(0));
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "http://rdf.freebase.com/ns/en.bringing_it_all_back_home"); //NB triples aren't created in order they were input
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].rowIndex, 0);
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].cellIndex, 0);

        //row2
        Assert.assertEquals(project.rows.get(2).cells.size(), 2);
        Assert.assertNull(project.rows.get(2).cells.get(0));
        Assert.assertEquals(project.rows.get(2).cells.get(1).value, "http://rdf.freebase.com/ns/en.under_the_red_sky"); //NB triples aren't created in order they were input
        Assert.assertEquals(project.recordModel.getRowDependency(2).cellDependencies[1].rowIndex, 0);
        Assert.assertEquals(project.recordModel.getRowDependency(2).cellDependencies[1].cellIndex, 0);
    }

    @Test
    public void CanParseMultiLineMultiPredicatesTriple(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.blood_on_the_tracks>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.genre> <http://rdf.freebase.com/ns/en.folk_rock>.\n" +
            "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/music.artist.album> <http://rdf.freebase.com/ns/en.bringing_it_all_back_home>.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, new ProjectMetadata(), options);
            project.update();
        } catch (Exception e) {
            Assert.fail();
        }

        //columns
        Assert.assertEquals(project.columnModel.columns.size(), 3);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/music.artist.album");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "http://rdf.freebase.com/ns/music.artist.genre");
        
        //rows
        Assert.assertEquals(project.rows.size(), 2);

        //row0
        Assert.assertEquals(project.rows.get(0).cells.size(), 3);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "http://rdf.freebase.com/ns/en.blood_on_the_tracks");
        Assert.assertEquals(project.rows.get(0).cells.get(2).value, "http://rdf.freebase.com/ns/en.folk_rock");

        //row1
        Assert.assertEquals(project.rows.get(1).cells.size(), 2);
        Assert.assertNull(project.rows.get(1).cells.get(0));
        Assert.assertEquals(project.rows.get(1).cells.get(1).value, "http://rdf.freebase.com/ns/en.bringing_it_all_back_home");
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].rowIndex, 0);
        Assert.assertEquals(project.recordModel.getRowDependency(1).cellDependencies[1].cellIndex, 0);
    }
    
    @Test
    public void CanParseTripleWithValue(){
        String sampleRdf = "<http://rdf.freebase.com/ns/en.bob_dylan> <http://rdf.freebase.com/ns/common.topic.alias> \"Robert Zimmerman\"@en.";
        StringReader reader = new StringReader(sampleRdf);

        try {
            SUT.read(reader, project, new ProjectMetadata(), options);
            project.update();
        } catch (Exception e) {
            Assert.fail();
        }

        Assert.assertEquals(project.columnModel.columns.size(), 2);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "subject");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "http://rdf.freebase.com/ns/common.topic.alias");
        Assert.assertEquals(project.rows.size(), 1);
        Assert.assertEquals(project.rows.get(0).cells.size(), 2);
        Assert.assertEquals(project.rows.get(0).cells.get(0).value, "http://rdf.freebase.com/ns/en.bob_dylan");
        Assert.assertEquals(project.rows.get(0).cells.get(1).value, "\"Robert Zimmerman\"@en");
    }
}
