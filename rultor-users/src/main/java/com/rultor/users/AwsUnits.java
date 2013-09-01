/**
 * Copyright (c) 2009-2013, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.users;

import com.jcabi.aspects.Cacheable;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.aspects.Tv;
import com.jcabi.dynamo.Attributes;
import com.jcabi.dynamo.Item;
import com.jcabi.dynamo.QueryValve;
import com.jcabi.dynamo.Region;
import com.jcabi.urn.URN;
import com.rultor.aws.SQSClient;
import com.rultor.spi.Spec;
import com.rultor.spi.Rule;
import com.rultor.spi.Rules;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Rules in DynamoDB.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
@Immutable
@ToString
@EqualsAndHashCode(of = { "region", "owner" })
@Loggable(Loggable.DEBUG)
final class AwsUnits implements Rules {

    /**
     * Dynamo.
     */
    private final transient Region region;

    /**
     * SQS client.
     */
    private final transient SQSClient client;

    /**
     * URN of the user.
     */
    private final transient URN owner;

    /**
     * Public ctor.
     * @param reg Region in Dynamo
     * @param sqs SQS client
     * @param urn URN of the user
     */
    protected AwsUnits(final Region reg, final SQSClient sqs, final URN urn) {
        this.region = reg;
        this.client = sqs;
        this.owner = urn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull(message = "list of units of a user is never NULL")
    public Iterator<Rule> iterator() {
        final Iterator<Item> items = this.fetch().iterator();
        return new Iterator<Rule>() {
            @Override
            public boolean hasNext() {
                return items.hasNext();
            }
            @Override
            public Rule next() {
                return new AwsUnit(AwsUnits.this.client, items.next());
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable.FlushAfter
    public void create(
        @NotNull(message = "unit name is mandatory when creating new unit")
        @Pattern(
            regexp = "[a-z][-a-z0-9]{2,}",
            message = "Only numbers, letters, and dashes are allowed"
        )
        final String unt) {
        if (this.contains(unt)) {
            throw new IllegalArgumentException(
                String.format("Unit `%s` already exists", unt)
            );
        }
        this.region.table(AwsUnit.TABLE).put(
            new Attributes()
                .with(AwsUnit.HASH_OWNER, this.owner.toString())
                .with(AwsUnit.RANGE_NAME, unt)
                .with(AwsUnit.FIELD_SPEC, new Spec.Simple().asText())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable.FlushAfter
    public void remove(@NotNull(message = "unit name is mandatory")
        final String unit) {
        final Iterator<Item> items = this.region.table(AwsUnit.TABLE).frame()
            .where(AwsUnit.HASH_OWNER, this.owner.toString())
            .where(AwsUnit.RANGE_NAME, unit)
            .through(new QueryValve())
            .iterator();
        if (!items.hasNext()) {
            throw new NoSuchElementException(
                String.format("Unit `%s` not found", unit)
            );
        }
        items.next();
        items.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(lifetime = Tv.FIVE, unit = TimeUnit.MINUTES)
    public Rule get(@NotNull(message = "unit name can't be NULL")
        final String unit) {
        final Collection<Item> items = this.region.table(AwsUnit.TABLE)
            .frame()
            .where(AwsUnit.HASH_OWNER, this.owner.toString())
            .where(AwsUnit.RANGE_NAME, unit)
            .through(new QueryValve());
        if (items.isEmpty()) {
            throw new NoSuchElementException(
                String.format("Unit `%s` doesn't exist", unit)
            );
        }
        return new AwsUnit(this.client, items.iterator().next());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(lifetime = Tv.FIVE, unit = TimeUnit.MINUTES)
    public boolean contains(final String unit) {
        return !this.region.table(AwsUnit.TABLE)
            .frame()
            .where(AwsUnit.HASH_OWNER, this.owner.toString())
            .where(AwsUnit.RANGE_NAME, unit)
            .through(new QueryValve())
            .isEmpty();
    }

    /**
     * Fetch them all.
     * @return All Rules
     */
    @Cacheable(lifetime = Tv.FIVE, unit = TimeUnit.MINUTES)
    private Collection<Item> fetch() {
        return this.region.table(AwsUnit.TABLE)
            .frame()
            .where(AwsUnit.HASH_OWNER, this.owner.toString())
            .through(new QueryValve());
    }

}
