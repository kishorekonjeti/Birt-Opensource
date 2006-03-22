/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.chart.reportitem.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.birt.chart.exception.ChartException;
import org.eclipse.birt.chart.reportitem.plugin.ChartReportItemPlugin;
import org.eclipse.birt.chart.reportitem.ui.dialogs.ExtendedItemFilterDialog;
import org.eclipse.birt.chart.reportitem.ui.dialogs.ReportItemParametersDialog;
import org.eclipse.birt.chart.ui.swt.interfaces.IDataServiceProvider;
import org.eclipse.birt.chart.ui.swt.wizard.ChartWizard;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.ui.frameworks.taskwizard.WizardBase;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.core.model.views.data.DataSetItemModel;
import org.eclipse.birt.report.designer.internal.ui.util.DataSetManager;
import org.eclipse.birt.report.designer.ui.actions.NewDataSetAction;
import org.eclipse.birt.report.designer.util.DEUtil;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.ReportItemHandle;
import org.eclipse.birt.report.model.api.SharedStyleHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.jface.window.Window;

/**
 * Data service provider for chart builder.
 */

public class ReportDataServiceProvider implements IDataServiceProvider
{

	private ExtendedItemHandle itemHandle;

	/**
	 * This flag indicates whether the error is found when fetching data. This
	 * is to help reduce invalid query.
	 */
	private boolean isErrorFound = false;

	public ReportDataServiceProvider( ExtendedItemHandle itemHandle )
	{
		super( );
		this.itemHandle = itemHandle;
	}

	public String[] getAllDataSets( )
	{
		List list = SessionHandleAdapter.getInstance( )
				.getReportDesignHandle( )
				.getVisibleDataSets( );
		String[] names = new String[list.size( )];
		for ( int i = 0; i < names.length; i++ )
		{
			names[i] = ( (DataSetHandle) list.get( i ) ).getQualifiedName( );
		}
		return names;
	}

	public final String[] getPreviewHeader( ) throws ChartException
	{
		DataSetItemModel[] columnsModel = getPreviewHeaderModel( );
		if ( columnsModel == null )
		{
			return null;
		}
		String[] header = new String[columnsModel.length];
		for ( int i = 0; i < header.length; i++ )
		{
			header[i] = columnsModel[i].getDisplayName( );
		}
		return header;
	}

	protected DataSetItemModel[] getPreviewHeaderModel( )
	{
		DataSetItemModel[] headers = null;
		try
		{
			headers = DataSetManager.getCurrentInstance( )
					.getCacheMetaData( getDataSetFromHandle( ),
							itemHandle.getPropertyHandle( ExtendedItemHandle.PARAM_BINDINGS_PROP )
									.iterator( ) );
			isErrorFound = false;
		}
		catch ( BirtException e )
		{
			WizardBase.displayException( e );
			isErrorFound = true;
		}
		return headers;
	}

	public final List getPreviewData( ) throws ChartException
	{
		return getPreviewRowData( null, -1, true );
	}

	protected final List getPreviewRowData( String[] columnExpression,
			int rowCount, boolean isStringType ) throws ChartException
	{
		ArrayList dataList = new ArrayList( );
		
		// Set thread context class loader so Rhino can find POJOs in workspace
		// projects
		ClassLoader oldContextLoader = Thread.currentThread( )
				.getContextClassLoader( );
		ClassLoader parentLoader = oldContextLoader;
		if ( parentLoader == null )
			parentLoader = this.getClass( ).getClassLoader( );
		ClassLoader newContextLoader = DataSetManager.getCustomScriptClassLoader( parentLoader );
		Thread.currentThread( ).setContextClassLoader( newContextLoader );
		
		try
		{
			DataSetHandle datasetHandle = getDataSetFromHandle( );
			IQueryResults actualResultSet = DataSetManager.getCurrentInstance( )
					.getCacheResult( datasetHandle,
							itemHandle.getPropertyHandle( ReportItemHandle.PARAM_BINDINGS_PROP )
									.iterator( ),
							itemHandle.getPropertyHandle( ExtendedItemHandle.FILTER_PROP )
									.iterator( ),
							columnExpression,
							rowCount <= 0 ? getMaxRow( ) : rowCount );
			if ( actualResultSet != null )
			{
				IBaseExpression[] expressions = extractExpressions( actualResultSet );
				int columnCount = expressions.length;
				IResultIterator iter = actualResultSet.getResultIterator( );
				while ( iter.next( ) )
				{
					if ( isStringType )
					{
						String[] record = new String[columnCount];
						for ( int n = 0; n < columnCount; n++ )
						{
							record[n] = iter.getString( expressions[n] );
						}
						dataList.add( record );
					}
					else
					{
						Object[] record = new Object[columnCount];
						for ( int n = 0; n < columnCount; n++ )
						{
							record[n] = iter.getValue( expressions[n] );
						}
						dataList.add( record );
					}
				}

				actualResultSet.close( );
			}
		}
		catch ( BirtException e )
		{
			throw new ChartException( ChartReportItemPlugin.ID,
					ChartException.DATA_BINDING,
					e );
		}
		finally
		{
			// Restore old thread context class loader
			Thread.currentThread( ).setContextClassLoader( oldContextLoader );
		}
		
		return dataList;
	}

	private IBaseExpression[] extractExpressions( IQueryResults qr )
	{
		Collection col = qr.getPreparedQuery( )
				.getReportQueryDefn( )
				.getRowExpressions( );
		return (IBaseExpression[]) col.toArray( new IBaseExpression[col.size( )] );
	}

	public String getBoundDataSet( )
	{
		if ( itemHandle.getDataSet( ) == null )
		{
			return null;
		}
		return itemHandle.getDataSet( ).getQualifiedName( );
	}

	public String getReportDataSet( )
	{
		List list = DEUtil.getDataSetList( itemHandle.getContainer( ) );
		if ( list.size( ) > 0 )
		{
			return ( (DataSetHandle) list.get( 0 ) ).getQualifiedName( );
		}
		return null;
	}

	public void setContext( Object context )
	{
		itemHandle = (ExtendedItemHandle) context;
	}

	public void setDataSet( String datasetName )
	{
		try
		{
			if ( datasetName == null )
			{
				itemHandle.setDataSet( null );
			}
			else
			{
				DataSetHandle dataset = SessionHandleAdapter.getInstance( )
						.getReportDesignHandle( )
						.findDataSet( datasetName );
				if ( itemHandle.getDataSet( ) != dataset )
				{
					// Clear parameters and filters binding if dataset changed
					itemHandle.getPropertyHandle( ReportItemHandle.PARAM_BINDINGS_PROP )
							.clearValue( );
					itemHandle.getPropertyHandle( ExtendedItemHandle.FILTER_PROP )
							.clearValue( );
					itemHandle.setDataSet( dataset );
				}
			}
		}
		catch ( SemanticException e )
		{
			ChartWizard.displayException( e );
		}
	}

	/**
	 * Gets dataset from ReportItemHandle at first. If null, get dataset from
	 * its container.
	 * 
	 * @return direct dataset
	 */
	protected DataSetHandle getDataSetFromHandle( )
	{
		if ( itemHandle.getDataSet( ) != null )
		{
			return itemHandle.getDataSet( );
		}
		List datasetList = DEUtil.getDataSetList( itemHandle.getContainer( ) );
		if ( datasetList.size( ) > 0 )
		{
			return (DataSetHandle) datasetList.get( 0 );
		}
		return null;
	}

	public int invoke( int command )
	{
		if ( command == COMMAND_NEW_DATASET )
		{
			return invokeNewDataSet( );
		}
		else if ( command == COMMAND_EDIT_FILTER )
		{
			return invokeEditFilter( );
		}
		else if ( command == COMMAND_EDIT_PARAMETER )
		{
			return invokeEditParameter( );
		}
		return Window.CANCEL;
	}

	protected int invokeNewDataSet( )
	{
		new NewDataSetAction( ).run( );
		return Window.CANCEL;
	}

	protected int invokeEditFilter( )
	{
		ExtendedItemFilterDialog page = new ExtendedItemFilterDialog( itemHandle,
				this );
		return page.open( );
	}

	protected int invokeEditParameter( )
	{
		ReportItemParametersDialog page = new ReportItemParametersDialog( itemHandle );
		return page.open( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.interfaces.IDataServiceProvider#getAllStyles()
	 */
	public String[] getAllStyles( )
	{
		List list = SessionHandleAdapter.getInstance( )
				.getReportDesignHandle( )
				.getAllStyles( );
		String[] names = new String[list.size( )];
		for ( int i = 0; i < names.length; i++ )
		{
			names[i] = ( (SharedStyleHandle) list.get( i ) ).getName( );
		}
		Arrays.sort( names );
		return names;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.interfaces.IDataServiceProvider#getCurrentStyle()
	 */
	public String getCurrentStyle( )
	{
		if ( itemHandle.getStyle( ) == null )
		{
			return null;
		}
		return itemHandle.getStyle( ).getName( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.interfaces.IDataServiceProvider#setStyle(java.lang.String)
	 */
	public void setStyle( String styleName )
	{
		try
		{
			if ( styleName == null )
			{
				itemHandle.setStyle( null );
			}
			else
			{
				itemHandle.setStyle( getStyle( styleName ) );
			}
		}
		catch ( SemanticException e )
		{
			ChartWizard.displayException( e );
		}
	}

	private SharedStyleHandle getStyle( String styleName )
	{
		return SessionHandleAdapter.getInstance( )
				.getReportDesignHandle( )
				.findStyle( styleName );
	}

	public final Object[] getDataForColumns( String[] sExpressions,
			int iMaxRecords, boolean byRow ) throws ChartException
	{
		List rowData = getPreviewRowData( sExpressions, iMaxRecords, false );
		if ( byRow )
		{
			return rowData.toArray( );
		}
		List columnData = new ArrayList( );
		for ( int i = 0; i < sExpressions.length; i++ )
		{
			Object[] columnArray = new Object[rowData.size( )];
			for ( int j = 0; j < rowData.size( ); j++ )
			{
				columnArray[j] = ( (Object[]) rowData.get( j ) )[i];
			}
			columnData.add( columnArray );
		}
		return columnData.toArray( );
	}

	public void dispose( )
	{
		// Do nothing
	}

	private int getMaxRow( )
	{
		return ChartReportItemPlugin.getDefault( )
				.getPluginPreferences( )
				.getInt( ChartReportItemUIActivator.PREFERENCE_MAX_ROW );
	}

	public boolean isLivePreviewEnabled( )
	{
		return !isErrorFound
				&& ChartReportItemPlugin.getDefault( )
						.getPluginPreferences( )
						.getBoolean( ChartReportItemUIActivator.PREFERENCE_ENALBE_LIVE );
	}

}
