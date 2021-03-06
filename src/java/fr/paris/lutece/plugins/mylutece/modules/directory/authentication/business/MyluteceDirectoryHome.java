/*
 * Copyright (c) 2002-2017, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.mylutece.modules.directory.authentication.business;

import fr.paris.lutece.plugins.directory.business.Record;
import fr.paris.lutece.plugins.directory.business.RecordField;
import fr.paris.lutece.plugins.directory.business.RecordFieldFilter;
import fr.paris.lutece.plugins.directory.business.RecordFieldHome;
import fr.paris.lutece.plugins.directory.service.DirectoryPlugin;
import fr.paris.lutece.plugins.directory.service.record.IRecordService;
import fr.paris.lutece.plugins.directory.service.record.RecordService;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.BaseUser;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.security.LuteceAuthentication;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * This class provides instances management methods (create, find, ...) for directoryUser objects
 */
public final class MyluteceDirectoryHome
{
	// Static variable pointed at the DAO instance
	private static IMyluteceDirectoryDAO _dao = SpringContextService.getBean( "mylutece-directory.myluteceDirectoryDAO" );

	/**
	 * Private constructor - this class need not be instantiated
	 */
	private MyluteceDirectoryHome( )
	{
	}

	/**
	 * Find users by login
	 * 
	 * @param strLogin the login
	 * @param plugin The Plugin using this data access service
	 * @param authenticationService the LuteceAuthentication object
	 * @return DirectoryUser the user corresponding to the login
	 */
	public static BaseUser findLuteceUserByLogin( String strLogin, Plugin plugin, LuteceAuthentication authenticationService )
	{
		Locale locale = null;

		// Get the Mylutece Directory User
		Collection<MyluteceDirectoryUser> directoryUsers = MyluteceDirectoryUserHome.findDirectoryUsersListForLogin( strLogin, plugin );

		if ( directoryUsers.size( ) != 1 )
		{
			return null;
		}

		MyluteceDirectoryUser directoryUser = directoryUsers.iterator( ).next( );

		// Get the Directory record
		RecordFieldFilter filter = new RecordFieldFilter( );
		filter.setIdRecord( directoryUser.getIdRecord( ) );

		Plugin directoryPlugin = PluginService.getPlugin( DirectoryPlugin.PLUGIN_NAME );
		IRecordService recordService = SpringContextService.getBean( RecordService.BEAN_SERVICE );
		Record record = recordService.findByPrimaryKey( directoryUser.getIdRecord( ), directoryPlugin );

		if ( record == null )
		{
			AppLogService.error( "MyLuteceDirectory - Inconsistency between the MyLuteceDirectoryUser and the directory record : " + "Record is null whereas MyLuteceDirectoryUser is not." );

			return null;
		}

		record.setListRecordField( RecordFieldHome.getRecordFieldList( filter, directoryPlugin ) );

		// Create the BaseUser
		BaseUser user = new BaseUser( strLogin, authenticationService );
		user.setLuteceAuthenticationService( authenticationService );
		user.setGroups( MyluteceDirectoryHome.findUserGroupsFromLogin( strLogin, plugin ) );
		user.setRoles( MyluteceDirectoryHome.findUserRolesFromLogin( strLogin, plugin ) );

		if ( directoryUser.getDateLastLogin( ) != null )
		{
			DateFormat dateFormat = new SimpleDateFormat( );
			user.setUserInfo( LuteceUser.DATE_LAST_LOGIN, dateFormat.format( directoryUser.getDateLastLogin( ) ) );
		}

		for ( RecordField rf : record.getListRecordField( ) )
		{
			AttributeMapping attributeMapping = AttributeMappingHome.findByPrimaryKey( rf.getEntry( ).getIdEntry( ), plugin );

			if ( attributeMapping != null )
			{
				user.setUserInfo( attributeMapping.getAttributeKey( ), rf.getEntry( ).convertRecordFieldValueToString( rf, locale, false, false ) );
			}
			else
			{
				user.setUserInfo( Integer.toString(rf.getEntry().getIdEntry()), rf.getEntry( ).convertRecordFieldValueToString( rf, locale, false, false ) );
			}
		}

		return user;
	}

	/**
	 * Gets the reset password attribute of the user from his login
	 * @param strLogin the login
	 * @param plugin The Plugin using this data access services
	 * @return True if the password has to be changed, false otherwise
	 */
	public static boolean findResetPasswordFromLogin( String strLogin, Plugin plugin )
	{
		return _dao.selectResetPasswordFromLogin( strLogin, plugin );
	}

	/**
	 * Gets the expiration date of the user's password
	 * @param strLogin The login of the user
	 * @param plugin The plugin
	 * @return The expiration date of the user's password
	 */
	public static Timestamp findPasswordMaxValideDateFromLogin( String strLogin, Plugin plugin )
	{
		return _dao.selectPasswordMaxValideDateFromLogin( strLogin, plugin );
	}

	/**
	 * Load the list of {@link BaseUser}
	 * @param plugin The Plugin using this data access service
	 * @param authenticationService the authentication service
	 * @return The Collection of the {@link BaseUser}
	 */
	public static Collection<BaseUser> findDirectoryUsersList( Plugin plugin, LuteceAuthentication authenticationService )
	{
		Locale locale = null;

		// Get the Mylutece Directory User
		Collection<MyluteceDirectoryUser> directoryUsers = MyluteceDirectoryUserHome.findDirectoryUsersList( plugin );
		Collection<BaseUser> baseUserList = new ArrayList<BaseUser>( );

		for ( MyluteceDirectoryUser directoryUser : directoryUsers )
		{
			// Get the Directory record
			RecordFieldFilter filter = new RecordFieldFilter( );
			filter.setIdRecord( directoryUser.getIdRecord( ) );

			Plugin directoryPlugin = PluginService.getPlugin( DirectoryPlugin.PLUGIN_NAME );
			IRecordService recordService = SpringContextService.getBean( RecordService.BEAN_SERVICE );
			Record record = recordService.findByPrimaryKey( directoryUser.getIdRecord( ), directoryPlugin );
			record.setListRecordField( RecordFieldHome.getRecordFieldList( filter, directoryPlugin ) );

			// Create the BaseUser
			BaseUser user = new BaseUser( directoryUser.getLogin( ), authenticationService );
			user.setGroups( MyluteceDirectoryHome.findUserGroupsFromLogin( directoryUser.getLogin( ), plugin ) );
			user.setRoles( MyluteceDirectoryHome.findUserRolesFromLogin( directoryUser.getLogin( ), plugin ) );

			for ( RecordField rf : record.getListRecordField( ) )
			{
				AttributeMapping attributeMapping = AttributeMappingHome.findByPrimaryKey( rf.getEntry( ).getIdEntry( ), plugin );

				if ( attributeMapping != null )
				{
					user.setUserInfo( attributeMapping.getAttributeKey( ), rf.getEntry( ).convertRecordFieldValueToString( rf, locale, false, false ) );
				}
			}

			baseUserList.add( user );
		}

		return baseUserList;
	}

	/**
	 * Assign a directory as a user directory
	 * @param nIdDirectory The directory identifier
	 * @param plugin The Plugin using this data access service
	 */
	public static void assignDirectory( int nIdDirectory, Plugin plugin )
	{
		_dao.assignDirectory( nIdDirectory, plugin );
	}

	/**
	 * Unassign a directory
	 * @param nIdDirectory The directory identifier
	 * @param plugin The Plugin using this data access service
	 */
	public static void unAssignDirectory( int nIdDirectory, Plugin plugin )
	{
		_dao.unAssignDirectory( nIdDirectory, plugin );
	}

	/**
	 * Unassign all directories
	 * @param plugin The Plugin using this data access service
	 */
	public static void unAssignDirectories( Plugin plugin )
	{
		_dao.unAssignDirectories( plugin );
	}

	/**
	 * Find the mapped directories
	 * @param plugin The Plugin using this data access service
	 * @return a collection of directouy
	 */
	public static Collection<Integer> findMappedDirectories( Plugin plugin )
	{
		return _dao.selectMappedDirectories( plugin );
	}

	/**
	 * Check if the specified directory is mapped for managing users
	 * @param nIdDirectory The directory identifier
	 * @param plugin The Plugin using this data access service
	 * @return true if the directory is mapped, false else
	 */
	public static boolean isMapped( int nIdDirectory, Plugin plugin )
	{
		return _dao.isMapped( nIdDirectory, plugin );
	}

	/**
	 * Find user's roles by login
	 * 
	 * @param strLogin the login
	 * @param plugin The Plugin using this data access service
	 * @return ArrayList the role key list corresponding to the login
	 */
	public static List<String> findUserRolesFromLogin( String strLogin, Plugin plugin )
	{
		return _dao.selectUserRolesFromLogin( strLogin, plugin );
	}

	/**
	 * Delete roles for a user
	 * @param nIdRecord The id of the user
	 * @param plugin The Plugin using this data access service
	 */
	public static void removeRolesForUser( int nIdRecord, Plugin plugin )
	{
		_dao.deleteRolesForUser( nIdRecord, plugin );
	}

	/**
	 * Assign a role to user
	 * @param nIdRecord The id of the user
	 * @param strRoleKey The key of the role
	 * @param plugin The Plugin using this data access service
	 */
	public static void addRoleForUser( int nIdRecord, String strRoleKey, Plugin plugin )
	{
		_dao.createRoleForUser( nIdRecord, strRoleKey, plugin );
	}

	/**
	 * Find user's groups by login
	 * 
	 * @param strLogin the login
	 * @param plugin The Plugin using this data access service
	 * @return ArrayList the group key list corresponding to the login
	 */
	public static List<String> findUserGroupsFromLogin( String strLogin, Plugin plugin )
	{
		return _dao.selectUserGroupsFromLogin( strLogin, plugin );
	}

	/**
	 * Delete groups for a user
	 * @param nIdRecord The id of the user
	 * @param plugin The Plugin using this data access service
	 */
	public static void removeGroupsForUser( int nIdRecord, Plugin plugin )
	{
		_dao.deleteGroupsForUser( nIdRecord, plugin );
	}

	/**
	 * Assign a group to user
	 * @param nIdRecord The id of the user
	 * @param strGroupKey The key of the group
	 * @param plugin The Plugin using this data access service
	 */
	public static void addGroupForUser( int nIdRecord, String strGroupKey, Plugin plugin )
	{
		_dao.createGroupForUser( nIdRecord, strGroupKey, plugin );
	}

	/**
	 * Returns a collection of DirectoryUser objects for a Lutece role
	 * 
	 * @param strRoleKey The role of the databseUser
	 * @param plugin The current plugin using this method
	 * @return A collection of logins
	 */
	public static Collection<String> findDirectoryUsersListForRoleKey( String strRoleKey, Plugin plugin )
	{
		return _dao.selectLoginListForRoleKey( strRoleKey, plugin );
	}

	/**
	 * Update the reset password attribut of a user from his login
	 * @param strUserName Login of the user to update
	 * @param bNewValue New value
	 */
	public static void updateResetPasswordFromLogin( String strUserName, boolean bNewValue, Plugin plugin )
	{
		_dao.updateResetPasswordFromLogin( strUserName, bNewValue, plugin );
	}

	/**
	 * Get the id of a user from his login
	 * @param strLogin Login of the user
	 * @param plugin The plugin
	 * @return The id of the user
	 */
	public static int findUserIdFromLogin( String strLogin, Plugin plugin )
	{
		return _dao.findUserIdFromLogin( strLogin, plugin );
	}
	
	
	
    /**
     * select the workflow action associate to the directory selected(used when a modification is made on the user )
     * @param nIdDirectory the id directory
     * @param plugin the plugin
     * @return the workflow action associate to the directory selected
     */
	public static  Integer findWorkflowModifyAction( int nIdDirectory, Plugin plugin )
	{
		
		return _dao.selectWorkflowModifyAction(nIdDirectory, plugin);
	}
       
    /**
     * assign the workflow action associate to the directory selected(used when a modification is made on the user )
     * @param nIdDirectory the id directory
     * @param nIdWfAction the worklow action
     * @param the workflow action associate to the directory selected(used when a modification is made on the user )
     */
    public static void assignWorkflowModifyAction( int nIdDirectory,int nIdWfAction, Plugin plugin )
    {
    	
    	_dao.assignWorkflowModifyAction(nIdDirectory, nIdWfAction, plugin);
    }
       
    /**
     * un asign the workflow action associate to the directory selected(used when a modification is made on the user )
     * @param nIdDirectory the id directory
     * @param the workflow action associate to the directory selected(used when a modification is made on the user )
     */
    public static void unAssignWorkflowModifyAction( int nIdDirectory ,Plugin plugin )
    {
    	_dao.unAssignWorkflowModifyAction(nIdDirectory, plugin);
    }

}
