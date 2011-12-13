/*
 * Copyright (c) 2002-2011, Mairie de Paris
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
package fr.paris.lutece.plugins.mylutece.modules.directory.authentication.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.directory.business.DirectoryRemovalListenerService;
import fr.paris.lutece.plugins.directory.business.EntryFilter;
import fr.paris.lutece.plugins.directory.business.EntryHome;
import fr.paris.lutece.plugins.directory.business.IEntry;
import fr.paris.lutece.plugins.directory.business.Record;
import fr.paris.lutece.plugins.directory.business.RecordField;
import fr.paris.lutece.plugins.directory.utils.DirectoryErrorException;
import fr.paris.lutece.plugins.directory.utils.DirectoryUtils;
import fr.paris.lutece.plugins.mylutece.authentication.MultiLuteceAuthentication;
import fr.paris.lutece.plugins.mylutece.business.attribute.AttributeField;
import fr.paris.lutece.plugins.mylutece.business.attribute.AttributeFieldHome;
import fr.paris.lutece.plugins.mylutece.business.attribute.AttributeHome;
import fr.paris.lutece.plugins.mylutece.business.attribute.IAttribute;
import fr.paris.lutece.plugins.mylutece.business.attribute.MyLuteceUserFieldFilter;
import fr.paris.lutece.plugins.mylutece.business.attribute.MyLuteceUserFieldHome;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.BaseAuthentication;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.business.AttributeMapping;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.business.MyluteceDirectoryDirectoryRemovalListener;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.business.MyluteceDirectoryUser;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.business.parameter.MyluteceDirectoryParameterHome;
import fr.paris.lutece.plugins.mylutece.modules.directory.authentication.web.FormErrors;
import fr.paris.lutece.plugins.mylutece.service.MyLutecePlugin;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;


/**
 *
 * DatabaseService
 *
 */
public class MyluteceDirectoryService
{
	private static final String AUTHENTICATION_BEAN_NAME = "myluteceDirectory.authentication";
    
    // CONSTANTS
    private static final String EMPTY_STRING = "";
    private static final String COMMA = ",";
    
    // PROPERTIES
    private static final String PROPERTY_ENCRYPTION_ALGORITHMS_LIST = "encryption.algorithmsList";
    
	// PARAMETERS
	private static final String PARAMETER_SEARCH_IS_SEARCH = "search_is_search";
	public static final String PARAMETER_ENABLE_PASSWORD_ENCRYPTION = "enable_password_encryption";
    public static final String PARAMETER_ENCRYPTION_ALGORITHM = "encryption_algorithm";
	
	// MARKS
    private static final String MARK_SEARCH_IS_SEARCH = "search_is_search";
    private static final String MARK_SEARCH_MYLUTECE_USER_FIELD_FILTER = "search_mylutece_user_field_filter";
    private static final String MARK_ATTRIBUTES_LIST = "attributes_list";
    private static final String MARK_LOCALE = "locale";
    private static final String MARK_SORT_SEARCH_ATTRIBUTE = "sort_search_attribute";
    private static final String MARK_ENABLE_PASSWORD_ENCRYPTION = "enable_password_encryption";
    private static final String MARK_ENCRYPTION_ALGORITHM = "encryption_algorithm";
    private static final String MARK_ENCRYPTION_ALGORITHMS_LIST = "encryption_algorithms_list";
    
    // ERRORS
    private static final String ERROR_DIRECTORY_FIELD = "error_directory_field";

    // PROPERTIES
    private static final String PROPERTY_ERROR_FIELD = "module.mylutece.directory.message.error.field";
    private static final String PROPERTY_ERROR_MANDATORY_FIELDS = "module.mylutece.directory.message.account.errorMandatoryFields";

    private static MyluteceDirectoryService _singleton = new MyluteceDirectoryService(  );
    private static MyluteceDirectoryDirectoryRemovalListener _listenerDirectory;
    
    /**
    * Initialize the Database service
    *
    */
    public void init(  )
    {
        // Create removal listeners for Directory and register them
        if ( _listenerDirectory == null )
        {
            _listenerDirectory = new MyluteceDirectoryDirectoryRemovalListener(  );
            DirectoryRemovalListenerService.getService(  ).registerListener( _listenerDirectory );
        }

        // Call init for MyluteceDirectoryUser to register others removal listeners
        MyluteceDirectoryUser.init(  );
        AttributeMapping.init(  );
        
        BaseAuthentication baseAuthentication = ( BaseAuthentication ) SpringContextService.getPluginBean( 
        		MyluteceDirectoryPlugin.PLUGIN_NAME, AUTHENTICATION_BEAN_NAME );
        if ( baseAuthentication != null )
        {
        	MultiLuteceAuthentication.registerAuthentication( baseAuthentication );
	    }
	    else
	    {
	    	AppLogService.error( "BaseAuthentication not found, please check your mylutece-directory_context.xml configuration" );
	    }
        
    }

    /**
     * Returns the instance of the singleton
     *
     * @return The instance of the singleton
     */
    public static MyluteceDirectoryService getInstance(  )
    {
        return _singleton;
    }
    
    /**
     * Get the filtered list of admin users
     * @param listUsers the initial list of users
     * @param request HttpServletRequest
     * @param model map
     * @param url URL of the current interface
     * @return The filtered list of admin users
     */
    public List<Integer> getFilteredUsersInterface
    	( List<Integer> listUserIds, HttpServletRequest request, Map<String, Object> model, UrlItem url )
    {
    	String strIsSearch = request.getParameter( PARAMETER_SEARCH_IS_SEARCH );
    	boolean bIsSearch = strIsSearch != null ? true : false;
    	
    	Plugin myLutecePlugin = PluginService.getPlugin( MyLutecePlugin.PLUGIN_NAME );
        List<Integer> listFilteredUserIds = new ArrayList<Integer>(  );
        
    	MyLuteceUserFieldFilter mlFieldFilter= new MyLuteceUserFieldFilter(  );
    	mlFieldFilter.setMyLuteceUserFieldFilter( request, request.getLocale(  ) );
        List<Integer> listFilteredUserIdsByUserFields = MyLuteceUserFieldHome.findUsersByFilter( mlFieldFilter, myLutecePlugin );
        
        if ( listFilteredUserIdsByUserFields != null )
        {
        	for ( Integer nFilteredUserId : listUserIds )
            {
            	for ( Integer nFilteredUserIdByUserField : listFilteredUserIdsByUserFields )
            	{
            		if ( nFilteredUserId == nFilteredUserIdByUserField )
            		{
            			listFilteredUserIds.add( nFilteredUserId );
            		}
            	}
            }
        }
        else
        {
        	listFilteredUserIds = listUserIds;
        }
        
        List<IAttribute> listAttributes = AttributeHome.findAll( request.getLocale(  ), myLutecePlugin );
        for ( IAttribute attribute : listAttributes )
        {
        	List<AttributeField> listAttributeFields = AttributeFieldHome.selectAttributeFieldsByIdAttribute( 
        			attribute.getIdAttribute(  ), myLutecePlugin );
        	attribute.setListAttributeFields( listAttributeFields );
        }
        
        String strSortSearchAttribute = EMPTY_STRING;
        if( bIsSearch )
        {
        	mlFieldFilter.setUrlAttributes( url );
        	strSortSearchAttribute = mlFieldFilter.getUrlAttributes(  );
        }
        
        model.put( MARK_SEARCH_IS_SEARCH, bIsSearch );
        model.put( MARK_SEARCH_MYLUTECE_USER_FIELD_FILTER, mlFieldFilter );
        model.put( MARK_LOCALE, request.getLocale(  ) );
        model.put( MARK_ATTRIBUTES_LIST, listAttributes );
        model.put( MARK_SORT_SEARCH_ATTRIBUTE, strSortSearchAttribute );
        
        return listFilteredUserIds;
    }

    /**
     * Get the map of id entry - list record fields from a given record
     * @param record the record
     * @return a map of id entry - list record fields
     */
    public Map<String, List<RecordField>> getMapIdEntryListRecordField( Record record )
    {
    	Map<String, List<RecordField>> map = new HashMap<String, List<RecordField>>(  );
    	List<RecordField> listRecordFields = record.getListRecordField(  );
    	if ( listRecordFields != null && !listRecordFields.isEmpty(  ) )
    	{
    		for ( RecordField recordField : listRecordFields )
    		{
    			String strIdEntry = Integer.toString( recordField.getEntry(  ).getIdEntry(  ) );
    			List<RecordField> listAssociatedRecordFields = map.get( strIdEntry );
    			if ( listAssociatedRecordFields == null )
    			{
    				listAssociatedRecordFields = new ArrayList<RecordField>(  );
    			}
    			listAssociatedRecordFields.add( recordField );
    			map.put( strIdEntry, listAssociatedRecordFields );
    		}
    	}

        return map;
    }

    /**
     * Get the directory record data
     * @param request the HTTP request
     * @param record the record
     * @param pluginDirectory the plugin
     * @param locale the locale
     * @param formErrors the form errors
     */
    public void getDirectoryRecordData( HttpServletRequest request, Record record, Plugin pluginDirectory, Locale locale, 
    		FormErrors formErrors )
    {
    	List<RecordField> listRecordFieldResult = new ArrayList<RecordField>(  );
        EntryFilter filter = new EntryFilter(  );
        filter.setIdDirectory( record.getDirectory(  ).getIdDirectory(  ) );
        filter.setIsComment( EntryFilter.FILTER_FALSE );
        filter.setIsEntryParentNull( EntryFilter.FILTER_TRUE );

        List<IEntry> listEntryFirstLevel = EntryHome.getEntryList( filter, pluginDirectory );

        for ( IEntry entry : listEntryFirstLevel )
        {
        	entry = EntryHome.findByPrimaryKey( entry.getIdEntry(  ), pluginDirectory );
        	if ( entry.getEntryType(  ).getGroup(  ) )
        	{
        		for ( IEntry entryChild : entry.getChildren(  ) )
        		{
        			getDirectoryRecordFieldData( record, request, entryChild.getIdEntry(  ), listRecordFieldResult, pluginDirectory, locale, formErrors );
        		}
        	}
        	else if ( !entry.getEntryType(  ).getComment(  ) )
        	{
        		getDirectoryRecordFieldData( record, request, entry.getIdEntry(  ), listRecordFieldResult, pluginDirectory, locale, formErrors );
        	}
        }
        
        record.setListRecordField( listRecordFieldResult );
    }

    /**
     * Get the directory record field data
     * @param record the record
     * @param request the request
     * @param nIdEntry the id entry
     * @param listRecordFieldResult the list of record field
     * @param pluginDirectory the plugin
     * @param locale the locale
     * @param formErrors the form errors
     */
    private void getDirectoryRecordFieldData( Record record, HttpServletRequest request, int nIdEntry, 
    		List<RecordField> listRecordFieldResult, Plugin pluginDirectory, Locale locale, FormErrors formErrors )
    {
    	try
		{
			DirectoryUtils.getDirectoryRecordFieldData( record, request, nIdEntry, true,
			        listRecordFieldResult, pluginDirectory, locale );
		}
		catch ( DirectoryErrorException e )
		{
			if ( e.isMandatoryError(  ) )
			{
				Object[] params = { e.getTitleField(  ) };
				String strErrorMessage = I18nService.getLocalizedString( PROPERTY_ERROR_MANDATORY_FIELDS, params, locale );
				formErrors.addError( ERROR_DIRECTORY_FIELD, strErrorMessage );
			}
			else
			{
				Object[] params = { e.getTitleField(  ), e.getErrorMessage(  ) };
				String strErrorMessage = I18nService.getLocalizedString( PROPERTY_ERROR_FIELD, params, locale );
				formErrors.addError( ERROR_DIRECTORY_FIELD, strErrorMessage );
			}
		}
    }
    
    /**
     * Build the advanced parameters management
     * @param request HttpServletRequest
     * @return The model for the advanced parameters
     */
    public static Map<String, Object> getManageAdvancedParameters( AdminUser user )
    {
    	Map<String, Object> model = new HashMap<String, Object>(  );
    	Plugin plugin = PluginService.getPlugin( MyluteceDirectoryPlugin.PLUGIN_NAME );
    	
    	// Encryption Password
    	if ( RBACService.isAuthorized( MyluteceDirectoryResourceIdService.RESOURCE_TYPE, RBAC.WILDCARD_RESOURCES_ID, 
    			MyluteceDirectoryResourceIdService.PERMISSION_MANAGE, user ) )
    	{
    		String[] listAlgorithms = AppPropertiesService.getProperty( PROPERTY_ENCRYPTION_ALGORITHMS_LIST ).split( COMMA );
        	for ( String strAlgorithm : listAlgorithms )
        	{
        		strAlgorithm.trim(  );
        	}
        	
    		model.put( MARK_ENABLE_PASSWORD_ENCRYPTION, 
    				MyluteceDirectoryParameterHome.findByKey( PARAMETER_ENABLE_PASSWORD_ENCRYPTION, plugin ).getName(  ) );
        	model.put( MARK_ENCRYPTION_ALGORITHM, 
        			MyluteceDirectoryParameterHome.findByKey( PARAMETER_ENCRYPTION_ALGORITHM, plugin ).getName(  ) );
        	model.put( MARK_ENCRYPTION_ALGORITHMS_LIST, listAlgorithms );
    	}
    	
    	return model;
    }
}
