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
package uk.ac.cam.cl.dtg.segue.database;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

/**
 * This class is a representation of the Git Database and provides some helper methods to allow file access.
 * 
 * It is responsible for providing basic functionality to search a specified Git Repository and find files based on a
 * given SHA.
 * 
 */
public class GitDb {
    private static final Logger log = LoggerFactory.getLogger(GitDb.class);

    private final String privateKey;
    private final String sshFetchUrl;

    private Git gitHandle;

    /**
     * Create a new instance of a GitDb object
     * 
     * This will immediately try and connect to the Git folder specified to check its validity.
     * 
     * @param repoLocation
     *            - location of the local git repository
     * @throws IOException
     *             - if we cannot access the repo location.
     */
    public GitDb(final String repoLocation) throws IOException {
        Validate.notBlank(repoLocation);

        // unused for this constructor
        this.privateKey = null;
        this.sshFetchUrl = null;

        gitHandle = Git.open(new File(repoLocation));
    }

    /**
     * Create a new instance of a GitDb object
     * 
     * This will immediately try and connect to the Git folder specified to check its validity.
     * 
     * This constructor is only necessary if we want to access a private repository.
     * 
     * @param repoLocation
     *            - location of the local git repository
     * @param sshFetchUrl
     *            - location of the remote git repository (ssh url used for fetching only)
     * @param privateKeyFileLocation
     *            - location of the local private key file used to access the ssh FetchUrl
     * @throws IOException
     *             - if we cannot access the repo location.
     */
    @Inject
    public GitDb(final String repoLocation, final String sshFetchUrl, final String privateKeyFileLocation)
            throws IOException {
        Validate.notBlank(repoLocation);

        this.sshFetchUrl = sshFetchUrl;
        this.privateKey = privateKeyFileLocation;

        gitHandle = Git.open(new File(repoLocation));
    }

    /**
     * Create a new instance of a GitDb object.
     * 
     * This is meant to be used for unit testing, allowing injection of a mocked Git object.
     * 
     * @param gitHandle
     *            - The (probably mocked) Git object to use.
     */
    public GitDb(final Git gitHandle) {
        Validate.notNull(gitHandle);

        this.privateKey = null;
        this.sshFetchUrl = null;

        this.gitHandle = gitHandle;
    }

    /**
     * getFileByCommitSHA
     * 
     * This method will access the git repository given a particular SHA and will attempt to locate a unique file and
     * return a bytearrayoutputstream of the files contents.
     * 
     * @param sha
     *            to search in.
     * @param fullFilePath
     *            file path to search for e.g. /src/filename.json
     * @return the ByteArrayOutputStream - which you can extract the file contents via the toString method.
     * @throws IOException
     *             - if we cannot access the repo location.
     * @throws UnsupportedOperationException
     *             - This method is intended to only locate one file at a time. If your search matches multiple files
     *             then this exception will be thrown.
     */
    public ByteArrayOutputStream getFileByCommitSHA(final String sha, final String fullFilePath) throws IOException,
            UnsupportedOperationException {
        if (null == sha || null == fullFilePath) {
            return null;
        }

        ObjectId objectId = this.findGitObject(sha, fullFilePath);

        if (null == objectId) {
            return null;
        }

        Repository repository = gitHandle.getRepository();
        ObjectLoader loader = repository.open(objectId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.copyTo(out);

        repository.close();
        return out;
    }

    /**
     * This method will configure a treewalk object that can be used to navigate the git repository.
     * 
     * @param sha
     *            - the version that the treewalk should be configured to search within.
     * @param searchString
     *            - the search string which can be a full path or simply a file extension.
     * @return A preconfigured treewalk object.
     * @throws IOException
     *             - if we cannot access the repo location.
     * @throws UnsupportedOperationException
     *             - if git does not support the operation requested.
     */
    public TreeWalk getTreeWalk(final String sha, final String searchString) throws IOException,
            UnsupportedOperationException {
        Validate.notBlank(sha);
        Validate.notNull(searchString);

        ObjectId commitId = gitHandle.getRepository().resolve(sha);
        if (null == commitId) {
            log.error("Failed to buildGitIndex - Unable to locate resource with sha: " + sha);
        } else {
            RevWalk revWalk = new RevWalk(gitHandle.getRepository());
            RevCommit commit = revWalk.parseCommit(commitId);

            RevTree tree = commit.getTree();

            TreeWalk treeWalk = new TreeWalk(gitHandle.getRepository());
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathSuffixFilter.create(searchString));

            return treeWalk;
        }
        return null;
    }

    /**
     * Get the git handle for the database.
     * 
     * @return the repository
     */
    public Repository getGitRepository() {
        return gitHandle.getRepository();
    }

    /**
     * Attempt to verify if an object exists in the git repository for a given sha and full path.
     * 
     * @param sha
     *            - the version that to search within.
     * @param fullfilePath
     *            - the full path of the file in git.
     * @return True if we can successfully find the object, false if not. False if we encounter an exception.
     */
    public boolean verifyGitObject(final String sha, final String fullfilePath) {
        try {
            if (findGitObject(sha, fullfilePath) != null) {
                return true;
            }
        } catch (UnsupportedOperationException | IOException e) {
            return false;
        }
        return false;
    }

    /**
     * Check that a commit sha exists within the git repository.
     * 
     * @param sha
     *            - the version that the treewalk should be configured to search within.
     * @return True if we have found the git sha false if not.
     */
    public boolean verifyCommitExists(final String sha) {
        if (null == sha) {
            log.warn("Null version provided. Unable to verify commit exists.");
            return false;
        }

        // we need to check that the local remote is up to date in order to
        // determine if the commit exists or not.
        this.fetchLatestFromRemote();

        try {
            Iterable<RevCommit> logs = gitHandle.log().add(ObjectId.fromString(this.getHeadSha())).call();

            for (RevCommit rev : logs) {
                if (rev.getName().equals(sha)) {
                    return true;
                }
            }

        } catch (NoHeadException e) {
            log.error("Git returned a no head exception. Unable to list all commits.");
            e.printStackTrace();
        } catch (GitAPIException e) {
            log.error("Git returned an API exception. Unable to list all commits.");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Git returned an IO exception. Unable to list all commits.");
            e.printStackTrace();
        }

        log.debug("Commit " + sha + " does not exist");
        return false;
    }

    /**
     * Get the time of the commit specified.
     * 
     * @param sha
     *            - to search for.
     * @return integer value representing time since epoch.
     * @throws NotFoundException
     *             - if we cannot find the commit requested.
     */
    public synchronized int getCommitTime(final String sha) throws NotFoundException {
        Validate.notBlank(sha);

        try {
            Iterable<RevCommit> logs = gitHandle.log().add(ObjectId.fromString(this.getHeadSha())).call();

            for (RevCommit rev : logs) {
                if (rev.getName().equals(sha)) {
                    return rev.getCommitTime();
                }
            }

        } catch (NoHeadException e) {
            log.error("Git returned a no head exception. Unable to list all commits.");
            e.printStackTrace();
        } catch (GitAPIException e) {
            log.error("Git returned an API exception. Unable to list all commits.");
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Git returned an IO exception. Unable to list all commits.");
            e.printStackTrace();
        }

        log.warn("Commit " + sha + " does not exist");
        throw new NotFoundException("Commit " + sha + " does not exist");
    }

    /**
     * Gets a complete list of commits with the most recent commit first.
     * 
     * Will return null if there is a problem and will write a log to the configured logger with the stack trace.
     * 
     * @return List of the commit shas we have found in the git repository.
     */
    public synchronized List<RevCommit> listCommits() {
        List<RevCommit> logList = null;
        try {
            Iterable<RevCommit> logs = gitHandle.log().add(ObjectId.fromString(this.getHeadSha())).call();
            logList = new ArrayList<RevCommit>();

            for (RevCommit rev : logs) {
                logList.add(rev);
            }

        } catch (GitAPIException e) {
            log.error("Git returned an API exception. While trying to to list all commits.", e);
        } catch (IOException e) {
            log.error("Git returned an IO exception. While trying to to list all commits.", e);
        }

        return logList;
    }

    /**
     * This method will execute a fetch on the configured remote git repository and will return the latest sha.
     * 
     * @return The version id of the latest version after the fetch.
     */
    public synchronized String fetchLatestFromRemote() {
        try {
            SshSessionFactory factory = new JschConfigSessionFactory() {
                @Override
                public void configure(final Host hc, final com.jcraft.jsch.Session session) {
                    session.setConfig("StrictHostKeyChecking", "no");
                }

                @Override
                protected JSch getJSch(final OpenSshConfig.Host hc, final org.eclipse.jgit.util.FS fs)
                        throws JSchException {
                    JSch jsch = super.getJSch(hc, fs);
                    jsch.removeAllIdentity();

                    if (null != privateKey) {
                        jsch.addIdentity(privateKey);
                    }

                    return jsch;
                }
            };

            if (this.sshFetchUrl != null) {
                SshSessionFactory.setInstance(factory);
            }

            RefSpec refSpec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
            FetchResult result = gitHandle.fetch().setRefSpecs(refSpec).setRemote(sshFetchUrl).call();

            TrackingRefUpdate refUpdate = result.getTrackingRefUpdate("refs/remotes/origin/master");
            if (refUpdate != null) {
                if (refUpdate.getResult() == RefUpdate.Result.LOCK_FAILURE) {
                    log.error("Failed to fetch. The git repository may be corrupted. "
                            + "Hopefully, this will not be a problem.");
                } else {
                    log.info("Fetched latest from git. Latest version is: " + this.getHeadSha());
                }
            }


        } catch (GitAPIException e) {
            log.error("Error while trying to pull the latest from the remote repository.", e);
        }
        return this.getHeadSha();
    }

    /**
     * Retrieve the SHA that is at the head of the repository (based on all fetched commits).
     * 
     * @return String of sha id
     */
    public synchronized String getHeadSha() {
        String result = null;

        try {
            ObjectId fetchHead = gitHandle.getRepository().resolve("origin/master");
            if (null != fetchHead) {
                result = fetchHead.getName();
            } else {
                log.warn("Problem fetching head from remote. Providing local head instead.");
                result = gitHandle.getRepository().resolve(Constants.HEAD).getName();
            }

        } catch (RevisionSyntaxException | IOException e) {
            log.error("Error getting the head from the repository.", e);
        }
        return result;
    }

    /**
     * Will find an object from the git repository if given a sha and a full git path.
     * 
     * @param sha
     *            - to search for.
     * @param filename
     *            - of the file in git to locate.
     * @return ObjectId which will allow you to access information about the node.
     * @throws IOException
     *             - if we cannot access the repo location.
     * @throws UnsupportedOperationException
     *             - if git does not support the operation requested.
     */
    private ObjectId findGitObject(final String sha, final String filename) throws IOException,
            UnsupportedOperationException {
        if (null == sha || null == filename) {
            return null;
        }

        Repository repository = gitHandle.getRepository();

        ObjectId commitId = repository.resolve(sha);

        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(commitId);

        RevTree tree = commit.getTree();

        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(filename));

        int count = 0;
        ObjectId objectId = null;
        String path = null;
        while (treeWalk.next()) {
            count++;
            if (null == objectId) {
                objectId = treeWalk.getObjectId(0);
                path = treeWalk.getPathString();
            } else if (count > 1) {
                // throw exception if we find that there is more than one that
                // matches the search.
                StringBuilder sb = new StringBuilder();
                sb.append("Multiple results have been found in the git repository for the following search: ");
                sb.append(filename + ".");
                sb.append(" in ");
                sb.append(sha);
                sb.append(" Unable to decide which one to return.");
                throw new UnsupportedOperationException(sb.toString());
            }
        }

        if (null == objectId) {
            return null;
        }

        revWalk.dispose();
        log.debug("Retrieved Commit Id: " + commitId.getName() + " Searching for: " + filename + " found: " + path);
        return objectId;
    }
}
