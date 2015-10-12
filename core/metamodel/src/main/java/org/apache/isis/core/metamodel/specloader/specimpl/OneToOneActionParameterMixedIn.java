/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.core.metamodel.specloader.specimpl;

import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;

public class OneToOneActionParameterMixedIn extends OneToOneActionParameterImpl implements ObjectActionParameterMixedIn {

    private final ObjectActionParameter mixinActionParameter;
    private final ObjectActionMixedIn mixedInAction;

    public OneToOneActionParameterMixedIn(
            final ObjectActionParameterAbstract mixinActionParameter,
            final ObjectActionMixedIn mixedInAction) {
        super(mixinActionParameter.getNumber(), mixedInAction, mixinActionParameter.getPeer());
        this.mixinActionParameter = mixinActionParameter;
        this.mixedInAction = mixedInAction;
    }

    @Override
    public ObjectAdapter[] getAutoComplete(
            final ObjectAdapter mixedInAdapter,
            final String searchArg,
            final InteractionInitiatedBy interactionInitiatedBy) {
        return mixinActionParameter.getAutoComplete(
                mixinAdapterFor(mixedInAdapter), searchArg,
                interactionInitiatedBy);
    }

    protected ObjectAdapter targetForDefaultOrChoices(
            final ObjectAdapter mixedInAdapter,
            final List<ObjectAdapter> argumentsIfAvailable) {
        return mixinAdapterFor(mixedInAdapter);
    }

    private ObjectAdapter mixinAdapterFor(final ObjectAdapter mixedInAdapter) {
        return mixedInAction.mixinAdapterFor(mixedInAdapter);
    }


}
