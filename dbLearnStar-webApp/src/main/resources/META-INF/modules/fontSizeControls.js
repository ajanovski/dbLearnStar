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

define(["jquery", "t5/core/ajax"], function($, ajax) {

    $('#toggleWordWrap').click(function() {
		var felements = document.querySelectorAll('pre');
		if (felements[i].style.whiteSpace == null || felements[i].style.whiteSpace == "") {
		    felements[i].style.whiteSpace = "pre-wrap";
		} else if (felements[i].style.whiteSpace == "pre") {
		    felements[i].style.whiteSpace = "pre-wrap";
		} else {
		    felements[i].style.whiteSpace = "pre";
		}
    });

    $('#increaseFontSize').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            currentSize = Math.round(parseFloat(window.getComputedStyle(felements[i]).fontSize));
            felements[i].style.fontSize = (currentSize + 1) + "px";
        }
    });

    $('#decreaseFontSize').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            currentSize = Math.round(parseFloat(window.getComputedStyle(felements[i]).fontSize));
            felements[i].style.fontSize = (currentSize - 1) + "px";
        }
    });

    $('#resetFontSize').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            felements[i].style.fontSize = "14px";
        }
    });

    $('#tableToggleWordWrap').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            if (felements[i].style.whiteSpace == null || felements[i].style.whiteSpace == "") {
                felements[i].style.whiteSpace = "pre-wrap";
            } else if (felements[i].style.whiteSpace == "pre") {
                felements[i].style.whiteSpace = "pre-wrap";
            } else {
                felements[i].style.whiteSpace = "pre";
            }
        }
    });

    $('#tableFontSizeIncrease').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            currentSize = Math.round(parseFloat(window.getComputedStyle(felements[i]).fontSize));
            felements[i].style.fontSize = (currentSize + 1) + "px";
        }
    });

    $('#tableFontSizeDecrease').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            currentSize = Math.round(parseFloat(window.getComputedStyle(felements[i]).fontSize));
            felements[i].style.fontSize = (currentSize - 1) + "px";
        }
    });

    $('#tableFontSizeReset').click(function() {
        var felements = document.querySelectorAll('pre');
        for (var i = 0;i < felements.length;i++) {
            felements[i].style.fontSize = "14px";
        }
    });


});
