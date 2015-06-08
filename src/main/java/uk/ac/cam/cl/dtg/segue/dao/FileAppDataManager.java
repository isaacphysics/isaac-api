/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data manager class that specialised in storing things as flat files in the file system.
 * 
 * @author Stephen Cummins
 *
 */
public class FileAppDataManager implements IAppDataManager<byte[]> {

    private static final Logger log = LoggerFactory.getLogger(FileAppDataManager.class);

    private final Path storageLocation;

    /**
     * Creates a new fileAppDataManager.
     * 
     * @param storageLocation
     *            - the location on the file system of a directory that we can store files in.
     * @throws IOException
     *             - if we are unable to get a valid handle on the storage location specified.
     */
    public FileAppDataManager(final String storageLocation) throws IOException {

        // verify storage location exists, if not try and create it throw an exception if there is a problem.
        File file = new File(storageLocation);
        if (file.exists() && file.isDirectory()) {
            log.debug("File App Data Manager initialised and storage location already exists.");
        } else if (!file.exists()) {
            // we will attempt to create it then.
            log.info("FileAppManager data directory does not exist. Attempting to create it.");
            if (!file.mkdirs()) {
                throw new IOException("Unable to create the directory: " + storageLocation
                        + " for use by the FileAppDataManager.");
            }
        }

        this.storageLocation = file.toPath();
    }

    @Override
    public byte[] getById(final String id) throws SegueDatabaseException {
        Path newPath = storageLocation.resolve(id);
        File f = newPath.toFile();
        if (f.exists() && f.isFile()) {
            try {
                return Files.readAllBytes(newPath);
            } catch (IOException e) {
                throw new SegueDatabaseException("IOException occurred while trying to read file " + f, e);
            }
        } else {
            return null;
        }
    }

    @Override
    public String save(final String preferredId, final byte[] objectToSave) throws SegueDatabaseException {
        Path newPath = storageLocation.resolve(preferredId);

        try {
            Files.write(newPath, objectToSave);
            return preferredId;
        } catch (IOException e) {
            throw new SegueDatabaseException("IOException occurred while trying to read file " + newPath, e);
        }
    }

    @Override
    public void delete(final String objectId) throws SegueDatabaseException {
        Path location = storageLocation.resolve(objectId);
        File f = location.toFile();

        if (f.exists()) {
            if (!f.delete()) {
                throw new SegueDatabaseException("Unable to delete the file.");
            }
        } else {
            throw new SegueDatabaseException("Unable to delete the file as it does not exist.");
        }
    }
}
