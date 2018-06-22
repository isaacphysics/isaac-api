package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.Collection;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public interface IAssignmentPersistenceManager {

    /**
     * Save an Assignment.
     * 
     * @param assignment
     *            - assignment to save
     * @return internal database id for the saved assignment.
     * @throws SegueDatabaseException
     *             - if there is a problem saving the assignment in the database.
     */
    Long saveAssignment(AssignmentDTO assignment) throws SegueDatabaseException;

    /**
     * Find a assignment by id.
     * 
     * @param assignmentId
     *            - the id to search for.
     * @return the assignment or null if we can't find it..
     * @throws SegueDatabaseException
     *             - if there is a problem accessing the database.
     */
    AssignmentDTO getAssignmentById(Long assignmentId) throws SegueDatabaseException;

    /**
     * Retrieve all Assignments for a given group.
     * 
     * @param groupId
     *            - to search for
     * @return assignments as a list
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database.
     */
    List<AssignmentDTO> getAssignmentsByGroupId(Long groupId) throws SegueDatabaseException;

    /**
     * Retrieve all Assignments for a given group and set by a given user.
     * 
     * @param assignmentOwnerId
     *            - to search for
     * @param groupId
     *            - to search for
     * @return assignments as a list
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database.
     */
    List<AssignmentDTO> getAssignmentsByOwnerIdAndGroupId(Long assignmentOwnerId, Long groupId)
            throws SegueDatabaseException;

    /**
     * getAssignmentsByGameboardAndGroup.
     * 
     * @param gameboardId
     *            - gameboard of interest
     * @param groupId
     *            - the group id has the gameboard assigned.
     * @return assignment if found null if not.
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database or if duplicate assignments exist in the
     *             database..
     */
    List<AssignmentDTO> getAssignmentsByGameboardAndGroup(String gameboardId, Long groupId)
            throws SegueDatabaseException;

    /**
     * getAssignmentsByOwner.
     * 
     * @param ownerId
     *            - the user id who might have assigned the gameboard.
     * @return list of assignments
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database.
     */
    List<AssignmentDTO> getAssignmentsByOwner(Long ownerId) throws SegueDatabaseException;

    /**
     * getAssignmentsByGroupList.
     *
     * @param groupIds
     *            - the group Ids to collect all assignments for.
     * @return list of assignments
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database.
     */
    List<AssignmentDTO> getAssignmentsByGroupList(Collection<Long> groupIds) throws SegueDatabaseException;


    /**
     * deleteAssignment.
     * 
     * @param id
     *            - assignment id to delete.
     * @throws SegueDatabaseException
     *             - if we are unable to perform the delete operation.
     */
    void deleteAssignment(Long id) throws SegueDatabaseException;

}