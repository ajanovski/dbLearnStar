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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.commons.Configuration;
import org.apache.tapestry5.commons.MappedConfiguration;
import org.apache.tapestry5.commons.OrderedConfiguration;
import org.apache.tapestry5.hibernate.HibernateEntityPackageManager;
import org.apache.tapestry5.hibernate.HibernateTransactionAdvisor;
import org.apache.tapestry5.http.services.RequestGlobals;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Decorate;
import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.services.ThreadLocale;
import org.apache.tapestry5.modules.Bootstrap4Module;
import org.apache.tapestry5.services.ApplicationStateContribution;
import org.apache.tapestry5.services.ApplicationStateCreator;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.ComponentSource;
import org.apache.tapestry5.services.ExceptionReporter;
import org.apache.tapestry5.services.PersistentLocale;
import org.hibernate.Session;
import org.slf4j.Logger;

import dblearnstar.model.entities.Person;
import dblearnstar.model.entities.PersonRole;
import dblearnstar.model.entities.Student;
import dblearnstar.model.model.UserInfo;
import dblearnstar.model.model.UserInfo.UserRole;
import dblearnstar.webapp.model.ApplicationConstants;
import dblearnstar.webapp.util.AppConfig;

@ImportModule(Bootstrap4Module.class)
public class AppModule {
	public static void bind(ServiceBinder binder) {
		binder.bind(AccessControllerRequestFilter.class);
		binder.bind(DigestService.class);
		binder.bind(GenericDao.class);
		binder.bind(GenericService.class);
		binder.bind(GroupManager.class);
		binder.bind(PersonDao.class);
		binder.bind(PersonManager.class);
		binder.bind(ActivityManager.class);
		binder.bind(ActivityDao.class);
		binder.bind(EvaluationService.class);
		binder.bind(TestManager.class);
		binder.bind(TranslationService.class);
		binder.bind(SystemConfigService.class);
	}

	public static void contributeFactoryDefaults(MappedConfiguration<String, Object> configuration) {
		configuration.override(SymbolConstants.APPLICATION_VERSION, AppConfig.getString("app.version"));
		configuration.override(SymbolConstants.PRODUCTION_MODE, false);
	}

	public static void contributeApplicationDefaults(MappedConfiguration<String, Object> configuration) {
		configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en,mk");
		configuration.add(SymbolConstants.HMAC_PASSPHRASE,
				AppConfig.getString("tapestry.hmac-passphrase") + UUID.randomUUID());
		configuration.add(SymbolConstants.ENABLE_HTML5_SUPPORT, true);
		configuration.add(SymbolConstants.COMPRESS_WHITESPACE, false);

		configuration.add("tapestry.hibernate.early-startup", true);

		configuration.add(SymbolConstants.JAVASCRIPT_INFRASTRUCTURE_PROVIDER, "jquery");

		configuration.add(SymbolConstants.EXCEPTION_REPORTS_DIR,
				AppConfig.getString("additionalFiles.path") + AppConfig.getString("exceptionReports.path"));
	}

	@Decorate(serviceInterface = ExceptionReporter.class)
	public static ExceptionReporter preventExceptionFileWriting(final ExceptionReporter exceptionReporter) {
		return new ExceptionReporter() {
			@Override
			public void reportException(Throwable exception) {
			}
		};
	}

	@Contribute(HibernateEntityPackageManager.class)
	public static void addHibernateEntityPackageManager(Configuration<String> configuration) {
		configuration.add(Person.class.getPackageName());
	}

	@Match({ "*Service", "*Dao", "*Manager" })
	public static void adviseEnableTransactions(HibernateTransactionAdvisor advisor, MethodAdviceReceiver receiver) {
		advisor.addTransactionCommitAdvice(receiver);
	}

	@Decorate(serviceInterface = ThreadLocale.class)
	public ThreadLocale decorateThreadLocale(final ThreadLocale threadLocale, final PersistentLocale persistentLocale) {
		return new ThreadLocale() {
			@Override
			public void setLocale(Locale locale) {
				threadLocale.setLocale(locale);
			}

			@Override
			public Locale getLocale() {
				if (!persistentLocale.isSet()) {
					setLocale(new Locale("mk"));
					persistentLocale.set(new Locale("mk"));
				}
				return threadLocale.getLocale();
			}
		};
	}

	public static final void contributeComponentRequestHandler(
			OrderedConfiguration<ComponentRequestFilter> configuration,
			ComponentRequestFilter accessControllerRequestFilter, ApplicationStateManager asm,
			ComponentSource componentSource) {
		configuration.add("AccessControllerRequestFilter", accessControllerRequestFilter, "before:*");
	}

	public void contributeApplicationStateManager(
			MappedConfiguration<Class, ApplicationStateContribution> configuration, Session session,
			PersonManager personManager, RequestGlobals requestGlobals, Logger logger) {
		ApplicationStateCreator<UserInfo> userInfoCreator = new ApplicationStateCreator<UserInfo>() {
			public UserInfo create() {
				logger.debug("userInfoCreator.create entered");

				UserInfo userInfo = new UserInfo();
				userInfo.setUserRoles(null);
				userInfo.setPersonId(null);
				userInfo.setUserName(null);

				String userName = requestGlobals.getHTTPServletRequest().getRemoteUser();
				userInfo.setUserName(userName);
				logger.info("External auth login by user {}. Checking privileges.", userName);

				Person loggedInPerson = (Person) session.getSession()
						.createQuery("from Person p where userName=:userName").setParameter("userName", userName)
						.getResultStream().findFirst().orElse(null);

				if (loggedInPerson == null) {
					logger.debug("These is no Person with userName: {}", userName);
					if (AppConfig.getString(ApplicationConstants.AUTH_AUTO_CREATE_USER).equalsIgnoreCase("true")) {
						logger.debug("  Creating Person with userName: {}", userName);
						loggedInPerson = new Person();
						loggedInPerson.setUserName(userName);
						loggedInPerson.setFirstName(userName);
						loggedInPerson.setLastName(userName);
						logger.debug("  Creating Student for the Person with userName: {}", userName);
						Student s = new Student();
						s.setPerson(loggedInPerson);
						session.save(loggedInPerson);
						session.save(s);
						session.getSession().getTransaction().commit();
						logger.info("  Created user {}", userName);
						loggedInPerson = (Person) session.getSession()
								.createQuery("from Person p where userName=:userName")
								.setParameter("userName", userName).getResultStream().findFirst().orElse(null);
					}
				}

				if (loggedInPerson == null) {
					userInfo.setUserRoles(null);
					userInfo.setPersonId(null);
					logger.info("Logged in userName: {} is not found", userInfo.getUserName());
				} else {
					logger.debug("Logged in personId: {}", loggedInPerson.getPersonId());

					List<UserInfo.UserRole> userRoles = new ArrayList<UserRole>();

					if (!(loggedInPerson.getStudents().isEmpty())) {
						logger.debug("Logged in user: {} is a student", userName);
						userRoles.add(UserRole.STUDENT);
					}

					for (PersonRole pr : personManager.getPersonRolesForPerson(loggedInPerson.getPersonId())) {
						if (pr.getRole().getName().equals("ADMINISTRATOR")) {
							logger.debug("Logged in user: {} is an administrator", userName);
							userRoles.add(UserRole.ADMINISTRATOR);
						} else if (pr.getRole().getName().equals("INSTRUCTOR")) {
							logger.debug("Logged in user: {} is instructor", userName);
							userRoles.add(UserRole.INSTRUCTOR);
						}
					}

					if (userRoles.size() == 0) {
						logger.debug("Loggen in user: {} role is set to NONE", userName);
						userRoles.add(UserRole.NONE);
					}

					logger.debug("Logged in user: {} has {} roles", userName, userRoles.size());

					userInfo.setUserName(userName);
					userInfo.setPersonId(loggedInPerson.getPersonId());
					userInfo.setUserRoles(userRoles);
					logger.debug("userInfo is now initialized for logged in user: {}", userName);
				}
				return userInfo;
			}
		};
		configuration.add(UserInfo.class, new ApplicationStateContribution("session", userInfoCreator));

	}

	public static void contributeClasspathAssetAliasManager(MappedConfiguration<String, String> configuration) {
		configuration.add("webjars", "META-INF/resources/webjars");
	}

	public Logger buildLogger(final Logger logger) {
		return logger;
	}

}
