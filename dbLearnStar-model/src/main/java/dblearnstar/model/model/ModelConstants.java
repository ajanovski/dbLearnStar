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

	public final static String	TaskCodeSQL		= "SQL";
	public final static String	TaskCodeDDL		= "DDL";
	public final static String	TaskCodeTEXT	= "TEXT";
	public final static String	TaskCodeUPLOAD	= "UPLOAD";

   public static final String ActivityEval = "TASK_EVAL";
   public static final String ActivityExecAll = "TASK_RUNONLY";
   public static final String ActivityExecSelection = "TASK_RUNONLY_SELECTION";
   public static final String ActivityPlan = "TASK_PLANONLY";
   public static final String ActivityViewTask = "VIEW_TASK";
   public static final String ActivityStillViewing = "TASK_STILL_VIEWING";
   public static final String ActivitySubmitText = "TASK_SUBMIT_TEXT";
   public static final String ActivityTryUpload = "TRIED TASK_UPLOAD";

	
	/**
	 * Pages
	 */
	public final static String	PageIndex		= "Index";

}
