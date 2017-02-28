/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.cassandra.core;

import static org.mockito.Mockito.*;

import lombok.Data;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cassandra.core.CqlOperations;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.UserDefinedType;
import org.springframework.data.cassandra.mapping.UserTypeResolver;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.UserType;

/**
 * Unit tests for {@link CassandraPersistentEntitySchemaCreator}.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class CassandraPersistentEntitySchemaCreatorUnitTests {

	@Mock CassandraAdminOperations adminOperations;
	@Mock CqlOperations operations;
	@Mock KeyspaceMetadata metadata;
	@Mock UserType universetype;
	@Mock UserType moontype;

	BasicCassandraMappingContext context = new BasicCassandraMappingContext();

	@Before
	public void setUp() throws Exception {

		context.setUserTypeResolver(new UserTypeResolver() {
			@Override
			public UserType resolveType(CqlIdentifier typeName) {
				return metadata.getUserType(typeName.toCql());
			}
		});

		when(adminOperations.getCqlOperations()).thenReturn(operations);
	}

	@Test
	public void createsCorrectTypeForSimpleTypes(){

		context.getPersistentEntity(MoonType.class);

		CassandraPersistentEntitySchemaCreator schemaCreator =
				new CassandraPersistentEntitySchemaCreator(context, adminOperations);

		schemaCreator.createUserTypes(false);

		verifyTypesGetCreatedInOrderFor(
				"universetype",
				"moontype"
		);
	}

	@Test
	public void createsCorrectTypeForSets(){

		context.getPersistentEntity(PlanetType.class);



		CassandraPersistentEntitySchemaCreator schemaCreator = new CassandraPersistentEntitySchemaCreator(context,
				adminOperations);

		schemaCreator.createUserTypes(false);

		verify(operations).execute(matches("CREATE TYPE planettype .* set<.*moontype>.*"));

		verifyTypesGetCreatedInOrderFor(
				"universetype",
				"moontype",
				"planettype"
		);
	}

	@Test
	public void createsCorrectTypeForLists(){
		context.getPersistentEntity(SpaceAgencyType.class);

		CassandraPersistentEntitySchemaCreator schemaCreator =
				new CassandraPersistentEntitySchemaCreator(context, adminOperations);

		schemaCreator.createUserTypes(false);

		verify(operations).execute(matches("CREATE TYPE spaceagencytype .* list<.*astronauttype>.*"));

		verifyTypesGetCreatedInOrderFor(
				"astronauttype",
				"spaceagencytype"
		);

	}

	@Test
	public void createsCorrectTypesForNestedTypes(){

		context.getPersistentEntity(PlanetType.class);

		CassandraPersistentEntitySchemaCreator schemaCreator =
				new CassandraPersistentEntitySchemaCreator(context, adminOperations);

		schemaCreator.createUserTypes(false);

		verifyTypesGetCreatedInOrderFor(
				"universetype",
				"moontype",
				"planettype"
		);
	}

	private void verifyTypesGetCreatedInOrderFor(String ... typenames) {

		InOrder inOrder = Mockito.inOrder(operations);
		for (String typename : typenames) {
			inOrder.verify(operations).execute(Mockito.contains("CREATE TYPE " + typename));
		}
	}

	@UserDefinedType
	static class UniverseType {
		String name;
	}

	@UserDefinedType
	static class MoonType {

		UniverseType universeType;
	}

	@UserDefinedType
	static class PlanetType {

		Set<MoonType> moons;
		UniverseType universeType;
	}

	@UserDefinedType
	static class AstronautType {
		String name;
	}

	@UserDefinedType
	static class SpaceAgencyType {
		List<AstronautType> astronauts;
	}
}