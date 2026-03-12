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

package dblearnstar.webapp.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.Session;
import org.slf4j.Logger;

import dblearnstar.model.entities.Model;
import dblearnstar.model.entities.SolutionAssessment;
import dblearnstar.model.entities.Student;
import dblearnstar.model.entities.StudentStartedTest;
import dblearnstar.model.entities.StudentSubmitSolution;
import dblearnstar.model.entities.Task;
import dblearnstar.model.entities.TaskInTestInstance;
import dblearnstar.model.entities.TaskIsOfType;
import dblearnstar.model.entities.TaskType;
import dblearnstar.model.entities.TestCollection;
import dblearnstar.model.entities.TestInstance;
import dblearnstar.model.entities.TestType;

public class TestManagerImpl implements TestManager {

	@Inject
	private Logger logger;

	@Inject
	private GenericService genericService;

	@Inject
	private Session session;

	@Override
	public boolean accessToTaskInTestInstanceAllowed(Student student, TaskInTestInstance tti) {
		// need a more appropriate implementation
		return getTestInstancesForStudentByTestType(student.getStudentId(),
				tti.getTestInstance().getTestTemplate().getTestType().getTestTypeId()).stream()
				.anyMatch(ti -> ti.getTestInstanceId() == tti.getTestInstance().getTestInstanceId());
	}

	@Override
	public List<TestInstance> getAllCurrentlyAvailableTestInstancesByTestType(long testTypeId) {
		return UsefulMethods.castList(TestInstance.class, getEntityManager().createQuery("""
				from TestInstance t
				where
					(t.scheduledFor is null or t.scheduledFor<=current_timestamp()) and
					t.testTemplate.testType.testTypeId = :testTypeId
				order by t.title desc
				""").setParameter("testTypeId", testTypeId).getResultList());
	}

	@Override
	public List<SolutionAssessment> getAllEvaluationsOfSolutionsForTaskInTestInstance(long studentId,
			long taskInTestInstanceId) {
		try {
			Query q = getEntityManager().createQuery("""
					select sa
					from SolutionAssessment sa
					where
						sa.studentSubmitSolution.studentStartedTest.student.studentId = :studentId and
					    sa.studentSubmitSolution.taskInTestInstance.taskInTestInstanceId = :taskInTestInstanceId
					order by sa.evaluatedOn desc, sa.studentSubmitSolution.submittedOn desc
					""");
			q.setParameter("studentId", studentId);
			q.setParameter("taskInTestInstanceId", taskInTestInstanceId);
			List<SolutionAssessment> output = UsefulMethods.castList(SolutionAssessment.class, q.getResultList());
			return output;
		} catch (Exception e) {
			logger.error("getAllEvaluationsOfSolutionsForTaskInTestInstance failed {}", e);
			return null;
		}
	}

	@Override
	public List<Model> getAllModels() {
		return UsefulMethods.castList(Model.class, genericService.getAll(Model.class));
	}

	@Override
	public List<TaskType> getAllTaskTypes() {
		return UsefulMethods.castList(TaskType.class, genericService.getAll(TaskType.class)).stream()
				.sorted((o1, o2) -> {
					return o1.getTitle().compareTo(o2.getTitle());
				}).toList();
	}

	@Override
	public List<TaskType> getAllTaskTypesDefinedOverModel(Model chosenModel) {
		List<TaskType> list = UsefulMethods.castList(TaskType.class,
				getEntityManager()
						.createQuery(
								"""
										select distinct tiot.taskType
										from TaskInTestInstance tti
										join tti.task.taskIsOfTypes tiot
										where
											(tti.task.model.modelId is not null and tti.task.model.modelId=:modelId) or
											(tti.testInstance.testTemplate.model.modelId is not null and tti.testInstance.testTemplate.model.modelId=:modelId)
										order by tiot.taskType.title asc
										""")
						.setParameter("modelId", chosenModel.getModelId()).getResultList());
		return list;
	}

	@Override
	public List<TestInstance> getAllTestInstances() {
		List<TestInstance> testInstances = UsefulMethods.castList(TestInstance.class,
				getEntityManager().createQuery("from TestInstance order by title desc").getResultList());
		return testInstances;
	}

	@Override
	public List<TestInstance> getAllTestInstancesByTestType(long testTypeId) {
		String query = """
				from TestInstance t
				where
					t.testTemplate.testType.testTypeId = :testTypeId
				order by t.ordering asc, t.title desc
				""";
		return UsefulMethods.castList(TestInstance.class,
				getEntityManager().createQuery(query).setParameter("testTypeId", testTypeId).getResultList());
	}

	@Override
	public List<TestInstance> getAllTestInstancesByTestTypeAndCollection(long testTypeId, long testCollectionId) {
		String query = """
				from TestInstance t
				where
					t.testTemplate.testType.testTypeId = :testTypeId and
					t.testCollection.testCollectionId = :testCollectionId
				order by t.ordering asc, t.title desc
				""";
		return UsefulMethods.castList(TestInstance.class,
				getEntityManager().createQuery(query).setParameter("testTypeId", testTypeId)
						.setParameter("testCollectionId", testCollectionId).getResultList());
	}

	@Override
	public List<TestType> getAllTestTypes() {
		return UsefulMethods.castList(TestType.class, genericService.getAll(TestType.class)).stream()
				.sorted((o1, o2) -> {
					return o1.getTitle().compareTo(o2.getTitle());
				}).toList();
	}

	@Override
	public String getCodeType(StudentSubmitSolution submittedSolution) {
		List<TaskIsOfType> listTypes = submittedSolution.getTaskInTestInstance().getTask().getTaskIsOfTypes();
		if (listTypes != null && listTypes.size() > 0) {
			return listTypes.get(0).getTaskType().getCodetype();
		} else {
			return "/";
		}
	}

	@Override
	public List<StudentSubmitSolution> getCorrectSolutionsByStudentAndTaskInTestInstance(long studentId,
			long taskInTestInstanceId) {
		try {
			Query q = getEntityManager().createQuery("""
					select sss
					from StudentSubmitSolution sss
					left outer join sss.evaluations eval
					where
						((eval.solutionAssessmentId is not null) or
						(sss.evaluationSimple=true and sss.evaluationComplex=true)) and
						sss.studentStartedTest.student.studentId=:studentId and
						sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId
					order by sss.submittedOn desc
					""");
			q.setParameter("studentId", studentId);
			q.setParameter("taskInTestInstanceId", taskInTestInstanceId);
			return UsefulMethods.castList(StudentSubmitSolution.class, q.getResultList());
		} catch (Exception e) {
			logger.error("getCorrectSolutionsByStudentAndTaskInTestInstance failed {}", e);
			return null;
		}
	}

	private Session getEntityManager() {
		return session.getSession();
	}

	@Override
	public List<StudentSubmitSolution> getEvaluatedSolutionsForTaskInTestInstance(long studentId,
			long taskInTestInstanceId) {
		try {
			Query q = getEntityManager().createQuery("""
					select sss
					from StudentSubmitSolution sss
					left outer join sss.evaluations eval
					where
						sss.studentStartedTest.student.studentId = :studentId and
					    sss.taskInTestInstance.taskInTestInstanceId = :taskInTestInstanceId
					order by eval.evaluatedOn desc, sss.submittedOn desc
					""");
			q.setParameter("studentId", studentId);
			q.setParameter("taskInTestInstanceId", taskInTestInstanceId);
			List<StudentSubmitSolution> output = UsefulMethods.castList(StudentSubmitSolution.class, q.getResultList());
			return output;
		} catch (Exception e) {
			logger.error("getEvaluatedSolutionsForTaskInTestInstance failed {}", e);
			return null;
		}
	}

	@Override
	public Float getGradeForTaskInTestInstanceByStudent(long taskInTestInstanceId, long studentId) {
		try {
			return (Float) getEntityManager().createQuery("""
					select max(sa.grade) from SolutionAssessment sa join sa.studentSubmitSolution sss
					where
						sss.taskInTestInstance.taskInTestInstanceId = :taskInTestInstanceId and
						sss.studentStartedTest.student.studentId = :studentId
					""").setParameter("taskInTestInstanceId", taskInTestInstanceId).setParameter("studentId", studentId)
					.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<StudentSubmitSolution> getHistoryOfSolutions(long taskInTestInstanceId, Boolean filterNotForEvalution,
			long studentId) {
		return UsefulMethods.castList(StudentSubmitSolution.class, getEntityManager().createQuery("""
				from StudentSubmitSolution sss
				where
					sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId and
					sss.notForEvaluation=:filterNotForEvalution and
					sss.studentStartedTest.student.studentId=:studentId
				order by sss.submittedOn desc
				""").setParameter("taskInTestInstanceId", taskInTestInstanceId).setParameter("studentId", studentId)
				.setParameter("filterNotForEvalution", filterNotForEvalution).getResultList());
	}

	@Override
	public List<StudentSubmitSolution> getIncorrectSolutionsByStudentAndTaskInTestInstance(long studentId,
			long taskInTestInstanceId) {
		try {
			Query q = getEntityManager().createQuery("""
					from StudentSubmitSolution sss
					where
						not(sss.evaluationSimple=true and sss.evaluationComplex=true) and
						sss.studentStartedTest.student.studentId=:studentId and
						sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId
					order by sss.submittedOn desc
					""");
			q.setParameter("studentId", studentId);
			q.setParameter("taskInTestInstanceId", taskInTestInstanceId);
			return UsefulMethods.castList(StudentSubmitSolution.class, q.getResultList());
		} catch (Exception e) {
			logger.error("getIncorrectSolutionsByStudentAndTaskInTestInstance failed {}", e);
			return null;
		}
	}

	@Override
	public Long getNumPersonsSuccessfulForTaskInTestInstance(long taskInTestInstanceId) {
		try {
			return (Long) getEntityManager().createQuery("""
					select count(distinct sss.studentStartedTest.student.studentId)
					from StudentSubmitSolution sss
					where
						sss.evaluationSimple=true and sss.evaluationComplex=true and
						sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId
					""").setParameter("taskInTestInstanceId", taskInTestInstanceId).getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Long getNumPersonsTriedToSolveTaskInTestInstance(long taskInTestInstanceId) {
		try {
			return (Long) getEntityManager().createQuery("""
					select count(distinct sss.studentStartedTest.student.studentId)
					from StudentSubmitSolution sss
					where sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId
					""").setParameter("taskInTestInstanceId", taskInTestInstanceId).getSingleResult();
		} catch (Exception e) {
			return Long.valueOf(0);
		}
	}

	@Override
	public List<StudentSubmitSolution> getSolutionsByStudent(long studentId) {
		javax.persistence.Query q = getEntityManager().createQuery(
				"from StudentSubmitSolution sss where sss.studentStartedTest.student.studentId = :studentId");
		q.setParameter("studentId", studentId);
		return UsefulMethods.castList(StudentSubmitSolution.class, q.getResultList());
	}

	@Override
	public List<StudentSubmitSolution> getSolutionsOfTaskInTestInstanceByOtherStudents(long taskInTestInstanceId,
			long studentId) {
		javax.persistence.Query q = getEntityManager().createQuery("""
				from StudentSubmitSolution sss
				where sss.evaluationSimple=true and
					sss.evaluationComplex=true and
					sss.studentStartedTest.student.studentId!=:studentId  and
					sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId
				order by sss.submittedOn desc
				""");
		q.setParameter("studentId", studentId);
		q.setParameter("taskInTestInstanceId", taskInTestInstanceId);
		return UsefulMethods.castList(StudentSubmitSolution.class, q.getResultList());
	}

	@Override
	public List<Object[]> getStudentsSolving() {
		try {
			Query query = getEntityManager().createQuery("""
					select p, count(distinct sss.taskInTestInstance.taskInTestInstanceId)
					from StudentSubmitSolution sss
					join sss.studentStartedTest sst
					join sst.student s join s.person p
					where
						sss.evaluationExam=true and sss.submittedOn>:datum
					group by p
					order by count(distinct sss.taskInTestInstance.taskInTestInstanceId) desc
					""");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -3);
			Date datum = cal.getTime();
			query.setParameter("datum", datum);
			return query.getResultList();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<Student> getStudentsWhoStartedTestInstance(TestInstance testInstance) {
		try {
			return UsefulMethods.castList(Student.class, getEntityManager().createQuery("""
					select s
					from StudentStartedTest sst join sst.student s
					where sst.testInstance.testInstanceId = :testInstanceId
					order by s.person.lastName, s.person.firstName
					""").setParameter("testInstanceId", testInstance.getTestInstanceId()).getResultList());
		} catch (Exception e) {
			logger.error("Error {}", e.getMessage());
			return new ArrayList<Student>();
		}
	}

	@Override
	public List<TaskInTestInstance> getTaskInTestInstancesByModel(long modelId) {
		List<TaskInTestInstance> tasks = UsefulMethods.castList(TaskInTestInstance.class,
				getEntityManager()
						.createQuery(
								"""
										from TaskInTestInstance tti
										where
											(tti.task.model.modelId is not null and tti.task.model.modelId=:modelId) or
											(tti.testInstance.testTemplate.model.modelId is not null and tti.testInstance.testTemplate.model.modelId=:modelId)
										order by tti.testInstance.title, tti.task.title
										""")
						.setParameter("modelId", modelId).getResultList());
		return tasks;
	}

	@Override
	public List<TaskInTestInstance> getTaskInTestInstancesByTaskType(TaskType taskType) {
		List<TaskInTestInstance> tasks = UsefulMethods.castList(TaskInTestInstance.class,
				getEntityManager().createQuery("""
						select tti
						from TaskInTestInstance tti
							join tti.task.taskIsOfTypes tiot
						where
							tiot.taskType.taskTypeId=:taskTypeId
						order by tti.testInstance.title, tti.task.title
						""").setParameter("taskTypeId", taskType.getTaskTypeId()).getResultList());
		return tasks;
	}

	@Override
	public List<TaskInTestInstance> getTaskInTestInstancesByTestInstance(long testInstanceId) {
		return UsefulMethods.castList(TaskInTestInstance.class, getEntityManager().createQuery("""
				from TaskInTestInstance tti
				where tti.testInstance.testInstanceId = :testInstanceId
				order by tti.task.title
				""").setParameter("testInstanceId", testInstanceId).getResultList());
	}

	@Override
	public List<Task> getTasksByModel(long modelId) {
		List<Task> tasks = UsefulMethods.castList(Task.class,
				getEntityManager().createQuery("from Task where model.modelId=:modelId")
						.setParameter("modelId", modelId).getResultList());
		return tasks;
	}

	@Override
	public List<TestCollection> getTestCollectionsWithTestInstances() {
		try {
			// TestCollections that have TestInstances
			List<TestCollection> list = UsefulMethods.castList(TestCollection.class, getEntityManager().createQuery("""
					select distinct ti.testCollection
					from TestInstance ti
					""").getResultList());
			// Add to them TestCollectionts that are Parents to another, or that have
			// subCollections
			List<TestCollection> listAdded = UsefulMethods.castList(TestCollection.class,
					getEntityManager().createQuery("""
							select distinct tc.parentCollection
							from TestCollection tc
							""").getResultList());
			list.addAll(listAdded);
			return list;
		} catch (Exception e) {
			logger.error("Error {}", e.getMessage());
			return null;
		}
	}

	@Override
	public List<Object[]> getTestInstanceResultsByStudentSortedByTaskName(Student student, TestInstance testInstance) {
		try {
			return getEntityManager()
					.createNativeQuery(
							"""
									select
									cast(max(sa.grade) as integer) max_grade,
									max(case when sss.student_submit_solution_id is null then null when sss.evaluation_simple and sss.evaluation_complex
									then 1 else 0 end)>0 should_pass,
									max(case when sa.passed is not null and sa.passed then 1 when sa.passed is not null and not(sa.passed) then 0 else null end )>0 passed
									from
									task_in_test_instance titi
									join task t on titi.task_id=t.task_id
									left join student_started_test sst on sst.student_id=?
									left join student_submit_solution sss on sss.task_in_test_instance_id=titi.task_in_test_instance_id and sss.student_started_test_id=sst.student_started_test_id
									left join solution_assessment sa on sss.student_submit_solution_id=sa.student_submit_solution_id
									where titi.test_instance_id=?
									group by titi.task_in_test_instance_id, t.task_id, t.title, sst.student_id
									order by t.title
																""")
					.setParameter(1, student.getStudentId()).setParameter(2, testInstance.getTestInstanceId())
					.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	@Override
	public List<TestInstance> getTestInstancesByTestType(long testTypeId) {
		javax.persistence.Query query = getEntityManager().createQuery(
				"""
				from TestInstance t 
				where t.testTemplate.testType.testTypeId = :testTypeId 
				order by t.title desc
				""")
				.setParameter("testTypeId", testTypeId);
		return UsefulMethods.castList(TestInstance.class, query.getResultList());
	}

	@Override
	public List<TestInstance> getTestInstancesForStudent(long studentId) {
		return UsefulMethods.castList(TestInstance.class,
				getEntityManager()
						.createQuery("from TestInstance t where t.openToAllStudents=true order by t.title desc")
						.getResultList());
	}

	@Override
	public List<TestInstance> getTestInstancesForStudentByTestType(long studentId, long testTypeId) {
		String query = """
				from TestInstance t
				where t.testTemplate.testType.testTypeId = :testTypeId and (
					t.openToAllStudents=true or
					t in (
						select ti from GroupMember gm
						join gm.student s
						join s.person p
						join gm.group g
						join g.groupFocusOnTests gft
						join gft.testInstance ti
						where
							s.studentId=:studentId and
							now() between ti.scheduledFor and ti.scheduledUntil
					)
				)
				order by t.title desc
				""";
		return UsefulMethods.castList(TestInstance.class, getEntityManager().createQuery(query)
				.setParameter("testTypeId", testTypeId).setParameter("studentId", studentId).getResultList());
	}

	@Override
	public List<TestInstance> getTestInstancesForStudentByTestTypeAndCollection(long studentId, long testTypeId,
			long testCollectionId) {
		String query = """
				select ti
				from TestInstance ti
				join ti.testTemplate ttem
				join ttem.testType tt
				join ti.testCollection tc
				where
					tt.testTypeId = :testTypeId and
				    tc.testCollectionId = :testCollectionId and
					(
						ti.openToAllStudents=true or
						ti in (
							select ti2
							from GroupMember gm
							join gm.student s
							join s.person p
							join gm.group g
							join g.groupFocusOnTests gft
							join gft.testInstance ti2
							where
								s.studentId=:studentId and
								(now() between ti2.scheduledFor and ti2.scheduledUntil or
								ti2.openForReviewByStudents=true)
						)
					)
				order by ti.ordering asc, ti.title desc
				""";
		return UsefulMethods.castList(TestInstance.class,
				getEntityManager().createQuery(query).setParameter("studentId", studentId)
						.setParameter("testTypeId", testTypeId).setParameter("testCollectionId", testCollectionId)
						.getResultList());
	}

	@Override
	public Float getTotalPoints(long studentId, long testInstanceId) {
		try {
			Query q = getEntityManager().createQuery("""
					select max(sa.grade) as maksot
					from SolutionAssessment sa
					join sa.studentSubmitSolution sss
					where
						sss.taskInTestInstance.testInstance.testInstanceId = :testInstanceId and
						sss.studentStartedTest.student.studentId = :studentId
					group by sss.taskInTestInstance.taskInTestInstanceId
					""");
			q.setParameter("studentId", studentId);
			q.setParameter("testInstanceId", testInstanceId);
			Float total = (float) 0;
			for (Float sa : UsefulMethods.castList(Float.class, q.getResultList())) {
				if (sa != null) {
					total += sa;
				}
			}
			return total;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Boolean isTaskInTestInstanceSolvedByStudent(long taskInTestInstanceId, long studentId) {
		Iterator<StudentSubmitSolution> i = getEntityManager().createQuery("""
				from StudentSubmitSolution sss
				where
					sss.evaluationSimple=true and
					sss.evaluationComplex=true and
					sss.notForEvaluation=false and
					sss.taskInTestInstance.taskInTestInstanceId = :taskInTestInstanceId and
					sss.studentStartedTest.student.studentId = :studentId
				""").setParameter("taskInTestInstanceId", taskInTestInstanceId).setParameter("studentId", studentId)
				.getResultList().iterator();
		if (i.hasNext()) {
			return true;
		} else {
			i = getEntityManager().createQuery("""
					from StudentSubmitSolution sss
					where
						sss.taskInTestInstance.taskInTestInstanceId=:taskInTestInstanceId and
						sss.studentStartedTest.student.studentId=:studentId
					""").setParameter("taskInTestInstanceId", taskInTestInstanceId).setParameter("studentId", studentId)
					.getResultList().iterator();
			if (i.hasNext()) {
				return false;
			} else {
				return null;
			}
		}
	}

	@Override
	public StudentStartedTest studentStartTest(long studentId, long testInstanceId) {
		Student student = (Student) session.get(Student.class, studentId);
		TestInstance testInstance = (TestInstance) session.get(TestInstance.class, testInstanceId);
		javax.persistence.Query q = getEntityManager().createQuery("""
				from StudentStartedTest st
				where
					st.testInstance.testInstanceId=:testInstanceId and
					st.student.studentId=:studentId
				""");
		q.setParameter("testInstanceId", testInstanceId);
		q.setParameter("studentId", student.getStudentId());
		List<StudentStartedTest> studentStartedTests = UsefulMethods.castList(StudentStartedTest.class,
				q.getResultList());
		StudentStartedTest studentStartedTest;
		if (studentStartedTests.isEmpty()) {
			studentStartedTest = new StudentStartedTest();
			studentStartedTest.setTestInstance(testInstance);
			studentStartedTest.setStudent(student);
			session.save(studentStartedTest);
		} else {
			studentStartedTest = studentStartedTests.get(0);
		}
		return studentStartedTest;
	}

}
