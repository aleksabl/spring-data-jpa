/*
 * Copyright 2008-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.jpa.domain.Specifications.*;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base integration test class for {@code UserRepository}. Loads a basic (non-namespace) Spring configuration file as
 * well as Hibernate configuration to execute tests.
 * <p>
 * To test further persistence providers subclass this class and provide a custom provider configuration.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context.xml" })
@Transactional
public class UserRepositoryTests {

	@PersistenceContext
	EntityManager em;

	// CUT
	@Autowired
	UserRepository repository;

	// Test fixture
	User firstUser, secondUser, thirdUser;
	Integer id;

	@Before
	public void setUp() {

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		thirdUser = new User("Dave", "Matthews", "no@email.com");
	}

	/**
	 * Tests creation of users.
	 */
	@Test
	public void testCreation() {

		Query countQuery = em.createQuery("select count(u) from User u");
		Long before = (Long) countQuery.getSingleResult();

		flushTestUsers();

		assertEquals(before + 3, countQuery.getSingleResult());
	}

	/**
	 * Tests reading a single user.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRead() throws Exception {

		flushTestUsers();

		User foundPerson = repository.findOne(id);
		assertEquals(firstUser.getFirstname(), foundPerson.getFirstname());
	}

	/**
	 * Asserts, that a call to {@code UserRepository#readId(Integer)} returns {@code null} for invalid not {@code null}
	 * ids.
	 */
	@Test
	public void testReadByIdReturnsNullForNotFoundEntities() {

		flushTestUsers();

		assertNull(repository.findOne(id * 27));
	}

	@Test
	public void savesCollectionCorrectly() throws Exception {

		List<User> result = repository.save(Arrays.asList(firstUser, secondUser, thirdUser));
		assertNotNull(result);
		assertThat(result.size(), is(3));
		assertThat(result, hasItems(firstUser, secondUser, thirdUser));
	}

	@Test
	public void savingNullCollectionIsNoOp() throws Exception {

		List<User> result = repository.save((Collection<User>) null);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void savingEmptyCollectionIsNoOp() throws Exception {

		List<User> result = repository.save(new ArrayList<User>());
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	/**
	 * Tests updating a user.
	 */
	@Test
	public void testUpdate() {

		flushTestUsers();

		User foundPerson = repository.findOne(id);
		foundPerson.setLastname("Schlicht");

		User updatedPerson = repository.findOne(id);
		assertEquals(foundPerson.getFirstname(), updatedPerson.getFirstname());
	}

	@Test
	public void existReturnsWhetherAnEntityCanBeLoaded() throws Exception {

		flushTestUsers();
		assertTrue(repository.exists(id));
		assertFalse(repository.exists(id * 27));
	}

	@Test
	public void deletesAUserById() {

		flushTestUsers();

		repository.delete(firstUser.getId());
		assertNull(repository.findOne(firstUser.getId()));
	}

	/**
	 * Tests deleting a user.
	 */
	@Test
	public void testDelete() {

		flushTestUsers();

		repository.delete(firstUser);
		assertNull(repository.findOne(id));
	}

	@Test
	public void returnsAllSortedCorrectly() throws Exception {

		flushTestUsers();
		List<User> result = repository.findAll(new Sort(ASC, "lastname"));
		assertNotNull(result);
		assertThat(result.size(), is(3));
		assertThat(result.get(0), is(secondUser));
		assertThat(result.get(1), is(firstUser));
		assertThat(result.get(2), is(thirdUser));
	}

	@Test
	public void deleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.delete(Arrays.asList(firstUser, secondUser));
		assertThat(repository.count(), is(before - 2));
	}

	@Test
	public void batchDeleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteInBatch(Arrays.asList(firstUser, secondUser));
		assertThat(repository.count(), is(before - 2));
	}

	@Test
	public void deleteEmptyCollectionDoesNotDeleteAnything() {

		assertDeleteCallDoesNotDeleteAnything(new ArrayList<User>());
	}

	@Test
	public void deleteWithNullDoesNotDeleteAnything() throws Exception {

		assertDeleteCallDoesNotDeleteAnything(null);
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<User> collection) {

		flushTestUsers();
		long count = repository.count();

		repository.delete(collection);
		assertEquals(count, repository.count());
	}

	@Test
	public void executesManipulatingQuery() throws Exception {

		flushTestUsers();
		repository.renameAllUsersTo("newLastname");

		long expected = repository.count();
		assertThat(repository.findByLastname("newLastname").size(), is(Long.valueOf(expected).intValue()));
	}

	/**
	 * Make sure no {@link NullPointerException} is being thrown.
	 * 
	 * @see Ticket #110
	 */
	@Test
	public void testFinderInvocationWithNullParameter() {

		flushTestUsers();

		repository.findByLastname(null);
	}

	/**
	 * Tests, that searching by the lastname of the reference user returns exactly that instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFindByLastname() throws Exception {

		flushTestUsers();

		List<User> byName = repository.findByLastname("Gierke");

		assertTrue(byName.size() == 1);
		assertEquals(firstUser, byName.get(0));
	}

	/**
	 * Tests, that searching by the email address of the reference user returns exactly that instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFindByEmailAddress() throws Exception {

		flushTestUsers();

		User byName = repository.findByEmailAddress("gierke@synyx.de");

		assertNotNull(byName);
		assertEquals(firstUser, byName);
	}

	/**
	 * Tests reading all users.
	 */
	@Test
	public void testReadAll() {

		flushTestUsers();

		List<User> reference = Arrays.asList(firstUser, secondUser);
		assertTrue(repository.findAll().containsAll(reference));
	}

	/**
	 * Tests that all users get deleted by triggering {@link UserRepository#deleteAll()}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deleteAll() throws Exception {

		flushTestUsers();

		repository.deleteAll();

		assertEquals(0L, repository.count());
	}

	/**
	 * Tests cascading persistence.
	 */
	@Test
	public void testCascadesPersisting() {

		// Create link prior to persisting
		firstUser.addColleague(secondUser);

		// Persist
		flushTestUsers();

		// Fetches first user from database
		User firstReferenceUser = repository.findOne(firstUser.getId());
		assertEquals(firstUser, firstReferenceUser);

		// Fetch colleagues and assert link
		Set<User> colleagues = firstReferenceUser.getColleagues();
		assertEquals(1, colleagues.size());
		assertTrue(colleagues.contains(secondUser));
	}

	/**
	 * Tests, that persisting a relationsship without cascade attributes throws a {@code DataAccessException}.
	 */
	@Test(expected = DataAccessException.class)
	public void testPreventsCascadingRolePersisting() {

		firstUser.addRole(new Role("USER"));

		flushTestUsers();
	}

	/**
	 * Tests cascading on {@literal merge} operation.
	 */
	@Test
	public void testMergingCascadesCollegueas() {

		firstUser.addColleague(secondUser);
		flushTestUsers();

		firstUser.addColleague(new User("Florian", "Hopf", "hopf@synyx.de"));
		firstUser = repository.save(firstUser);

		User reference = repository.findOne(firstUser.getId());
		Set<User> colleagues = reference.getColleagues();

		assertNotNull(colleagues);
		assertEquals(2, colleagues.size());
	}

	/**
	 * Tests, that the generic repository implements count correctly.
	 */
	@Test
	public void testCountsCorrectly() {

		long count = repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		repository.save(user);

		assertTrue(repository.count() == count + 1);
	}

	/**
	 * Tests invoking a method of a custom implementation of the repository interface.
	 */
	@Test
	public void testInvocationOfCustomImplementation() {

		repository.someCustomMethod(new User());
	}

	/**
	 * Tests that overriding a finder method is recognized by the repository implementation. If an overriding method is
	 * found it will will be invoked instead of the automatically generated finder.
	 */
	@Test
	public void testOverwritingFinder() {

		repository.findByOverrridingMethod();
	}

	@Test
	public void testUsesQueryAnnotation() {

		assertEquals(null, repository.findByAnnotatedQuery("gierke@synyx.de"));
	}

	@Test
	public void testExecutionOfProjectingMethod() {

		flushTestUsers();
		assertEquals(1, repository.countWithFirstname("Oliver").longValue());
	}

	@Test
	public void executesSpecificationCorrectly() {

		flushTestUsers();
		assertThat(repository.findAll(where(userHasFirstname("Oliver"))).size(), is(1));
	}

	@Test
	public void executesSingleEntitySpecificationCorrectly() throws Exception {

		flushTestUsers();
		assertThat(repository.findOne(userHasFirstname("Oliver")), is(firstUser));
	}

	@Test
	public void returnsNullIfNoEntityFoundForSingleEntitySpecification() throws Exception {

		flushTestUsers();
		assertThat(repository.findOne(userHasLastname("Beauford")), is(nullValue()));
	}

	@Test(expected = IncorrectResultSizeDataAccessException.class)
	public void throwsExceptionForUnderSpecifiedSingleEntitySpecification() {

		flushTestUsers();
		repository.findOne(userHasFirstnameLike("e"));
	}

	@Test
	public void executesCombinedSpecificationsCorrectly() {

		flushTestUsers();
		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Arrasz"));
		assertThat(repository.findAll(spec).size(), is(2));
	}

	@Test
	public void executesCombinedSpecificationsWithPageableCorrectly() {

		flushTestUsers();
		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Arrasz"));

		Page<User> users = repository.findAll(spec, new PageRequest(0, 1));
		assertThat(users.getSize(), is(1));
		assertThat(users.hasPreviousPage(), is(false));
		assertThat(users.getTotalElements(), is(2L));
	}

	/**
	 * Flushes test users to the database.
	 */
	private void flushTestUsers() {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);

		repository.flush();

		id = firstUser.getId();

		assertThat(id, is(notNullValue()));
		assertThat(secondUser.getId(), is(notNullValue()));
		assertThat(thirdUser.getId(), is(notNullValue()));

		assertThat(repository.exists(id), is(true));
		assertThat(repository.exists(secondUser.getId()), is(true));
		assertThat(repository.exists(thirdUser.getId()), is(true));
	}

	@Test
	public void executesMethodWithAnnotatedNamedParametersCorrectly() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertTrue(repository.findByLastnameOrFirstname("Oliver", "Arrasz").containsAll(
				Arrays.asList(firstUser, secondUser)));
	}

	@Test
	@Ignore
	public void executesMethodWithNamedParametersCorrectly() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertThat(repository.findByLastnameOrFirstnameUnannotated("Oliver", "Arrasz"), hasItems(firstUser, secondUser));
	}

	@Test
	public void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		List<User> result = repository.findByFirstnameOrLastname("Oliver", "Arrasz");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	@Test
	public void executesLikeAndOrderByCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameLikeOrderByFirstnameDesc("%r%");
		assertThat(result.size(), is(2));
		assertEquals(firstUser, result.get(0));
		assertEquals(secondUser, result.get(1));
	}

	@Test
	public void executesNotLikeCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameNotLike("%er%");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	@Test
	public void executesSimpleNotCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameNot("Gierke");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	@Test
	public void returnsSameListIfNoSpecGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll(), repository.findAll((Specification<User>) null));
	}

	@Test
	public void returnsSameListIfNoSortIsGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll((Sort) null), repository.findAll());
	}

	@Test
	public void returnsSamePageIfNoSpecGiven() throws Exception {

		Pageable pageable = new PageRequest(0, 1);

		flushTestUsers();
		assertEquals(repository.findAll(pageable), repository.findAll(null, pageable));
	}

	@Test
	public void returnsAllAsPageIfNoPageableIsGiven() throws Exception {

		flushTestUsers();
		assertEquals(new PageImpl<User>(repository.findAll()), repository.findAll((Pageable) null));
	}

	private static <T> void assertSameElements(Collection<T> first, Collection<T> second) {

		for (T element : first) {
			assertThat(element, isIn(second));
		}

		for (T element : second) {
			assertThat(element, isIn(first));
		}
	}

	@Test
	public void removeDetachedObject() throws Exception {

		flushTestUsers();

		em.detach(firstUser);
		repository.delete(firstUser);

		assertThat(repository.count(), is(2L));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executesPagedSpecificationsCorrectly() throws Exception {

		Page<User> result = executeSpecWithSort(null);
		assertThat(result.getContent(), anyOf(hasItem(firstUser), hasItem(thirdUser)));
		assertThat(result.getContent(), not(hasItem(secondUser)));
	}

	@Test
	public void executesPagedSpecificationsWithSortCorrectly() throws Exception {

		Page<User> result = executeSpecWithSort(new Sort(Direction.ASC, "lastname"));

		assertThat(result.getContent(), hasItem(firstUser));
		assertThat(result.getContent(), not(hasItem(secondUser)));
		assertThat(result.getContent(), not(hasItem(thirdUser)));
	}

	@Test
	public void executesPagedSpecificationWithSortCorrectly2() throws Exception {

		Page<User> result = executeSpecWithSort(new Sort(Direction.DESC, "lastname"));

		assertThat(result.getContent(), hasItem(thirdUser));
		assertThat(result.getContent(), not(hasItem(secondUser)));
		assertThat(result.getContent(), not(hasItem(firstUser)));
	}

	@Test
	public void executesQueryMethodWithDeepTraversalCorrectly() throws Exception {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.save(Arrays.asList(firstUser, thirdUser));

		List<User> result = repository.findByManagerLastname("Arrasz");

		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));

		result = repository.findByManagerLastname("Gierke");
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(thirdUser));
	}

	@Test
	public void executesFindByColleaguesLastnameCorrectly() throws Exception {

		flushTestUsers();

		firstUser.addColleague(secondUser);
		thirdUser.addColleague(firstUser);
		repository.save(Arrays.asList(firstUser, thirdUser));

		List<User> result = repository.findByColleaguesLastname(secondUser.getLastname());

		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));

		result = repository.findByColleaguesLastname("Gierke");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(thirdUser, secondUser));
	}

	@Test
	public void executesFindByNotNullLastnameCorrectly() throws Exception {

		flushTestUsers();
		List<User> result = repository.findByLastnameNotNull();

		assertThat(result.size(), is(3));
		assertThat(result, hasItems(firstUser, secondUser, thirdUser));
	}

	@Test
	public void executesFindByNullLastnameCorrectly() throws Exception {

		flushTestUsers();
		User forthUser = repository.save(new User("Foo", null, "email@address.com"));

		List<User> result = repository.findByLastnameNull();

		assertThat(result.size(), is(1));
		assertThat(result, hasItems(forthUser));
	}

	@Test
	public void findsSortedByLastname() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByEmailAddressLike("%@%", new Sort(Direction.ASC, "lastname"));

		assertThat(result.size(), is(3));
		assertThat(result.get(0), is(secondUser));
		assertThat(result.get(1), is(firstUser));
		assertThat(result.get(2), is(thirdUser));
	}

	@Test
	public void findsUsersBySpringDataNamedQuery() {

		flushTestUsers();

		List<User> result = repository.findBySpringDataNamedQuery("Gierke");
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));
	}

	/**
	 * @see DATADOC-86
	 */
	@Test
	public void readsPageWithGroupByClauseCorrectly() {

		flushTestUsers();

		Page<String> result = repository.findByLastnameGrouped(new PageRequest(0, 10));
		assertThat(result.getTotalPages(), is(1));
	}

	private Page<User> executeSpecWithSort(Sort sort) {

		flushTestUsers();

		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Matthews"));

		Page<User> result = repository.findAll(spec, new PageRequest(0, 1, sort));
		assertThat(result.getTotalElements(), is(2L));
		return result;
	}
}
