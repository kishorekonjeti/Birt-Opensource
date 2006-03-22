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

import org.eclipse.birt.chart.reportitem.plugin.ChartReportItemPlugin;
import org.eclipse.birt.chart.reportitem.ui.i18n.Messages;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for Charting
 */

public class ChartPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage,
		IPropertyChangeListener
{

	private transient Button btnEnableLivePreview;
	private transient IntegerFieldEditor txtMaxRow;

	private static final int MAX_ROW_DEFAULT = 6;

	private static final int MAX_ROW_LIMIT = 10000;

	protected Control createContents( Composite parent )
	{
		Composite cmpTop = new Composite( parent, SWT.NONE );
		{
			GridLayout layout = new GridLayout( 2, false );
			cmpTop.setLayout( layout );
		}

		btnEnableLivePreview = new Button( cmpTop, SWT.CHECK );
		{
			GridData gd = new GridData( );
			gd.horizontalSpan = 2;
			btnEnableLivePreview.setLayoutData( gd );
			btnEnableLivePreview.setText( Messages.getString( "ChartPreferencePage.Label.EnableLivePreview" ) ); //$NON-NLS-1$
			btnEnableLivePreview.setSelection( ChartReportItemPlugin.getDefault( )
					.getPluginPreferences( )
					.getBoolean( ChartReportItemUIActivator.PREFERENCE_ENALBE_LIVE ) );
		}

		txtMaxRow = new IntegerFieldEditor( ChartReportItemUIActivator.PREFERENCE_MAX_ROW,
				Messages.getString( "ChartPreferencePage.Label.MaxRowNumber" ), cmpTop ); //$NON-NLS-1$ 
		{
			txtMaxRow.setErrorMessage( Messages.getString( "ChartPreferencePage.Error.MaxRowInvalid", //$NON-NLS-1$
					new Object[]{
						new Integer( MAX_ROW_LIMIT )
					} ) );
			txtMaxRow.setValidateStrategy( StringFieldEditor.VALIDATE_ON_KEY_STROKE );
			txtMaxRow.setValidRange( 1, MAX_ROW_LIMIT );
			txtMaxRow.setEmptyStringAllowed( false );
			txtMaxRow.setStringValue( ChartReportItemPlugin.getDefault( )
					.getPluginPreferences( )
					.getString( ChartReportItemUIActivator.PREFERENCE_MAX_ROW ) );
			txtMaxRow.setPage( this );
			txtMaxRow.setPropertyChangeListener( this );
		}

		return cmpTop;
	}

	public void init( IWorkbench workbench )
	{
		init( );
	}

	public static void init( )
	{
		int maxRow = ChartReportItemPlugin.getDefault( )
				.getPluginPreferences( )
				.getInt( ChartReportItemUIActivator.PREFERENCE_MAX_ROW );
		if ( maxRow <= 0 )
		{
			ChartReportItemPlugin.getDefault( )
					.getPluginPreferences( )
					.setValue( ChartReportItemUIActivator.PREFERENCE_ENALBE_LIVE,
							true );
			ChartReportItemPlugin.getDefault( )
					.getPluginPreferences( )
					.setValue( ChartReportItemUIActivator.PREFERENCE_MAX_ROW,
							MAX_ROW_DEFAULT );
		}
	}

	public void propertyChange( PropertyChangeEvent event )
	{
		if ( event.getProperty( ).equals( FieldEditor.IS_VALID ) )
		{
			setValid( txtMaxRow.isValid( ) );
		}
	}

	protected void performDefaults( )
	{
		btnEnableLivePreview.setSelection( true );
		txtMaxRow.setStringValue( String.valueOf( MAX_ROW_DEFAULT ) );
		super.performDefaults( );
	}

	public boolean performOk( )
	{
		ChartReportItemPlugin.getDefault( )
				.getPluginPreferences( )
				.setValue( ChartReportItemUIActivator.PREFERENCE_MAX_ROW,
						txtMaxRow.getIntValue( ) );
		ChartReportItemPlugin.getDefault( )
				.getPluginPreferences( )
				.setValue( ChartReportItemUIActivator.PREFERENCE_ENALBE_LIVE,
						btnEnableLivePreview.getSelection( ) );
		ChartReportItemPlugin.getDefault( ).savePluginPreferences( );
		return super.performOk( );
	}

}
