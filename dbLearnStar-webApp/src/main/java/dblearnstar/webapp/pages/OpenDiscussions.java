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

package dblearnstar.webapp.pages;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.SelectModelFactory;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.slf4j.Logger;

import dblearnstar.model.entities.AssessmentDiscussion;
import dblearnstar.model.entities.SolutionAssessment;
import dblearnstar.model.entities.TestInstance;
import dblearnstar.model.model.ComparatorTestInstance;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.annotations.StudentPage;
import dblearnstar.webapp.services.EvaluationService;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;
import dblearnstar.webapp.services.TestManager;
import dblearnstar.webapp.services.UsefulMethods;

@StudentPage
@AdministratorPage
public class OpenDiscussions {

	@Property
	@SessionState
	private UserInfo userInfo;

	@Inject
	private Logger logger;
	@Inject
	private SelectModelFactory selectModelFactory;
	@Inject
	private GenericService genericService;
	@Inject
	private TestManager testManager;
	@Inject
	private PersonManager personManager;
	@Inject
	private EvaluationService evaluationService;


	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;
	@InjectComponent
	private Zone zTestInstance;

	@Persist
	@Property
	TestInstance selectedTestInstance;
	@Property
	SolutionAssessment runningSolutionAssessment;

	public void onActivate() {
		if (selectedTestInstance != null) {
			selectedTestInstance = genericService.getByPK(TestInstance.class, selectedTestInstance.getTestInstanceId());
		}
	}

	public void onActivate(TestInstance ti) {
		selectedTestInstance = ti;
	}

	public List<SolutionAssessment> getSolutionAssessmentsWithDiscussion() {
		return evaluationService
				.getSolutionAssessmentsWithDiscussionForTestInstance(selectedTestInstance.getTestInstanceId());
	}

	public List<TestInstance> getTestInstances() {
		if (userInfo.isAdministrator()) {
			List<AssessmentDiscussion> listAD = UsefulMethods.castList(AssessmentDiscussion.class,
					genericService.getAll(AssessmentDiscussion.class));
			List<TestInstance> tilist = listAD.stream().map(
					p -> p.getSolutionEvaluation().getStudentSubmitSolution().getStudentStartedTest().getTestInstance())
					.distinct().collect(Collectors.toList());
			ComparatorTestInstance c = new ComparatorTestInstance();
			Collections.sort(tilist, c);
			return tilist;
		} else if (userInfo.isStudent()) {
			long studentId = personManager.getStudentsByPersonId(userInfo.getPersonId()).get(0).getStudentId();
			List<AssessmentDiscussion> listAD = UsefulMethods.castList(AssessmentDiscussion.class,
					genericService.getAll(AssessmentDiscussion.class));
			List<TestInstance> tilist = listAD.stream().map(
					p -> p.getSolutionEvaluation().getStudentSubmitSolution().getStudentStartedTest().getTestInstance())
					.distinct().collect(Collectors.toList());
			List<TestInstance> list = testManager.getTestInstancesForStudent(studentId).stream()
					.filter(p -> tilist.contains(p)).collect(Collectors.toList());
			ComparatorTestInstance c = new ComparatorTestInstance();
			Collections.sort(list, c);
			return list;
		} else {
			return null;
		}
	}

	public SelectModel getTestInstanceSelectModel() {
		return selectModelFactory.create(getTestInstances(), "title");
	}

	public void onValueChanged(TestInstance newTestInstance) {
		selectedTestInstance = newTestInstance;
		ajaxResponseRenderer.addRender(zTestInstance);
	}

	public boolean answered(AssessmentDiscussion ad) {
		logger.debug("   ad = {} {}", ad.getAssessmentDiscussionId(), ad.getPerson().getUserName());
		if (ad.getReplies() != null && ad.getReplies().size() > 0) {
			logger.debug("      has replies");
			boolean allAnswered = true;
			for (AssessmentDiscussion i : ad.getReplies()) {
				if (!answered(i)) {
					allAnswered = false;
				}
			}
			return allAnswered;
		} else {
			logger.info("      no replies");
			return personManager.isInstructor(ad.getPerson());
		}
	}

	public boolean isAnsweredByInstructor() {
		boolean allAnswered = true;
		logger.debug("sa = {}", runningSolutionAssessment.getSolutionAssessmentId());
		for (AssessmentDiscussion ad : runningSolutionAssessment.getAssessmentDiscussions().stream()
				.filter(p -> p.getReplyTo() == null).toList()) {
			allAnswered = allAnswered && answered(ad);
		}
		return allAnswered;
	}

}
