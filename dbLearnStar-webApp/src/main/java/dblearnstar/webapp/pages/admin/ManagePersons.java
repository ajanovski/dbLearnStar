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

package dblearnstar.webapp.pages.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.hibernate.annotations.CommitAfter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.SelectModelFactory;
import org.slf4j.Logger;

import dblearnstar.model.entities.Person;
import dblearnstar.model.entities.PersonRole;
import dblearnstar.model.entities.Role;
import dblearnstar.model.entities.Student;
import dblearnstar.model.model.ModelConstants;
import dblearnstar.model.model.UserInfo;
import dblearnstar.webapp.annotations.AdministratorPage;
import dblearnstar.webapp.services.GenericService;
import dblearnstar.webapp.services.PersonManager;

@AdministratorPage
public class ManagePersons {

	@Property
	@SessionState
	private UserInfo userInfo;

	@Inject
	private SelectModelFactory selectModelFactory;

	@Inject
	private PersonManager personManager;

	@Inject
	private GenericService genericService;

	@Inject
	private Logger logger;

	@InjectComponent
	private Form frmImport;

	private Boolean cancelForm = false;

	@Property
	private Person person;

	@Persist
	@Property
	private Person personToEdit;

	@Property
	@Persist
	private String personListToImport;

	@Property
	Role selectedRole;

	@Property
	@Persist
	private String searchString;

	@Persist
	@Property
	private String errors;

	public List<Person> getAllPersons() {
		List<Person> list = personManager.getAllPersons();
		if (searchString == null || searchString.equals("")) {
			return list;
		} else {
			return list.stream()
					.filter(p -> (p.getFirstName() + p.getLastName() + p.getUserName()).contains(searchString))
					.collect(Collectors.toList());
		}
	}

	public void onActionFromImportPersons() {
		personListToImport = "firstName\tlastName\tuserName";
	}

	@CommitAfter
	public void onSuccessFromFrmImport() {
		if (personListToImport != null) {
			errors = new String();
			for (String line : personListToImport.split("\\r?\\n")) {
				logger.info(">>> Importing {} <<<", line);
				String[] lineFields = line.split("[,\t]");
				Person p;
				try {
					p = personManager.getPersonByUsername(lineFields[2]);
					if (p != null) {
						errors += ">>> Person " + p.getUserName() + " already exists, skipping creation, activating.";
						if (p.getStudents().size() > 0) {
							p.setFirstName(lineFields[0]);
							p.setLastName(lineFields[1]);
							genericService.save(p);
						}
					} else {
						p = new Person();
						p.setFirstName(lineFields[0]);
						p.setLastName(lineFields[1]);
						p.setUserName(lineFields[2]);
						genericService.save(p);
						Student pr = new Student();
						pr.setPerson(p);
						genericService.save(pr);
					}
				} catch (Exception e) {
					errors += "--- Person " + lineFields[2] + " can not be imported due to: " + e.getLocalizedMessage()+"<br>";
				}
			}
			if (!(errors.length() > 0)) {
				errors = "OK";
			}
		}
	}

	@CommitAfter
	public void onActionFromDeletePerson(Person p) {
		try {
			if (p.getStudents() != null) {
				for (Student s : p.getStudents()) {
					genericService.delete(s);
				}
			}
			for (PersonRole pr : personManager.getPersonRolesForPerson(p.getPersonId())) {
				genericService.delete(pr);
			}
			genericService.delete(p);
		} catch (Exception e) {
			logger.error("Can't delete person {} due to {}", p.getUserName(), e);
		}
	}

	@CommitAfter
	public void onTogglePersonStatus(Person p) {
		if (p.getUserName().contains(ModelConstants.PersonDeactivatedSuffix)) {
			p.setUserName(p.getUserName().replace(ModelConstants.PersonDeactivatedSuffix, ""));
		} else {
			p.setUserName(p.getUserName() + ModelConstants.PersonDeactivatedSuffix);
		}
	}

	public void onActionFromNewPerson() {
		personToEdit = new Person();
	}

	public void onActionFromEditPerson(Person p) {
		personToEdit = p;
	}

	@CommitAfter
	public void saveChanges() {
		genericService.saveOrUpdate(personToEdit);
		if (selectedRole != null) {
			PersonRole pr = new PersonRole();
			pr.setRole(selectedRole);
			pr.setPerson(personToEdit);
			genericService.saveOrUpdate(pr);
		}
	}

	public void onCanceledFromFrmNewPerson() {
		cancelForm = true;
	}

	public void onValidateFromFrmNewPerson() {
		if (!cancelForm) {
			saveChanges();
		}
	}

	public void onSuccessFromFrmNewPerson() {
		personToEdit = null;
	}

	public SelectModel getSelectRoleModel() {
		return selectModelFactory.create(genericService.getAll(Role.class), "name");
	}

}
