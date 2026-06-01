/*******************************************************************************
 * Copyright (C) 2021 Vangel V. Ajanovski
 *     
 * This file is part of the dbLearnStar system (hereinafter: dbLearn*).
 *     
 * dbLearn* is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *     
 * dbLearn* is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
 * details.
 *     
 * You should have received a copy of the GNU General Public License along 
 * with dbLearn*.  If not, see <https://www.gnu.org/licenses/>.
 * 
 ******************************************************************************/

package dblearnstar.model.model;

public class ModelConstants {

	public static final String TaskCodeSQL = "SQL";
	public static final String TaskCodeDDL = "DDL";
	public static final String TaskCodeTEXT = "TEXT";
	public static final String TaskCodeUPLOAD = "UPLOAD";
	public static final String TaskCodeTRIGGER = "TRIGGER";

	public static final String ActivityEval = "TASK_EVAL";
	public static final String ActivityExecAll = "TASK_RUNONLY";
	public static final String ActivityExecSelection = "TASK_RUNONLY_SELECTION";
	public static final String ActivityPlan = "TASK_PLANONLY";
	public static final String ActivityViewTask = "VIEW_TASK";
	public static final String ActivityStillViewing = "TASK_STILL_VIEWING";
	public static final String ActivitySubmitText = "TASK_SUBMIT_TEXT";
	public static final String ActivityTryUpload = "TRIED TASK_UPLOAD";

	public static final String PersonDeactivatedSuffix = "---";

	/**
	 * Pages
	 */
	public static final String PageIndex = "Index";

	public static final Object InstructorRole = "INSTRUCTOR";
	public static final Object AdministratorRole = "ADMINISTRATOR";

	/*
	 * Object types
	 */
	public static final String DBRelationalObjectTypesTABLE = "TABLE";
	public static final String DBRelationalObjectTypesPK = "PK";
	public static final String DBRelationalObjectTypesFK = "FK";
	public static final String DBRelationalObjectTypesNN = "NN";
	public static final String DBRelationalObjectTypesCHECK = "CHECK";
	public static final String DBRelationalObjectTypesUK = "UK";
	public static final String DBRelationalObjectTypesDATA = "DATA";

	public static final String[] AllDBRelationalObjectTypes = { DBRelationalObjectTypesTABLE, DBRelationalObjectTypesPK,
			DBRelationalObjectTypesFK, DBRelationalObjectTypesNN, DBRelationalObjectTypesCHECK,
			DBRelationalObjectTypesUK, DBRelationalObjectTypesDATA };
	
	public static final String SystemParameterUserPreferences = "UserPreferences";
	public static final String SystemParameterTypeCodeMirrorTheme = "CodeMirror-Theme";

}
