package com.google.refine.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.preference.TopList;

public class FileProjectManager extends ProjectManager {
    final static protected String s_projectDirNameSuffix = ".project";

    protected File                       _workspaceDir;

    final static Logger logger = LoggerFactory.getLogger("FileProjectManager");

    static public synchronized void initialize(File dir) {
        if (singleton == null) {
            logger.info("Using workspace directory: {}", dir.getAbsolutePath());
            singleton = new FileProjectManager(dir);
        }

    }

    protected FileProjectManager(File dir) {
        super();
        _workspaceDir = dir;
        if (!_workspaceDir.exists() && !_workspaceDir.mkdirs()) {
            logger.error("Failed to create directory : " + _workspaceDir);
            return;
        }

        load();
        recover();
    }

    public File getWorkspaceDir() {
        return _workspaceDir;
    }

    static public File getProjectDir(File workspaceDir, long projectID) {
        File dir = new File(workspaceDir, projectID + s_projectDirNameSuffix);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    public File getProjectDir(long projectID) {
        return getProjectDir(_workspaceDir, projectID);
    }

    /**
     * Import an external project that has been received as a .tar file, expanded, and
     * copied into our workspace directory.
     *
     * @param projectID
     */
    public boolean loadProjectMetadata(long projectID) {
        synchronized (this) {
            ProjectMetadata metadata = ProjectMetadataUtilities.load(getProjectDir(projectID));
            if (metadata != null) {
                _projectsMetadata.put(projectID, metadata);
                return true;
            } else {
                return false;
            }
        }
    }

    public void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException {
        File destDir = this.getProjectDir(projectID);
        destDir.mkdirs();

        if (gziped) {
            GZIPInputStream gis = new GZIPInputStream(inputStream);
            untar(destDir, gis);
        } else {
            untar(destDir, inputStream);
        }
    }

    protected void untar(File destDir, InputStream inputStream) throws IOException {
        TarInputStream tin = new TarInputStream(inputStream);
        TarEntry tarEntry = null;

        while ((tarEntry = tin.getNextEntry()) != null) {
            File destEntry = new File(destDir, tarEntry.getName());
            File parent = destEntry.getParentFile();

            if (!parent.exists()) {
                parent.mkdirs();
            }

            if (tarEntry.isDirectory()) {
                destEntry.mkdirs();
            } else {
                FileOutputStream fout = new FileOutputStream(destEntry);
                try {
                    tin.copyEntryContents(fout);
                } finally {
                    fout.close();
                }
            }
        }
    }

    public void exportProject(long projectId, TarOutputStream tos) throws IOException {
        File dir = this.getProjectDir(projectId);
        this.tarDir("", dir, tos);
    }

    protected void tarDir(String relative, File dir, TarOutputStream tos) throws IOException{
        File[] files = dir.listFiles();
        for (File file : files) {
            if (!file.isHidden()) {
                String path = relative + file.getName();

                if (file.isDirectory()) {
                    tarDir(path + File.separator, file, tos);
                } else {
                    TarEntry entry = new TarEntry(path);

                    entry.setMode(TarEntry.DEFAULT_FILE_MODE);
                    entry.setSize(file.length());
                    entry.setModTime(file.lastModified());

                    tos.putNextEntry(entry);

                    copyFile(file, tos);

                    tos.closeEntry();
                }
            }
        }
    }

    protected void copyFile(File file, OutputStream os) throws IOException {
        final int buffersize = 4096;

        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buf = new byte[buffersize];
            int count;

            while((count = fis.read(buf, 0, buffersize)) != -1) {
                os.write(buf, 0, count);
            }
        } finally {
            fis.close();
        }
    }

    @Override
    protected void saveMetadata(ProjectMetadata metadata, long projectId) throws Exception {
        File projectDir = getProjectDir(projectId);
        ProjectMetadataUtilities.save(metadata, projectDir);
    }

    @Override
    protected void saveProject(Project project){
        ProjectUtilities.save(project);
    }

    public Project loadProject(long id) {
        return ProjectUtilities.load(getProjectDir(id), id);
    }



    /**
     * Save the workspace's data out to file in a safe way: save to a temporary file first
     * and rename it to the real file.
     */
    @Override
    protected void saveWorkspace() {
        synchronized (this) {
            File tempFile = new File(_workspaceDir, "workspace.temp.json");
            try {
                saveToFile(tempFile);
            } catch (Exception e) {
                e.printStackTrace();

                logger.warn("Failed to save workspace");
                return;
            }

            File file = new File(_workspaceDir, "workspace.json");
            File oldFile = new File(_workspaceDir, "workspace.old.json");

            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            logger.info("Saved workspace");
        }
    }

    protected void saveToFile(File file) throws IOException, JSONException {
        FileWriter writer = new FileWriter(file);
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            jsonWriter.object();
            jsonWriter.key("projectIDs");
                jsonWriter.array();
                for (Long id : _projectsMetadata.keySet()) {
                    ProjectMetadata metadata = _projectsMetadata.get(id);
                    if (metadata != null) {
                        jsonWriter.value(id);

                        try {
                            ProjectMetadataUtilities.save(metadata, getProjectDir(id));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                jsonWriter.endArray();
                writer.write('\n');

            jsonWriter.key("preferences");
                _preferenceStore.write(jsonWriter, new Properties());

            jsonWriter.endObject();
        } finally {
            writer.close();
        }
    }



    public void deleteProject(long projectID) {
        synchronized (this) {
            removeProject(projectID);

            File dir = getProjectDir(projectID);
            if (dir.exists()) {
                deleteDir(dir);
            }
        }

        saveWorkspace();
    }

    static protected void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    protected void load() {
        if (loadFromFile(new File(_workspaceDir, "workspace.json"))) return;
        if (loadFromFile(new File(_workspaceDir, "workspace.temp.json"))) return;
        if (loadFromFile(new File(_workspaceDir, "workspace.old.json"))) return;
        logger.error("Failed to load workspace from any attempted alternatives.");
    }

    protected boolean loadFromFile(File file) {
        logger.info("Loading workspace: {}", file.getAbsolutePath());

        _projectsMetadata.clear();

        boolean found = false;

        if (file.exists() || file.canRead()) {
            FileReader reader = null;
            try {
                reader = new FileReader(file);
                JSONTokener tokener = new JSONTokener(reader);
                JSONObject obj = (JSONObject) tokener.nextValue();

                JSONArray a = obj.getJSONArray("projectIDs");
                int count = a.length();
                for (int i = 0; i < count; i++) {
                    long id = a.getLong(i);

                    File projectDir = getProjectDir(id);
                    ProjectMetadata metadata = ProjectMetadataUtilities.load(projectDir);

                    _projectsMetadata.put(id, metadata);
                }

                if (obj.has("preferences") && !obj.isNull("preferences")) {
                    _preferenceStore.load(obj.getJSONObject("preferences"));
                }

                if (obj.has("expressions") && !obj.isNull("expressions")) { // backward compatibility
                    ((TopList) _preferenceStore.get("scripting.expressions"))
                        .load(obj.getJSONArray("expressions"));
                }

                found = true;
            } catch (JSONException e) {
                logger.warn("Error reading file", e);
            } catch (IOException e) {
                logger.warn("Error reading file", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Exception closing file",e);
                }
            }
        }

        return found;
    }
    
    protected void recover() {
        for (File file : _workspaceDir.listFiles()) {
            if (file.isDirectory() && !file.isHidden()) {
                String name = file.getName();
                if (file.getName().endsWith(s_projectDirNameSuffix)) {
                    String idString = name.substring(0, name.length() - s_projectDirNameSuffix.length());
                    long id = -1;
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                    
                    if (id > 0 && !_projectsMetadata.containsKey(id)) {
                        if (loadProjectMetadata(id)) {
                            logger.info(
                                "Recovered project named " + 
                                    getProjectMetadata(id).getName() +
                                        " in directory " + name);
                        } else {
                            logger.warn("Failed to recover project in directory " + name);
                            
                            file.renameTo(new File(file.getParentFile(), name + ".corrupted"));
                        }
                    }
                }
            }
        }
    }
    
    public HistoryEntryManager getHistoryEntryManager(){
        return new FileHistoryEntryManager();
    }
}