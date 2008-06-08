/*
 * OpenWMS, the open Warehouse Management System
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.openwms.domain.common.system.usermanagement;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.openwms.domain.common.AbstractPDOTestCase;

public class RoleTest extends AbstractPDOTestCase {

	@Test
	public void testRoleConstraint() {
		EntityTransaction entityTransaction = em.getTransaction();
		Role role = new Role("ROLE_TEST");
		Role role2 = new Role("ROLE_TEST");

		try {
			entityTransaction.begin();
			em.persist(role);
			em.persist(role2);
			entityTransaction.commit();
			fail("No unique constraint on rolename");
		} catch (PersistenceException pe) {
			if (pe.getCause() instanceof ConstraintViolationException) {
				LOG.debug("OK:Tested unique constraint on rolename.");
			} else {
				fail("Unknown PersistenceException");
			}
		}
	}

	@Test
	public void testRoleLifecycle() {
		EntityTransaction entityTransaction = em.getTransaction();
		Role role = new Role("ROLE_TEST");
		User unknownUser = new User("UNKNOWN_USER");
		User knownUser = new User("KNOWN_USER");

		entityTransaction.begin();
		em.persist(knownUser);
		entityTransaction.commit();

		role.addUser(knownUser);
		role.addUser(unknownUser);

		entityTransaction.begin();
		em.merge(role);
		entityTransaction.commit();

		Query query = em.createQuery("select count(u) from User u where u.username = :username");
		query.setParameter("username", "UNKNOWN_USER");
		Long cnt = (Long) query.getSingleResult();
		assertEquals("User must be persisted with Role", 1, cnt.intValue());

		query = em.createQuery("select r from Role r where r.rolename = :rolename");
		query.setParameter("rolename", "ROLE_TEST");
		role = (Role)query.getSingleResult();
		assertNotSame("Role must be persisted", 0, role.getId());

		entityTransaction.begin();
		em.remove(role);
		entityTransaction.commit();

		query = em.createQuery("select count(r) from Role r where r.rolename = :rolename");
		query.setParameter("rolename", "ROLE_TEST");
		cnt = (Long) query.getSingleResult();
		assertEquals("Role must be removed", 0, cnt.intValue());

		query = em.createQuery("select count(u) from User u where u.username = :username");
		query.setParameter("username", "KNOWN_USER");
		cnt = (Long) query.getSingleResult();
		assertEquals("User may not be removed", 1, cnt.intValue());
	}
}
