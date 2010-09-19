/**
 * JiraSoapService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.jira.rpc.soap.jirasoapservice_v2;

public interface JiraSoapService extends java.rmi.Remote {
    public com.atlassian.jira.rpc.soap.beans.RemoteComment getComment(java.lang.String in0, long in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteGroup createGroup(java.lang.String in0, java.lang.String in1,
            com.atlassian.jira.rpc.soap.beans.RemoteUser in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteSecurityLevel getSecurityLevel(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteServerInfo getServerInfo(java.lang.String in0) throws java.rmi.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteGroup getGroup(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteUser createUser(java.lang.String in0, java.lang.String in1,
            java.lang.String in2, java.lang.String in3, java.lang.String in4) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteUser getUser(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteComponent[] getComponents(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue getIssue(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue createIssue(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteIssue in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteNamedObject[] getAvailableActions(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssueType[] getSubTaskIssueTypes(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteConfiguration getConfiguration(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject createProject(java.lang.String in0, java.lang.String in1,
            java.lang.String in2, java.lang.String in3, java.lang.String in4, java.lang.String in5,
            com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme in6, com.atlassian.jira.rpc.soap.beans.RemoteScheme in7,
            com.atlassian.jira.rpc.soap.beans.RemoteScheme in8) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject updateProject(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProject in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject getProjectByKey(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void removeAllRoleActorsByProject(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteProject in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemotePriority[] getPriorities(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteResolution[] getResolutions(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssueType[] getIssueTypes(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteStatus[] getStatuses(java.lang.String in0) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProjectRole[] getProjectRoles(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProjectRole getProjectRole(java.lang.String in0, long in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProjectRoleActors getProjectRoleActors(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1, com.atlassian.jira.rpc.soap.beans.RemoteProject in2)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteRoleActors getDefaultRoleActors(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public void removeAllRoleActorsByNameAndType(java.lang.String in0, java.lang.String in1, java.lang.String in2)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteProjectRole(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1, boolean in2)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public void updateProjectRole(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProjectRole createProjectRole(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public boolean isProjectRoleNameUnique(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public void addActorsToProjectRole(java.lang.String in0, java.lang.String[] in1,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in2, com.atlassian.jira.rpc.soap.beans.RemoteProject in3,
            java.lang.String in4) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public void removeActorsFromProjectRole(java.lang.String in0, java.lang.String[] in1,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in2, com.atlassian.jira.rpc.soap.beans.RemoteProject in3,
            java.lang.String in4) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public void addDefaultActorsToProjectRole(java.lang.String in0, java.lang.String[] in1,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in2, java.lang.String in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public void removeDefaultActorsFromProjectRole(java.lang.String in0, java.lang.String[] in1,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in2, java.lang.String in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteScheme[] getAssociatedNotificationSchemes(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteScheme[] getAssociatedPermissionSchemes(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProjectRole in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteProject(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject getProjectById(java.lang.String in0, long in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteVersion[] getVersions(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteField[] getCustomFields(java.lang.String in0) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteComment[] getComments(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteFilter[] getFavouriteFilters(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void releaseVersion(java.lang.String in0, java.lang.String in1, com.atlassian.jira.rpc.soap.beans.RemoteVersion in2)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public void archiveVersion(java.lang.String in0, java.lang.String in1, java.lang.String in2, boolean in3)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue updateIssue(java.lang.String in0, java.lang.String in1,
            com.atlassian.jira.rpc.soap.beans.RemoteFieldValue[] in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteField[] getFieldsForEdit(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssueType[] getIssueTypesForProject(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssueType[] getSubTaskIssueTypesForProject(java.lang.String in0,
            java.lang.String in1) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException;

    public java.lang.String login(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void addUserToGroup(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteGroup in1,
            com.atlassian.jira.rpc.soap.beans.RemoteUser in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void removeUserFromGroup(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteGroup in1,
            com.atlassian.jira.rpc.soap.beans.RemoteUser in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void addComment(java.lang.String in0, java.lang.String in1, com.atlassian.jira.rpc.soap.beans.RemoteComment in2)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean logout(java.lang.String in0) throws java.rmi.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject getProjectWithSchemesById(java.lang.String in0, long in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteSecurityLevel[] getSecurityLevels(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteAvatar[] getProjectAvatars(java.lang.String in0, java.lang.String in1,
            boolean in2) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public void setProjectAvatar(java.lang.String in0, java.lang.String in1, long in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteAvatar getProjectAvatar(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteProjectAvatar(java.lang.String in0, long in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteScheme[] getNotificationSchemes(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme[] getPermissionSchemes(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemotePermission[] getAllPermissions(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme createPermissionScheme(java.lang.String in0,
            java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme addPermissionTo(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme in1, com.atlassian.jira.rpc.soap.beans.RemotePermission in2,
            com.atlassian.jira.rpc.soap.beans.RemoteEntity in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme deletePermissionFrom(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemotePermissionScheme in1, com.atlassian.jira.rpc.soap.beans.RemotePermission in2,
            com.atlassian.jira.rpc.soap.beans.RemoteEntity in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deletePermissionScheme(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue createIssueWithSecurityLevel(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteIssue in1, long in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean addAttachmentsToIssue(java.lang.String in0, java.lang.String in1, java.lang.String[] in2, byte[][] in3)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteAttachment[] getAttachmentsFromIssue(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteIssue(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean hasPermissionToEditComment(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteComment in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteComment editComment(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteComment in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteField[] getFieldsForAction(java.lang.String in0, java.lang.String in1,
            java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue progressWorkflowAction(java.lang.String in0, java.lang.String in1,
            java.lang.String in2, com.atlassian.jira.rpc.soap.beans.RemoteFieldValue[] in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue getIssueById(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteWorklog addWorklogWithNewRemainingEstimate(java.lang.String in0,
            java.lang.String in1, com.atlassian.jira.rpc.soap.beans.RemoteWorklog in2, java.lang.String in3)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteWorklog addWorklogAndAutoAdjustRemainingEstimate(java.lang.String in0,
            java.lang.String in1, com.atlassian.jira.rpc.soap.beans.RemoteWorklog in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteWorklog addWorklogAndRetainRemainingEstimate(java.lang.String in0,
            java.lang.String in1, com.atlassian.jira.rpc.soap.beans.RemoteWorklog in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteWorklogWithNewRemainingEstimate(java.lang.String in0, java.lang.String in1, java.lang.String in2)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteWorklogAndAutoAdjustRemainingEstimate(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteWorklogAndRetainRemainingEstimate(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void updateWorklogWithNewRemainingEstimate(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteWorklog in1,
            java.lang.String in2) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void updateWorklogAndAutoAdjustRemainingEstimate(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteWorklog in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void updateWorklogAndRetainRemainingEstimate(java.lang.String in0, com.atlassian.jira.rpc.soap.beans.RemoteWorklog in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteWorklog[] getWorklogs(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean hasPermissionToCreateWorklog(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean hasPermissionToDeleteWorklog(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean hasPermissionToUpdateWorklog(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteValidationException, com.atlassian.jira.rpc.exception.RemoteException;

    public java.util.Calendar getResolutionDateByKey(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public java.util.Calendar getResolutionDateById(java.lang.String in0, long in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public long getIssueCountForFilter(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue[] getIssuesFromTextSearch(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue[] getIssuesFromTextSearchWithProject(java.lang.String in0,
            java.lang.String[] in1, java.lang.String in2, int in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue[] getIssuesFromJqlSearch(java.lang.String in0, java.lang.String in1,
            int in2) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteUser(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteGroup updateGroup(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteGroup in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void deleteGroup(java.lang.String in0, java.lang.String in1, java.lang.String in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void refreshCustomFields(java.lang.String in0) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteFilter[] getSavedFilters(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public boolean addBase64EncodedAttachmentsToIssue(java.lang.String in0, java.lang.String in1, java.lang.String[] in2,
            java.lang.String[] in3) throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject createProjectFromObject(java.lang.String in0,
            com.atlassian.jira.rpc.soap.beans.RemoteProject in1) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteValidationException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteScheme[] getSecuritySchemes(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteVersion addVersion(java.lang.String in0, java.lang.String in1,
            com.atlassian.jira.rpc.soap.beans.RemoteVersion in2) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue[] getIssuesFromFilter(java.lang.String in0, java.lang.String in1)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue[] getIssuesFromFilterWithLimit(java.lang.String in0,
            java.lang.String in1, int in2, int in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteIssue[] getIssuesFromTextSearchWithLimit(java.lang.String in0,
            java.lang.String in1, int in2, int in3) throws java.rmi.RemoteException,
            com.atlassian.jira.rpc.exception.RemoteException;

    public com.atlassian.jira.rpc.soap.beans.RemoteProject[] getProjectsNoSchemes(java.lang.String in0)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteAuthenticationException, com.atlassian.jira.rpc.exception.RemoteException;

    public void setNewProjectAvatar(java.lang.String in0, java.lang.String in1, java.lang.String in2, java.lang.String in3)
            throws java.rmi.RemoteException, com.atlassian.jira.rpc.exception.RemotePermissionException,
            com.atlassian.jira.rpc.exception.RemoteException;
}
