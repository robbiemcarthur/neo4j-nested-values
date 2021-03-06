/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bolt.v1.runtime;

import java.time.Clock;

import org.neo4j.bolt.BoltChannel;
import org.neo4j.bolt.runtime.BoltConnectionFatality;
import org.neo4j.bolt.runtime.BoltStateMachine;
import org.neo4j.bolt.runtime.BoltStateMachineSPI;
import org.neo4j.bolt.runtime.TransactionStateMachineSPI;
import org.neo4j.bolt.security.auth.AuthenticationException;
import org.neo4j.bolt.v1.messaging.DiscardAll;
import org.neo4j.bolt.v1.messaging.Init;
import org.neo4j.bolt.v1.messaging.Run;
import org.neo4j.kernel.api.security.AuthToken;
import org.neo4j.values.storable.MapValue;
import org.neo4j.values.storable.Values;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo4j.bolt.testing.BoltMatchers.hasTransaction;
import static org.neo4j.bolt.testing.NullResponseHandler.nullResponseHandler;

/**
 * Helpers for testing the {@link BoltStateMachine}.
 */
public class MachineRoom
{
    static final MapValue EMPTY_PARAMS = Values.EMPTY_MAP;
    static final String USER_AGENT = "BoltStateMachineTest/0.0";

    private MachineRoom()
    {
    }

    public static BoltStateMachine newMachine()
    {
        return newMachine( mock( BoltStateMachineV1SPI.class, RETURNS_MOCKS ) );
    }

    public static BoltStateMachine newMachine( BoltStateMachineV1SPI spi )
    {
        BoltChannel boltChannel = mock( BoltChannel.class );
        return new BoltStateMachineV1( spi, boltChannel, Clock.systemUTC() );
    }

    public static BoltStateMachine newMachineWithTransaction() throws AuthenticationException, BoltConnectionFatality
    {
        BoltStateMachine machine = newMachine();
        init( machine );
        runBegin( machine );
        return machine;
    }

    public static BoltStateMachine newMachineWithTransactionSPI( TransactionStateMachineSPI transactionSPI ) throws
            AuthenticationException, BoltConnectionFatality
    {
        BoltStateMachineSPI spi = mock( BoltStateMachineSPI.class, RETURNS_MOCKS );
        when( spi.transactionSpi() ).thenReturn( transactionSPI );

        BoltChannel boltChannel = mock( BoltChannel.class );
        BoltStateMachine machine = new BoltStateMachineV1( spi, boltChannel, Clock.systemUTC() );
        init( machine );
        return machine;
    }

    public static BoltStateMachine init( BoltStateMachine machine ) throws AuthenticationException, BoltConnectionFatality
    {
        return init( machine, null );
    }

    private static BoltStateMachine init( BoltStateMachine machine, String owner ) throws AuthenticationException, BoltConnectionFatality
    {
        machine.process( new Init( USER_AGENT, owner == null ? emptyMap() : singletonMap( AuthToken.PRINCIPAL, owner ) ), nullResponseHandler() );
        return machine;
    }

    private static void runBegin( BoltStateMachine machine ) throws BoltConnectionFatality
    {
        machine.process( new Run( "BEGIN", EMPTY_PARAMS ), nullResponseHandler() );
        machine.process( DiscardAll.INSTANCE, nullResponseHandler() );
        assertThat( machine, hasTransaction() );
    }

}
