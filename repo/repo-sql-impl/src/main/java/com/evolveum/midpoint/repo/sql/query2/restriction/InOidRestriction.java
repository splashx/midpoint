/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;

/**
 * @author lazyman
 */
public class InOidRestriction extends Restriction<InOidFilter> {

    public InOidRestriction(InterpretationContext context, InOidFilter filter, EntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        return getContext().getHibernateQuery().createIn(getBaseHqlPath() + ".oid", filter.getOids());
    }

}