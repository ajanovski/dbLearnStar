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

public class TaskTypeChecker {

	public static boolean isSQL(String codeType) {
		if (codeType != null) {
			return codeType.equals(ModelConstants.TaskCodeSQL);
		} else {
			return false;
		}
	}

	public static boolean isTEXT(String codeType) {
		if (codeType != null) {
			return codeType.equals(ModelConstants.TaskCodeTEXT);
		} else {
			return false;
		}
	}

	public static boolean isDDL(String codeType) {
		if (codeType != null) {
			return codeType.equals(ModelConstants.TaskCodeDDL);
		} else {
			return false;
		}
	}

	public static boolean isUPLOAD(String codeType) {
		if (codeType != null) {
			return codeType.equals(ModelConstants.TaskCodeUPLOAD);
		} else {
			return false;
		}
	}

	public static boolean isTRIGGER(String codeType) {
		if (codeType != null) {
			return codeType.equals(ModelConstants.TaskCodeTRIGGER);
		} else {
			return false;
		}
	}

}
